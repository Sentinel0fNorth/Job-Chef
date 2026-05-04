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
 * Applies the offline-first strategy: UI always reads from Room,
 * network data flows in via [refreshJobBatch].
 */
class JobRepositoryImpl(
    private val jobDao: JobDao,
    private val apiService: JobChefApiService
) : JobRepository {

    override fun getJobBatch(): Flow<List<Job>> =
        jobDao.getAllJobs().map { entities -> entities.map { it.toDomain() } }

    override fun getSavedJobs(): Flow<List<Job>> =
        jobDao.getSavedJobs().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateJobStatus(jobId: String, status: JobStatus) {
        jobDao.updateJobStatus(jobId, status.name)
    }

    override suspend fun refreshJobBatch() {
        // TODO: Read user preferences (query/location) from DataStore
        val response = apiService.getJobBatch(query = "Software Engineer", location = "")
        jobDao.clearNonSavedJobs()
        jobDao.insertJobs(response.results.map { it.toEntity() })
    }

    private fun JobEntity.toDomain() = Job(
        id = id,
        title = title,
        company = company,
        location = location,
        description = description,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        redirectUrl = redirectUrl,
        skills = skills.split(",").filter { it.isNotBlank() },
        workMode = WorkMode.valueOf(workMode),
        status = JobStatus.valueOf(status),
        createdAt = createdAt
    )
}
