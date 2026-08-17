package com.exposures.datalayer

import com.exposures.datalayer.dto.CameraBodyDto
import com.exposures.datalayer.dto.CapturePhotoCommand
import com.exposures.datalayer.dto.CaptureResultCommand
import com.exposures.datalayer.dto.ExposureDto
import com.exposures.datalayer.dto.FilmBackDto
import com.exposures.datalayer.dto.FilmRollDto
import com.exposures.datalayer.dto.LensDto
import com.exposures.datalayer.dto.PhotoStatusDto
import com.exposures.datalayer.dto.ShutterSpeedDto
import org.junit.Assert.assertEquals
import org.junit.Test

class DataLayerJsonTest {

    @Test
    fun `camera bodies round-trip through json, including nested shutter speeds`() {
        val bodies = listOf(
            CameraBodyDto(
                id = "body-1",
                name = "RZ67 Pro II",
                manufacturer = "Mamiya",
                availableShutterSpeeds = listOf(ShutterSpeedDto("FRACTION", 1, 400), ShutterSpeedDto("BULB", 1, 1)),
                hasBulbMode = true,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )

        val json = DataLayerJson.encodeCameraBodies(bodies)

        assertEquals(bodies, DataLayerJson.decodeCameraBodies(json))
    }

    @Test
    fun `empty list round-trips to an empty list, not a decode error`() {
        assertEquals(emptyList<LensDto>(), DataLayerJson.decodeLenses(DataLayerJson.encodeLenses(emptyList())))
    }

    @Test
    fun `film backs round-trip through json`() {
        val filmBacks = listOf(
            FilmBackDto(
                id = "back-1", name = "6x7 back", cameraBodyId = "body-1", type = "ROLL_6X7",
                availableFrameCounts = listOf(10, 11), createdAt = 0L, updatedAt = 0L,
            ),
        )
        assertEquals(filmBacks, DataLayerJson.decodeFilmBacks(DataLayerJson.encodeFilmBacks(filmBacks)))
    }

    @Test
    fun `rolls round-trip through json`() {
        val rolls = listOf(
            FilmRollDto(
                id = "roll-1", name = "Portra 400", filmStock = "Kodak Portra 400", boxSpeedIso = 400,
                format = "MEDIUM_FORMAT_120", colorType = "COLOR", cameraBodyId = "body-1", filmBackId = "back-1",
                targetFrameCount = 10, status = "AVAILABLE",
                createdAt = 0L, updatedAt = 0L,
            ),
        )
        assertEquals(rolls, DataLayerJson.decodeRolls(DataLayerJson.encodeRolls(rolls)))
    }

    @Test
    fun `exposures round-trip through json`() {
        val exposures = listOf(
            ExposureDto(
                id = "exp-1", filmRollId = "roll-1", frameNumber = 1, lensId = "lens-1",
                shutterSpeed = ShutterSpeedDto("FRACTION", 1, 125), aperture = 8.0, isoUsed = 400,
                notes = "test note", capturedAt = 0L, referencePhotoStatus = "NONE", createdAt = 0L, updatedAt = 0L,
            ),
        )
        assertEquals(exposures, DataLayerJson.decodeExposures(DataLayerJson.encodeExposures(exposures)))
    }

    @Test
    fun `photo statuses round-trip through json`() {
        val statuses = listOf(PhotoStatusDto(exposureId = "exp-1", referencePhotoStatus = "CAPTURED", remoteUrl = null))
        assertEquals(statuses, DataLayerJson.decodePhotoStatuses(DataLayerJson.encodePhotoStatuses(statuses)))
    }

    @Test
    fun `capture-photo command round-trips through json`() {
        val command = CapturePhotoCommand(exposureId = "exp-1", filmRollId = "roll-1", frameNumber = 3)
        assertEquals(command, DataLayerJson.decodeCapturePhotoCommand(DataLayerJson.encodeCapturePhotoCommand(command)))
    }

    @Test
    fun `capture-result command round-trips through json`() {
        val command = CaptureResultCommand(exposureId = "exp-1", status = "CAPTURED")
        assertEquals(command, DataLayerJson.decodeCaptureResultCommand(DataLayerJson.encodeCaptureResultCommand(command)))
    }

    @Test
    fun `unknown fields are ignored rather than failing decode`() {
        val json = """[{"id":"body-1","name":"RZ67","manufacturer":"Mamiya","availableShutterSpeeds":[],"hasBulbMode":true,"createdAt":0,"updatedAt":0,"futureField":"ignored"}]"""

        val decoded = DataLayerJson.decodeCameraBodies(json)

        assertEquals("body-1", decoded.single().id)
    }

    @Test
    fun `a roll payload from a writer built before colorType existed still decodes`() {
        val json = """[{"id":"roll-1","name":"Portra 400","filmStock":"Kodak Portra 400","boxSpeedIso":400,"format":"MEDIUM_FORMAT_120","cameraBodyId":"body-1","filmBackId":"back-1","targetFrameCount":10,"status":"AVAILABLE","createdAt":0,"updatedAt":0}]"""

        val decoded = DataLayerJson.decodeRolls(json)

        assertEquals("COLOR", decoded.single().colorType)
    }

    @Test
    fun `a lens payload from a writer built before referencePhotoZoomRatio existed still decodes`() {
        val json = """[{"id":"lens-1","name":"110mm f2.8","minAperture":2.8,"maxAperture":32.0,"stopIncrement":"HALF_STOP","createdAt":0,"updatedAt":0}]"""

        val decoded = DataLayerJson.decodeLenses(json)

        assertEquals(1.0, decoded.single().referencePhotoZoomRatio, 0.0)
    }
}
