package com.exposures.phone.voice

import com.exposures.datalayer.dto.CreateExposureAckCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Republishes the watch's [CreateExposureAckCommand] messages (received by
 * `WearMessageListenerService`) as a hot flow, so [VoiceCaptureRequestHandler] can await the ack
 * matching the exposureId it just sent without the two classes needing a direct reference to each
 * other.
 */
class CreateExposureAckBroadcaster {

    private val _acks = MutableSharedFlow<CreateExposureAckCommand>(replay = 0, extraBufferCapacity = 1)
    val acks: SharedFlow<CreateExposureAckCommand> = _acks

    suspend fun emit(ack: CreateExposureAckCommand) {
        _acks.emit(ack)
    }
}
