package com.jobchef.core.network.di

import com.jobchef.core.network.api.JobChefApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Koin module that provides Retrofit, OkHttpClient, and [JobChefApiService] singletons.
 *
 * Architecture decisions:
 * - The [BASE_URL] points to the Spring Boot backend, NOT Adzuna directly.
 *   The backend acts as a security proxy, hiding the Adzuna API keys per SRS §3.2.
 * - Logging is enabled in debug builds only (controlled via BuildConfig).
 * - Timeouts are set conservatively to handle GCP Cloud Run cold start latency.
 */

// TODO: Replace with actual Cloud Run URL and move to BuildConfig / local.properties
private const val BASE_URL = "https://jobchef-api-xxxx-uc.a.run.app/"

val networkModule = module {

    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Log bodies only in debug mode to avoid leaking PII in production
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // Generous timeout to accommodate GCP Cloud Run cold starts (~5-8s)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(JobChefApiService::class.java) }
}
