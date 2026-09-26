package com.example.calendario

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val versionName: String,
        val changelog: String,
        val downloadUrl: String
    ) : UpdateCheckResult()

    object AlreadyUpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/onso99/CalendarioVisual-Updates/releases/latest"

    /**
     * Consulta asíncrona a la API de GitHub para verificar si existe una nueva versión.
     */
    suspend fun checkLatestRelease(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", AppConstants.APP_SIGNATURE)
            }

            if (connection.responseCode == 404) {
                // HTTP 404: No hay publicaciones/releases creadas en GitHub aún.
                SettingsManager.setUpdateAvailable(context, false)
                SettingsManager.saveLastUpdateCheckTime(context)
                return@withContext UpdateCheckResult.AlreadyUpToDate
            }

            if (connection.responseCode != 200) {
                Log.e(TAG, "Error HTTP ${connection.responseCode} al consultar GitHub Releases")
                return@withContext UpdateCheckResult.Error("HTTP ${connection.responseCode}")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val rawTagName = json.optString("tag_name", "").trim()
            val remoteVersionName = rawTagName.removePrefix("v").removePrefix("V")
            val changelog = json.optString("body", "").trim()

            // Buscar el asset .apk en la respuesta
            var apkDownloadUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.getJSONObject(i)
                    val name = assetObj.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = assetObj.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (apkDownloadUrl.isNullOrBlank()) {
                // Fallback si no hay asset explícito
                apkDownloadUrl = "https://github.com/onso99/CalendarioVisual-Updates/releases/download/$rawTagName/app-release.apk"
            }

            val currentVersionName = try {
                @Suppress("DEPRECATION")
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "0.0.0"
            } catch (_: Exception) {
                "0.0.0"
            }

            val currentVersionCode = try {
                @Suppress("DEPRECATION")
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.longVersionCode
            } catch (_: Exception) {
                0L
            }

            val remoteVersionCode = extractVersionCode(json, rawTagName)

            val isNewer = if (remoteVersionCode > 0L && currentVersionCode > 0L) {
                remoteVersionCode > currentVersionCode
            } else {
                isVersionNewer(remoteVersionName, currentVersionName)
            }

            if (isNewer) {
                SettingsManager.setUpdateAvailable(context, true, remoteVersionName, changelog, apkDownloadUrl, remoteVersionCode)
                UpdateCheckResult.UpdateAvailable(
                    versionName = remoteVersionName,
                    changelog = changelog,
                    downloadUrl = apkDownloadUrl
                )
            } else {
                SettingsManager.setUpdateAvailable(context, false)
                UpdateCheckResult.AlreadyUpToDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando actualizaciones en GitHub: ${e.message}", e)
            UpdateCheckResult.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Descarga e invoca el instalador del sistema Android usando FileProvider.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", AppConstants.APP_SIGNATURE)
            }

            val totalSize = connection.contentLength
            val apkFile = File(context.cacheDir, "update_app.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalSize > 0) {
                            onProgress(downloaded.toFloat() / totalSize.toFloat())
                        }
                    }
                }
            }

            if (!apkFile.exists() || apkFile.length() == 0L) return@withContext false

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando o instalando APK: ${e.message}", e)
            false
        }
    }

    /**
     * Compara dos números de versión semánticos (ej: "3.5.0" vs "3.4.08").
     */
    fun isVersionNewer(remote: String, local: String): Boolean {
        return try {
            val remoteParts = remote.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            val localParts = local.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun extractVersionCode(json: JSONObject, tagName: String): Long {
        if (json.has("versionCode") && !json.isNull("versionCode")) {
            return json.optLong("versionCode", 0L)
        }
        val releaseName = json.optString("name", "")
        val body = json.optString("body", "")

        val codeRegex = Regex("""(?:code|versionCode|build)[\s:=]+(\d+)""", RegexOption.IGNORE_CASE)
        codeRegex.find(releaseName)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }
        codeRegex.find(body)?.groupValues?.get(1)?.toLongOrNull()?.let { return it }

        val dashPart = tagName.substringAfterLast("-", "")
        dashPart.toLongOrNull()?.let { return it }

        return 0L
    }
}
