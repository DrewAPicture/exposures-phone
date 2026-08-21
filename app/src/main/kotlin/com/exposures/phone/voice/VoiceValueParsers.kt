package com.exposures.phone.voice

/**
 * Forgiving parsers for the optional voice-capture slots. Unlike [ShutterSpeedVoiceParser], a
 * parse failure here just means "treat as unspecified" — these fields have a last-used fallback
 * on the watch, so an unparseable value shouldn't reject the whole command.
 */
object VoiceValueParsers {

    /** Strips a leading "f"/"f/" prefix (e.g. "f/2.8", "f2.8") before parsing. */
    fun parseAperture(text: String): Double? =
        text.trim().removePrefix("f/").removePrefix("F/").removePrefix("f").removePrefix("F").trim().toDoubleOrNull()

    fun parseIso(text: String): Int? = text.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.toIntOrNull()
}
