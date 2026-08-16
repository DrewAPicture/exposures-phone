package com.exposures.database

import android.content.Context
import androidx.room.Room

/**
 * Builds the app's [ExposuresDatabase]. Keeps Room itself an implementation detail of this module.
 * Pre-release, no real user data at stake — schema bumps destructively wipe and recreate rather
 * than carrying explicit [androidx.room.migration.Migration]s.
 */
object ExposuresDatabaseProvider {
    fun create(context: Context): ExposuresDatabase = Room
        .databaseBuilder(context.applicationContext, ExposuresDatabase::class.java, ExposuresDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigration(true)
        .build()
}
