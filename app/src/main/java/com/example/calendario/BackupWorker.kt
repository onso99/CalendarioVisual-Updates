@file:Suppress("DEPRECATION")

package com.example.calendario

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. Verificar si el autoguardado está activo
        val isAutoBackupEnabled = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false)
        if (!isAutoBackupEnabled) return Result.success()

        // 2. Obtener la cuenta de Google
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.e("BackupWorker", "No hay cuenta de Google vinculada")
            return Result.failure()
        }

        // 3. Subir archivo
        val driveHelper = GoogleDriveHelper(context, account)
        val success = driveHelper.uploadHistoryFile()

        return if (success) {
            appPrefs.edit { putLong(AppConstants.KEY_LAST_BACKUP_TIME, System.currentTimeMillis()) }
            Log.d("BackupWorker", "Copia automática completada con éxito")
            Result.success()
        } else {
            Log.e("BackupWorker", "Error en la copia automática")
            Result.retry()
        }
    }
}
