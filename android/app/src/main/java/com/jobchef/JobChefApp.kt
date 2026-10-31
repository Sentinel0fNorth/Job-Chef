package com.jobchef

import android.app.Application
import androidx.work.Configuration
import com.jobchef.core.database.di.databaseModule
import com.jobchef.core.network.di.networkModule
import com.jobchef.feature.discovery.di.discoveryModule
import com.jobchef.feature.onboarding.di.onboardingModule
import com.jobchef.feature.resume.di.resumeModule
import com.jobchef.feature.saved.di.savedModule
import com.jobchef.feature.tailor.di.tailorModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class JobChefApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JobChefApp)
            workManagerFactory()
            modules(
                // core
                networkModule,
                databaseModule,
                // feature presentation modules
                onboardingModule,
                discoveryModule,
                resumeModule,
                savedModule,
                tailorModule
            )
        }
    }
}