package com.exposures.phone.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomRatioTest {

    @Test
    fun `a requested ratio within range is used as-is`() {
        assertEquals(2.0f, ZoomRatio.clamp(requested = 2.0, min = 1.0f, max = 5.0f), 0.0f)
    }

    @Test
    fun `a requested ratio above the device max is clamped down`() {
        assertEquals(5.0f, ZoomRatio.clamp(requested = 8.0, min = 1.0f, max = 5.0f), 0.0f)
    }

    @Test
    fun `a requested ratio below the device min is clamped up`() {
        assertEquals(1.0f, ZoomRatio.clamp(requested = 0.5, min = 1.0f, max = 5.0f), 0.0f)
    }

    @Test
    fun `a lens with no real zoom just resolves to 1x`() {
        assertEquals(1.0f, ZoomRatio.clamp(requested = 1.0, min = 1.0f, max = 10.0f), 0.0f)
    }
}
