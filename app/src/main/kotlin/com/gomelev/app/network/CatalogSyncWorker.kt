package com.gomelev.app.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gomelev.app.GomelEvApp
import kotlinx.coroutines.CancellationException

class CatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? GomelEvApp ?: return Result.success()
            app.repository.syncLiveWebData()
            val snapshot = app.repository.snapshot.value
            if (snapshot.syncLogs.any { it.ok }) Result.success() else Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
