package com.jobchef.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.dao.TailoredApplicationDao
import com.jobchef.core.database.dao.DecisionOutboxDao
import com.jobchef.core.database.dao.BaseResumeDao
import com.jobchef.core.database.dao.ResumeOutboxDao
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.entity.TailoredApplicationEntity
import com.jobchef.core.database.entity.DecisionOutboxEntity
import com.jobchef.core.database.entity.BaseResumeEntity
import com.jobchef.core.database.entity.ResumeOutboxEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The single Room database instance for the app.
 *
 * ### Version History
 * | Version | Change |
 * |---------|--------|
 * | 1       | Initial schema: `jobs`, `tailored_applications` |
 * | 2       | Renamed `jobs.description` to `jobs.snippet`. |
 * | 3       | Added job matching/decision fields, generated content, and decision outbox. |
 * | 4       | Added the local resume vault and resume sync outbox. |
 * | 5       | Added source resume id/version to tailored applications. |
 *
 * Schema JSON files are exported to `/schemas` and committed to version control
 * so that future migrations can be validated against the real historical schema.
 *
 * ### FTS Note
 * The finite V1 job dataset uses indexed Room records and SQL LIKE search; FTS5 is out of scope.
 */
@Database(
    entities = [
        JobEntity::class,
        TailoredApplicationEntity::class,
        DecisionOutboxEntity::class,
        BaseResumeEntity::class,
        ResumeOutboxEntity::class
    ],
    version = 5,
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
    abstract fun decisionOutboxDao(): DecisionOutboxDao
    abstract fun baseResumeDao(): BaseResumeDao
    abstract fun resumeOutboxDao(): ResumeOutboxDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN salaryCurrency TEXT")
                db.execSQL("ALTER TABLE jobs ADD COLUMN matchScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE jobs ADD COLUMN matchMethod TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE jobs ADD COLUMN decisionRevision INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE jobs ADD COLUMN inCurrentBatch INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN resumeContentJson TEXT")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN coverLetter TEXT")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN generationVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN errorCode TEXT")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN errorMessage TEXT")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN demoMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS decision_outbox (
                        idempotencyKey TEXT NOT NULL PRIMARY KEY,
                        jobId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        revision INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        attempts INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_decision_outbox_createdAt ON decision_outbox(createdAt)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS base_resumes (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        sourceUri TEXT NOT NULL,
                        sourceText TEXT NOT NULL,
                        sanitizedText TEXT NOT NULL,
                        contactName TEXT,
                        contactEmail TEXT,
                        contactPhone TEXT,
                        contactLocation TEXT,
                        active INTEGER NOT NULL,
                        remoteId TEXT,
                        remoteVersion INTEGER,
                        syncPending INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_base_resumes_active ON base_resumes(active)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS resume_outbox (
                        idempotencyKey TEXT NOT NULL PRIMARY KEY,
                        operation TEXT NOT NULL,
                        resumeId TEXT,
                        remoteResumeId TEXT,
                        createdAt INTEGER NOT NULL,
                        attempts INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }

        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN resumeId TEXT")
                db.execSQL("ALTER TABLE tailored_applications ADD COLUMN resumeVersion INTEGER")
            }
        }
    }

    @RenameColumn(
        tableName = "jobs",
        fromColumnName = "description",
        toColumnName = "snippet"
    )
    class Migration1To2 : AutoMigrationSpec
}
