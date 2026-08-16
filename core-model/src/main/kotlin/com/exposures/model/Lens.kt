package com.exposures.model

/** Phone-authoritative: lenses are configured on the phone, the watch only ever reads them. */
data class Lens(
    val id: String,
    val name: String,
    val minAperture: Double,
    val maxAperture: Double,
    val stopIncrement: StopIncrement,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
) {
    /** Apertures selectable for this lens, per its [stopIncrement]. */
    fun availableApertures(): List<Double> = StandardApertures.forLens(minAperture, maxAperture, stopIncrement)
}
