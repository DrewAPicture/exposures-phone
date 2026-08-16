package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CaptureResultCommand
import com.exposures.datalayer.mapper.toPhotoStatusDto
import com.exposures.model.PhotoStatus
import kotlinx.coroutines.flow.first

/**
 * Publishes the outcome of a capture attempt: updates the local exposure mirror, pushes the
 * updated photo-status payload (durable fallback), and acks the watch (fast path when reachable).
 * Called by [com.exposures.phone.CaptureForegroundService] after a real CameraX capture succeeds
 * or fails — this class has no camera code in it, just the result-plumbing that's identical
 * either way, which is what makes it worth unit testing on its own.
 */
class CaptureResultPublisher(
    private val repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
) {
    suspend fun publish(exposureId: String, status: PhotoStatus) {
        repository.updateExposurePhotoStatus(exposureId, status)
        pushPhotoStatuses()

        val result = CaptureResultCommand(exposureId = exposureId, status = status.name)
        gateway.sendMessage(DataLayerPaths.CAPTURE_RESULT_COMMAND, DataLayerJson.encodeCaptureResultCommand(result))
    }

    private suspend fun pushPhotoStatuses() {
        val statuses = repository.observeAllExposures().first().map { it.toPhotoStatusDto() }
        gateway.putPayload(DataLayerPaths.PHOTO_STATUSES, DataLayerJson.encodePhotoStatuses(statuses))
    }
}
