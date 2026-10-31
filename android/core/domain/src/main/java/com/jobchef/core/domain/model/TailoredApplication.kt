package com.jobchef.core.domain.model

/**
 * Domain model for a tailored resume/cover-letter generation job.
 * Created by the backend when a user saves a job (status = SAVED).
 * The AI pipeline (Pub/Sub + Gemini) populates the [outputPdfUrl] on completion.
 */
data class TailoredApplication(
    val id: String,           // UUIDv4
    val jobId: String,        // FK → Job.id
    val resumeId: String? = null,
    val resumeVersion: Int? = null,
    val status: GenerationStatus,
    val outputPdfUrl: String? = null,
    val resumeContentJson: String? = null,
    val coverLetter: String? = null,
    val generationVersion: Int = 0,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val demoMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GenerationStatus {
    WAITING_FOR_RESUME, QUEUED, PROCESSING, READY, FAILED, QUOTA_BLOCKED, CANCELLED
}
