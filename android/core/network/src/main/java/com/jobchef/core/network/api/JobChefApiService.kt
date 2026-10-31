package com.jobchef.core.network.api

import com.jobchef.core.network.model.AdzunaResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Spring Boot backend that proxies Adzuna.
 * The backend shields the Adzuna API keys and enforces rate-limiting (Bucket4j).
 * The Android client authenticates to the backend with a Bearer token per SRS §3.3.
 */
interface JobChefApiService {

    /**
     * Fetches a curated batch of 15–20 jobs from the backend.
     * The [query] and [location] are user preferences set during onboarding.
     */
    @GET("api/v1/jobs")
    suspend fun getJobBatch(
        @Query("what") query: String,
        @Query("where") location: String,
        @Query("results_per_page") pageSize: Int = 20
    ): AdzunaResponse

    /**
     * Triggers the AI tailoring pipeline for a saved job.
     * The backend publishes a Pub/Sub event which invokes Gemini asynchronously.
     */
    @GET("api/v1/applications/{jobId}/status")
    suspend fun getApplicationStatus(
        @Path("jobId") jobId: String
    ): ApplicationStatusResponse

    /**
     * Sends the selected resume text along with a target jobId to prompt backend tailoring.
     * Returns the generated list of tailored bullet points and custom cover letter.
     */
    @POST("v1/api/applications/generate")
    suspend fun generateApplication(
        @Body request: GenerateApplicationRequest
    ): GenerateApplicationResponse
}

data class ApplicationStatusResponse(
    val jobId: String,
    val status: String,
    val outputPdfUrl: String?
)

@Serializable
data class GenerateApplicationRequest(
    val jobId: String,
    val resumeText: String
)

@Serializable
data class GenerateApplicationResponse(
    val tailoredBullets: List<String>,
    val coverLetter: String
)
