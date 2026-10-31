package com.jobchef.core.database.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jobchef.core.domain.repository.JobRepository

class SyncJobsWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: JobRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val role = inputData.getString(KEY_ROLE) ?: "Software Engineer"
        val location = inputData.getString(KEY_LOCATION) ?: ""
        val keywords = inputData.getString(KEY_KEYWORDS) ?: ""
        val workMode = inputData.getString(KEY_WORK_MODE)
        return try {
            repository.refreshJobBatch(query = role, location = location, keywords = keywords, workMode = workMode)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_ROLE = "KEY_ROLE"
        const val KEY_LOCATION = "KEY_LOCATION"
        const val KEY_KEYWORDS = "KEY_KEYWORDS"
        const val KEY_WORK_MODE = "KEY_WORK_MODE"
    }
}
