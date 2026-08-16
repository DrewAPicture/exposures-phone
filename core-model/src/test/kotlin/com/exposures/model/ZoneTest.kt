package com.exposures.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneTest {

    @Test
    fun `zone 0 is labeled 0, not a roman numeral`() {
        assertEquals("0", Zone.label(0))
    }

    @Test
    fun `zones 1 through 10 are labeled with roman numerals`() {
        val expected = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

        val actual = (1..10).map(Zone::label)

        assertEquals(expected, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a zone below the minimum`() {
        Zone.label(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a zone above the maximum`() {
        Zone.label(11)
    }
}
