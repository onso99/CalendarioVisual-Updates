package com.example.calendario

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BackupScheduler {
    private const val BACKUP_WORK_NAME = "google_drive_backup_work"
    private const val RECOVERY_WORK_NAME = "google_drive_recovery_work"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun scheduleBackup(context: Context, frequency: String) {
        if (frequency == "manual") {
            cancelBackup(context)
            return
        }

        val repeatInterval = getInterval(frequency)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(repeatInterval, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }

    fun ensureBackupScheduled(context: Context) {
        scope.launch {
            try {
                val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                val frequency = appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "manual") ?: "manual"
                val isAutoBackupEnabled = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false) && (frequency != "manual")
                
                if (!isAutoBackupEnabled) return@launch

                val workManager = WorkManager.getInstance(context)
                val lastBackup = appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_TIME, 0L)
                val intervalMillis = TimeUnit.DAYS.toMillis(getInterval(frequency))
                
                // Si hay retraso grave, lanzamos copia de rescate
                if (lastBackup != 0L) {
                    val diff = System.currentTimeMillis() - lastBackup
                    if (diff > (intervalMillis + TimeUnit.HOURS.toMillis(6))) {
                        val recoveryRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()
                        workManager.enqueueUniqueWork(RECOVERY_WORK_NAME, ExistingWorkPolicy.REPLACE, recoveryRequest)
                    }
                }

                val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(getInterval(frequency), TimeUnit.DAYS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    BACKUP_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    backupRequest
                )
            } catch (_: Exception) {
                // Error en segundo plano ignorado para no colapsar la App
            }
        }
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
        WorkManager.getInstance(context).cancelUniqueWork(RECOVERY_WORK_NAME)
    }
}
