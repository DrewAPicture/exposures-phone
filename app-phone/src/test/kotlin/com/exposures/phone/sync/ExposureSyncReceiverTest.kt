package com.exposures.phone.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.dto.ExposureDto
import com.exposures.datalayer.dto.ShutterSpeedDto
import com.exposures.model.PhotoStatus
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExposureSyncReceiverTest {

    private fun exposureDto(id: String) = ExposureDto(
        id = id, filmRollId = "roll-1", frameNumber = 1, lensId = "lens-1",
        shutterSpeed = ShutterSpeedDto("FRACTION", 1, 125), aperture = 8.0, isoUsed = 400,
        notes = null, capturedAt = 0L, referencePhotoStatus = "NONE", createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `handlePayload decodes and merges a never-before-seen exposure as pending backend sync`() = runTest {
        val repository = createTestRepository()
        val receiver = ExposureSyncReceiver(repository)
        val json = DataLayerJson.encodeExposures(listOf(exposureDto("exp-1")))

        receiver.handlePayload(json)

        val stored = repository.observeExposures("roll-1").first().single()
        assertEquals("exp-1", stored.id)
        // PENDING_SYNC here is about the remote backend, not the watch<->phone Data Layer sync
        // that just delivered this payload — that part is already done by the time this runs.
        assertEquals(SyncStatus.PENDING_SYNC, stored.syncStatus)
        assertEquals(PhotoStatus.NONE, stored.referencePhotoStatus)
    }

    @Test
    fun `a second payload preserves a locally-known photo status`() = runTest {
        val repository = createTestRepository()
        val receiver = ExposureSyncReceiver(repository)
        receiver.handlePayload(DataLayerJson.encodeExposures(listOf(exposureDto("exp-1"))))
        repository.updateExposurePhotoStatus("exp-1", PhotoStatus.CAPTURED)

        receiver.handlePayload(DataLayerJson.encodeExposures(listOf(exposureDto("exp-1"))))

        assertEquals(PhotoStatus.CAPTURED, repository.observeExposures("roll-1").first().single().referencePhotoStatus)
    }

    @Test
    fun `a second payload preserves a locally-known backend sync status and remote id`() = runTest {
        val repository = createTestRepository()
        val receiver = ExposureSyncReceiver(repository)
        receiver.handlePayload(DataLayerJson.encodeExposures(listOf(exposureDto("exp-1"))))
        val uploaded = repository.getExposure("exp-1")!!
        repository.markExposureSynced(uploaded, remoteId = "server-exp-1")

        // The watch re-syncs (e.g. after logging another frame) — this must not undo the upload.
        receiver.handlePayload(DataLayerJson.encodeExposures(listOf(exposureDto("exp-1"))))

        val stored = repository.getExposure("exp-1")!!
        assertEquals(SyncStatus.SYNCED, stored.syncStatus)
        assertEquals("server-exp-1", stored.remoteId)
    }
}
