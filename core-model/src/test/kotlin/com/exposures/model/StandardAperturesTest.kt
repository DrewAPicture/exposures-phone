package com.exposures.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StandardAperturesTest {

    @Test
    fun `full stop range for a clean f2_8 to f22 lens matches the printed scale`() {
        val apertures = StandardApertures.forLens(2.8, 22.0, StopIncrement.FULL_STOP)
        assertEquals(listOf(2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0), apertures)
    }

    @Test
    fun `half stop range for the same lens includes the intermediate stops`() {
        val apertures = StandardApertures.forLens(2.8, 22.0, StopIncrement.HALF_STOP)
        assertEquals(
            listOf(2.8, 3.3, 4.0, 4.8, 5.6, 6.7, 8.0, 9.5, 11.0, 13.0, 16.0, 19.0, 22.0),
            apertures,
        )
    }

    @Test
    fun `physical min and max are always included even off the standard scale`() {
        val apertures = StandardApertures.forLens(3.5, 45.0, StopIncrement.FULL_STOP)
        assertEquals(listOf(3.5, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0, 45.0), apertures)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a max aperture smaller than the min`() {
        StandardApertures.forLens(minAperture = 8.0, maxAperture = 2.8, increment = StopIncrement.FULL_STOP)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a non-positive min aperture`() {
        StandardApertures.forLens(minAperture = 0.0, maxAperture = 8.0, increment = StopIncrement.FULL_STOP)
    }
}
