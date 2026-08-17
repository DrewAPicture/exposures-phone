package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.FilmBackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmBackDao {

    /** Create-or-update, matched by [FilmBackEntity.id] — phone owns this entity, so one save action covers both. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(filmBack: FilmBackEntity)

    @Delete
    suspend fun delete(filmBack: FilmBackEntity)

    @Query("SELECT * FROM film_backs ORDER BY name")
    fun getAll(): Flow<List<FilmBackEntity>>

    @Query("SELECT * FROM film_backs WHERE id = :id")
    suspend fun getById(id: String): FilmBackEntity?
}
