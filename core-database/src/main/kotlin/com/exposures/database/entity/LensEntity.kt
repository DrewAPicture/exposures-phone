package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus

@Entity(tableName = "lenses")
data class LensEntity(
    @PrimaryKey val id: String,
    val name: String,
    val minAperture: Double,
    val maxAperture: Double,
    val stopIncrement: StopIncrement,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
