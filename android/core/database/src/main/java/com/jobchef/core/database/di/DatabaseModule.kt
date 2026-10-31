package com.jobchef.core.database.di

import androidx.room.Room
import com.jobchef.core.database.JobChefDatabase
import com.jobchef.core.database.repository.JobRepositoryImpl
import com.jobchef.core.database.repository.TailoredApplicationRepositoryImpl
import com.jobchef.core.database.session.SessionManager
import com.jobchef.core.database.worker.SyncJobsWorker
import com.jobchef.core.database.worker.ResumeOutboxSyncWorker
import com.jobchef.core.domain.repository.JobRepository
import com.jobchef.core.domain.repository.TailoredApplicationRepository
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module providing the Room database, DAOs, and repository bindings.
 *
 * Uses the lambda overload for the database since it requires a factory-method call
 * (Room.databaseBuilder). DAOs and repositories use constructor-reference shorthand.
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            get(),
            JobChefDatabase::class.java,
            "jobchef.db"
        ).addMigrations(
            JobChefDatabase.MIGRATION_2_3,
            JobChefDatabase.MIGRATION_3_4,
            JobChefDatabase.MIGRATION_4_5
        ).build()
    }

    single { get<JobChefDatabase>().jobDao() }
    single { get<JobChefDatabase>().tailoredApplicationDao() }
    single { get<JobChefDatabase>().decisionOutboxDao() }
    single { get<JobChefDatabase>().baseResumeDao() }
    single { get<JobChefDatabase>().resumeOutboxDao() }

    singleOf(::JobRepositoryImpl) { bind<JobRepository>() }
    singleOf(::TailoredApplicationRepositoryImpl) { bind<TailoredApplicationRepository>() }

    worker { SyncJobsWorker(get(), get(), get()) }
    worker { ResumeOutboxSyncWorker(get(), get(), get(), get(), get(), get(), get()) }
    single { androidx.work.WorkManager.getInstance(get()) }
    single { ResumeOutboxSyncScheduler(get()) }

    single { SessionManager(get()) }
}
