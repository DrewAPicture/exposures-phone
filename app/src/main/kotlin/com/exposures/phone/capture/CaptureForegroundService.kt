package com.exposures.phone.capture

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.exposures.model.PhotoStatus
import com.exposures.model.ReferencePhoto
import com.exposures.model.SyncStatus
import com.exposures.phone.ExposuresApplication
import com.exposures.phone.MainActivity
import com.exposures.phone.settings.CaptureCameraPreference
import com.exposures.phone.sync.CaptureResultPublisher
import com.exposures.phone.sync.UploadScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real capture, replacing Phase 2's immediate-CAPTURED stub. Started by [com.exposures.phone.sync.WearMessageListenerService]
 * with the exposure/roll ids as intent extras; runs no UI (no Preview, no Activity) — just the
 * mandatory foreground-service notification, per the plan. Can't be unit tested here (needs a
 * real camera), so it's kept as thin as possible: the zoom/file-naming decisions it makes are
 * pulled out into [ZoomRatio]/[CaptureFileNaming], and the result handling is [CaptureResultPublisher] —
 * both unit tested. This class is just the CameraX/Android glue between them.
 */
class CaptureForegroundService : LifecycleService() {

    private val container get() = (application as ExposuresApplication).container

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val exposureId = intent?.getStringExtra(EXTRA_EXPOSURE_ID)
        if (exposureId == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val startedForeground = runCatching { startForegroundWithNotification() }.isSuccess
        if (!startedForeground) {
            // Android 14+ can reject camera-type FGS startup when the app is backgrounded and not
            // currently eligible for while-in-use camera access. Report FAILED instead of crashing.
            lifecycleScope.launch {
                CaptureResultPublisher(container.repository, container.dataLayerClient)
                    .publish(exposureId, PhotoStatus.FAILED)
                stopSelf(startId)
            }
            return START_NOT_STICKY
        }

        lifecycleScope.launch {
            val status = runCatching { captureAndSave(exposureId) }
                .getOrElse { PhotoStatus.FAILED }
            CaptureResultPublisher(container.repository, container.dataLayerClient).publish(exposureId, status)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun captureAndSave(exposureId: String): PhotoStatus {
        val exposure = awaitExposure(exposureId) ?: return PhotoStatus.FAILED
        val lens = container.repository.getLens(exposure.lensId)

        val cameraProvider = ProcessCameraProvider.getInstance(this).await()
        val imageCapture = ImageCapture.Builder().build()
        val selector = when (container.captureCameraPreferences.preference.value) {
            CaptureCameraPreference.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CaptureCameraPreference.REAR -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        val camera = cameraProvider.bindToLifecycle(this, selector, imageCapture)

        try {
            val zoomState = camera.cameraInfo.zoomState.value
            val requestedZoom = lens?.referencePhotoZoomRatio ?: 1.0
            if (zoomState != null) {
                val zoom = ZoomRatio.clamp(requestedZoom, zoomState.minZoomRatio, zoomState.maxZoomRatio)
                camera.cameraControl.setZoomRatio(zoom).await()
            }

            val destination = createCaptureDestination(exposure.filmRollId, exposureId)
            val savedUri = takePicture(imageCapture, destination.outputOptions)
            val localUri = savedUri?.toString() ?: destination.fallbackLocalUri

            val location = lastKnownLocationOrNull()
            container.repository.saveReferencePhoto(
                ReferencePhoto(
                    id = UUID.randomUUID().toString(),
                    exposureId = exposureId,
                    localUri = localUri,
                    remoteUrl = null,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    capturedAt = System.currentTimeMillis(),
                    uploadStatus = SyncStatus.PENDING_SYNC,
                    retryCount = 0,
                    lastError = null,
                ),
            )
            UploadScheduler.enqueue(applicationContext)
            return PhotoStatus.CAPTURED
        } catch (e: ImageCaptureException) {
            container.repository.saveReferencePhoto(
                ReferencePhoto(
                    id = UUID.randomUUID().toString(),
                    exposureId = exposureId,
                    localUri = null,
                    remoteUrl = null,
                    latitude = null,
                    longitude = null,
                    capturedAt = null,
                    uploadStatus = SyncStatus.SYNC_FAILED,
                    retryCount = 0,
                    lastError = e.message,
                ),
            )
            return PhotoStatus.FAILED
        } finally {
            cameraProvider.unbindAll()
        }
    }

    /**
     * The watch can fire capture immediately after creating/pushing an exposure, so the command
     * can occasionally arrive a moment before the phone has merged that exposure into local DB.
     * Retry briefly instead of failing fast on this sync race.
     */
    private suspend fun awaitExposure(exposureId: String): com.exposures.model.Exposure? {
        repeat(EXPOSURE_LOOKUP_ATTEMPTS) { attempt ->
            container.repository.getExposure(exposureId)?.let { return it }
            if (attempt < EXPOSURE_LOOKUP_ATTEMPTS - 1) delay(EXPOSURE_LOOKUP_INTERVAL_MS)
        }
        return null
    }

    /**
     * On API 29+ (Q), saves into the shared Pictures/Exposures/&lt;filmRollId&gt; gallery folder via
     * MediaStore — visible in the system Photos app, no storage permission needed since the app
     * only ever reads/writes entries it created itself. Below Q, MediaStore's RELATIVE_PATH insert
     * API isn't available, so this falls back to app-private external storage instead (a real file,
     * not shared with the gallery) — [CaptureDestination.fallbackLocalUri] is only populated in
     * that case, since the Q+ path always gets its saved location back from [takePicture].
     */
    private fun createCaptureDestination(filmRollId: String, exposureId: String): CaptureDestination {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$exposureId.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/${CaptureFileNaming.ROOT_DIR_NAME}/$filmRollId")
            }
            return CaptureDestination(
                outputOptions = ImageCapture.OutputFileOptions.Builder(
                    contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values,
                ).build(),
                fallbackLocalUri = null,
            )
        }

        val file = CaptureFileNaming.outputFile(
            baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir,
            filmRollId = filmRollId,
            exposureId = exposureId,
        )
        file.parentFile?.mkdirs()
        return CaptureDestination(
            outputOptions = ImageCapture.OutputFileOptions.Builder(file).build(),
            fallbackLocalUri = file.toURI().toString(),
        )
    }

    private suspend fun takePicture(imageCapture: ImageCapture, outputOptions: ImageCapture.OutputFileOptions): Uri? {
        return suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(outputFileResults.savedUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
    }

    /** Best-effort only — a stale or missing fix just means the reference photo isn't geotagged. */
    private fun lastKnownLocationOrNull(): Location? {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .firstOrNull()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Reference photo capture", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification() {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Exposures")
            .setContentText("Capturing reference photo…")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val EXTRA_EXPOSURE_ID = "exposureId"
        private const val NOTIFICATION_CHANNEL_ID = "capture"
        private const val NOTIFICATION_ID = 1
        private const val EXPOSURE_LOOKUP_ATTEMPTS = 10
        private const val EXPOSURE_LOOKUP_INTERVAL_MS = 300L
    }
}

private data class CaptureDestination(
    val outputOptions: ImageCapture.OutputFileOptions,
    val fallbackLocalUri: String?,
)
