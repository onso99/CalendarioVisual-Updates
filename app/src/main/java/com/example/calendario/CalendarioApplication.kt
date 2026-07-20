package com.example.calendario

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarioApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // 1. InicializaciÃ³n inmediata de logs (Vital para evitar cierres)
        LogCollector.init(this)
        
        // 2. ProgramaciÃ³n diferida del Backup
        // Usamos un hilo de fondo para no entorpecer el arranque de la UI
        CoroutineScope(Dispatchers.Main).launch {
            try {
                BackupScheduler.ensureBackupScheduled(applicationContext)
            } catch (e: Exception) {
                Log.e("CalendarioApp", "Error programando backup inicial: ${e.message}")
            }
        }
    }
}
