package com.exposures.phone.voice

import com.exposures.model.ShutterSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShutterSpeedVoiceParserTest {

    @Test
    fun `accepted formats parse to the expected shutter speed`() {
        val cases = listOf(
            "125" to ShutterSpeed.fraction(125),
            "125th" to ShutterSpeed.fraction(125),
            "125nd" to ShutterSpeed.fraction(125),
            "1st" to ShutterSpeed.fraction(1),
            "1/125" to ShutterSpeed.fraction(125),
            "1/2" to ShutterSpeed.fraction(2),
            "2 seconds" to ShutterSpeed.wholeSeconds(2),
            "2 second" to ShutterSpeed.wholeSeconds(2),
            "1 second" to ShutterSpeed.wholeSeconds(1),
            "bulb" to ShutterSpeed.BULB,
            "Bulb" to ShutterSpeed.BULB,
            "BULB" to ShutterSpeed.BULB,
            "  125  " to ShutterSpeed.fraction(125),
        )

        cases.forEach { (input, expected) ->
            assertEquals("parsing \"$input\"", expected, ShutterSpeedVoiceParser.parse(input))
        }
    }

    @Test
    fun `unrecognized or invalid input returns null instead of crashing`() {
        val cases = listOf("", "not a shutter speed", "0", "0th", "1/0", "seconds", "f/2.8", "-125")

        cases.forEach { input ->
            assertNull("parsing \"$input\"", ShutterSpeedVoiceParser.parse(input))
        }
    }
}
