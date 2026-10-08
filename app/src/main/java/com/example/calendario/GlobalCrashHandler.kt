package com.example.calendario

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object GlobalCrashHandler {

    private const val TAG = "GlobalCrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        if (defaultHandler == null) {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    Log.e(TAG, "Excepción no controlada detectada en hilo '${thread.name}': ${throwable.message}", throwable)

                    // 1. Exportar informe de crash a la carpeta Descargas (Downloads) con fecha y hora
                    exportCrashReportToDownloads(context, throwable)

                    // 2. AUTO-SANACIÓN (SELF-HEALING): Limpiar preferencias corruptas
                    val prefsNames = listOf(
                        AppConstants.APP_SETTINGS_PREFS_NAME,
                        AppConstants.ALARM_PREFS_NAME,
                        AppConstants.HOLIDAY_PREFS_NAME,
                        WidgetConstants.GLOBAL_WIDGET_PREFS_NAME,
                        "calendar_prefs",
                    )
                    for (prefName in prefsNames) {
                        try {
                            context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit { clear() }
                        } catch (_: Exception) {}
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error en exportación o auto-sanación de crash: ${e.message}", e)
                } finally {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        }
    }

    private fun exportCrashReportToDownloads(context: Context, throwable: Throwable) {
        try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "crash_report_$timestamp.txt"
            val reportContent = buildString {
                appendLine("=== CALENDARIO VISUAL - CRASH REPORT ===")
                appendLine("Fecha: $timestamp")
                appendLine("Mensaje: ${throwable.localizedMessage}")
                appendLine("----------------------------------------")
                appendLine(throwable.stackTraceToString())
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(reportContent.toByteArray(Charsets.UTF_8))
                }
            }
            Log.i(TAG, "Informe de crash guardado con éxito en Descargas: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo guardar el informe de crash en Descargas: ${e.message}", e)
        }
    }
}
