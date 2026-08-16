package com.exposures.model

enum class ShutterSpeedKind {
    FRACTION,
    WHOLE_SECONDS,
    BULB,
}

/**
 * A shutter speed, represented as an exact fraction rather than a floating-point duration so
 * equality and storage stay lossless. [BULB] means "open for as long as the shutter release is held."
 */
data class ShutterSpeed(
    val kind: ShutterSpeedKind,
    val numerator: Int = 1,
    val denominator: Int = 1,
) : Comparable<ShutterSpeed> {

    init {
        if (kind != ShutterSpeedKind.BULB) {
            require(numerator > 0) { "numerator must be positive" }
            require(denominator > 0) { "denominator must be positive" }
        }
    }

    /** Duration in seconds. [Double.POSITIVE_INFINITY] for [ShutterSpeedKind.BULB]. */
    val durationSeconds: Double
        get() = when (kind) {
            ShutterSpeedKind.BULB -> Double.POSITIVE_INFINITY
            else -> numerator.toDouble() / denominator
        }

    /** Display label matching how these are printed on camera dials, e.g. "1/125", "2\"", "B". */
    val label: String
        get() = when (kind) {
            ShutterSpeedKind.BULB -> "B"
            ShutterSpeedKind.WHOLE_SECONDS -> "$numerator\""
            ShutterSpeedKind.FRACTION -> "$numerator/$denominator"
        }

    override fun compareTo(other: ShutterSpeed): Int = durationSeconds.compareTo(other.durationSeconds)

    override fun toString(): String = label

    companion object {
        fun fraction(denominator: Int) = ShutterSpeed(ShutterSpeedKind.FRACTION, numerator = 1, denominator = denominator)
        fun wholeSeconds(seconds: Int) = ShutterSpeed(ShutterSpeedKind.WHOLE_SECONDS, numerator = seconds, denominator = 1)
        val BULB = ShutterSpeed(ShutterSpeedKind.BULB)

        /**
         * The standard full-stop shutter speed dial, fastest to slowest — the same detents found on
         * mechanical camera shutters (note the conventional rounding: 1/15 and 1/30, not 1/16 and 1/32).
         */
        val STANDARD_FULL_STOPS: List<ShutterSpeed> = (
            listOf(8000, 4000, 2000, 1000, 500, 250, 125, 60, 30, 15, 8, 4, 2).map(::fraction) +
                listOf(1, 2, 4, 8, 15, 30).map(::wholeSeconds)
            ).sorted()

        /**
         * The subset of [STANDARD_FULL_STOPS] between [fastest] and [slowest] (inclusive), optionally
         * with [BULB] appended. Use this to build seed/default data for a camera body's shutter dial;
         * a body whose physical range doesn't land on standard stops (e.g. a leaf shutter topping out
         * at 1/400) should list its speeds explicitly instead.
         */
        fun standardRange(fastest: ShutterSpeed, slowest: ShutterSpeed, includeBulb: Boolean = true): List<ShutterSpeed> {
            require(fastest <= slowest) { "fastest ($fastest) must not be slower than slowest ($slowest)" }
            val inRange = STANDARD_FULL_STOPS.filter { it.durationSeconds in fastest.durationSeconds..slowest.durationSeconds }
            return if (includeBulb) inRange + BULB else inRange
        }
    }
}
