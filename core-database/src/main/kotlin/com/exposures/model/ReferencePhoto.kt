package com.exposures.model

// Lives in core-database, not core-model: phone-only, no watch counterpart, and core-model now
// points at exposures-common (which deliberately excludes it). Package kept as com.exposures.model
// to match its callers (CaptureForegroundService.kt, UploadCoordinator.kt, EquipmentRepository.kt)
// unchanged; see docs/rebaseline/report-2026-08-17.md in exposures-common for the exclusion call.

/** Phone-owned: the reference photo captured for an exposure, plus its upload state. */
data class ReferencePhoto(
    val id: String,
    val exposureId: String,
    val localUri: String?,
    val remoteUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val capturedAt: Long?,
    val uploadStatus: SyncStatus,
    val retryCount: Int,
    val lastError: String?,
)
