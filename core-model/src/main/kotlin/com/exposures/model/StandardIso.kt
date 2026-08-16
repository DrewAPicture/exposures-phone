package com.exposures.model

import kotlin.math.pow
import kotlin.math.roundToInt

/** Standard full-stop ISO/film-speed scale and push/pull arithmetic. */
object StandardIso {

    val FULL_STOP_SCALE = listOf(25, 50, 100, 200, 400, 800, 1600, 3200, 6400, 12800)

    /**
     * The ISO a shooter is exposing at when pushing or pulling [stops] full stops from [boxSpeed]
     * (the film's rated speed) — e.g. `pushPull(400, +1) == 800`, `pushPull(400, -1) == 200`.
     */
    fun pushPull(boxSpeed: Int, stops: Int): Int {
        require(boxSpeed > 0) { "boxSpeed must be positive" }
        return (boxSpeed * 2.0.pow(stops)).roundToInt()
    }
}
