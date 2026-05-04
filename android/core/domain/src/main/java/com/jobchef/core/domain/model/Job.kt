package com.jobchef.core.domain.model

/**
 * Core domain model representing a job listing fetched from the Adzuna API.
 * UUIDs are used for all primary keys to prevent IDOR attacks.
 */
data class Job(
    val id: String,                  // UUIDv4 from backend
    val title: String,
    val company: String,
    val location: String,
    val description: String,
    val salaryMin: Double?,
    val salaryMax: Double?,
    val redirectUrl: String,
    val skills: List<String>,        // Extracted deterministically by backend
    val workMode: WorkMode,          // Extracted deterministically by backend
    val status: JobStatus = JobStatus.UNSEEN,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WorkMode { REMOTE, HYBRID, ON_SITE, UNKNOWN }

enum class JobStatus { UNSEEN, SAVED, SKIPPED }
