package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.CameraBodyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraBodyDao {

    /** Create-or-update, matched by [CameraBodyEntity.id] — phone owns this entity, so one save action covers both. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(body: CameraBodyEntity)

    @Delete
    suspend fun delete(body: CameraBodyEntity)

    @Query("SELECT * FROM camera_bodies ORDER BY name")
    fun getAll(): Flow<List<CameraBodyEntity>>

    @Query("SELECT * FROM camera_bodies WHERE id = :id")
    suspend fun getById(id: String): CameraBodyEntity?
}
