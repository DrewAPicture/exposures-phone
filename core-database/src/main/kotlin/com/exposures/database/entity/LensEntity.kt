package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus

@Entity(
    tableName = "lenses",
    foreignKeys = [
        ForeignKey(
            entity = CameraBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["cameraBodyId"],
        ),
    ],
    indices = [Index("cameraBodyId")],
)
data class LensEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cameraBodyId: String?,
    val minAperture: Double,
    val maxAperture: Double,
    val stopIncrement: StopIncrement,
    val referencePhotoZoomRatio: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
