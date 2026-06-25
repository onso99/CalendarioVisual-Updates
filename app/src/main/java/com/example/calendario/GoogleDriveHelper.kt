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
import java.io.FileOutputStream
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
     * Sube el archivo JSON del histórico a Google Drive (Carpeta AppData)
     */
    suspend fun uploadHistoryFile(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val historyFile = context.getFileStreamPath("calendar_history_v2.json")
            if (!historyFile.exists()) return@withContext false

            // Buscar si ya existe una copia anterior para sobrescribirla
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
            
            Log.d("DriveHelper", "Histórico subido con éxito")
            true
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error subiendo a Drive", e)
            false
        }
    }

    /**
     * Descarga el histórico de Google Drive y reemplaza la local
     */
    suspend fun downloadHistoryFile(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = 'calendar_history_backup.json'")
                .execute()
            val files = result.files

            if (files.isNullOrEmpty()) return@withContext false

            val driveFileId = files[0].id
            val historyFile = context.getFileStreamPath("calendar_history_v2.json")

            context.openFileOutput("calendar_history_v2.json", Context.MODE_PRIVATE).use { outputStream ->
                driveService.files().get(driveFileId).executeMediaAndDownloadTo(outputStream)
            }
            
            Log.d("DriveHelper", "Histórico descargado con éxito")
            true
        } catch (e: Exception) {
            Log.e("DriveHelper", "Error descargando de Drive", e)
            false
        }
    }
}
