package com.exposures.phone.voice

import com.exposures.model.Lens

/**
 * Resolves a spoken lens name to a [Lens.id] using the phone's local (phone-owned) lens list, so
 * the watch never needs to do fuzzy text matching. An exact case-insensitive match wins; otherwise
 * an unambiguous case-insensitive substring match; anything ambiguous or unmatched returns null so
 * the caller falls back to the watch's last-used lens rather than failing the command.
 */
object LensVoiceMatcher {

    fun match(spokenText: String, lenses: List<Lens>): String? {
        val query = spokenText.trim()
        if (query.isEmpty()) return null

        lenses.firstOrNull { it.name.equals(query, ignoreCase = true) }?.let { return it.id }

        return lenses.filter { it.name.contains(query, ignoreCase = true) }.singleOrNull()?.id
    }
}
