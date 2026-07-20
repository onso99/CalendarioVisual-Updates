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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class GoogleDriveHelper(private val context: Context, account: GoogleSignInAccount) {

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
     * Motor de Sincronización Incremental (Download-Merge-Upload)
     */
    suspend fun syncHistoryWithDrive(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Descargar copia actual de Drive
            Log.d("DriveHelper", "Descargando copia de Drive para fusionar...")
            val remoteContent = downloadHistoryContent()
            val remoteEvents = if (remoteContent != null) {
                val type = object : TypeToken<List<FestivoDto>>() {}.type
                val dtos: List<FestivoDto> = Gson().fromJson(remoteContent, type) ?: emptyList()
                dtos.mapNotNull { it.toFestivo() }
            } else {
                emptyList()
            }

            // 2. Cargar copia local
            val localEvents = loadHistoryFromDisk(context)

            // 3. Fusión Maestra (Incremental)
            val (mergedEvents, purgedCount) = mergeHistoryLists(context, localEvents, remoteEvents)

            // 4. Guardar resultado localmente
            saveHistoryToDisk(context, mergedEvents)

            // 5. Subir resultado final a Drive
            val historyFile = context.getFileStreamPath("calendar_history_v2.json")
            if (!historyFile.exists()) return@withContext SyncResult(0, 0, false)

            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'calendar_history_backup.json'")
                .execute()
            val existingFiles = result.files

            val fileMetadata = File().apply {
                name = "calendar_history_backup.json"
                parents = Collections.singletonList("appDataFolder")
            }
            val mediaContent = FileContent("application/json", historyFile)

            if (existingFiles.isNullOrEmpty()) {
                driveService.files().create(fileMetadata, mediaContent).execute()
            } else {
                driveService.files().update(existingFiles[0].id, null, mediaContent).execute()
            }

            // Limpiar lista de borrados tras subida exitosa
            clearDeletedEventIds(context)
            Log.d("DriveHelper", "Sincronización incremental completada con éxito")
            SyncResult(mergedEvents.size, purgedCount, true)
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en la sincronización incremental", e)
            SyncResult(0, 0, false)
        }
    }

    /**
     * Descarga el histórico de Google Drive y lo devuelve como String
     */
    private suspend fun downloadHistoryContent(): String? = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'calendar_history_backup.json'")
                .execute()
            val files = result.files

            if (files.isNullOrEmpty()) return@withContext null

            val driveFileId = files[0].id
            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(driveFileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error descargando contenido de Drive", e)
            null
        }
    }

    /**
     * Descarga el histórico de Google Drive y reemplaza la local (Restauración clásica)
     */
    suspend fun downloadHistoryFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = downloadHistoryContent() ?: return@withContext false
            context.openFileOutput("calendar_history_v2.json", Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            Log.d("DriveHelper", "Archivo histórico reemplazado por copia de Drive")
            true
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error reemplazando histórico local", e)
            false
        }
    }
}
