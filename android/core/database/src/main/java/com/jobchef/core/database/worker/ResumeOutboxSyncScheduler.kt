package com.jobchef.core.database.worker

import androidx.work.WorkManager

class ResumeOutboxSyncScheduler(private val workManager: WorkManager) {
    fun schedule() = ResumeOutboxSyncWorker.enqueue(workManager)
}
