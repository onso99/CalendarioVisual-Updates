package com.example.calendario

import android.content.Context
import android.util.Log

object GlobalCrashHandler {

    private const val TAG = "GlobalCrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        if (defaultHandler == null) {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    Log.e(TAG, "Excepción no controlada detectada en hilo '${thread.name}': ${throwable.message}", throwable)
                    val errorDetail = throwable.localizedMessage ?: throwable.toString()
                    
                    // Guardamos la bandera de fallo al arrancar
                    SettingsManager.setStartupCrashFlag(context, true, errorDetail)
                } catch (e: Exception) {
                    Log.e(TAG, "Error guardando el registro de crash: ${e.message}", e)
                } finally {
                    // Pasar la excepción al manejador por defecto de Android para un cierre limpio
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        }
    }
}
