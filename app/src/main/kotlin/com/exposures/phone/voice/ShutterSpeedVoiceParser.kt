package com.exposures.phone.voice

import com.exposures.model.ShutterSpeed

/**
 * Parses a spoken shutter speed slot. Accepts bare numbers ("125" -> 1/125), ordinal form ("125th"
 * -> 1/125), a literal slash fraction ("1/125" -> 1/125), whole seconds ("2 seconds" -> 2"), and
 * "bulb" (case-insensitive) -> [ShutterSpeed.BULB]. Anything else — including a non-positive
 * numerator/denominator — returns null so the caller can reject the command with a spoken
 * "didn't catch that" rather than crash on [ShutterSpeed]'s validation.
 */
object ShutterSpeedVoiceParser {

    private val secondsPattern = Regex("""^(\d+)\s*seconds?$""", RegexOption.IGNORE_CASE)
    private val ordinalPattern = Regex("""^(\d+)(?:st|nd|rd|th)$""", RegexOption.IGNORE_CASE)
    private val slashFractionPattern = Regex("""^\d*/(\d+)$""")
    private val barePattern = Regex("""^(\d+)$""")

    fun parse(text: String): ShutterSpeed? {
        val trimmed = text.trim()
        if (trimmed.equals("bulb", ignoreCase = true)) return ShutterSpeed.BULB

        secondsPattern.find(trimmed)?.let { match ->
            return match.groupValues[1].toPositiveIntOrNull()?.let(ShutterSpeed::wholeSeconds)
        }
        ordinalPattern.find(trimmed)?.let { match ->
            return match.groupValues[1].toPositiveIntOrNull()?.let(ShutterSpeed::fraction)
        }
        slashFractionPattern.find(trimmed)?.let { match ->
            return match.groupValues[1].toPositiveIntOrNull()?.let(ShutterSpeed::fraction)
        }
        barePattern.find(trimmed)?.let { match ->
            return match.groupValues[1].toPositiveIntOrNull()?.let(ShutterSpeed::fraction)
        }
        return null
    }

    private fun String.toPositiveIntOrNull(): Int? = toIntOrNull()?.takeIf { it > 0 }
}
