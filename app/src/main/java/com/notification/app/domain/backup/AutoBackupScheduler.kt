package com.notification.app.domain.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Sprint 3 — schedules (and cancels) the periodic automatic backup.
 *
 * Frequency maps to a WorkManager repeat interval:
 *   OFF     → no work (any existing schedule is cancelled)
 *   DAILY   → every 1 day
 *   WEEKLY  → every 7 days
 *   MONTHLY → every 30 days
 *
 * Constraints honour the user's choices: "charging only" and "Wi-Fi only"
 * (UNMETERED network). WorkManager guarantees the work survives reboot and
 * process death, and defers it until the constraints are met — exactly the
 * "back up automatically, but not on mobile data / not on battery" contract.
 */
object AutoBackupScheduler {

    fun reschedule(
        context: Context,
        frequency: String,
        chargingOnly: Boolean,
        wifiOnly: Boolean
    ) {
        val wm = WorkManager.getInstance(context)

        val days = when (frequency) {
            "DAILY" -> 1L
            "WEEKLY" -> 7L
            "MONTHLY" -> 30L
            else -> 0L // OFF or unknown
        }

        if (days == 0L) {
            wm.cancelUniqueWork(BackupWorker.UNIQUE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED)
            .setRequiresCharging(chargingOnly)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(days, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        // REPLACE so changing frequency/constraints re-arms with the new plan.
        wm.enqueueUniquePeriodicWork(
            BackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.UNIQUE_WORK_NAME)
    }
}
