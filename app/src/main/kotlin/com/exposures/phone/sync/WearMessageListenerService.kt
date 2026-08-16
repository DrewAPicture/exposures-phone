package com.exposures.phone.sync

import android.content.Intent
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.phone.ExposuresApplication
import com.exposures.phone.capture.CaptureForegroundService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Manifest-registered (see AndroidManifest.xml) so the system can start this even when the app
 * isn't running. Delegates immediately to [CaptureForegroundService]/[RollCompletionHandler]/
 * [ExposureSyncReceiver] — those hold the actual logic and (where they can be) are unit tested;
 * this class is just the GMS entry point wiring, which can't be meaningfully tested outside a
 * real device/emulator pair.
 */
class WearMessageListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container get() = (application as ExposuresApplication).container

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            DataLayerPaths.CAPTURE_PHOTO_COMMAND -> handleCapturePhoto(messageEvent)
            DataLayerPaths.COMPLETE_ROLL_COMMAND -> handleCompleteRoll(messageEvent)
            DataLayerPaths.REQUEST_ROLLS_SYNC_COMMAND -> handleRequestRollsSync()
            DataLayerPaths.CONNECTIVITY_PING_COMMAND -> handleConnectivityPing()
        }
    }

    private fun handleCapturePhoto(messageEvent: MessageEvent) {
        val command = DataLayerJson.decodeCapturePhotoCommand(String(messageEvent.data))
        val intent = Intent(this, CaptureForegroundService::class.java)
            .putExtra(CaptureForegroundService.EXTRA_EXPOSURE_ID, command.exposureId)
        startForegroundService(intent)
    }

    private fun handleCompleteRoll(messageEvent: MessageEvent) {
        val command = DataLayerJson.decodeCompleteRollCommand(String(messageEvent.data))
        serviceScope.launch {
            RollCompletionHandler(container.repository, container.syncPusher).handle(command.rollId)
        }
    }

    private fun handleRequestRollsSync() {
        serviceScope.launch {
            RequestRollsSyncHandler(container.syncPusher).handle()
        }
    }

    private fun handleConnectivityPing() {
        serviceScope.launch {
            container.dataLayerClient.sendMessage(DataLayerPaths.CONNECTIVITY_PING_ACK_COMMAND, "ack")
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == DataLayerPaths.EXPOSURES) {
                    val json = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(DataLayerPaths.KEY_PAYLOAD)
                        ?: continue
                    serviceScope.launch {
                        ExposureSyncReceiver(container.repository).handlePayload(json)
                        UploadScheduler.enqueue(applicationContext)
                    }
                }
            }
        } finally {
            dataEvents.release()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
