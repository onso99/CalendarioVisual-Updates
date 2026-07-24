@file:Suppress("DEPRECATION")

package com.example.calendario

import android.content.Context
import android.util.Log
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
            val remoteContent = downloadFileContent(backupFileName)
            
            // Si hay datos remotos, los procesamos para fusión incremental antes de subir el nuevo estado
            if (remoteContent != null) {
                val remoteJson = JSONObject(remoteContent)
                // Importamos SOLO Notas y Eventos de forma silenciosa para fusionar con lo local
                BackupManager.importFullBackupFromJson(
                    context, remoteJson, 
                    restorePrefs = false, restoreHolidays = false, 
                    restoreNotes = true, restoreEvents = true
                )
            }

            // Creamos el nuevo paquete con los datos fusionados (o nuevos si no había remotos)
            val fullBackupJson = BackupManager.createFullBackupJson(context)
            
            // Calculamos estadísticas para el resultado
            val totalEvents = fullBackupJson.optJSONArray("calendar_history")?.length() ?: 0
            val totalNotes = fullBackupJson.optJSONArray("daily_notes")?.length() ?: 0
            
            val jsonString = fullBackupJson.toString()
            val sizeBytes = jsonString.toByteArray(Charsets.UTF_8).size.toLong()

            // Guardamos temporalmente para subir
            val tempFile = java.io.File(context.cacheDir, "temp_backup.json")
            tempFile.writeText(jsonString)
            
            uploadFileToDrive(backupFileName, tempFile)
            tempFile.delete()

            clearDeletedEventIds(context)
            
            SyncResult(
                totalEvents = totalEvents + totalNotes, 
                deletedCount = 0, // El motor de fusión ya limpió, informamos total resultante
                success = true,
                sizeBytes = sizeBytes
            )
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en la sincronización unificada", e)
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
            val content = downloadFileContent(backupFileName) ?: return@withContext false
            val json = JSONObject(content)
            
            // Delegamos toda la lógica al motor maestro
            BackupManager.importFullBackupFromJson(
                context, json, restorePrefs, restoreHolidays, restoreNotes, restoreEvents
            )
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en restauración selectiva nube", e)
            false
        }
    }

    private suspend fun downloadFileContent(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName'")
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

    private suspend fun uploadFileToDrive(fileName: String, localFile: java.io.File) = withContext(Dispatchers.IO) {
        if (!localFile.exists()) return@withContext
        val result = driveService.files().list().setSpaces("appDataFolder").setQ("name = '$fileName'").execute()
        val fileMetadata = File().apply { name = fileName; parents = Collections.singletonList("appDataFolder") }
        val mediaContent = FileContent("application/json", localFile)
        if (result.files.isNullOrEmpty()) driveService.files().create(fileMetadata, mediaContent).execute()
        else driveService.files().update(result.files[0].id, null, mediaContent).execute()
    }
}
