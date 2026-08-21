package com.exposures.phone.export

import com.exposures.model.Exposure
import com.exposures.model.Zone
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds a CSV of exposures for sharing/backup. Rolls are distinguished by a "Roll" column rather
 * than separate files or sheets — plain CSV has no concept of multiple sheets, and a flat table
 * with a grouping column is the closest honest equivalent, filterable/pivotable in any spreadsheet
 * app. Used for both the per-roll export (a single roll's name in [rollNames]) and the all-rolls
 * export (every roll's name).
 */
object ExposureCsvWriter {
    private val HEADER = listOf(
        "Roll", "Frame", "Lens", "Focal Length (mm)", "Shutter Speed", "Aperture", "ISO", "Zone", "Notes",
        "Captured At", "Photo Status", "Favorite",
    )
    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun write(
        exposures: List<Exposure>,
        rollNames: Map<String, String>,
        lensNames: Map<String, String>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val sorted = exposures.sortedWith(compareBy({ rollNames[it.filmRollId] ?: it.filmRollId }, { it.frameNumber }))
        val rows = sorted.map { exposure ->
            listOf(
                rollNames[exposure.filmRollId] ?: exposure.filmRollId,
                exposure.frameNumber.toString(),
                lensNames[exposure.lensId] ?: exposure.lensId,
                exposure.focalLengthMm?.toString().orEmpty(),
                exposure.shutterSpeed.label,
                "ƒ/${exposure.aperture}",
                exposure.isoUsed.toString(),
                exposure.zone?.let(Zone::label).orEmpty(),
                exposure.notes.orEmpty(),
                formatTimestamp(exposure.capturedAt, zoneId),
                exposure.referencePhotoStatus.name,
                if (exposure.isFavorite) "Y" else "N",
            )
        }
        return (listOf(HEADER) + rows).joinToString("\n") { row -> row.joinToString(",", transform = ::escapeField) }
    }

    private fun formatTimestamp(epochMillis: Long, zoneId: ZoneId): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(TIMESTAMP_FORMAT)

    /** RFC 4180: quote a field that contains a comma, quote, or newline, doubling any embedded quotes. */
    private fun escapeField(field: String): String = if (field.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${field.replace("\"", "\"\"")}\""
    } else {
        field
    }
}
