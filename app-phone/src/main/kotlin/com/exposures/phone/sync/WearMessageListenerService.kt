package com.exposures.phone.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

// Manifest-registered per the intent filters in AndroidManifest.xml; the system can start this
// even when the app isn't running. Real handling (capture-command processing, exposure-sync
// merge) lands here once CaptureCommandHandler exists — kept as a bare stub for now so the
// manifest reference resolves and the module compiles.
class WearMessageListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
    }
}
