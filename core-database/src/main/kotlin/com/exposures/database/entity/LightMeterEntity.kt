package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exposures.model.LightMeterType
import com.exposures.model.SyncStatus

@Entity(tableName = "light_meters")
data class LightMeterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturer: String,
    val type: LightMeterType,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
