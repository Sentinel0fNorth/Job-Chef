package com.jobchef.core.database

import androidx.room.TypeConverter
import com.jobchef.core.domain.model.JobStatus

/**
 * Room [TypeConverter] registrations for the [JobChefDatabase].
 *
 * Converts the [JobStatus] enum to/from its String name for SQLite storage.
 * Using [enumValueOf] with a fallback to [JobStatus.UNSEEN] makes the converter
 * resilient to potential future schema divergence without crashing at runtime.
 */
class Converters {

    @TypeConverter
    fun fromJobStatus(status: JobStatus): String = status.name

    @TypeConverter
    fun toJobStatus(value: String): JobStatus =
        runCatching { enumValueOf<JobStatus>(value) }.getOrDefault(JobStatus.UNSEEN)
}
