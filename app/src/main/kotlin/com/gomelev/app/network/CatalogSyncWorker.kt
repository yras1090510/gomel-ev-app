package com.gomelev.app.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gomelev.app.GomelEvApp

class CatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? GomelEvApp ?: return Result.success()
            app.repository.syncLiveWebData()
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
