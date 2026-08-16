package com.exposures.database

import android.content.Context
import androidx.room.Room

/** Builds the app's [ExposuresDatabase]. Keeps Room itself an implementation detail of this module. */
object ExposuresDatabaseProvider {
    fun create(context: Context): ExposuresDatabase =
        Room.databaseBuilder(context.applicationContext, ExposuresDatabase::class.java, ExposuresDatabase.DATABASE_NAME).build()
}
