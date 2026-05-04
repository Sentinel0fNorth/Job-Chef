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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(application: TailoredApplicationEntity)

    @Query("UPDATE tailored_applications SET status = :status, outputPdfUrl = :pdfUrl WHERE jobId = :jobId")
    suspend fun updateStatus(jobId: String, status: String, pdfUrl: String?)
}
