package com.zhengyang.redbook.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        inputData.getString("key")
        return try {
            performDataSync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun performDataSync() {
        // Placeholder for actual sync work.
    }
}
