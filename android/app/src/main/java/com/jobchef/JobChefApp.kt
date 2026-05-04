package com.jobchef

import android.app.Application
import com.jobchef.core.database.di.databaseModule
import com.jobchef.core.network.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class JobChefApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JobChefApp)
            modules(
                // core
                networkModule,
                databaseModule,
                // feature presentation modules will be added here
            )
        }
    }
}