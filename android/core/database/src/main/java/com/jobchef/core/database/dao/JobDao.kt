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
    @Query("SELECT * FROM jobs WHERE status = 'UNSEEN' ORDER BY createdAt ASC LIMIT 20")
    fun getUnseenJobs(): Flow<List<JobEntity>>

    /** SAVED jobs only, newest first — the Saved Opportunities screen. */
    @Query("SELECT * FROM jobs WHERE status = 'SAVED' ORDER BY createdAt DESC")
    fun getSavedJobs(): Flow<List<JobEntity>>

    /**
     * Keyword search across title, company, snippet, and skills using SQL LIKE.
     *
     * NOTE: FTS5 full-text search is deferred to Phase 7 due to a Room 2.8.4 + KSP
     * limitation with content-backed FTS entities. This LIKE query is functionally
     * correct for the current data-set size and will be replaced with
     * `jobs_fts MATCH :query` once the KSP issue is resolved.
     *
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)

    @Query("UPDATE jobs SET status = :status WHERE id = :jobId")
    suspend fun updateJobStatus(jobId: String, status: JobStatus)

    @Query("DELETE FROM jobs WHERE status != 'SAVED'")
    suspend fun clearNonSavedJobs()
}
