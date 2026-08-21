package com.exposures.phone.sync

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ReferencePhoto
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class UploadCoordinatorTest {

    private fun exposure(id: String = UUID.randomUUID().toString(), syncStatus: SyncStatus = SyncStatus.PENDING_SYNC) = Exposure(
        id = id,
        filmRollId = "roll-1",
        frameNumber = 1,
        lensId = "lens-1",
        focalLengthMm = null,
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = null,
        notes = null,
        capturedAt = 0L,
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = syncStatus,
        remoteId = null,
    )

    private fun referencePhoto(exposureId: String, localFile: File?) = ReferencePhoto(
        id = UUID.randomUUID().toString(),
        exposureId = exposureId,
        localUri = localFile?.toURI()?.toString(),
        remoteUrl = null,
        latitude = null,
        longitude = null,
        capturedAt = 1L,
        uploadStatus = SyncStatus.PENDING_SYNC,
        retryCount = 0,
        lastError = null,
    )

    private fun referencePhotoWithUri(exposureId: String, uri: Uri) = ReferencePhoto(
        id = UUID.randomUUID().toString(),
        exposureId = exposureId,
        localUri = uri.toString(),
        remoteUrl = null,
        latitude = null,
        longitude = null,
        capturedAt = 1L,
        uploadStatus = SyncStatus.PENDING_SYNC,
        retryCount = 0,
        lastError = null,
    )

    @Test
    fun `drainExposures uploads every dirty exposure and marks it synced with the server's remote id`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(listOf(exposure("exp-1"), exposure("exp-2")))
        val syncApi = FakeSyncApi()
        val coordinator = UploadCoordinator(repository, syncApi)

        val result = coordinator.drainExposures()

        assertEquals(2, result.succeeded)
        assertEquals(0, result.failed)
        assertEquals(setOf("exp-1", "exp-2"), syncApi.uploadedExposures.map { it.id }.toSet())
        val exp1 = repository.getExposure("exp-1")!!
        assertEquals(SyncStatus.SYNCED, exp1.syncStatus)
        assertEquals("server-exp-1", exp1.remoteId)
    }

    @Test
    fun `drainExposures leaves already-synced exposures untouched`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(listOf(exposure("exp-1", syncStatus = SyncStatus.SYNCED)))
        val syncApi = FakeSyncApi()
        val coordinator = UploadCoordinator(repository, syncApi)

        val result = coordinator.drainExposures()

        assertEquals(0, result.succeeded)
        assertTrue(syncApi.uploadedExposures.isEmpty())
    }

    @Test
    fun `drainExposures marks a failed upload SYNC_FAILED instead of retrying inline`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(listOf(exposure("exp-1")))
        val syncApi = FakeSyncApi().apply { failUploads = true }
        val coordinator = UploadCoordinator(repository, syncApi)

        val result = coordinator.drainExposures()

        assertEquals(0, result.succeeded)
        assertEquals(1, result.failed)
        assertEquals(SyncStatus.SYNC_FAILED, repository.getExposure("exp-1")!!.syncStatus)
    }

    @Test
    fun `drainReferencePhotos uploads a photo with a local file and marks it synced with the remote url`() = runTest {
        val repository = createTestRepository()
        val file = File.createTempFile("photo", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        repository.saveReferencePhoto(referencePhoto("exp-1", file))
        val syncApi = FakeSyncApi()
        val coordinator = UploadCoordinator(repository, syncApi)

        val result = coordinator.drainReferencePhotos()

        assertEquals(1, result.succeeded)
        assertEquals(listOf("exp-1"), syncApi.uploadedPhotoExposureIds)
        val stored = repository.getReferencePhoto("exp-1")!!
        assertEquals(SyncStatus.SYNCED, stored.uploadStatus)
        assertEquals("https://cdn.example/exp-1.jpg", stored.remoteUrl)
        file.delete()
    }

    @Test
    fun `drainReferencePhotos uploads a photo behind a content Uri, reading bytes via the content resolver`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://media/external/images/media/1")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        val repository = createTestRepository()
        repository.saveReferencePhoto(referencePhotoWithUri("exp-1", uri))
        val syncApi = FakeSyncApi()
        val coordinator = UploadCoordinator(repository, syncApi, context)

        val result = coordinator.drainReferencePhotos()

        assertEquals(1, result.succeeded)
        assertEquals(listOf("exp-1"), syncApi.uploadedPhotoExposureIds)
        assertEquals(SyncStatus.SYNCED, repository.getReferencePhoto("exp-1")!!.uploadStatus)
    }

    @Test
    fun `drainReferencePhotos never sees a photo with no local file`() = runTest {
        val repository = createTestRepository()
        repository.saveReferencePhoto(referencePhoto("exp-capture-failed", localFile = null).copy(uploadStatus = SyncStatus.SYNC_FAILED))
        val syncApi = FakeSyncApi()
        val coordinator = UploadCoordinator(repository, syncApi)

        val result = coordinator.drainReferencePhotos()

        assertEquals(0, result.succeeded)
        assertEquals(0, result.failed)
        assertTrue(syncApi.uploadedPhotoExposureIds.isEmpty())
    }

    @Test
    fun `drainAll sums the results of draining exposures and reference photos`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(listOf(exposure("exp-1")))
        val file = File.createTempFile("photo", ".jpg").apply { writeBytes(byteArrayOf(1)) }
        repository.saveReferencePhoto(referencePhoto("exp-1", file))
        val syncApi = FakeSyncApi()
        val coordinator = UploadCoordinator(repository, syncApi)

        val result = coordinator.drainAll()

        assertEquals(2, result.succeeded)
        assertEquals(0, result.failed)
        file.delete()
    }
}
