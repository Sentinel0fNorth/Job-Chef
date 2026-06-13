package com.jobchef.core.database.mapper

import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.network.model.AdzunaJob

private const val SNIPPET_MAX_CHARS = 200

/**
 * Maps a network-layer [AdzunaJob] DTO to a Room [JobEntity].
 * Skill extraction and work mode detection are done by the Spring Boot backend;
 * the Android client trusts the server-provided values from the proxied Adzuna response.
 *
 * **Snippet truncation**: Only the first [SNIPPET_MAX_CHARS] characters of the full
 * description are persisted. This preserves mobile bandwidth and on-device DB size —
 * the SwipeDeck UI only needs a short excerpt, not the full job description.
 *
 * For V1, skills and workMode default to empty/UNKNOWN since extraction happens server-side.
 * These will be populated once the backend enrichment endpoint is integrated.
 */
fun AdzunaJob.toEntity(): JobEntity = JobEntity(
    id = id,
    title = title,
    company = company.displayName,
    location = location.displayName,
    snippet = description.take(SNIPPET_MAX_CHARS),
    salaryMin = salaryMin,
    salaryMax = salaryMax,
    redirectUrl = redirectUrl,
    skills = "",               // Populated by backend enrichment (V2)
    workMode = "UNKNOWN",      // Populated by backend enrichment (V2)
    status = JobStatus.UNSEEN,
    createdAt = System.currentTimeMillis()
)
