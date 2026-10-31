package com.jobchef.feature.resume.data

import androidx.room.withTransaction
import com.jobchef.core.database.JobChefDatabase
import com.jobchef.core.database.dao.BaseResumeDao
import com.jobchef.core.database.dao.ResumeOutboxDao
import com.jobchef.core.database.dao.TailoredApplicationDao
import com.jobchef.core.database.entity.BaseResumeEntity
import com.jobchef.core.database.entity.ResumeOutboxEntity
import com.jobchef.core.database.worker.ResumeOutboxSyncScheduler
import com.jobchef.feature.resume.presentation.vault.ResumeDraft
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ResumeVaultRepository(
    private val database: JobChefDatabase,
    private val resumes: BaseResumeDao,
    private val applications: TailoredApplicationDao,
    private val outbox: ResumeOutboxDao,
    private val syncScheduler: ResumeOutboxSyncScheduler
) {
    fun observeResumes(): Flow<List<BaseResumeEntity>> = resumes.observeAll()

    suspend fun saveDraft(draft: ResumeDraft, active: Boolean) {
        database.withTransaction {
            if (active) resumes.clearActive()
            resumes.insert(
                BaseResumeEntity(
                    id = draft.id,
                    title = draft.title,
                    sourceUri = draft.sourceUri,
                    sourceText = draft.sourceText,
                    sanitizedText = draft.sanitizedText,
                    contactName = draft.contactName,
                    contactEmail = draft.contactEmail,
                    contactPhone = draft.contactPhone,
                    contactLocation = draft.contactLocation,
                    active = active,
                    remoteId = null,
                    remoteVersion = null,
                    syncPending = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            outbox.insert(
                ResumeOutboxEntity(
                    idempotencyKey = draft.id,
                    operation = "UPLOAD",
                    resumeId = draft.id,
                    remoteId = null,
                    createdAt = System.currentTimeMillis()
                )
            )

            syncScheduler.schedule()
        }
    }

    suspend fun setActive(id: String) {
        database.withTransaction {
            val selected = resumes.getById(id) ?: error("The selected resume is no longer available.")
            resumes.clearActive()
            resumes.setActive(id)
            outbox.insert(
                ResumeOutboxEntity(
                    idempotencyKey = UUID.randomUUID().toString(),
                    operation = "SET_ACTIVE",
                    resumeId = id,
                    remoteId = selected.remoteId,
                    createdAt = System.currentTimeMillis()
                )
            )
            syncScheduler.schedule()
        }
    }

    suspend fun delete(id: String) {
        database.withTransaction {
            val resume = resumes.getById(id) ?: return@withTransaction
            val wasActive = resume.active
            outbox.removePendingUpload(id)
            outbox.removePendingActivationForLocalResume(id)
            if (!resume.remoteId.isNullOrBlank()) {
                outbox.removePendingActivation(resume.remoteId)
                applications.deleteByRemoteResumeId(resume.remoteId)
                outbox.insert(
                    ResumeOutboxEntity(
                        idempotencyKey = UUID.randomUUID().toString(),
                        operation = "DELETE",
                        resumeId = null,
                        remoteId = resume.remoteId,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            resumes.delete(id)
            if (wasActive) {
                val next = resumes.getNewestExcluding(id)
                if (next != null) {
                    resumes.setActive(next.id)
                    outbox.insert(
                        ResumeOutboxEntity(
                            idempotencyKey = UUID.randomUUID().toString(),
                            operation = "SET_ACTIVE",
                            resumeId = next.id,
                            remoteId = next.remoteId,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    syncScheduler.schedule()
                }
            }
        }
    }
}
