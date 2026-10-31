package com.jobchef.core.database.repository

import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.mapper.toEntity
import com.jobchef.core.domain.model.Job
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.domain.model.WorkMode
import com.jobchef.core.domain.repository.JobRepository
import com.jobchef.core.network.api.JobChefApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of [JobRepository].
 * Applies the offline-first strategy: the UI always reads from the local Room cache,
 * and network data flows in via [refreshJobBatch].
 *
 * [status] conversion is handled transparently by [com.jobchef.core.database.Converters]
 * so no manual [JobStatus.valueOf] / [JobStatus.name] calls are needed here.
 */
class JobRepositoryImpl(
    private val jobDao: JobDao,
    private val apiService: JobChefApiService
) : JobRepository {

    override fun getJobBatch(): Flow<List<Job>> =
        jobDao.getAllJobs().map { entities -> entities.map { it.toDomain() } }

    override fun getSavedJobs(): Flow<List<Job>> =
        jobDao.getSavedJobs().map { entities -> entities.map { it.toDomain() } }

    override fun getUnseenJobs(): Flow<List<Job>> =
        jobDao.getUnseenJobs().map { entities -> entities.map { it.toDomain() } }

    override fun searchJobs(query: String): Flow<List<Job>> =
        jobDao.searchJobs(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateJobStatus(jobId: String, status: JobStatus) {
        jobDao.updateJobStatus(jobId, status)
    }

    override suspend fun refreshJobBatch(query: String, location: String) {
        val response = apiService.getJobBatch(query = query, location = location)
        jobDao.clearNonSavedJobs()
        jobDao.insertJobs(response.results.map { it.toEntity() })
    }

    // ---- Private mapping ----

    private fun JobEntity.toDomain() = Job(
        id = id,
        title = title,
        company = company,
        location = location,
        description = snippet,   // The snippet IS the on-device description for V1
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        redirectUrl = redirectUrl,
        skills = skills.split(",").filter { it.isNotBlank() },
        // Safely map to WorkMode enum, falling back to UNKNOWN if an unexpected string is stored
        workMode = runCatching { WorkMode.valueOf(workMode) }.getOrDefault(WorkMode.UNKNOWN),
        status = status,
        createdAt = createdAt
    )
}
