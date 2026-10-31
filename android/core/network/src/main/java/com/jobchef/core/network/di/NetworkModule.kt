package com.jobchef.core.network.di

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.jobchef.core.network.api.JobChefApiService
import com.jobchef.core.network.model.DeviceCredentialsResponse
import com.jobchef.core.network.session.DeviceCredentialStore
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

val networkModule = module {
    single { DeviceCredentialStore(androidContext()) }
    single { Gson() }

    single {
        val credentials = get<DeviceCredentialStore>()
        val gson = get<Gson>()
        val configuredUrl = com.jobchef.core.network.BuildConfig.API_BASE_URL
        require(configuredUrl.endsWith("/")) {
            "JOB_CHEF_API_BASE_URL must end with '/'."
        }
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        val refreshAuthenticator = Authenticator { _, response ->
            if (responseCount(response) >= 2) {
                null
            } else {
                synchronized(credentials) {
                    val currentToken = credentials.accessToken() ?: return@synchronized null
                    val failedToken = response.request.header("Authorization")
                        ?.removePrefix("Bearer ")
                    if (currentToken != failedToken) {
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    } else {
                        val refreshRequest = Request.Builder()
                            .url("${configuredUrl}api/v1/auth/refresh")
                            .header("Authorization", "Bearer $currentToken")
                            .post(ByteArray(0).toRequestBody("application/json".toMediaType()))
                            .build()
                        refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                            if (refreshResponse.code == 401) credentials.clear()
                            if (!refreshResponse.isSuccessful) {
                                return@synchronized null
                            }
                            val body = refreshResponse.body?.string()
                                ?: return@synchronized null
                            val next = try {
                                gson.fromJson(body, DeviceCredentialsResponse::class.java)
                            } catch (_: JsonParseException) {
                                null
                            } ?: return@synchronized null
                            if (next.deviceId.isBlank() || next.accessToken.isBlank()) {
                                return@synchronized null
                            }
                            credentials.storeAccessToken(next.accessToken)
                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${next.accessToken}")
                                .build()
                        }
                    }
                }
            }
        }
        val authentication = Interceptor { chain ->
            val request = chain.request()
            val token = credentials.accessToken()
            val authenticatedRequest = if (token.isNullOrBlank()
                || request.url.encodedPath == "/api/v1/auth/register"
            ) {
                request
            } else {
                request.newBuilder().header("Authorization", "Bearer $token").build()
            }
            chain.proceed(authenticatedRequest)
        }
        OkHttpClient.Builder()
            .addInterceptor(authentication)
            .authenticator(refreshAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    single {
        val configuredUrl = com.jobchef.core.network.BuildConfig.API_BASE_URL
        require(configuredUrl.endsWith("/")) {
            "JOB_CHEF_API_BASE_URL must end with '/'."
        }
        Retrofit.Builder()
            .baseUrl(configuredUrl)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    single { get<Retrofit>().create(JobChefApiService::class.java) }
}

private fun responseCount(response: okhttp3.Response): Int {
    var count = 1
    var prior = response.priorResponse
    while (prior != null) {
        count++
        prior = prior.priorResponse
    }
    return count
}
