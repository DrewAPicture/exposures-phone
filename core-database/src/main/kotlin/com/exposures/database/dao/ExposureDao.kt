package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.exposures.database.entity.ExposureEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phone-side mirror of the watch's exposures — never user-edited here, only replaced wholesale
 * whenever a fresh sync arrives from the watch (see [replaceAll]).
 */
@Dao
interface ExposureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exposures: List<ExposureEntity>)

    @Query("DELETE FROM exposures")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(exposures: List<ExposureEntity>) {
        deleteAll()
        upsertAll(exposures)
    }

    @Query("SELECT * FROM exposures WHERE filmRollId = :filmRollId ORDER BY frameNumber")
    fun getByRoll(filmRollId: String): Flow<List<ExposureEntity>>

    @Query("SELECT * FROM exposures ORDER BY filmRollId, frameNumber")
    fun getAll(): Flow<List<ExposureEntity>>

    @Query("SELECT * FROM exposures WHERE id = :id")
    suspend fun getById(id: String): ExposureEntity?
}
