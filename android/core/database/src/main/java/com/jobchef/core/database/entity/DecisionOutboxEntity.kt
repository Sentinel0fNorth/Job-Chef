package com.jobchef.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decision_outbox",
    indices = [Index(value = ["createdAt"])]
)
data class DecisionOutboxEntity(
    @PrimaryKey val idempotencyKey: String,
    val jobId: String,
    val status: String,
    val revision: Long,
    val createdAt: Long,
    val attempts: Int = 0
)
