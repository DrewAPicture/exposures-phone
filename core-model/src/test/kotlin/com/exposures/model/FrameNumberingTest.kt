package com.exposures.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameNumberingTest {

    private fun exposure(frameNumber: Int) = Exposure(
        id = "exp-$frameNumber",
        filmRollId = "roll-1",
        frameNumber = frameNumber,
        lensId = "lens-1",
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = null,
        notes = null,
        capturedAt = 0L,
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun roll(targetFrameCount: Int) = FilmRoll(
        id = "roll-1",
        name = "Test roll",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.COLOR,
        cameraBodyId = "body-1",
        lightMeterId = null,
        filmBackId = "back-1",
        targetFrameCount = targetFrameCount,
        status = RollStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    @Test
    fun `next frame number for an empty roll is 1`() {
        assertEquals(1, emptyList<Exposure>().nextFrameNumber())
    }

    @Test
    fun `next frame number is one past the highest existing frame`() {
        val exposures = listOf(exposure(1), exposure(2), exposure(3))
        assertEquals(4, exposures.nextFrameNumber())
    }

    @Test
    fun `next frame number ignores list order`() {
        val exposures = listOf(exposure(3), exposure(1), exposure(2))
        assertEquals(4, exposures.nextFrameNumber())
    }

    @Test
    fun `roll is not complete before reaching its target frame count`() {
        assertFalse(roll(targetFrameCount = 10).isComplete(exposureCount = 9))
    }

    @Test
    fun `roll is complete once exposure count reaches the target`() {
        assertTrue(roll(targetFrameCount = 10).isComplete(exposureCount = 10))
    }
}
