package com.example.calendario

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val BACKUP_WORK_NAME = "google_drive_backup_work"

    fun scheduleBackup(context: Context, frequency: String) {
        val repeatInterval = getInterval(frequency)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(repeatInterval, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        // REPLACE se usa cuando el usuario cambia explícitamente la configuración
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            backupRequest
        )
    }

    /**
     * Asegura que el trabajo esté programado sin reiniciar el contador de tiempo.
     * Ideal para llamar en el inicio de la app.
     */
    fun ensureBackupScheduled(context: Context) {
        val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val isAutoBackupEnabled = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false)
        if (!isAutoBackupEnabled) return

        val frequency = appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "daily") ?: "daily"
        val repeatInterval = getInterval(frequency)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(repeatInterval, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        // KEEP asegura que si ya existe un trabajo con este nombre, NO lo toque (no reinicia las 24h)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    private fun getInterval(frequency: String): Long {
        return when (frequency) {
            "daily" -> 1L
            "weekly" -> 7L
            "monthly" -> 30L
            else -> 1L
        }
    }

    fun cancelBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
    }
}
