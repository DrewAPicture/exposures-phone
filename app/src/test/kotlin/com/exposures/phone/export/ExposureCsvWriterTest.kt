package com.exposures.phone.export

import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureCsvWriterTest {

    private fun exposure(
        id: String = "exp-1",
        filmRollId: String = "roll-1",
        frameNumber: Int = 1,
        lensId: String = "lens-1",
        focalLengthMm: Int? = null,
        zone: Int? = null,
        notes: String? = null,
        capturedAt: Long = 0L,
        isFavorite: Boolean = false,
    ) = Exposure(
        id = id, filmRollId = filmRollId, frameNumber = frameNumber, lensId = lensId, focalLengthMm = focalLengthMm,
        shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0, isoUsed = 400, zone = zone, notes = notes,
        capturedAt = capturedAt, referencePhotoStatus = PhotoStatus.CAPTURED, createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, remoteId = null, isFavorite = isFavorite,
    )

    @Test
    fun `writes a header row followed by one row per exposure`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure()),
            rollNames = mapOf("roll-1" to "Portra 400 — Roll 1"),
            lensNames = mapOf("lens-1" to "110mm f-2.8"),
            zoneId = ZoneOffset.UTC,
        )

        val lines = csv.lines()
        assertEquals(
            "Roll,Frame,Lens,Focal Length (mm),Shutter Speed,Aperture,ISO,Zone,Notes,Captured At,Photo Status,Favorite",
            lines[0],
        )
        assertEquals(2, lines.size)
    }

    @Test
    fun `a row resolves roll and lens names, formats aperture and timestamp, and includes photo status`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure(focalLengthMm = 110, capturedAt = 1_700_000_000_000L)),
            rollNames = mapOf("roll-1" to "Portra 400 — Roll 1"),
            lensNames = mapOf("lens-1" to "110mm f/2.8 W"),
            zoneId = ZoneOffset.UTC,
        )

        val row = csv.lines()[1]
        assertEquals(
            "Portra 400 — Roll 1,1,110mm f/2.8 W,110,${ShutterSpeed.fraction(125).label},ƒ/8.0,400,,,2023-11-14 22:13,CAPTURED,N",
            row,
        )
    }

    @Test
    fun `isFavorite renders as Y or N`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(
                exposure(id = "a", filmRollId = "roll-1", frameNumber = 1, isFavorite = true),
                exposure(id = "b", filmRollId = "roll-1", frameNumber = 2, isFavorite = false),
            ),
            rollNames = mapOf("roll-1" to "Roll"),
            lensNames = mapOf("lens-1" to "Lens"),
            zoneId = ZoneOffset.UTC,
        )

        val favoriteColumn = csv.lines().drop(1).map { it.split(",").last() }
        assertEquals(listOf("Y", "N"), favoriteColumn)
    }

    @Test
    fun `a null focal length renders as an empty field`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure(focalLengthMm = null)),
            rollNames = mapOf("roll-1" to "Roll"),
            lensNames = mapOf("lens-1" to "Lens"),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals("", csv.lines()[1].split(",")[3])
    }

    @Test
    fun `a zone is rendered as its roman numeral`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure(zone = 6)),
            rollNames = mapOf("roll-1" to "Roll"),
            lensNames = mapOf("lens-1" to "Lens"),
            zoneId = ZoneOffset.UTC,
        )

        assertEquals("VI", csv.lines()[1].split(",")[7])
    }

    @Test
    fun `an unknown roll or lens id falls back to the raw id`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure(filmRollId = "roll-deleted", lensId = "lens-deleted")),
            rollNames = emptyMap(),
            lensNames = emptyMap(),
            zoneId = ZoneOffset.UTC,
        )

        val fields = csv.lines()[1].split(",")
        assertEquals("roll-deleted", fields[0])
        assertEquals("lens-deleted", fields[2])
    }

    @Test
    fun `notes containing a comma are wrapped in quotes as a single field`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure(notes = "backlit, metered for shadows", capturedAt = 1_700_000_000_000L)),
            rollNames = mapOf("roll-1" to "Roll"),
            lensNames = mapOf("lens-1" to "Lens"),
            zoneId = ZoneOffset.UTC,
        )

        val expectedRow = "Roll,1,Lens,,${ShutterSpeed.fraction(125).label},ƒ/8.0,400,," +
            "\"backlit, metered for shadows\",2023-11-14 22:13,CAPTURED,N"
        assertEquals(expectedRow, csv.lines()[1])
    }

    @Test
    fun `notes containing a quote are escaped by doubling it`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(exposure(notes = "used a 6\" filter")),
            rollNames = mapOf("roll-1" to "Roll"),
            lensNames = mapOf("lens-1" to "Lens"),
            zoneId = ZoneOffset.UTC,
        )

        assertTrue(csv.lines()[1].contains("\"used a 6\"\" filter\""))
    }

    @Test
    fun `rows are sorted by roll name, then frame number`() {
        val csv = ExposureCsvWriter.write(
            exposures = listOf(
                exposure(id = "a", filmRollId = "roll-b", frameNumber = 2),
                exposure(id = "b", filmRollId = "roll-a", frameNumber = 5),
                exposure(id = "c", filmRollId = "roll-a", frameNumber = 1),
            ),
            rollNames = mapOf("roll-a" to "Alpha Roll", "roll-b" to "Beta Roll"),
            lensNames = mapOf("lens-1" to "Lens"),
            zoneId = ZoneOffset.UTC,
        )

        val dataLines = csv.lines().drop(1)
        val rollAndFrame = dataLines.map { it.split(",").take(2).joinToString(",") }
        assertEquals(listOf("Alpha Roll,1", "Alpha Roll,5", "Beta Roll,2"), rollAndFrame)
    }
}
