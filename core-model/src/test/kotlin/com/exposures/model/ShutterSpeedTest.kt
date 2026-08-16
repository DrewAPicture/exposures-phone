package com.exposures.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShutterSpeedTest {

    @Test
    fun `fraction label matches camera dial convention`() {
        assertEquals("1/125", ShutterSpeed.fraction(125).label)
    }

    @Test
    fun `whole seconds label uses a trailing quote mark`() {
        assertEquals("2\"", ShutterSpeed.wholeSeconds(2).label)
    }

    @Test
    fun `bulb label is B`() {
        assertEquals("B", ShutterSpeed.BULB.label)
    }

    @Test
    fun `duration seconds is computed from the fraction`() {
        assertEquals(1.0 / 250, ShutterSpeed.fraction(250).durationSeconds, 0.0)
    }

    @Test
    fun `bulb duration is positive infinity`() {
        assertEquals(Double.POSITIVE_INFINITY, ShutterSpeed.BULB.durationSeconds, 0.0)
    }

    @Test
    fun `faster fraction sorts before slower fraction`() {
        val fast = ShutterSpeed.fraction(1000)
        val slow = ShutterSpeed.fraction(30)
        assertTrue(fast < slow)
    }

    @Test
    fun `fractions sort before whole seconds which sort before bulb`() {
        val speeds = listOf(ShutterSpeed.BULB, ShutterSpeed.wholeSeconds(4), ShutterSpeed.fraction(500))
        assertEquals(
            listOf(ShutterSpeed.fraction(500), ShutterSpeed.wholeSeconds(4), ShutterSpeed.BULB),
            speeds.sorted(),
        )
    }

    @Test
    fun `standardRange for a leaf-shutter body like the RZ67 clamps to its physical range`() {
        val range = ShutterSpeed.standardRange(
            fastest = ShutterSpeed.fraction(400),
            slowest = ShutterSpeed.wholeSeconds(8),
            includeBulb = true,
        )

        val expectedFractions = listOf(250, 125, 60, 30, 15, 8, 4, 2).map(ShutterSpeed::fraction)
        val expectedWholeSeconds = listOf(1, 2, 4, 8).map(ShutterSpeed::wholeSeconds)
        val expected = expectedFractions + expectedWholeSeconds + ShutterSpeed.BULB

        assertEquals(expected.sortedBy { if (it == ShutterSpeed.BULB) Double.MAX_VALUE else it.durationSeconds }, range)
    }

    @Test
    fun `standardRange without bulb omits it`() {
        val range = ShutterSpeed.standardRange(
            fastest = ShutterSpeed.fraction(1000),
            slowest = ShutterSpeed.fraction(60),
            includeBulb = false,
        )
        assertTrue(ShutterSpeed.BULB !in range)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `standardRange rejects an inverted range`() {
        ShutterSpeed.standardRange(fastest = ShutterSpeed.wholeSeconds(8), slowest = ShutterSpeed.fraction(400))
    }
}
