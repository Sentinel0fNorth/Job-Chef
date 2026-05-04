package com.jobchef.core.database.di

import androidx.room.Room
import com.jobchef.core.database.JobChefDatabase
import com.jobchef.core.database.repository.JobRepositoryImpl
import com.jobchef.core.database.repository.TailoredApplicationRepositoryImpl
import com.jobchef.core.domain.repository.JobRepository
import com.jobchef.core.domain.repository.TailoredApplicationRepository
import org.koin.dsl.module
import org.koin.dsl.bind

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
        ).build()
    }

    single { get<JobChefDatabase>().jobDao() }
    single { get<JobChefDatabase>().tailoredApplicationDao() }

    singleOf(::JobRepositoryImpl) { bind<JobRepository>() }
    singleOf(::TailoredApplicationRepositoryImpl) { bind<TailoredApplicationRepository>() }
}
