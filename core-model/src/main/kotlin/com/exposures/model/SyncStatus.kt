package com.exposures.model

/** Where a syncable record stands relative to the (not-yet-built) remote backend. */
enum class SyncStatus {
    PENDING_SYNC,
    SYNCING,
    SYNCED,
    SYNC_FAILED,
}
