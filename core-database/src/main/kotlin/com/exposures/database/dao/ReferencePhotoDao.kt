package com.exposures.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exposures.database.entity.ReferencePhotoEntity

@Dao
interface ReferencePhotoDao {

    /** One reference photo per exposure — a retry replaces the earlier attempt's row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(photo: ReferencePhotoEntity)

    @Query("SELECT * FROM reference_photos WHERE exposureId = :exposureId")
    suspend fun getByExposureId(exposureId: String): ReferencePhotoEntity?
}
