package com.jobchef.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jobchef.core.database.entity.BaseResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BaseResumeDao {
    @Query("SELECT * FROM base_resumes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BaseResumeEntity>>

    @Query("SELECT * FROM base_resumes WHERE active = 1 LIMIT 1")
    suspend fun getActive(): BaseResumeEntity?

    @Query("SELECT title FROM base_resumes WHERE active = 1 LIMIT 1")
    fun observeActiveTitle(): Flow<String?>

    @Query("SELECT * FROM base_resumes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BaseResumeEntity?

    @Query("SELECT * FROM base_resumes WHERE remoteId = :remoteId AND remoteVersion = :version LIMIT 1")
    suspend fun getByRemoteVersion(remoteId: String, version: Int): BaseResumeEntity?

    @Query("SELECT * FROM base_resumes WHERE id != :excludedId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getNewestExcluding(excludedId: String): BaseResumeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(resume: BaseResumeEntity)

    @Query("UPDATE base_resumes SET active = 0")
    suspend fun clearActive()

    @Query("UPDATE base_resumes SET active = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("""
        UPDATE base_resumes SET syncPending = 0, remoteId = :remoteId, remoteVersion = :remoteVersion
        WHERE id = :id
    """)
    suspend fun markSynced(id: String, remoteId: String, remoteVersion: Int): Int

    @Query("UPDATE base_resumes SET syncPending = 1 WHERE id = :id")
    suspend fun markPending(id: String)

    @Query("DELETE FROM base_resumes WHERE id = :id")
    suspend fun delete(id: String)
}
