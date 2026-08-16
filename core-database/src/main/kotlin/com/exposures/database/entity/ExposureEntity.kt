package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus

// Deliberately no FK constraints to film_rolls/lenses here, unlike the watch's copy of this
// entity: this table is a passive mirror populated by an independent Data Layer sync that can
// legitimately race ahead of the equipment/roll sync (no cross-path ordering guarantee), so a
// synced exposure may reference a roll/lens the phone hasn't received yet.
@Entity(
    tableName = "exposures",
    indices = [Index("filmRollId"), Index("lensId")],
)
data class ExposureEntity(
    @PrimaryKey val id: String,
    val filmRollId: String,
    val frameNumber: Int,
    val lensId: String,
    val shutterSpeed: ShutterSpeed,
    val aperture: Double,
    val isoUsed: Int,
    val zone: Int?,
    val notes: String?,
    val capturedAt: Long,
    val referencePhotoStatus: PhotoStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
