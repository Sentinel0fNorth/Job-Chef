package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "base_resumes",
    indices = [Index(value = ["active"])]
)
data class BaseResumeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUri: String,
    val sourceText: String,
    val sanitizedText: String,
    val contactName: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val contactLocation: String?,
    val active: Boolean,
    val remoteId: String?,
    val remoteVersion: Int?,
    val syncPending: Boolean,
    val createdAt: Long
)
