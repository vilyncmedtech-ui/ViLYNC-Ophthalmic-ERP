package com.vilync.ophthalmicerp.feature.backup.background

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Orchestrates the scheduling of automatic nightly backups.
 */
object BackupScheduler {

    private const val WORK_NAME = "NIGHTLY_ERP_BACKUP"

    /**
     * Schedules a periodic backup task to run once every 24 hours.
     * Enforces constraints: Network must be connected.
     */
    fun scheduleNightlyBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<NightlyBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS) // GRACE PERIOD: Prevent immediate empty backup after setup
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            backupRequest
        )
    }

    fun cancelNightlyBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
