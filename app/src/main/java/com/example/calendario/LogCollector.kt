package com.example.calendario

import android.content.Context
import java.util.concurrent.ConcurrentLinkedQueue
import java.time.LocalTime

object LogCollector {
    private val logs = ConcurrentLinkedQueue<String>()
    private const val MAX_LOGS = 200
    private val formatter = AppFormats.TimeWithSeconds
    
    // Cache del estado para evitar lecturas constantes de disco
    private var isEnabledCache: Boolean? = null

    /**
     * Inicializa el estado desde SharedPreferences si es necesario.
     * Esta función debe llamarse al menos una vez con un contexto (ej. al abrir la app o el widget).
     */
    fun init(context: Context) {
        if (isEnabledCache == null) {
            isEnabledCache = SettingsManager.isLoggingEnabled(context)
        }
    }

    fun isLoggingEnabled(context: Context): Boolean {
        init(context)
        return isEnabledCache ?: false
    }

    fun setLoggingEnabled(context: Context, enabled: Boolean) {
        SettingsManager.saveLoggingEnabled(context, enabled)
        isEnabledCache = enabled
        if (!enabled) {
            clear()
        } else {
            // Al activar, ponemos un estado pendiente hasta que llegue la primera alarma real
            SettingsManager.saveNextRefreshTime(context, "PENDING")
        }
    }



    fun getNextRefreshTime(context: Context): String {
        if (!isLoggingEnabled(context)) return "OFF"
        return SettingsManager.getNextRefreshTime(context)
    }

    fun addLog(message: String) {
        // Solo logueamos si el cache está activo. 
        // El cache se inicializa en el arranque de la app o al abrir ajustes.
        if (isEnabledCache == true) {
            val timestamp = LocalTime.now().format(formatter)
            val logEntry = "[$timestamp] $message"
            logs.add(logEntry)
            if (logs.size > MAX_LOGS) {
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
