package com.jobchef.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.dao.TailoredApplicationDao
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.entity.TailoredApplicationEntity

/**
 * The single Room database instance for the app.
 * Version is incremented here alongside a Flyway-style migration whenever
 * the schema changes — keeping the Android schema in sync with the backend.
 */
@Database(
    entities = [JobEntity::class, TailoredApplicationEntity::class],
    version = 1,
    exportSchema = true
)
abstract class JobChefDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun tailoredApplicationDao(): TailoredApplicationDao
}
