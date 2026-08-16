package com.exposures.model

/**
 * Standard photographic f-stop scales, at full/half/third-stop granularity, matching the values
 * printed on lens barrels (not raw geometric roundings — e.g. the full-stop scale reads 11 and 22,
 * not the geometrically exact 11.3 and 22.6).
 */
object StandardApertures {

    private val FULL_STOP = listOf(1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0, 45.0, 64.0)

    private val HALF_STOP = listOf(
        1.0, 1.2, 1.4, 1.7, 2.0, 2.4, 2.8, 3.3, 4.0, 4.8, 5.6, 6.7, 8.0,
        9.5, 11.0, 13.0, 16.0, 19.0, 22.0, 27.0, 32.0, 38.0, 45.0, 54.0, 64.0,
    )

    private val THIRD_STOP = listOf(
        1.0, 1.1, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5,
        5.0, 5.6, 6.3, 7.1, 8.0, 9.0, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 20.0,
        22.0, 25.0, 28.0, 32.0, 36.0, 40.0, 45.0, 51.0, 57.0, 64.0,
    )

    fun stopsFor(increment: StopIncrement): List<Double> = when (increment) {
        StopIncrement.FULL_STOP -> FULL_STOP
        StopIncrement.HALF_STOP -> HALF_STOP
        StopIncrement.THIRD_STOP -> THIRD_STOP
    }

    /**
     * Apertures available on a lens, given its physical [minAperture]/[maxAperture] (the widest and
     * smallest f-numbers printed on the lens) and picker granularity. The lens's actual physical
     * limits are always included, even when they don't land exactly on the standard scale.
     */
    fun forLens(minAperture: Double, maxAperture: Double, increment: StopIncrement): List<Double> {
        require(minAperture > 0.0) { "minAperture must be positive" }
        require(maxAperture >= minAperture) { "maxAperture ($maxAperture) must be >= minAperture ($minAperture)" }

        val inRange = stopsFor(increment).filter { it in minAperture..maxAperture }
        return (inRange + listOf(minAperture, maxAperture)).distinct().sorted()
    }
}
