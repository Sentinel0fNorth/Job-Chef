package com.jobchef.core.database.repository

import androidx.room.withTransaction
import com.jobchef.core.database.JobChefDatabase
import com.jobchef.core.database.dao.BaseResumeDao
import com.jobchef.core.database.dao.DecisionOutboxDao
import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.entity.DecisionOutboxEntity
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.mapper.toEntity
import com.jobchef.core.database.worker.ResumeOutboxSyncScheduler
import com.jobchef.core.domain.model.Job
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.domain.model.WorkMode
import com.jobchef.core.domain.repository.JobRepository
import com.jobchef.core.network.api.JobChefApiService
import com.jobchef.core.network.model.SearchRequestDto
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JobRepositoryImpl(
    private val database: JobChefDatabase,
    private val jobDao: JobDao,
    private val resumeDao: BaseResumeDao,
    private val decisionOutbox: DecisionOutboxDao,
    private val apiService: JobChefApiService,
    private val outboxScheduler: ResumeOutboxSyncScheduler
) : JobRepository {
    override fun getJobBatch(): Flow<List<Job>> =
        jobDao.getAllJobs().map { entities -> entities.map { it.toDomain() } }

    override fun getSavedJobs(): Flow<List<Job>> =
        jobDao.getSavedJobs().map { entities -> entities.map { it.toDomain() } }

    override fun observeActiveResumeTitle(): Flow<String?> = resumeDao.observeActiveTitle()

    override fun getUnseenJobs(): Flow<List<Job>> =
        jobDao.getUnseenJobs().map { entities -> entities.map { it.toDomain() } }

    override fun searchJobs(query: String): Flow<List<Job>> =
        jobDao.searchJobs(query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateJobStatus(jobId: String, status: JobStatus) {
        database.withTransaction {
            val current = jobDao.getJob(jobId) ?: return@withTransaction
            if (current.status == status) return@withTransaction
            val revision = current.decisionRevision + 1
            val key = UUID.randomUUID().toString()
            jobDao.updateJobDecision(jobId, status, revision)
            decisionOutbox.insert(
                DecisionOutboxEntity(
                    idempotencyKey = key,
                    jobId = jobId,
                    status = status.name,
                    revision = revision,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        outboxScheduler.schedule()
    }

    override suspend fun refreshJobBatch(
        query: String,
        location: String,
        keywords: String,
        workMode: String?
    ) {
        val response = apiService.getJobBatch(
            SearchRequestDto(
                role = query.trim(),
                location = location.trim(),
                keywords = keywords.trim(),
                workMode = workMode,
                limit = 20
            )
        )
        require(response.jobs.size <= 20) { "The server returned more jobs than the V1 search limit." }
        database.withTransaction {
            jobDao.clearCurrentBatch()
            response.jobs.forEach { dto ->
                val mapped = dto.toEntity()
                if (jobDao.getJob(mapped.id) == null) {
                    jobDao.insertMissingJobs(listOf(mapped))
                } else {
                    jobDao.updateListing(
                        id = mapped.id,
                        title = mapped.title,
                        company = mapped.company,
                        location = mapped.location,
                        snippet = mapped.snippet,
                        salaryMin = mapped.salaryMin,
                        salaryMax = mapped.salaryMax,
                        salaryCurrency = mapped.salaryCurrency,
                        redirectUrl = mapped.redirectUrl,
                        skills = mapped.skills,
                        workMode = mapped.workMode,
                        matchScore = mapped.matchScore,
                        matchMethod = mapped.matchMethod,
                        createdAt = mapped.createdAt
                    )
                }
            }
        }
    }

    private fun JobEntity.toDomain() = Job(
        id = id,
        title = title,
        company = company,
        location = location,
        description = snippet,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        salaryCurrency = salaryCurrency,
        redirectUrl = redirectUrl,
        skills = skills.split(",").filter(String::isNotBlank),
        workMode = runCatching { WorkMode.valueOf(workMode) }.getOrDefault(WorkMode.UNKNOWN),
        matchScore = matchScore,
        matchMethod = matchMethod,
        status = status,
        decisionRevision = decisionRevision,
        createdAt = createdAt
    )
}
