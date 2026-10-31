package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.jobchef.core.domain.model.JobStatus

/**
 * Room entity that mirrors the [com.jobchef.core.domain.model.Job] domain model.
 * Stored locally for offline-first access. UUIDv4 strings are used as primary
 * keys to prevent IDOR attacks and align with the backend schema.
 *
 * The [snippet] field holds a pre-truncated 200-character excerpt of the job description.
 * The full description is intentionally NOT stored on-device to conserve bandwidth and
 * local DB size — only the snippet is required by the SwipeDeck UI.
 *
 * [status] is persisted via [com.jobchef.core.database.Converters] so the raw string
 * in SQLite matches the enum name exactly.
 */
@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val company: String,
    val location: String,
    /** Pre-truncated 200-character excerpt used by the Swipe Deck card UI. */
    val snippet: String,
    val salaryMin: Double?,
    val salaryMax: Double?,
    val salaryCurrency: String? = null,
    val redirectUrl: String,
    /** Comma-separated list of extracted skills. */
    val skills: String,
    val workMode: String,
    @ColumnInfo(defaultValue = "0") val matchScore: Int = 0,
    @ColumnInfo(defaultValue = "''") val matchMethod: String = "",
    /** Persisted via Converters as a string matching the enum name exactly. */
    val status: JobStatus,
    @ColumnInfo(defaultValue = "0") val decisionRevision: Long = 0,
    @ColumnInfo(defaultValue = "1") val inCurrentBatch: Boolean = true,
    val createdAt: Long
)
