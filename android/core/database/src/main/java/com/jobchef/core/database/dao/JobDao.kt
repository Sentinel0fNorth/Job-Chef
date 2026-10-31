package com.jobchef.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.domain.model.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    /** All jobs, newest first. Used for admin/debug views. */
    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    /** UNSEEN jobs only, oldest first, capped at 20 — the Swipe Deck feed. */
    @Query("SELECT * FROM jobs WHERE status = 'UNSEEN' AND inCurrentBatch = 1 ORDER BY createdAt ASC, id ASC LIMIT 20")
    fun getUnseenJobs(): Flow<List<JobEntity>>

    /** SAVED jobs only, newest first — the Saved Opportunities screen. */
    @Query("SELECT * FROM jobs WHERE status = 'SAVED' ORDER BY createdAt DESC")
    fun getSavedJobs(): Flow<List<JobEntity>>

    /**
     * Keyword search across title, company, snippet, and skills using SQL LIKE.
     * The query wraps [query] with `%` wildcards — callers should pass the raw keyword.
     */
    @Query(
        """
        SELECT * FROM jobs
        WHERE title LIKE '%' || :query || '%'
           OR company LIKE '%' || :query || '%'
           OR snippet LIKE '%' || :query || '%'
           OR skills LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """
    )
    fun searchJobs(query: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJob(jobId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingJobs(jobs: List<JobEntity>)

    @Query("""
        UPDATE jobs SET title = :title, company = :company, location = :location,
            snippet = :snippet, salaryMin = :salaryMin, salaryMax = :salaryMax,
            salaryCurrency = :salaryCurrency, redirectUrl = :redirectUrl,
            skills = :skills, workMode = :workMode, matchScore = :matchScore,
            matchMethod = :matchMethod, inCurrentBatch = 1, createdAt = :createdAt
        WHERE id = :id
    """)
    suspend fun updateListing(
        id: String,
        title: String,
        company: String,
        location: String,
        snippet: String,
        salaryMin: Double?,
        salaryMax: Double?,
        salaryCurrency: String?,
        redirectUrl: String,
        skills: String,
        workMode: String,
        matchScore: Int,
        matchMethod: String,
        createdAt: Long
    )

    @Query("UPDATE jobs SET inCurrentBatch = 0")
    suspend fun clearCurrentBatch()

    @Query("UPDATE jobs SET status = :status, decisionRevision = :revision WHERE id = :jobId")
    suspend fun updateJobDecision(jobId: String, status: JobStatus, revision: Long)
}
