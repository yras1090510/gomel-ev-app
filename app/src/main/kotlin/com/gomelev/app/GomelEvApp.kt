package com.gomelev.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gomelev.app.data.CatalogRepository
import com.gomelev.app.network.CatalogSyncWorker
import java.util.concurrent.TimeUnit

class GomelEvApp : Application() {

    val repository: CatalogRepository by lazy { CatalogRepository(this) }

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundSync()
    }

    fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "gomel_ev_periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
