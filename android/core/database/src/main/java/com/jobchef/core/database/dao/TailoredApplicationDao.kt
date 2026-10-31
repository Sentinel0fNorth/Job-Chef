package com.jobchef.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jobchef.core.database.entity.TailoredApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TailoredApplicationDao {

    @Query("SELECT * FROM tailored_applications ORDER BY createdAt DESC")
    fun getAllApplications(): Flow<List<TailoredApplicationEntity>>

    @Query("SELECT * FROM tailored_applications WHERE jobId = :jobId LIMIT 1")
    suspend fun getByJobId(jobId: String): TailoredApplicationEntity?

    @Query("DELETE FROM tailored_applications WHERE resumeId = :remoteResumeId")
    suspend fun deleteByRemoteResumeId(remoteResumeId: String)

    @Query("DELETE FROM tailored_applications WHERE jobId = :jobId")
    suspend fun deleteByJobId(jobId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(application: TailoredApplicationEntity)

    @Query("""
        UPDATE tailored_applications
        SET status = :status, outputPdfUrl = NULL, resumeContentJson = :resumeContentJson,
            coverLetter = :coverLetter, generationVersion = :generationVersion,
            errorCode = :errorCode, errorMessage = :errorMessage, demoMode = :demoMode
        WHERE jobId = :jobId
    """)
    suspend fun updateFromServer(
        jobId: String,
        status: String,
        resumeContentJson: String?,
        coverLetter: String?,
        generationVersion: Int,
        errorCode: String?,
        errorMessage: String?,
        demoMode: Boolean
    )
}
