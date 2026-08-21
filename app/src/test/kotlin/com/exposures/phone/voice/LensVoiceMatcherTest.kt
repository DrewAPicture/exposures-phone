package com.exposures.phone.voice

import com.exposures.model.Lens
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LensVoiceMatcherTest {

    private fun lens(id: String, name: String) = Lens(
        id = id,
        name = name,
        cameraBodyId = null,
        minAperture = 2.8,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 1.0,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private val lenses = listOf(
        lens("lens-110", "Mamiya-Sekor Z 110mm f/2.8 W"),
        lens("lens-50", "Mamiya-Sekor Z 50mm f/4.5 W"),
    )

    @Test
    fun `exact case-insensitive match wins`() {
        assertEquals("lens-110", LensVoiceMatcher.match("mamiya-sekor z 110mm f/2.8 w", lenses))
    }

    @Test
    fun `unambiguous substring match resolves to that lens`() {
        assertEquals("lens-50", LensVoiceMatcher.match("50mm", lenses))
    }

    @Test
    fun `ambiguous substring match returns null`() {
        assertNull(LensVoiceMatcher.match("mamiya", lenses))
    }

    @Test
    fun `no match returns null`() {
        assertNull(LensVoiceMatcher.match("nonexistent lens", lenses))
    }

    @Test
    fun `blank input returns null`() {
        assertNull(LensVoiceMatcher.match("   ", lenses))
    }

    @Test
    fun `empty lens list returns null`() {
        assertNull(LensVoiceMatcher.match("50mm", emptyList()))
    }
}
