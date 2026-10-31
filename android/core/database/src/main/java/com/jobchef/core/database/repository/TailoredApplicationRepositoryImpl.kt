package com.jobchef.core.database.repository

import com.jobchef.core.database.dao.TailoredApplicationDao
import com.jobchef.core.database.entity.TailoredApplicationEntity
import com.jobchef.core.domain.model.GenerationStatus
import com.jobchef.core.domain.model.TailoredApplication
import com.jobchef.core.domain.repository.TailoredApplicationRepository
import com.jobchef.core.network.api.JobChefApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TailoredApplicationRepositoryImpl(
    private val dao: TailoredApplicationDao,
    private val apiService: JobChefApiService
) : TailoredApplicationRepository {

    override fun getAllApplications(): Flow<List<TailoredApplication>> =
        dao.getAllApplications().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getApplicationByJobId(jobId: String): TailoredApplication? =
        dao.getByJobId(jobId)?.toDomain()

    override suspend fun syncApplicationStatus(jobId: String) {
        val response = apiService.getApplicationStatus(jobId)
        dao.updateStatus(jobId, response.status, response.outputPdfUrl)
    }

    private fun TailoredApplicationEntity.toDomain() = TailoredApplication(
        id = id,
        jobId = jobId,
        // Safely map to GenerationStatus enum, falling back to PENDING if database value is unexpected
        status = runCatching { GenerationStatus.valueOf(status) }.getOrDefault(GenerationStatus.PENDING),
        outputPdfUrl = outputPdfUrl,
        createdAt = createdAt
    )
}
