package com.exposures.phone.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.phone.ExposuresApplication
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
 * isn't running. Delegates immediately to [CaptureCommandHandler]/[ExposureSyncReceiver] — those
 * hold the actual logic and are unit tested; this class is just the GMS entry point wiring, which
 * can't be meaningfully tested outside a real device/emulator pair.
 */
class WearMessageListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container get() = (application as ExposuresApplication).container

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != DataLayerPaths.CAPTURE_PHOTO_COMMAND) return
        val command = DataLayerJson.decodeCapturePhotoCommand(String(messageEvent.data))
        serviceScope.launch {
            CaptureCommandHandler(container.repository, container.dataLayerClient).handle(command)
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
