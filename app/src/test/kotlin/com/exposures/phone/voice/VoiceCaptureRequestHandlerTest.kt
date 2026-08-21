package com.exposures.phone.voice

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.dto.CreateExposureAckCommand
import com.exposures.datalayer.dto.CreateExposureCommand
import com.exposures.model.Lens
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import com.exposures.phone.sync.FakeDataLayerGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceCaptureRequestHandlerTest {

    /**
     * Waits for [handler.handle][VoiceCaptureRequestHandler.handle] (running concurrently in
     * another coroutine) to reach the point where it sends the create-exposure command, so a test
     * can react with a matching ack. Lens resolution reads
     * [com.exposures.database.repository.EquipmentRepository.observeLenses], a Room `Flow` that
     * completes on Room's own real executor rather than the test dispatcher, so this alternates
     * [runCurrent] (runs everything the test dispatcher can do *right now*, without advancing
     * virtual time — unlike `advanceUntilIdle`, this won't also fast-forward through and fire the
     * handler's own ack-await timeout) with a short real sleep (gives Room's background thread a
     * chance to actually finish and schedule the resumption) until the command shows up.
     */
    private suspend fun TestScope.awaitSentCommand(gateway: FakeDataLayerGateway): CreateExposureCommand {
        var command: CreateExposureCommand? = null
        val deadline = System.nanoTime() + 5_000_000_000L
        while (command == null) {
            runCurrent()
            command = gateway.sentMessages.firstOrNull { it.first == DataLayerPaths.CREATE_EXPOSURE_COMMAND }
                ?.let { DataLayerJson.decodeCreateExposureCommand(it.second) }
            if (command == null) {
                check(System.nanoTime() < deadline) { "Timed out waiting for CREATE_EXPOSURE_COMMAND to be sent" }
                withContext(Dispatchers.Default) { delay(5) }
            }
        }
        return command
    }

    @Test
    fun `happy path sends the command and reports success on an accepted ack`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val broadcaster = CreateExposureAckBroadcaster()
        val handler = VoiceCaptureRequestHandler(repository, gateway, broadcaster)

        val resultDeferred = async { handler.handle("125", null, null, null) }
        val command = awaitSentCommand(gateway)
        broadcaster.emit(CreateExposureAckCommand(command.exposureId, accepted = true))

        assertEquals("Saved.", resultDeferred.await())
        assertEquals("FRACTION", command.shutterSpeed.kind)
        assertEquals(125, command.shutterSpeed.denominator)
        assertNull(command.notes) // voice never populates notes
    }

    @Test
    fun `resolves a matching spoken lens name into the command's lensId`() = runTest {
        val repository = createTestRepository()
        repository.saveLens(
            Lens(
                id = "lens-50", name = "50mm f/4.5", cameraBodyId = null, minAperture = 4.5, maxAperture = 32.0,
                stopIncrement = StopIncrement.HALF_STOP, referencePhotoZoomRatio = 1.0, createdAt = 0L, updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED, remoteId = null,
            ),
        )
        val gateway = FakeDataLayerGateway()
        val broadcaster = CreateExposureAckBroadcaster()
        val handler = VoiceCaptureRequestHandler(repository, gateway, broadcaster)

        val resultDeferred = async { handler.handle("125", "50mm", "4.5", "400") }
        val command = awaitSentCommand(gateway)
        broadcaster.emit(CreateExposureAckCommand(command.exposureId, accepted = true))
        resultDeferred.await()

        assertEquals("lens-50", command.lensId)
        assertEquals(4.5, command.aperture)
        assertEquals(400, command.isoUsed)
    }

    @Test
    fun `unreachable watch is rejected immediately without sending anything`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = null }
        val handler = VoiceCaptureRequestHandler(repository, gateway, CreateExposureAckBroadcaster())

        val result = handler.handle("125", null, null, null)

        assertEquals("Watch isn't connected.", result)
        assertTrue(gateway.sentMessages.isEmpty())
    }

    @Test
    fun `unparseable shutter speed is rejected without checking reachability`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = null }
        val handler = VoiceCaptureRequestHandler(repository, gateway, CreateExposureAckBroadcaster())

        val result = handler.handle("not a shutter speed", null, null, null)

        assertEquals("Didn't catch that shutter speed.", result)
        assertTrue(gateway.sentMessages.isEmpty())
    }

    @Test
    fun `no ack within the timeout reports back that nothing was heard`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val handler = VoiceCaptureRequestHandler(repository, gateway, CreateExposureAckBroadcaster())

        val result = handler.handle("125", null, null, null)

        assertEquals("Didn't hear back from the watch.", result)
    }

    @Test
    fun `a rejected ack reports the watch's reason`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val broadcaster = CreateExposureAckBroadcaster()
        val handler = VoiceCaptureRequestHandler(repository, gateway, broadcaster)

        val resultDeferred = async { handler.handle("125", null, null, null) }
        val command = awaitSentCommand(gateway)
        broadcaster.emit(
            CreateExposureAckCommand(command.exposureId, accepted = false, reason = "No active roll selected on watch."),
        )

        assertEquals("No active roll selected on watch.", resultDeferred.await())
    }
}
