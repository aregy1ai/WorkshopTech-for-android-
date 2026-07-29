package com.workshoptech.data.backup

import android.content.Context
import androidx.work.*
import com.workshoptech.WorkshopTechApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as WorkshopTechApp
            val repository = app.repository
            val cases = repository.observeCases(null).first()
            val customers = repository.observeCustomers(null).first()
            BackupManager.exportFull(applicationContext, cases, customers, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            BackupManager.cleanupOldBackups(applicationContext)
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }

    companion object {
        private const val WORK_NAME = "weekly_backup"
        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiresCharging(true).setRequiresBatteryNotLow(true).build()
            val request = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS).setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
        fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
    }
}
