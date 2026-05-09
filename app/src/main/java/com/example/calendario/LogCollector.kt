package com.example.calendario

import android.content.Context
import java.util.concurrent.ConcurrentLinkedQueue
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

object LogCollector {
    private val logs = ConcurrentLinkedQueue<String>()
    private val maxLogs = 200 
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private const val PREFS_NAME = "widget_log_prefs"
    private const val KEY_NEXT_REFRESH = "next_refresh_time"
    
    // Cache del estado para evitar lecturas constantes de disco
    private var isEnabledCache: Boolean? = null

    /**
     * Inicializa el estado desde SharedPreferences si es necesario.
     * Esta función debe llamarse al menos una vez con un contexto (ej. al abrir la app o el widget).
     */
    fun init(context: Context) {
        if (isEnabledCache == null) {
            val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            isEnabledCache = prefs.getBoolean(AppConstants.KEY_LOGGING_ENABLED, false)
        }
    }

    fun isLoggingEnabled(context: Context): Boolean {
        init(context)
        return isEnabledCache ?: false
    }

    fun setLoggingEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(AppConstants.KEY_LOGGING_ENABLED, enabled).apply()
        isEnabledCache = enabled
        if (!enabled) clear()
    }

    fun setNextRefreshTime(context: Context, timeMillis: Long) {
        if (!isLoggingEnabled(context)) return
        
        val timeStr = java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .format(formatter)
            
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NEXT_REFRESH, timeStr).apply()
    }

    fun getNextRefreshTime(context: Context): String {
        if (!isLoggingEnabled(context)) return "OFF"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NEXT_REFRESH, "No programado") ?: "No programado"
    }

    fun addLog(message: String) {
        // Solo logueamos si el cache está activo. 
        // El cache se inicializa en el arranque de la app o al abrir ajustes.
        if (isEnabledCache == true) {
            val timestamp = LocalTime.now().format(formatter)
            val logEntry = "[$timestamp] $message"
            logs.add(logEntry)
            if (logs.size > maxLogs) {
                logs.poll()
            }
        }
    }

    fun getLogs(): String {
        return logs.joinToString("\n")
    }
    
    fun clear() {
        logs.clear()
    }
}
