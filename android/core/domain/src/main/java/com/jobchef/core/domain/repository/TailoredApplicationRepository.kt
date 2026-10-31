package com.jobchef.core.domain.repository

import com.jobchef.core.domain.model.TailoredApplication
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accessing tailored resume generation results.
 * Implemented in :core:database, consumed by :feature:onboarding (Resume Vault).
 */
interface TailoredApplicationRepository {

    /**
     * Returns all tailored application records ordered by creation time (newest first).
     */
    fun getAllApplications(): Flow<List<TailoredApplication>>

    /**
     * Returns the tailored application record for a specific saved job.
     */
    suspend fun getApplicationByJobId(jobId: String): TailoredApplication?

    /**
     * Syncs the latest generation status from the backend for a specific job.
     */
    suspend fun syncApplicationStatus(jobId: String)

    /**
     * Removes local generated data and queues device-owned server deletion for replay.
     */
    suspend fun deleteApplication(jobId: String)
}
