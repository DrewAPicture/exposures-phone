package com.exposures.phone.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exposures.datalayer.DataLayerGateway
import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
import com.exposures.phone.export.CsvExportCoordinator
import com.exposures.phone.settings.AppThemePreference
import com.exposures.phone.settings.CaptureCameraPreference
import com.exposures.phone.settings.CaptureCameraPreferences
import com.exposures.phone.settings.ThemePreferences
import com.exposures.phone.sync.FakeDataLayerGateway
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createThemePreferences(): ThemePreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return ThemePreferences(context)
    }

    private fun createCaptureCameraPreferences(): CaptureCameraPreferences {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return CaptureCameraPreferences(context)
    }

    @Test
    fun `reports the watch as reachable when the gateway finds a node`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = "watch-node" }
        val viewModel = SettingsViewModel(
            repository,
            gateway,
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )

        val state = viewModel.uiState.first { it.watchReachable != null }

        assertTrue(state.watchReachable == true)
    }

    @Test
    fun `reports the watch as unreachable when the gateway finds no node`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = null }
        val viewModel = SettingsViewModel(
            repository,
            gateway,
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )

        val state = viewModel.uiState.first { it.watchReachable != null }

        assertFalse(state.watchReachable == true)
    }

    @Test
    fun `refreshPairingStatus re-checks reachability`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = null }
        val viewModel = SettingsViewModel(
            repository,
            gateway,
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )
        viewModel.uiState.first { it.watchReachable != null }

        gateway.reachableNodeId = "watch-node"
        viewModel.refreshPairingStatus()

        val state = viewModel.uiState.first { it.watchReachable == true }
        assertEquals(true, state.watchReachable)
    }

    /** A gateway whose [findReachableNodeId] suspends until a result is pushed, so a test can observe in-flight state. */
    private class SuspendingGateway : DataLayerGateway {
        val results = Channel<String?>(Channel.UNLIMITED)
        override suspend fun putPayload(path: String, json: String) = Unit
        override fun observePayload(path: String): Flow<String> = emptyFlow()
        override suspend fun sendMessage(path: String, payload: String): Boolean = true
        override suspend fun findReachableNodeId(): String? = results.receive()
    }

    @Test
    fun `refreshPairingStatus resets watchReachable to unknown while a new check is in flight`() = runTest {
        val repository = createTestRepository()
        val gateway = SuspendingGateway()
        gateway.results.trySend("watch-node")
        val viewModel = SettingsViewModel(
            repository,
            gateway,
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )
        viewModel.uiState.first { it.watchReachable == true }

        viewModel.refreshPairingStatus()

        assertNull(viewModel.uiState.value.watchReachable)

        gateway.results.trySend(null)
        val finalState = viewModel.uiState.first { it.watchReachable == false }
        assertEquals(false, finalState.watchReachable)
    }

    private fun exposure(id: String, syncStatus: SyncStatus) = Exposure(
        id = id,
        filmRollId = "roll-1",
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
        syncStatus = syncStatus,
        remoteId = null,
    )

    @Test
    fun `pendingSyncCount counts exposures not yet uploaded to the backend`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(
            listOf(
                exposure("exp-1", SyncStatus.PENDING_SYNC),
                exposure("exp-2", SyncStatus.SYNCED),
            ),
        )
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.pendingSyncCount)
    }

    @Test
    fun `syncNow invokes the injected upload trigger`() = runTest {
        var triggered = false
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
            triggerUpload = { triggered = true },
        )

        viewModel.syncNow()

        assertTrue(triggered)
    }

    @Test
    fun `exportAllCsv delegates to the csv export coordinator`() = runTest {
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )

        val csv = viewModel.exportAllCsv()

        assertTrue(csv.startsWith("Roll,Frame,Lens,Focal Length (mm),Shutter Speed,Aperture,ISO,Zone,Notes,Captured At,Photo Status"))
    }

    @Test
    fun `setThemePreference updates theme selection`() = runTest {
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )

        viewModel.setThemePreference(AppThemePreference.DARK)

        val state = viewModel.uiState.first { it.themePreference == AppThemePreference.DARK }
        assertEquals(AppThemePreference.DARK, state.themePreference)
    }

    @Test
    fun `setCaptureCameraPreference updates capture camera selection`() = runTest {
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )

        viewModel.setCaptureCameraPreference(CaptureCameraPreference.FRONT)

        val state = viewModel.uiState.first { it.captureCameraPreference == CaptureCameraPreference.FRONT }
        assertEquals(CaptureCameraPreference.FRONT, state.captureCameraPreference)
    }

    @Test
    fun `setThemePreference emits a saved event`() = runTest {
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )
        var emissions = 0
        val job = launch { viewModel.savedEvent.collect { emissions++ } }
        runCurrent()

        viewModel.setThemePreference(AppThemePreference.DARK)
        runCurrent()

        assertEquals(1, emissions)
        job.cancel()
    }

    @Test
    fun `setCaptureCameraPreference emits a saved event`() = runTest {
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )
        var emissions = 0
        val job = launch { viewModel.savedEvent.collect { emissions++ } }
        runCurrent()

        viewModel.setCaptureCameraPreference(CaptureCameraPreference.FRONT)
        runCurrent()

        assertEquals(1, emissions)
        job.cancel()
    }

    @Test
    fun `a second save is not dropped while the collector is still handling the first`() = runTest {
        val repository = createTestRepository()
        val viewModel = SettingsViewModel(
            repository,
            FakeDataLayerGateway(),
            CsvExportCoordinator(repository),
            createThemePreferences(),
            createCaptureCameraPreferences(),
        )
        val received = mutableListOf<Unit>()
        // Holds the collector suspended inside its first `collect` invocation, simulating a
        // Snackbar still on screen from the first save when a second one comes in — exactly the
        // scenario extraBufferCapacity = 1 (rather than the default 0) protects against.
        val releaseFirst = CompletableDeferred<Unit>()
        val job = launch {
            viewModel.savedEvent.collect {
                received += Unit
                if (received.size == 1) releaseFirst.await()
            }
        }
        runCurrent()

        viewModel.setThemePreference(AppThemePreference.DARK)
        runCurrent() // collector receives it and is now suspended awaiting releaseFirst

        viewModel.setCaptureCameraPreference(CaptureCameraPreference.FRONT)
        runCurrent()

        releaseFirst.complete(Unit)
        runCurrent()

        assertEquals(2, received.size)
        job.cancel()
    }
}
