package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateCalendarDataWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG_WORKER = "UpdateCalendarWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG_WORKER, "Worker INICIADO. ID: ${this.id}, Tags: ${this.tags.joinToString()}")

        return try {
            val context = applicationContext

            val selectedCalendarIds = loadSelectedCalendarIds(context)
            if (selectedCalendarIds.isEmpty()) {
                Log.i(TAG_WORKER, "No hay calendarios seleccionados. Trabajo finalizado sin acción. ID: ${this.id}")
                return Result.success()
            }
            Log.d(TAG_WORKER, "Calendarios seleccionados cargados: $selectedCalendarIds. ID: ${this.id}")

            val availableCalendars = loadAvailableCalendarsSuspend(context)
            if (availableCalendars.isEmpty() && selectedCalendarIds.isNotEmpty()) {
                Log.w(TAG_WORKER, "No se encontraron calendarios disponibles, pero había IDs seleccionados. No se puede continuar. ID: ${this.id}")
                return Result.failure()
            }
            
            Log.d(TAG_WORKER, "Calendarios disponibles cargados: ${availableCalendars.size}. ID: ${this.id}")

            val validSelectedCalendarIds = selectedCalendarIds.filter { selectedId ->
                availableCalendars.any { it.id == selectedId }
            }.toSet()

            if (validSelectedCalendarIds.isEmpty()) {
                if (selectedCalendarIds.isNotEmpty()) {
                    Log.w(TAG_WORKER, "Ninguno de los calendarios seleccionados previamente está disponible. Trabajo finalizado. ID: ${this.id}")
                } else {
                    Log.i(TAG_WORKER, "No hay calendarios válidos seleccionados. Trabajo finalizado. ID: ${this.id}")
                }
                return Result.success()
            }
            Log.d(TAG_WORKER, "Calendarios válidos a procesar: $validSelectedCalendarIds. ID: ${this.id}")

            val eventsMap = readFestivosFromCalendarsSuspend(context, validSelectedCalendarIds)
            Log.d(TAG_WORKER, "Eventos leídos del calendario: ${eventsMap.size} días con eventos. ID: ${this.id}")

            saveEventsToPrefs(context, eventsMap)
            Log.i(TAG_WORKER, "Eventos guardados en SharedPreferences. ID: ${this.id}")

            // USAMOS EL MÉTODO UNIFICADO: Esto refresca tanto la lista como el layout
            CalendarAppWidgetProvider.triggerWidgetUpdate(context)

            Log.i(TAG_WORKER, "Worker COMPLETADO EXITOSAMENTE. ID: ${this.id}")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG_WORKER, "Error durante la ejecución del worker. ID: ${this.id}", e)
            Result.failure()
        }
    }

    // Eliminamos este método porque ahora usamos el unificado de CalendarAppWidgetProvider
}