@file:Suppress("DEPRECATION")

package com.example.calendario

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.tasks.await

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        LogCollector.addLog(">>> TRABAJADOR: Iniciando respaldo automático...")
        val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. Verificar si el autoguardado está activo
        val isAutoBackupEnabled = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false)
        if (!isAutoBackupEnabled) {
            LogCollector.addLog(">>> TRABAJADOR: Respaldo desactivado en ajustes. Abortando.")
            return Result.success()
        }

        // 2. Intentar inicio de sesión silencioso para refrescar el token
        LogCollector.addLog(">>> TRABAJADOR: Refrescando sesión de Google...")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        val account = try {
            googleSignInClient.silentSignIn().await()
        } catch (e: Exception) {
            LogCollector.addLog(">>> TRABAJADOR: Error en silentSignIn: ${e.message}")
            null
        }

        if (account == null) {
            LogCollector.addLog(">>> TRABAJADOR: No se pudo obtener cuenta válida (token caducado o vínculo roto).")
            return Result.failure()
        }

        // 3. Sincronización Incremental (Bajar + Mezclar + Subir)
        LogCollector.addLog(">>> TRABAJADOR: Sincronizando con Drive...")
        val driveHelper = GoogleDriveHelper(context, account)
        val success = driveHelper.syncHistoryWithDrive()

        return if (success) {
            val now = System.currentTimeMillis()
            appPrefs.edit { putLong(AppConstants.KEY_LAST_BACKUP_TIME, now) }
            LogCollector.addLog(">>> TRABAJADOR: ¡ÉXITO! Copia completada.")
            Result.success()
        } else {
            LogCollector.addLog(">>> TRABAJADOR: ERROR en la subida a Drive. Reintentando luego...")
            Result.retry()
        }
    }
}
