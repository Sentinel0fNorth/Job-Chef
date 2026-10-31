package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "resume_outbox")
data class ResumeOutboxEntity(
    @PrimaryKey val idempotencyKey: String,
    val operation: String,
    val resumeId: String?,
    @ColumnInfo(name = "remoteResumeId") val remoteId: String?,
    val createdAt: Long,
    val attempts: Int = 0
)
