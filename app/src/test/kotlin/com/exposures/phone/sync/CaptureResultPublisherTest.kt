package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureResultPublisherTest {

    private fun exposure(id: String, rollId: String = "roll-1") = Exposure(
        id = id,
        filmRollId = rollId,
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
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private suspend fun setUp(repository: EquipmentRepository, exposureId: String) {
        repository.mergeExposureSync(listOf(exposure(exposureId)))
    }

    @Test
    fun `publish marks the exposure with the given status in the local mirror`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        setUp(repository, "exp-1")
        val publisher = CaptureResultPublisher(repository, gateway)

        publisher.publish("exp-1", PhotoStatus.CAPTURED)

        val exposures = repository.observeExposures("roll-1").first()
        assertEquals(PhotoStatus.CAPTURED, exposures.single { it.id == "exp-1" }.referencePhotoStatus)
    }

    @Test
    fun `publish records a failure status just as readily as a success`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        setUp(repository, "exp-1")
        val publisher = CaptureResultPublisher(repository, gateway)

        publisher.publish("exp-1", PhotoStatus.FAILED)

        assertEquals(PhotoStatus.FAILED, repository.getExposure("exp-1")?.referencePhotoStatus)
    }

    @Test
    fun `publish pushes an updated photo-status payload reflecting the new status`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        setUp(repository, "exp-1")
        val publisher = CaptureResultPublisher(repository, gateway)

        publisher.publish("exp-1", PhotoStatus.CAPTURED)

        val payload = gateway.lastPayload(DataLayerPaths.PHOTO_STATUSES)
        val statuses = DataLayerJson.decodePhotoStatuses(requireNotNull(payload))
        assertEquals("CAPTURED", statuses.single { it.exposureId == "exp-1" }.referencePhotoStatus)
    }

    @Test
    fun `publish acks the watch with a capture-result message matching the status`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        setUp(repository, "exp-1")
        val publisher = CaptureResultPublisher(repository, gateway)

        publisher.publish("exp-1", PhotoStatus.FAILED)

        val (path, payload) = gateway.sentMessages.single()
        assertEquals(DataLayerPaths.CAPTURE_RESULT_COMMAND, path)
        val result = DataLayerJson.decodeCaptureResultCommand(payload)
        assertEquals("exp-1", result.exposureId)
        assertEquals("FAILED", result.status)
    }

    @Test
    fun `publish only affects the targeted exposure's status`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        repository.mergeExposureSync(listOf(exposure("exp-1"), exposure("exp-2")))
        val publisher = CaptureResultPublisher(repository, gateway)

        publisher.publish("exp-1", PhotoStatus.CAPTURED)

        val exposures = repository.observeExposures("roll-1").first().associateBy { it.id }
        assertEquals(PhotoStatus.CAPTURED, exposures.getValue("exp-1").referencePhotoStatus)
        assertEquals(PhotoStatus.NONE, exposures.getValue("exp-2").referencePhotoStatus)
    }
}
