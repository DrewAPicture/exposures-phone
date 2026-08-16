package com.exposures.model

/**
 * Ansel Adams's Zone System: 11 zones from pure black (0) to pure white (X). Modeled as a plain
 * 0..10 Int on [Exposure] rather than its own enum/value class, since it's stored and synced just
 * like the other scalar exposure fields — this object only exists to render it for humans (e.g.
 * CSV export), mirroring exposures-watch's copy of the same helper.
 */
object Zone {
    const val MIN = 0
    const val MAX = 10

    private val LABELS = listOf("0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

    /** Roman-numeral label for [zone] — Zone 0 itself is rendered "0", not a numeral. */
    fun label(zone: Int): String {
        require(zone in MIN..MAX) { "Zone must be $MIN..$MAX, was $zone" }
        return LABELS[zone]
    }
}
