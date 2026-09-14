package com.example.calendario

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.example.calendario.database.AppDatabase
import com.example.calendario.database.toFestivo
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Motor de datos del Widget Moderno.
 */
object WidgetStateManager {
    val KEY_WIDGET_DATA = stringPreferencesKey("widget_events_json")
    val KEY_BG_COLOR = intPreferencesKey("widget_bg_color")
    val KEY_EVENT_COLOR = intPreferencesKey("widget_event_color")
    val KEY_TODAY_COLOR = intPreferencesKey("widget_today_color")
    val KEY_TEXT_BOOST = floatPreferencesKey("widget_text_boost")
    val KEY_FONT_FAMILY = stringPreferencesKey("widget_font_family")
    val KEY_FONT_BOLD = booleanPreferencesKey("widget_font_bold")
    val KEY_REFRESH_TOKEN = longPreferencesKey("widget_refresh_token")
    
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    fun refreshWithCurrentEvents(context: Context) {
        updateJob?.cancel()
        updateJob = scope.launch {
            val database = AppDatabase.getDatabase(context)
            val events = withContext(Dispatchers.IO) {
                database.calendarDao().getAllEventsSync().map { it.toFestivo() }
            }
            performUpdate(context, events)
        }
    }

    private fun cleanAndFilterEvents(context: Context, events: List<Festivo>): List<WidgetEvent> {
        val limit = SettingsManager.getWidgetEventCount(context)
        
        // 1. Obtener IDs seleccionados del Widget y activos de la App
        val widgetSelectedIds = SettingsManager.getWidgetSelectedCalendarIds(context)
        val appActiveIds = loadSelectedCalendarIds(context)

        // Usamos la fecha de hoy a medianoche para una comparación limpia
        val today = LocalDate.now()
        val now = LocalDateTime.now().withNano(0).withSecond(0)
        
        return events.asSequence()
            .filter { event ->
                // FILTRO DE CALENDARIOS: 
                // 1. Si el widget tiene selección propia, debe ser respetada.
                // 2. Si esa selección es "basura" (IDs viejos de backup), detectamos la falta de intersección.
                if (widgetSelectedIds.isNotEmpty()) {
                    val hasValidOverlap = widgetSelectedIds.any { id -> appActiveIds.contains(id) || id == -1L }
                    if (hasValidOverlap) {
                        val isCalendarSelectedInWidget = widgetSelectedIds.contains(event.calendarId)
                        if (!isCalendarSelectedInWidget) return@filter false
                    }
                }
                
                // Además, siempre respetamos lo que el usuario haya desactivado en la App principal
                val isCalendarActiveInApp = appActiveIds.contains(event.calendarId) || event.calendarId == -1L
                if (!isCalendarActiveInApp) return@filter false

                // REGLA DE ORO: Si es hoy, se queda. Si es futuro, se queda.
                if (event.date.isBefore(today)) return@filter false
                
                // Si es hoy, verificamos si ya terminó (solo para eventos con hora)
                if (event.date.isEqual(today) && !event.isAllDay) {
                    val eventEndTime = event.endTime ?: event.startTime?.plusHours(1) ?: java.time.LocalTime.MAX
                    val eventEndDateTime = LocalDateTime.of(event.date, eventEndTime)
                    if (eventEndDateTime.isBefore(now)) return@filter false
                }
                
                true
            }
            .sortedWith(compareBy({ it.date }, { it.startTime ?: java.time.LocalTime.MIN }))
            .take(limit)
            .map { event ->
                WidgetEvent(event.title, event.date.toEpochDay(), event.startTime?.toString(), event.isAllDay, event.isBirthday, event.age, event.isLongPeriod, event.currentDay, event.totalDays, AlarmUtils.getAlarmTimeString(context, event))
            }
            .toList()
    }

    private suspend fun performUpdate(context: Context, events: List<Festivo>) {
        val (json, prefsMap) = withContext(Dispatchers.Default) {
            val cleaned = cleanAndFilterEvents(context, events)
            val jsonStr = gson.toJson(cleaned)

            val map = mapOf(
                "bg" to SettingsManager.getWidgetBackgroundColor(context).toArgb(),
                "event" to SettingsManager.getWidgetEventColor(context).toArgb(),
                "today" to SettingsManager.getWidgetTodayEventColor(context).toArgb(),
                "boost" to SettingsManager.getWidgetTextBoost(context),
                "font" to SettingsManager.getWidgetFontFamily(context),
                "bold" to SettingsManager.isWidgetFontBold(context)
            )
            jsonStr to map
        }

        try {
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
                    state[KEY_REFRESH_TOKEN] = System.currentTimeMillis()
                }
            }
            ModernCalendarWidget().updateAll(context)
        } catch (_: Exception) { }
    }

    data class WidgetEvent(val title: String, val dateEpochDay: Long, val startTimeStr: String?, val isAllDay: Boolean, val isBirthday: Boolean, val age: Int?, val isLongPeriod: Boolean, val currentDay: Int, val totalDays: Int, val alarmTimeStr: String?)

    /**
     * Fallback mejorado: Lee directamente de Room de forma síncrona
     */
    fun getWidgetEvents(context: Context): List<WidgetEvent> {
        val database = AppDatabase.getDatabase(context)
        val rawEvents = database.calendarDao().getAllEventsSync().map { it.toFestivo() }
        return cleanAndFilterEvents(context, rawEvents)
    }
}
