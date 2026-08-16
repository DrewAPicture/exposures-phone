package com.exposures.phone.ui.lens

import com.exposures.model.CameraBody
import com.exposures.model.Lens
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state includes associated camera body names`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(
            CameraBody(
                id = "body-1",
                name = "RZ67 Pro II",
                manufacturer = "Mamiya",
                availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
                hasBulbMode = true,
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )
        repository.saveLens(
            Lens(
                id = "lens-1",
                name = "Mamiya-Sekor Z 110mm f/2.8 W",
                cameraBodyId = "body-1",
                minAperture = 2.8,
                maxAperture = 32.0,
                stopIncrement = StopIncrement.HALF_STOP,
                referencePhotoZoomRatio = 1.0,
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )
        repository.saveLens(
            Lens(
                id = "lens-2",
                name = "Mamiya-Sekor Z 50mm f/4.5 W",
                cameraBodyId = null,
                minAperture = 4.5,
                maxAperture = 32.0,
                stopIncrement = StopIncrement.HALF_STOP,
                referencePhotoZoomRatio = 0.7,
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )

        val state = LensListViewModel(repository).uiState.first { !it.isLoading }

        val byId = state.lenses.associateBy { it.lens.id }
        assertEquals("RZ67 Pro II", byId.getValue("lens-1").cameraBodyName)
        assertNull(byId.getValue("lens-2").cameraBodyName)
    }
}
