package com.example.calendario

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Motor de datos del Widget Moderno.
 * Agrupa actualizaciones para evitar saturaciÃ³n pero garantiza el refresco visual.
 */
object WidgetStateManager {
    private const val PREFS_NAME = "modern_widget_shared_prefs"
    private const val KEY_JSON = "events_json"
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    data class WidgetEvent(
        val title: String,
        val dateEpochDay: Long,
        val startTimeStr: String?,
        val isAllDay: Boolean,
        val isBirthday: Boolean,
        val age: Int?,
        val isLongPeriod: Boolean,
        val currentDay: Int,
        val totalDays: Int,
        val alarmTimeStr: String?
    )

    fun updateWidgetState(context: Context, events: List<Festivo>) {
        updateJob?.cancel()
        updateJob = scope.launch {
            // Agrupamos peticiones rÃ¡pidas (250ms) para no colapsar Android
            delay(250.milliseconds)

            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val limit = widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)

            val now = LocalDateTime.now().withNano(0).withSecond(0)
            val futureEvents = events.filter { event ->
                val eventEndDateTime = if (event.isAllDay) {
                    event.date.plusDays(1).atStartOfDay()
                } else {
                    val endTime = event.endTime ?: event.startTime?.plusHours(1) ?: java.time.LocalTime.MAX
                    LocalDateTime.of(event.date, endTime)
                }
                eventEndDateTime.isAfter(now)
            }.sortedWith(compareBy({ it.date }, { it.startTime }))
            .take(limit)

            val json = gson.toJson(futureEvents.map { event ->
                WidgetEvent(
                    title = event.title,
                    dateEpochDay = event.date.toEpochDay(),
                    startTimeStr = event.startTime?.toString(),
                    isAllDay = event.isAllDay,
                    isBirthday = event.isBirthday,
                    age = event.age,
                    isLongPeriod = event.isLongPeriod,
                    currentDay = event.currentDay,
                    totalDays = event.totalDays,
                    alarmTimeStr = AlarmUtils.getAlarmTimeString(context, event)
                )
            })

            // Guardado sÃ­ncrono para asegurar que el dato estÃ© listo para la UI
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit(commit = true) {
                putString(KEY_JSON, json)
            }

            try {
                // DISPARADOR NATIVO: Despertamos al receptor para prioridad alta
                val intent = Intent(context, ModernCalendarWidgetReceiver::class.java).apply {
                    action = ModernCalendarWidgetReceiver.ACTION_REFRESH_WIDGET
                }
                context.sendBroadcast(intent)

                // DISPARADOR GLANCE: Redibujado de la interfaz
                ModernCalendarWidget().updateAll(context)
                
                LogCollector.addLog("GLANCE PUSH: OK (${futureEvents.size} eventos)")
            } catch (_: Exception) {}
        }
    }

    fun refreshWithCurrentEvents(context: Context) {
        val events = loadHistoryFromDisk(context)
        updateWidgetState(context, events)
    }

    fun getWidgetEvents(context: Context): List<WidgetEvent> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<WidgetEvent>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
