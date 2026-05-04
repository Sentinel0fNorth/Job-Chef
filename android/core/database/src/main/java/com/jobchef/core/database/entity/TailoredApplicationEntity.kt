package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for AI resume generation jobs.
 * References [JobEntity] via foreign key to ensure referential integrity.
 */
@Entity(
    tableName = "tailored_applications",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["jobId"], unique = true)]
)
data class TailoredApplicationEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val status: String,
    val outputPdfUrl: String?,
    val createdAt: Long
)
