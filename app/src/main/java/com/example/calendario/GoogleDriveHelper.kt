@file:Suppress("DEPRECATION")

package com.example.calendario

import android.content.Context
import android.util.Log
import com.example.calendario.database.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Collections

class GoogleDriveHelper(private val context: Context, account: GoogleSignInAccount) {

    private val backupFileName = "calendario_full_backup.json"

    private val driveService: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_APPDATA))
        credential.selectedAccount = account.account
        
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(AppConstants.APP_SIGNATURE).build()
    }

    /**
     * Sincronización Dual Unificada:
     * Descarga el paquete -> Fusiona Notas y Eventos -> Sube el paquete resultante.
     */
    suspend fun syncHistoryWithDrive(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val remoteContent = downloadFileContent()
            
            // Si hay datos remotos, los procesamos para fusión incremental antes de subir el nuevo estado
            if (remoteContent != null) {
                val remoteJson = JSONObject(remoteContent)
                // Importamos SOLO Notas y Eventos de forma silenciosa para fusionar con lo local
                BackupManager.importFullBackupFromJson(
                    context, remoteJson, 
                    restorePrefs = false, restoreHolidays = false, 
                    restoreNotes = true, restoreEvents = true,
                    source = BackupSource.DRIVE,
                    logEntry = false,
                    isSync = true // PROTECCIÓN (v3.3.06): Fusionar en lugar de reemplazar borrados
                )
                
                // Tras la fusión silenciosa en Room, volcamos de nuevo al disco legado (para no romper el flujo)
                // pero Room ya manda en la App.
            }

            // Creamos el nuevo paquete ligero con los metadatos de la App
            val selectedIds = SettingsManager.getSelectedCalendarIds(context)
            val favoriteId = SettingsManager.getFavoriteCalendarId(context)

            val fullBackupJson = BackupManager.createFullBackupJson(context, selectedIds, favoriteId)
            
            // Calculamos estadísticas para el resultado (Contando solo notas activas v3.4.08)
            val database = AppDatabase.getDatabase(context)
            val totalNotes = database.calendarDao().getAllNotesSync().count { !it.isDeleted && it.content.isNotBlank() }
            val colorsCount = fullBackupJson.optJSONObject("period_colors_by_adn")?.length() ?: 0
            val alarmOffsetsCount = fullBackupJson.optJSONObject("alarm_offsets_by_adn")?.length() ?: 0
            
            val jsonString = fullBackupJson.toString()
            val sizeBytes = jsonString.toByteArray(Charsets.UTF_8).size.toLong()

            // Guardamos temporalmente para subir
            val tempFile = java.io.File(context.cacheDir, "temp_backup.json")
            tempFile.writeText(jsonString)
            
            uploadFileToDrive(tempFile)
            tempFile.delete()

            // Registrar en historial
            BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                timestamp = System.currentTimeMillis(),
                source = BackupSource.DRIVE,
                action = BackupAction.SYNC,
                isSuccess = true,
                eventsCount = colorsCount + alarmOffsetsCount,
                notesCount = totalNotes,
                includePrefs = true,
                sizeBytes = sizeBytes
            ))
            
            SyncResult(
                totalEvents = colorsCount + alarmOffsetsCount + totalNotes, 
                deletedCount = 0,
                success = true,
                sizeBytes = sizeBytes
            )
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en la sincronización unificada", e)
            BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                timestamp = System.currentTimeMillis(),
                source = BackupSource.DRIVE,
                action = BackupAction.SYNC,
                isSuccess = false,
                technicalError = e.message
            ))
            SyncResult(0, 0, false, 0L)
        }
    }

    /**
     * Restauración Selectiva desde la Nube (Unificada)
     */
    suspend fun downloadAndRestoreSelective(
        restorePrefs: Boolean,
        restoreHolidays: Boolean,
        restoreNotes: Boolean,
        restoreEvents: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = downloadFileContent() ?: return@withContext false
            val json = JSONObject(content)
            
            // Delegamos toda la lógica al motor maestro
            BackupManager.importFullBackupFromJson(
                context, json, restorePrefs, restoreHolidays, restoreNotes, restoreEvents, BackupSource.DRIVE
            )
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en restauración selectiva nube", e)
            false
        }
    }

    private suspend fun downloadFileContent(): String? = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$backupFileName'")
                .execute()
            val files = result.files
            if (files.isNullOrEmpty()) return@withContext null

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(files[0].id).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun uploadFileToDrive(localFile: java.io.File) = withContext(Dispatchers.IO) {
        if (!localFile.exists()) return@withContext
        val result = driveService.files().list().setSpaces("appDataFolder").setQ("name = '$backupFileName'").execute()
        val fileMetadata = File().apply { name = backupFileName; parents = Collections.singletonList("appDataFolder") }
        val mediaContent = FileContent("application/json", localFile)
        if (result.files.isNullOrEmpty()) driveService.files().create(fileMetadata, mediaContent).execute()
        else driveService.files().update(result.files[0].id, null, mediaContent).execute()
    }
}
