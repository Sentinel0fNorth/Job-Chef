package com.jobchef.core.database.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jobchef.core.database.dao.BaseResumeDao
import com.jobchef.core.database.dao.DecisionOutboxDao
import com.jobchef.core.database.dao.ResumeOutboxDao
import com.jobchef.core.database.entity.ResumeOutboxEntity
import com.jobchef.core.network.api.JobChefApiService
import com.jobchef.core.network.model.ResumeUploadRequestDto
import com.jobchef.core.network.model.DecisionRequestDto
import com.jobchef.core.network.model.DecisionSyncRequestDto
import com.jobchef.core.network.session.DeviceCredentialStore
import com.jobchef.core.domain.model.JobStatus
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.UUID
import retrofit2.HttpException

class ResumeOutboxSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val outbox: ResumeOutboxDao,
    private val decisions: DecisionOutboxDao,
    private val resumes: BaseResumeDao,
    private val api: JobChefApiService,
    private val credentials: DeviceCredentialStore
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (credentials.accessToken().isNullOrBlank()) return Result.failure()
        return try {
            while (true) {
                val batch = outbox.getPending()
                if (batch.isNotEmpty()) {
                    for (event in batch) {
                        outbox.incrementAttempts(event.idempotencyKey)
                        when (event.operation) {
                            OP_UPLOAD -> upload(event.resumeId ?: return Result.failure(), event.idempotencyKey)
                            OP_SET_ACTIVE -> activate(event.resumeId, event.remoteId)
                            OP_DELETE -> {
                                try {
                                    api.deleteResume(event.remoteId ?: return Result.failure())
                                } catch (error: HttpException) {
                                    if (error.code() != 404) throw error
                                }
                            }
                            OP_DELETE_APPLICATION -> {
                                try {
                                    api.deleteApplication(event.remoteId ?: return Result.failure())
                                } catch (error: HttpException) {
                                    if (error.code() != 404) throw error
                                }
                            }
                            else -> return Result.failure()
                        }
                        outbox.delete(event.idempotencyKey)
                    }
                    continue
                }
                if (!syncDecisions()) break
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: HttpException) {
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncDecisions(): Boolean {
        val pending = decisions.getPending()
        if (pending.isEmpty()) return false
        pending.forEach { decisions.incrementAttempts(it.idempotencyKey) }
        val response = api.syncDecisions(
            DecisionSyncRequestDto(
                decisions = pending.map { event ->
                    DecisionRequestDto(
                        jobId = event.jobId,
                        idempotencyKey = event.idempotencyKey,
                        revision = event.revision,
                        status = JobStatus.valueOf(event.status).name
                    )
                }
            )
        )
        val rejectedJobIds = response.errors.mapNotNull { error ->
            error.substringBefore(':').takeIf(String::isNotBlank)
        }.toSet()
        val completedKeys = pending
            .filterNot { it.jobId in rejectedJobIds }
            .map { it.idempotencyKey }
        if (completedKeys.isNotEmpty()) decisions.delete(completedKeys)
        if (rejectedJobIds.isNotEmpty()) throw IOException("The server rejected one or more saved-job decisions.")
        return true
    }

    private suspend fun upload(localId: String, clientKey: String) {
        val resume = resumes.getById(localId) ?: return
        val result = api.uploadResume(
            ResumeUploadRequestDto(
                clientKey = clientKey,
                displayName = resume.title,
                sanitizedText = resume.sanitizedText,
                active = resume.active
            )
        )
        val updated = resumes.markSynced(localId, result.id, result.version)
        if (updated == 0) {
            outbox.insert(
                ResumeOutboxEntity(
                    idempotencyKey = UUID.randomUUID().toString(),
                    operation = OP_DELETE,
                    resumeId = null,
                    remoteId = result.id,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun activate(localId: String?, remoteId: String?) {
        val resolvedRemoteId = remoteId
            ?: localId?.let { resumes.getById(it)?.remoteId }
            ?: return
        api.setActiveResume(resolvedRemoteId)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "resume-outbox-sync"
        private const val OP_UPLOAD = "UPLOAD"
        private const val OP_SET_ACTIVE = "SET_ACTIVE"
        private const val OP_DELETE = "DELETE"
        private const val OP_DELETE_APPLICATION = "DELETE_APPLICATION"

        fun enqueue(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<ResumeOutboxSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
