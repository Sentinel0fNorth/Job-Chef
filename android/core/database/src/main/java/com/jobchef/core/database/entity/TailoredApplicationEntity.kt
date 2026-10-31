package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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
    val resumeId: String? = null,
    val resumeVersion: Int? = null,
    val status: String,
    val outputPdfUrl: String?,
    val resumeContentJson: String? = null,
    val coverLetter: String? = null,
    @ColumnInfo(defaultValue = "0") val generationVersion: Int = 0,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    @ColumnInfo(defaultValue = "0") val demoMode: Boolean = false,
    val createdAt: Long
)
