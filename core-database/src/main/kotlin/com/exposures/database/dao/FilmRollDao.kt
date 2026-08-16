package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.FilmRollEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmRollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(roll: FilmRollEntity)

    @Delete
    suspend fun delete(roll: FilmRollEntity)

    @Query("SELECT * FROM film_rolls ORDER BY name")
    fun getAll(): Flow<List<FilmRollEntity>>

    @Query("SELECT * FROM film_rolls WHERE id = :id")
    suspend fun getById(id: String): FilmRollEntity?
}
