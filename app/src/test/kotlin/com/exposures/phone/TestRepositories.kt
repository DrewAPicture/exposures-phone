package com.exposures.phone

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.database.repository.EquipmentRepository

/** An in-memory repository for tests. Room is test-only here — see app's build.gradle.kts. */
fun createTestRepository(): EquipmentRepository {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, ExposuresDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    return EquipmentRepository(database)
}
