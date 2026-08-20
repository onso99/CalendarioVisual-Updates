package com.example.calendario

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.example.calendario.database.toEntity
import com.example.calendario.database.toFestivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCalendarDataWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG_WORKER = "UpdateCalendarWorker"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "widget_update_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(channelId, "Actualización de Widget", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Actualizando calendario...")
            .setSmallIcon(R.drawable.ic_notification_icon) // Icono vectorial específico para notificaciones
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(1001, notification)
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

            val eventsMap = readFestivosFromCalendarsSync(context, validSelectedCalendarIds)
            LogCollector.addLog("WORKER: Datos leídos (${eventsMap.size} días)")

            // ACTUALIZACIÓN DE BASE DE DATOS (Fase 3/4)
            val database = com.example.calendario.database.AppDatabase.getDatabase(context)
            val dao = database.calendarDao()
            
            val currentEntities = dao.getAllEventsSync()
            val cachedHistory = currentEntities.map { it.toFestivo() }
            
            // Fusión inteligente (usando la lógica que perfeccionamos en el ViewModel)
            // Necesitamos acceder a mergeHistoryWithSystem. Como es privada en ViewModel, 
            // deberíamos haberla movido a Utils. Lo haré en el siguiente paso si es necesario, 
            // pero por ahora usemos una lógica similar.
            
            // Para simplificar esta fase, vamos a confiar en que la App principal 
            // es la que hace las fusiones pesadas, y el Worker solo inyecta lo nuevo de Google.
            val systemEvents = eventsMap.values.flatten()
            
            // FUSIÓN INTELIGENTE COMPLETA: Usamos el mismo cerebro que el ViewModel
            val mergedEvents = mergeHistoryWithSystemData(context, cachedHistory, systemEvents, availableCalendars)
            
            // Actualizamos Room de forma segura
            dao.refreshEvents(mergedEvents.map { it.toEntity() })

            saveEventsToPrefs(context, eventsMap)
            
            // Reprogramamos todas las alarmas para asegurar que coinciden con los nuevos datos sincronizados
            AlarmUtils.rescheduleAllAlarms(context)
            
            // SISTEMA DE AUTO-SANACIÓN: Asegurar que la tarea de Drive sigue programada
            BackupScheduler.ensureBackupScheduled(context)
            
            // Notificamos al widget de forma directa
            CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            WidgetStateManager.refreshWithCurrentEvents(context)

            LogCollector.addLog("WORKER: Completado con éxito")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG_WORKER, "Error durante la ejecución del worker. ID: ${this.id}", e)
            Result.failure()
        }
    }

    // Eliminamos este método porque ahora usamos el unificado de CalendarAppWidgetProvider
}