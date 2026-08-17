package com.exposures.database

import com.exposures.model.FilmBackType
import com.exposures.model.FilmFormat
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `shutter speed round-trips through its string encoding`() {
        val original = ShutterSpeed.fraction(125)
        assertEquals(original, converters.toShutterSpeed(converters.fromShutterSpeed(original)))
    }

    @Test
    fun `shutter speed list round-trips and preserves order`() {
        val original = listOf(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(2), ShutterSpeed.BULB)
        assertEquals(original, converters.toShutterSpeedList(converters.fromShutterSpeedList(original)))
    }

    @Test
    fun `enum converters round-trip every declared value`() {
        SyncStatus.entries.forEach { assertEquals(it, converters.toSyncStatus(converters.fromSyncStatus(it))) }
        RollStatus.entries.forEach { assertEquals(it, converters.toRollStatus(converters.fromRollStatus(it))) }
        PhotoStatus.entries.forEach { assertEquals(it, converters.toPhotoStatus(converters.fromPhotoStatus(it))) }
        FilmFormat.entries.forEach { assertEquals(it, converters.toFilmFormat(converters.fromFilmFormat(it))) }
        StopIncrement.entries.forEach { assertEquals(it, converters.toStopIncrement(converters.fromStopIncrement(it))) }
        FilmBackType.entries.forEach { assertEquals(it, converters.toFilmBackType(converters.fromFilmBackType(it))) }
    }

    @Test
    fun `frame count list round-trips and preserves order`() {
        val original = listOf(10, 11)
        assertEquals(original, converters.toFrameCountList(converters.fromFrameCountList(original)))
    }

    @Test
    fun `an empty frame count list round-trips to empty, not a list with one blank entry`() {
        assertEquals(emptyList<Int>(), converters.toFrameCountList(converters.fromFrameCountList(emptyList())))
    }
}
