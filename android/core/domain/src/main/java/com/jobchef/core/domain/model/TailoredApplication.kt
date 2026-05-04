package com.jobchef.core.domain.model

/**
 * Domain model for a tailored resume/cover-letter generation job.
 * Created by the backend when a user saves a job (status = SAVED).
 * The AI pipeline (Pub/Sub + Gemini) populates the [outputPdfUrl] on completion.
 */
data class TailoredApplication(
    val id: String,           // UUIDv4
    val jobId: String,        // FK → Job.id
    val status: GenerationStatus,
    val outputPdfUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GenerationStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }
