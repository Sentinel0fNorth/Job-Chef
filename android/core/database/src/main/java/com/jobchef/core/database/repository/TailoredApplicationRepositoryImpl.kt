package com.jobchef.core.database.repository

import androidx.room.withTransaction
import com.jobchef.core.database.JobChefDatabase
import com.jobchef.core.database.dao.DecisionOutboxDao
import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.dao.ResumeOutboxDao
import com.jobchef.core.database.dao.TailoredApplicationDao
import com.jobchef.core.database.entity.DecisionOutboxEntity
import com.jobchef.core.database.entity.ResumeOutboxEntity
import com.jobchef.core.database.entity.TailoredApplicationEntity
import com.jobchef.core.domain.model.GenerationStatus
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.domain.model.TailoredApplication
import com.jobchef.core.domain.repository.TailoredApplicationRepository
import com.jobchef.core.network.api.JobChefApiService
import com.jobchef.core.database.worker.ResumeOutboxSyncScheduler
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TailoredApplicationRepositoryImpl(
    private val database: JobChefDatabase,
    private val dao: TailoredApplicationDao,
    private val jobs: JobDao,
    private val decisionOutbox: DecisionOutboxDao,
    private val resumeOutbox: ResumeOutboxDao,
    private val apiService: JobChefApiService,
    private val gson: Gson,
    private val outboxScheduler: ResumeOutboxSyncScheduler
) : TailoredApplicationRepository {

    override fun getAllApplications(): Flow<List<TailoredApplication>> =
        dao.getAllApplications().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getApplicationByJobId(jobId: String): TailoredApplication? =
        dao.getByJobId(jobId)?.toDomain()

    override suspend fun syncApplicationStatus(jobId: String) {
        val response = apiService.getApplication(jobId)
        dao.insert(
            TailoredApplicationEntity(
                id = response.id,
                jobId = jobId,
                resumeId = response.resumeId,
                resumeVersion = response.resumeVersion,
                status = response.status,
                outputPdfUrl = null,
                resumeContentJson = response.resumeContent?.let(gson::toJson),
                coverLetter = response.coverLetter,
                generationVersion = response.generationVersion,
                errorCode = response.errorCode,
                errorMessage = response.errorMessage,
                demoMode = response.demoMode,
                createdAt = java.time.Instant.parse(response.updatedAt).toEpochMilli()
            )
        )
    }

    override suspend fun deleteApplication(jobId: String) {
        database.withTransaction {
            val job = jobs.getJob(jobId)
            if (job != null && job.status != JobStatus.UNSEEN) {
                val revision = job.decisionRevision + 1
                jobs.updateJobDecision(jobId, JobStatus.UNSEEN, revision)
                decisionOutbox.insert(
                    DecisionOutboxEntity(
                        idempotencyKey = UUID.randomUUID().toString(),
                        jobId = jobId,
                        status = JobStatus.UNSEEN.name,
                        revision = revision,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            resumeOutbox.insert(
                ResumeOutboxEntity(
                    idempotencyKey = UUID.randomUUID().toString(),
                    operation = OP_DELETE_APPLICATION,
                    resumeId = null,
                    remoteId = jobId,
                    createdAt = System.currentTimeMillis()
                )
            )
            dao.deleteByJobId(jobId)
        }
        outboxScheduler.schedule()
    }

    private fun TailoredApplicationEntity.toDomain() = TailoredApplication(
        id = id,
        jobId = jobId,
        resumeId = resumeId,
        resumeVersion = resumeVersion,
        status = runCatching { GenerationStatus.valueOf(status) }.getOrDefault(GenerationStatus.FAILED),
        outputPdfUrl = outputPdfUrl,
        resumeContentJson = resumeContentJson,
        coverLetter = coverLetter,
        generationVersion = generationVersion,
        errorCode = errorCode,
        errorMessage = errorMessage,
        demoMode = demoMode,
        createdAt = createdAt
    )

    private companion object {
        const val OP_DELETE_APPLICATION = "DELETE_APPLICATION"
    }
}
