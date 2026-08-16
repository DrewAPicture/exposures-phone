package com.exposures.model

/** Phone-authoritative: lenses are configured on the phone, the watch only ever reads them. */
data class Lens(
    val id: String,
    val name: String,
    /** The body this lens is used with, if configured — nullable so existing lenses don't need backfilling. */
    val cameraBodyId: String?,
    val minAperture: Double,
    val maxAperture: Double,
    val stopIncrement: StopIncrement,
    /** Phone camera zoom applied to this lens's reference photo (e.g. 50mm -> 1.0, 180mm -> 3.0). */
    val referencePhotoZoomRatio: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
) {
    /** Apertures selectable for this lens, per its [stopIncrement]. */
    fun availableApertures(): List<Double> = StandardApertures.forLens(minAperture, maxAperture, stopIncrement)
}
