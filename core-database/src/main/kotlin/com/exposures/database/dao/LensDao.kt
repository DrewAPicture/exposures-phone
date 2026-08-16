package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.LensEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LensDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(lens: LensEntity)

    @Delete
    suspend fun delete(lens: LensEntity)

    @Query("SELECT * FROM lenses ORDER BY name")
    fun getAll(): Flow<List<LensEntity>>

    @Query("SELECT * FROM lenses WHERE id = :id")
    suspend fun getById(id: String): LensEntity?
}
