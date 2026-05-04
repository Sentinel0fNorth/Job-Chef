package com.jobchef.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * Adzuna API response wrapper for the job search endpoint:
 * GET /v1/api/jobs/{country}/search/1
 */
data class AdzunaResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("results") val results: List<AdzunaJob>
)

data class AdzunaJob(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("company") val company: AdzunaCompany,
    @SerializedName("location") val location: AdzunaLocation,
    @SerializedName("description") val description: String,
    @SerializedName("salary_min") val salaryMin: Double?,
    @SerializedName("salary_max") val salaryMax: Double?,
    @SerializedName("redirect_url") val redirectUrl: String,
    @SerializedName("created") val created: String
)

data class AdzunaCompany(
    @SerializedName("display_name") val displayName: String
)

data class AdzunaLocation(
    @SerializedName("display_name") val displayName: String
)
