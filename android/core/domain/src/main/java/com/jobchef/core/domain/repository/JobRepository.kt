package com.jobchef.core.domain.repository

import com.jobchef.core.domain.model.Job
import com.jobchef.core.domain.model.JobStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accessing and mutating [Job] data.
 * Implemented in :core:database to enforce Clean Architecture separation.
 * The :feature modules depend only on this interface, never the Room implementation.
 */
interface JobRepository {

    /**
     * Returns the current batch of jobs to display in the Swipe Deck.
     * Emits updates reactively whenever the local Room cache changes.
     */
    fun getJobBatch(): Flow<List<Job>>

    /**
     * Returns saved jobs (status = SAVED) for the Resume Vault screen.
     */
    fun getSavedJobs(): Flow<List<Job>>

    /**
     * Returns only UNSEEN jobs for the Swipe Deck, capped at 20 for mobile performance.
     */
    fun getUnseenJobs(): Flow<List<Job>>

    /**
     * Keyword search over job title, company, snippet, and skills.
     * Currently implemented as a SQL LIKE query; will be upgraded to FTS5 in Phase 7.
     */
    fun searchJobs(query: String): Flow<List<Job>>

    /**
     * Updates the swipe decision for a job.
     * A SAVED status triggers the AI pipeline on the backend.
     */
    suspend fun updateJobStatus(jobId: String, status: JobStatus)

    /**
     * Fetches a fresh batch from the backend and replaces the local cache.
     * Respects the JIT batch limit (15-20 jobs) from the CONCEPT doc.
     */
    suspend fun refreshJobBatch(query: String, location: String)
}
