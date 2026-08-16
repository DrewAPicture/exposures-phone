package com.exposures.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.exposures.database.dao.CameraBodyDao
import com.exposures.database.dao.ExposureDao
import com.exposures.database.dao.FilmRollDao
import com.exposures.database.dao.LensDao
import com.exposures.database.entity.CameraBodyEntity
import com.exposures.database.entity.ExposureEntity
import com.exposures.database.entity.FilmRollEntity
import com.exposures.database.entity.LensEntity

@Database(
    entities = [
        CameraBodyEntity::class,
        LensEntity::class,
        FilmRollEntity::class,
        ExposureEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ExposuresDatabase : RoomDatabase() {
    abstract fun cameraBodyDao(): CameraBodyDao
    abstract fun lensDao(): LensDao
    abstract fun filmRollDao(): FilmRollDao
    abstract fun exposureDao(): ExposureDao

    companion object {
        const val DATABASE_NAME = "exposures.db"
    }
}
