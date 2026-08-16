package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus

@Entity(tableName = "camera_bodies")
data class CameraBodyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturer: String,
    val availableShutterSpeeds: List<ShutterSpeed>,
    val hasBulbMode: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
