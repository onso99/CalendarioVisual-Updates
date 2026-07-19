package com.example.calendario

import android.app.Application
import android.util.Log
import androidx.work.Configuration // Importante para Configuration.Provider
// Los siguientes imports ya no son necesarios aquí si CalendarAppWidgetProvider
// maneja el encolamiento de trabajos relacionados con el widget.
// import androidx.work.Constraints
// import androidx.work.ExistingPeriodicWorkPolicy
// import androidx.work.PeriodicWorkRequestBuilder
// import androidx.work.WorkManager
// import java.util.concurrent.TimeUnit

class CalendarioApplication : Application(), Configuration.Provider {

    /**
     * Proporciona la configuración personalizada para WorkManager.
     * Esto se llamará automáticamente cuando WorkManager necesite inicializarse.
     * Habilitamos los logs de DEBUG de WorkManager para facilitar la depuración.
     */
    override val workManagerConfiguration: Configuration
        get() {
            Log.i(TAG, "Creando y proporcionando WorkManagerConfiguration con logging DEBUG.")
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG) // Habilita logs detallados de WorkManager
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate() de CalendarioApplication FUE LLAMADO.")

        // SISTEMA DE AUTO-SANACIÃ“N: Aseguramos que el backup estÃ© programado al arrancar la App
        BackupScheduler.ensureBackupScheduled(this)

        Log.d(TAG, "WorkManager se inicializarÃ¡ bajo demanda con la configuraciÃ³n proporcionada.")
    }

    companion object {
        private const val TAG = "CalendarioApplication"
    }

    // Los métodos setupRecurringWorkWithKeepPolicy() y la versión original de setupRecurringWork()
    // pueden ser eliminados completamente si CalendarAppWidgetProvider maneja todo el encolamiento
    // de trabajos para el widget. Si tenías otros trabajos periódicos globales que no estaban
    // relacionados con el widget y se encolaban aquí, necesitarías mantener esa lógica
    // o moverla a otro lugar apropiado.
    // Para el caso que hemos estado discutiendo (actualizar el widget),
    // esta simplificación es adecuada.

    /*
    // Ejemplo de la función que eliminamos (si solo era para el widget):
    private fun setupRecurringWorkWithKeepPolicy() {
        Log.i(TAG, "setupRecurringWorkWithKeepPolicy() ya no es necesario aquí si el widget lo maneja.")
        // ... LÓGICA ANTERIOR PARA ENCOLAR UN TRABAJO PERIÓDICO DESDE AQUÍ HA SIDO ELIMINADA ...
    }
    */
}
