package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CapturePhotoCommand
import com.exposures.datalayer.dto.CaptureResultCommand
import com.exposures.datalayer.mapper.toPhotoStatusDto
import com.exposures.model.PhotoStatus
import kotlinx.coroutines.flow.first

/**
 * Handles the watch's capture-photo command. Phase 2 stub: there's no camera capture yet, so this
 * immediately marks the exposure CAPTURED — Phase 3 replaces the body of [handle] with a real
 * CameraX capture before acking, without changing the surrounding contract (update mirror, push
 * photo-status, ack the watch).
 */
class CaptureCommandHandler(
    private val repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
) {
    suspend fun handle(command: CapturePhotoCommand) {
        repository.updateExposurePhotoStatus(command.exposureId, PhotoStatus.CAPTURED)
        pushPhotoStatuses()

        val result = CaptureResultCommand(exposureId = command.exposureId, status = PhotoStatus.CAPTURED.name)
        gateway.sendMessage(DataLayerPaths.CAPTURE_RESULT_COMMAND, DataLayerJson.encodeCaptureResultCommand(result))
    }

    private suspend fun pushPhotoStatuses() {
        val statuses = repository.observeAllExposures().first().map { it.toPhotoStatusDto() }
        gateway.putPayload(DataLayerPaths.PHOTO_STATUSES, DataLayerJson.encodePhotoStatuses(statuses))
    }
}
