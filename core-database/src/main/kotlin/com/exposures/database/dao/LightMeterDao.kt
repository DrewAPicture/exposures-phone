package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.LightMeterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LightMeterDao {

    /** Create-or-update, matched by [LightMeterEntity.id] — phone owns this entity, so one save action covers both. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(lightMeter: LightMeterEntity)

    @Delete
    suspend fun delete(lightMeter: LightMeterEntity)

    @Query("SELECT * FROM light_meters ORDER BY name")
    fun getAll(): Flow<List<LightMeterEntity>>

    @Query("SELECT * FROM light_meters WHERE id = :id")
    suspend fun getById(id: String): LightMeterEntity?
}
