package com.jobchef.core.network.api

import com.jobchef.core.network.model.ApplicationResponseDto
import com.jobchef.core.network.model.DecisionSyncRequestDto
import com.jobchef.core.network.model.DecisionSyncResponseDto
import com.jobchef.core.network.model.DeviceCredentialsResponse
import com.jobchef.core.network.model.DeviceRegistrationRequest
import com.jobchef.core.network.model.ResumeResponseDto
import com.jobchef.core.network.model.ResumeUploadRequestDto
import com.jobchef.core.network.model.SearchRequestDto
import com.jobchef.core.network.model.SearchResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface JobChefApiService {
    @POST("api/v1/auth/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): DeviceCredentialsResponse

    @POST("api/v1/auth/refresh")
    suspend fun refreshDevice(): DeviceCredentialsResponse

    @POST("api/v1/auth/revoke")
    suspend fun revokeDevice()

    @POST("api/v1/discovery/search")
    suspend fun getJobBatch(@Body request: SearchRequestDto): SearchResponseDto

    @POST("api/v1/resumes")
    suspend fun uploadResume(@Body request: ResumeUploadRequestDto): ResumeResponseDto

    @GET("api/v1/resumes")
    suspend fun getResumes(): List<ResumeResponseDto>

    @PUT("api/v1/resumes/{resumeId}/active")
    suspend fun setActiveResume(@Path("resumeId") resumeId: String)

    @DELETE("api/v1/resumes/{resumeId}")
    suspend fun deleteResume(@Path("resumeId") resumeId: String)

    @POST("api/v1/decisions/sync")
    suspend fun syncDecisions(@Body request: DecisionSyncRequestDto): DecisionSyncResponseDto

    @GET("api/v1/applications/by-job/{jobId}")
    suspend fun getApplication(@Path("jobId") jobId: String): ApplicationResponseDto

    @POST("api/v1/applications/by-job/{jobId}/regenerate")
    suspend fun regenerate(@Path("jobId") jobId: String)

    @DELETE("api/v1/applications/by-job/{jobId}")
    suspend fun deleteApplication(@Path("jobId") jobId: String)
}
