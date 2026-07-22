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
     * Motor de Sincronización Incremental (Eventos y Notas)
     */
    suspend fun syncHistoryWithDrive(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // --- 1. SINCRONIZACIÓN DE EVENTOS ---
            Log.d("DriveHelper", "Sincronizando eventos...")
            val remoteContent = downloadFileContent("calendar_history_backup.json")
            val remoteEvents = if (remoteContent != null) {
                val type = object : TypeToken<List<FestivoDto>>() {}.type
                val dtos: List<FestivoDto> = Gson().fromJson(remoteContent, type) ?: emptyList()
                dtos.mapNotNull { it.toFestivo() }
            } else emptyList()

            val localEvents = loadHistoryFromDisk(context)
            val (mergedEvents, purgedCount) = mergeHistoryLists(context, localEvents, remoteEvents)
            saveHistoryToDisk(context, mergedEvents)
            uploadFileToDrive("calendar_history_backup.json", context.getFileStreamPath("calendar_history_v2.json"))

            // --- 2. SINCRONIZACIÓN DE NOTAS ---
            Log.d("DriveHelper", "Sincronizando notas...")
            val remoteNotesContent = downloadFileContent("notes_history_backup.json")
            val remoteNotes = if (remoteNotesContent != null) {
                val type = object : TypeToken<List<DailyNoteDto>>() {}.type
                val dtos: List<DailyNoteDto> = Gson().fromJson(remoteNotesContent, type) ?: emptyList()
                dtos.mapNotNull { it.toDailyNote() }
            } else emptyList()

            val localNotes = loadNotesFromDisk(context)
            val mergedNotes = mergeNotesLists(localNotes, remoteNotes)
            saveNotesToDisk(context, mergedNotes)
            uploadFileToDrive("notes_history_backup.json", context.getFileStreamPath("notes_history.json"))

            clearDeletedEventIds(context)
            Log.d("DriveHelper", "Sincronización completa finalizada")
            SyncResult(mergedEvents.size, purgedCount, true)
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en la sincronización dual", e)
            SyncResult(0, 0, false)
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
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error descargando $fileName", e)
            null
        }
    }

    private suspend fun uploadFileToDrive(fileName: String, localFile: java.io.File) = withContext(Dispatchers.IO) {
        if (!localFile.exists()) return@withContext
        
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName'")
            .execute()
        
        val fileMetadata = File().apply {
            name = fileName
            parents = java.util.Collections.singletonList("appDataFolder")
        }
        val mediaContent = FileContent("application/json", localFile)

        if (result.files.isNullOrEmpty()) {
            driveService.files().create(fileMetadata, mediaContent).execute()
        } else {
            driveService.files().update(result.files[0].id, null, mediaContent).execute()
        }
    }

    /**
     * Descarga y reemplaza todos los archivos locales (Restauración total)
     */
    suspend fun downloadHistoryFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val eventsContent = downloadFileContent("calendar_history_backup.json")
            if (eventsContent != null) {
                context.openFileOutput("calendar_history_v2.json", Context.MODE_PRIVATE).use { it.write(eventsContent.toByteArray()) }
            }
            
            val notesContent = downloadFileContent("notes_history_backup.json")
            if (notesContent != null) {
                context.openFileOutput("notes_history.json", Context.MODE_PRIVATE).use { it.write(notesContent.toByteArray()) }
            }
            true
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error en restauraciÃ³n", e)
            false
        }
    }
}
