package com.exposures.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StandardIsoTest {

    @Test
    fun `pushing one stop doubles the ISO`() {
        assertEquals(800, StandardIso.pushPull(boxSpeed = 400, stops = 1))
    }

    @Test
    fun `pulling one stop halves the ISO`() {
        assertEquals(200, StandardIso.pushPull(boxSpeed = 400, stops = -1))
    }

    @Test
    fun `zero stops returns box speed unchanged`() {
        assertEquals(400, StandardIso.pushPull(boxSpeed = 400, stops = 0))
    }

    @Test
    fun `pushing two stops quadruples the ISO`() {
        assertEquals(1600, StandardIso.pushPull(boxSpeed = 400, stops = 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a non-positive box speed`() {
        StandardIso.pushPull(boxSpeed = 0, stops = 1)
    }
}
