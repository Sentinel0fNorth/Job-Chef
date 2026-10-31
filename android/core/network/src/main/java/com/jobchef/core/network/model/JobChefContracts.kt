package com.jobchef.core.network.model

import com.google.gson.annotations.SerializedName

data class DeviceRegistrationRequest(
    @SerializedName("enrollmentCode") val enrollmentCode: String
)

data class DeviceCredentialsResponse(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("expiresAt") val expiresAt: String
)

data class SearchRequestDto(
    @SerializedName("role") val role: String,
    @SerializedName("location") val location: String,
    @SerializedName("keywords") val keywords: String,
    @SerializedName("country") val country: String = "in",
    @SerializedName("workMode") val workMode: String? = null,
    @SerializedName("limit") val limit: Int = 20
)

data class SearchResponseDto(
    @SerializedName("jobs") val jobs: List<JobResponseDto>,
    @SerializedName("attribution") val attribution: String,
    @SerializedName("fetchedAt") val fetchedAt: String
)

data class JobResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("company") val company: String,
    @SerializedName("location") val location: String,
    @SerializedName("description") val description: String,
    @SerializedName("salaryMin") val salaryMin: Double?,
    @SerializedName("salaryMax") val salaryMax: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("redirectUrl") val redirectUrl: String,
    @SerializedName("skills") val skills: List<String>,
    @SerializedName("workMode") val workMode: String,
    @SerializedName("matchScore") val matchScore: Int = 0,
    @SerializedName("matchMethod") val matchMethod: String
)

data class DecisionRequestDto(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("idempotencyKey") val idempotencyKey: String,
    @SerializedName("revision") val revision: Long,
    @SerializedName("status") val status: String
)

data class DecisionSyncRequestDto(
    @SerializedName("decisions") val decisions: List<DecisionRequestDto>
)

data class DecisionSyncResponseDto(
    @SerializedName("accepted") val accepted: Int,
    @SerializedName("stale") val stale: Int,
    @SerializedName("errors") val errors: List<String>
)

data class ResumeUploadRequestDto(
    @SerializedName("clientKey") val clientKey: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("sanitizedText") val sanitizedText: String,
    @SerializedName("active") val active: Boolean
)

data class ResumeResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("version") val version: Int,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("createdAt") val createdAt: String
)

data class ApplicationResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("jobId") val jobId: String,
    @SerializedName("resumeId") val resumeId: String?,
    @SerializedName("resumeVersion") val resumeVersion: Int?,
    @SerializedName("status") val status: String,
    @SerializedName("generationVersion") val generationVersion: Int,
    @SerializedName("errorCode") val errorCode: String?,
    @SerializedName("errorMessage") val errorMessage: String?,
    @SerializedName("resumeContent") val resumeContent: ResumeContentDto?,
    @SerializedName("coverLetter") val coverLetter: String?,
    @SerializedName("demoMode") val demoMode: Boolean,
    @SerializedName("previewAvailable") val previewAvailable: Boolean = false,
    @SerializedName("updatedAt") val updatedAt: String
)

data class ResumeContentDto(
    @SerializedName("headline") val headline: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("skills") val skills: List<String>,
    @SerializedName("experience") val experience: List<ExperienceDto>,
    @SerializedName("education") val education: List<EducationDto>
)

data class ExperienceDto(
    @SerializedName("employer") val employer: String,
    @SerializedName("role") val role: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("bullets") val bullets: List<String>
)

data class EducationDto(
    @SerializedName("qualification") val qualification: String,
    @SerializedName("institution") val institution: String,
    @SerializedName("dates") val dates: String
)
