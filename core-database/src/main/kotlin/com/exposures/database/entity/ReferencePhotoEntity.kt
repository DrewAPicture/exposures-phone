package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.SyncStatus

@Entity(tableName = "reference_photos", indices = [Index("exposureId", unique = true)])
data class ReferencePhotoEntity(
    @PrimaryKey val id: String,
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
