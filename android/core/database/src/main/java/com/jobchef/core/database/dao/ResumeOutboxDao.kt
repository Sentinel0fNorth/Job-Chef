package com.jobchef.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jobchef.core.database.entity.ResumeOutboxEntity

@Dao
interface ResumeOutboxDao {
    @Query("SELECT * FROM resume_outbox ORDER BY createdAt, rowid LIMIT 100")
    suspend fun getPending(): List<ResumeOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outbox: ResumeOutboxEntity)

    @Query("DELETE FROM resume_outbox WHERE idempotencyKey = :key")
    suspend fun delete(key: String)

    @Query("UPDATE resume_outbox SET attempts = attempts + 1 WHERE idempotencyKey = :key")
    suspend fun incrementAttempts(key: String)

    @Query("DELETE FROM resume_outbox WHERE resumeId = :resumeId AND operation = 'UPLOAD'")
    suspend fun removePendingUpload(resumeId: String)

    @Query("DELETE FROM resume_outbox WHERE remoteResumeId = :remoteResumeId AND operation = 'SET_ACTIVE'")
    suspend fun removePendingActivation(remoteResumeId: String)

    @Query("DELETE FROM resume_outbox WHERE resumeId = :resumeId AND operation = 'SET_ACTIVE'")
    suspend fun removePendingActivationForLocalResume(resumeId: String)
}
