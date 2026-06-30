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
            ExistingPeriodicWorkPolicy.UPDATE, // Android 12+: Actualiza la lógica sin reiniciar el cronómetro
            backupRequest
        )
    }

    /**
     * Asegura que el trabajo esté programado sin reiniciar el contador de tiempo.
     * Incluye auto-sanación si la tarea ha desaparecido del sistema.
     */
    fun ensureBackupScheduled(context: Context) {
        val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val isAutoBackupEnabled = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false)
        
        if (!isAutoBackupEnabled) return

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(BACKUP_WORK_NAME).get()
        
        if (workInfos.isNullOrEmpty()) {
            LogCollector.addLog(">>> AUTO-SANACIÓN: La tarea había desaparecido. Recreándola...")
        }

        val lastBackup = appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_TIME, 0L)
        val frequency = appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "daily") ?: "daily"
        val intervalMillis = TimeUnit.DAYS.toMillis(getInterval(frequency))
        
        // INSPECTOR: Detectar retrasos
        if (lastBackup != 0L) {
            val diff = System.currentTimeMillis() - lastBackup
            if (diff > (intervalMillis + TimeUnit.HOURS.toMillis(6))) { // Margen de 6h de gracia
                LogCollector.addLog(">>> ALERTA INSPECTOR: Respaldo con ${diff/3600000}h de retraso.")
            }
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(getInterval(frequency), TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        // KEEP asegura que si ya existe un trabajo con este nombre, NO lo toque (no reinicia las 24h)
        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    /**
     * Sonda de diagnóstico: Consulta el estado real de la tarea en WorkManager
     * y lo reporta al LogCollector sin alterar la ejecución.
     */
    fun debugBackupStatus(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(BACKUP_WORK_NAME).get()
        
        if (workInfos.isNullOrEmpty()) {
            LogCollector.addLog(">>> DIAGNÓSTICO: La tarea '$BACKUP_WORK_NAME' NO existe en el sistema.")
        } else {
            val info = workInfos[0]
            val state = info.state
            val runAttemptCount = info.runAttemptCount
            
            LogCollector.addLog(">>> DIAGNÓSTICO: Tarea Drive: $state | Intentos: $runAttemptCount")
            
            if (state == WorkInfo.State.ENQUEUED) {
                // En Android 12+ (WorkManager 2.7+) podemos ver si hay retrasos por restricciones
                LogCollector.addLog(">>> DIAGNÓSTICO: Esperando condiciones (Red/Batería)...")
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
    }
}
