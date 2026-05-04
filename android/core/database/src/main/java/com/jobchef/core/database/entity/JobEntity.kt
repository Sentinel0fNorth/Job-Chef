package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that mirrors the [com.jobchef.core.domain.model.Job] domain model.
 * Stored locally for offline-first access. UUIDv4 strings are used as primary
 * keys to prevent IDOR attacks and align with the backend schema.
 */
@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val company: String,
    val location: String,
    val description: String,
    val salaryMin: Double?,
    val salaryMax: Double?,
    val redirectUrl: String,
    /** Comma-separated list of extracted skills. */
    val skills: String,
    val workMode: String,
    val status: String,
    val createdAt: Long
)
