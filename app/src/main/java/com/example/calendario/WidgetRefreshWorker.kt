package com.example.calendario

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            WidgetStateManager.refreshWithCurrentEvents(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
