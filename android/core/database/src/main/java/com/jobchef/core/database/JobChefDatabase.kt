package com.jobchef.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.dao.TailoredApplicationDao
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.entity.TailoredApplicationEntity

/**
 * The single Room database instance for the app.
 *
 * ### Version History
 * | Version | Change |
 * |---------|--------|
 * | 1       | Initial schema: `jobs`, `tailored_applications` |
 * | 2       | Added `snippet` column to `jobs` (replaces full `description` field
 * |         | for bandwidth efficiency). Status column is stored as the enum name string. |
 *
 * Schema JSON files are exported to `/schemas` and committed to version control
 * so that future migrations can be validated against the real historical schema.
 *
 * ### FTS Note
 * Full-text search via FTS5 virtual tables is deferred to Phase 7 due to a known
 * Room 2.8.4 + KSP limitation with content-backed FTS entities in the same
 * compilation unit. For Phase 6, [JobDao.searchJobs] uses SQL LIKE queries.
 */
@Database(
    entities = [
        JobEntity::class,
        TailoredApplicationEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = JobChefDatabase.Migration1To2::class
        )
    ]
)
@TypeConverters(Converters::class)
abstract class JobChefDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun tailoredApplicationDao(): TailoredApplicationDao

    @RenameColumn(
        tableName = "jobs",
        fromColumnName = "description",
        toColumnName = "snippet"
    )
    class Migration1To2 : AutoMigrationSpec
}
