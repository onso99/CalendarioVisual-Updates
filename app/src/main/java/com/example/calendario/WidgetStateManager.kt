package com.example.calendario

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Motor de datos del Widget Moderno.
 * Sincroniza tanto eventos como ajustes visuales en el estado nativo para forzar redibujados.
 */
object WidgetStateManager {
    // Claves de Estado Nativo (Glance)
    val KEY_WIDGET_DATA = stringPreferencesKey("widget_events_json")
    val KEY_BG_COLOR = intPreferencesKey("widget_bg_color")
    val KEY_EVENT_COLOR = intPreferencesKey("widget_event_color")
    val KEY_TODAY_COLOR = intPreferencesKey("widget_today_color")
    val KEY_TEXT_BOOST = floatPreferencesKey("widget_text_boost")
    val KEY_FONT_FAMILY = stringPreferencesKey("widget_font_family")
    val KEY_FONT_BOLD = booleanPreferencesKey("widget_font_bold")
    val KEY_REFRESH_TOKEN = longPreferencesKey("widget_refresh_token") // Fuerza el cambio siempre
    
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    fun updateWidgetState(context: Context, events: List<Festivo>) {
        updateJob?.cancel()
        updateJob = scope.launch {
            delay(300.milliseconds)
            performUpdate(context, events)
        }
    }

    fun refreshWithCurrentEvents(context: Context) {
        updateJob?.cancel()
        val events = loadHistoryFromDisk(context)
        updateJob = scope.launch {
            LogCollector.addLog("WIDGET MOTOR: Sincronizando estado completo (Ajustes + Eventos)")
            performUpdate(context, events)
        }
    }

    private suspend fun performUpdate(context: Context, events: List<Festivo>) {
        val startTime = System.currentTimeMillis()
        
        // 1. Preparar datos y leer preferencias actuales
        val (json, prefsMap) = withContext(Dispatchers.Default) {
            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val limit = widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
            val now = LocalDateTime.now().withNano(0).withSecond(0)
            
            val futureEvents = events.filter { event ->
                val eventEndDateTime = if (event.isAllDay) event.date.plusDays(1).atStartOfDay()
                else LocalDateTime.of(event.date, event.endTime ?: event.startTime?.plusHours(1) ?: java.time.LocalTime.MAX)
                eventEndDateTime.isAfter(now)
            }.sortedWith(compareBy({ it.date }, { it.startTime })).take(limit)

            val jsonStr = gson.toJson(futureEvents.map { event ->
                WidgetEvent(event.title, event.date.toEpochDay(), event.startTime?.toString(), event.isAllDay, event.isBirthday, event.age, event.isLongPeriod, event.currentDay, event.totalDays, AlarmUtils.getAlarmTimeString(context, event))
            })

            val map = mapOf(
                "bg" to widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB),
                "event" to widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB),
                "today" to widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB),
                "boost" to widgetPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f),
                "font" to (widgetPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: ""),
                "bold" to widgetPrefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD)
            )
            jsonStr to map
        }

        try {
            // 2. Inyectar TODO en el estado nativo (Glance DataStore)
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(ModernCalendarWidget::class.java)
            
            glanceIds.forEach { id ->
                updateAppWidgetState(context, id) { state ->
                    state[KEY_WIDGET_DATA] = json
                    state[KEY_BG_COLOR] = prefsMap["bg"] as Int
                    state[KEY_EVENT_COLOR] = prefsMap["event"] as Int
                    state[KEY_TODAY_COLOR] = prefsMap["today"] as Int
                    state[KEY_TEXT_BOOST] = prefsMap["boost"] as Float
                    state[KEY_FONT_FAMILY] = prefsMap["font"] as String
                    state[KEY_FONT_BOLD] = prefsMap["bold"] as Boolean
                    state[KEY_REFRESH_TOKEN] = System.currentTimeMillis() // TOKEN SIEMPRE NUEVO
                }
            }
            
            // 3. Orden final de redibujado
            ModernCalendarWidget().updateAll(context)
            
            val duration = System.currentTimeMillis() - startTime
            LogCollector.addLog("GLANCE SYNC: Estado actualizado en ${duration}ms")
        } catch (e: Exception) {
            LogCollector.addLog("GLANCE SYNC ERROR: ${e.message}")
        }
    }

    data class WidgetEvent(val title: String, val dateEpochDay: Long, val startTimeStr: String?, val isAllDay: Boolean, val isBirthday: Boolean, val age: Int?, val isLongPeriod: Boolean, val currentDay: Int, val totalDays: Int, val alarmTimeStr: String?)

    fun getWidgetEvents(context: Context): List<WidgetEvent> {
        // Fallback para SharedPreferences clÃ¡sicas (por si acaso)
        val json = context.getSharedPreferences("modern_widget_shared_prefs", Context.MODE_PRIVATE).getString("events_json", null) ?: return emptyList()
        return try { gson.fromJson(json, Array<WidgetEvent>::class.java).toList() } catch (_: Exception) { emptyList() }
    }
}
