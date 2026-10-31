package com.jobchef.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jobchef.core.database.entity.DecisionOutboxEntity

@Dao
interface DecisionOutboxDao {
    @Query("SELECT * FROM decision_outbox ORDER BY createdAt LIMIT 100")
    suspend fun getPending(): List<DecisionOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(decision: DecisionOutboxEntity)

    @Query("DELETE FROM decision_outbox WHERE idempotencyKey IN (:keys)")
    suspend fun delete(keys: List<String>)

    @Query("UPDATE decision_outbox SET attempts = attempts + 1 WHERE idempotencyKey = :key")
    suspend fun incrementAttempts(key: String)
}
