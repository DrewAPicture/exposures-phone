package com.exposures.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.exposures.model.FilmBackType
import com.exposures.model.SyncStatus

@Entity(
    tableName = "film_backs",
    foreignKeys = [
        ForeignKey(
            entity = CameraBodyEntity::class,
            parentColumns = ["id"],
            childColumns = ["cameraBodyId"],
        ),
    ],
    indices = [Index("cameraBodyId")],
)
data class FilmBackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cameraBodyId: String,
    val type: FilmBackType,
    val availableFrameCounts: List<Int>,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
