package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.FilmFormat
import com.exposures.model.RollStatus
import com.exposures.model.SyncStatus

@Entity(
    tableName = "film_rolls",
    foreignKeys = [
        ForeignKey(
            entity = CameraBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["cameraBodyId"],
        ),
    ],
    indices = [Index("cameraBodyId")],
)
data class FilmRollEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filmStock: String,
    val boxSpeedIso: Int,
    val format: FilmFormat,
    val cameraBodyId: String,
    val targetFrameCount: Int,
    val status: RollStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
