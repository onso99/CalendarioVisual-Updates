package com.example.calendario

import android.content.Context
import java.util.concurrent.ConcurrentLinkedQueue
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

object LogCollector {
    private val logs = ConcurrentLinkedQueue<String>()
    private val maxLogs = 200 // Reducimos para mayor legibilidad
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private const val PREFS_NAME = "widget_log_prefs"
    private const val KEY_NEXT_REFRESH = "next_refresh_time"

    fun setNextRefreshTime(context: Context, timeMillis: Long) {
        val timeStr = java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .format(formatter)
            
        // Guardamos en RAM para acceso rápido
        // Y en SharedPreferences para que sobreviva al cierre de la app
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NEXT_REFRESH, timeStr).apply()
    }

    fun getNextRefreshTime(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NEXT_REFRESH, "No programado") ?: "No programado"
    }

    fun addLog(message: String) {
        val timestamp = LocalTime.now().format(formatter)
        val logEntry = "[$timestamp] $message"
        logs.add(logEntry)
        if (logs.size > maxLogs) {
            logs.poll()
        }
    }

    fun getLogs(): String {
        return logs.joinToString("\n")
    }
    
    fun clear() {
        logs.clear()
    }
}
