package com.example.calendario

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper // Import necesario
import android.provider.CalendarContract
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class CalendarObserver(private val context: Context) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var isRegistered = false

    companion object {
        private const val TAG = "CalendarObserver"
        const val UNIQUE_ON_DEMAND_WORK_NAME = "UniqueCalendarUpdateOnDemand"
    }

    fun register() {
        if (!isRegistered) {
            try {
                context.contentResolver.registerContentObserver(
                    CalendarContract.Events.CONTENT_URI,
                    true, // true para notificar descendientes (importante para eventos)
                    this
                )
                isRegistered = true
                Log.i(TAG, "register() - CalendarObserver registrado exitosamente para CalendarContract.Events.CONTENT_URI. isRegistered = $isRegistered")
            } catch (e: Exception) {
                Log.e(TAG, "register() - Error registrando CalendarObserver", e)
                isRegistered = false // Asegurar que el flag es correcto en caso de error
            }
        } else {
            Log.w(TAG, "register() - CalendarObserver ya estaba marcado como registrado. isRegistered = $isRegistered. No se re-registra.")
        }
    }

    fun unregister() {
        if (isRegistered) {
            try {
                context.contentResolver.unregisterContentObserver(this)
                isRegistered = false
                Log.i(TAG, "unregister() - CalendarObserver desregistrado exitosamente. isRegistered = $isRegistered")
            } catch (e: Exception) {
                Log.e(TAG, "unregister() - Error desregistrando CalendarObserver", e)
            }
        } else {
            Log.w(TAG, "unregister() - CalendarObserver no estaba marcado como registrado. isRegistered = $isRegistered. No se puede desregistrar.")
        }
    }

    override fun onChange(selfChange: Boolean) {
        this.onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        Log.d(TAG, "onChange - INICIO. selfChange: $selfChange, URI: $uri, isRegistered: $isRegistered")

        if (!isRegistered) {
            Log.e(TAG, "onChange - ¡ALERTA! onChange fue llamado pero el observer NO está marcado como registrado (isRegistered = false). Esto indica un problema en el ciclo de vida o estado. Saliendo de onChange.")
            return
        }

        Log.i(TAG, "onChange - Cambio detectado. Encolando trabajo OneTime ($UNIQUE_ON_DEMAND_WORK_NAME) UpdateCalendarDataWorker.")
        val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateCalendarDataWorker>()
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_ON_DEMAND_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            updateWorkRequest
        )
        Log.d(TAG, "onChange - FIN. Trabajo encolado.")
    }

    // Este método sigue aquí por si lo necesitas para depuración desde otros sitios,
    // pero CalendarAppWidgetProvider ya no lo usa directamente en onEnabled.
    fun isObserverRegistered(): Boolean {
        Log.d(TAG, "isObserverRegistered() - Devuelve: $isRegistered")
        return isRegistered
    }
}

