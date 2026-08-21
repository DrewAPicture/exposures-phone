package com.exposures.phone.voice

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CreateExposureCommand
import com.exposures.datalayer.mapper.toDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Orchestrates a single voice-capture "save exposure" request: parses/validates the spoken
 * shutter speed, checks the watch is reachable before sending anything (no outbox/retry — see
 * exp--google-assistant-capture-plan.md), resolves an optional spoken lens name against the
 * phone's local lens list, sends the create-exposure command, and awaits the watch's ack (or times
 * out) to produce a spoken result string.
 */
class VoiceCaptureRequestHandler(
    private val repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
    private val ackBroadcaster: CreateExposureAckBroadcaster,
) {
    suspend fun handle(
        shutterSpeedText: String?,
        lensNameText: String?,
        apertureText: String?,
        isoText: String?,
    ): String {
        val shutterSpeed = shutterSpeedText?.let(ShutterSpeedVoiceParser::parse)
            ?: return "Didn't catch that shutter speed."

        if (gateway.findReachableNodeId() == null) {
            return "Watch isn't connected."
        }

        val lensId = lensNameText?.let { LensVoiceMatcher.match(it, repository.observeLenses().first()) }
        val aperture = apertureText?.let(VoiceValueParsers::parseAperture)
        val isoUsed = isoText?.let(VoiceValueParsers::parseIso)

        val exposureId = UUID.randomUUID().toString()
        val command = CreateExposureCommand(
            exposureId = exposureId,
            shutterSpeed = shutterSpeed.toDto(),
            lensId = lensId,
            aperture = aperture,
            isoUsed = isoUsed,
            notes = null, // voice never populates notes — see the plan's Phase 0 finding (2-Text-param cap)
        )
        gateway.sendMessage(DataLayerPaths.CREATE_EXPOSURE_COMMAND, DataLayerJson.encodeCreateExposureCommand(command))

        val ack = withTimeoutOrNull(ACK_TIMEOUT_MS) {
            ackBroadcaster.acks.first { it.exposureId == exposureId }
        }
        return when {
            ack == null -> "Didn't hear back from the watch."
            ack.accepted -> "Saved."
            else -> ack.reason ?: "Watch rejected the exposure."
        }
    }

    companion object {
        const val ACK_TIMEOUT_MS = 5_000L
    }
}
