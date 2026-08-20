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
import com.example.calendario.database.AppDatabase
import com.example.calendario.database.toFestivo
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

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

    fun updateWidgetState(context: Context, events: List<Festivo>) {
        updateJob?.cancel()
        updateJob = scope.launch {
            delay(300.milliseconds)
            performUpdate(context, events)
        }
    }

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
        val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val limit = try { 
            widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) 
        } catch (_: ClassCastException) { 
            (widgetPrefs.all[WidgetConstants.KEY_EVENT_COUNT] as? Number)?.toInt() 
                ?: widgetPrefs.all[WidgetConstants.KEY_EVENT_COUNT]?.toString()?.toIntOrNull() 
                ?: WidgetConstants.DEFAULT_EVENT_COUNT 
        }
        
        // 1. Obtener IDs seleccionados del Widget y activos de la App
        val widgetSelectedIds = try {
            widgetPrefs.getStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, emptySet())
        } catch (_: ClassCastException) {
            emptySet()
        }?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        
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
                    // Si el ID del evento no está en la selección del widget -> Ocultar
                    // EXCEPCIÓN: Si la selección del widget no tiene NADA que ver con los calendarios activos de la App,
                    // es una señal clara de que los IDs han cambiado (Backup). En ese caso, mostramos todo por seguridad.
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
                // Usamos la misma lógica que el widget clásico para evitar discrepancias.
                if (event.date.isBefore(today)) return@filter false
                
                // Si es hoy, verificamos si ya terminó (solo para eventos con hora)
                if (event.date.isEqual(today) && !event.isAllDay) {
                    val eventEndTime = event.endTime ?: event.startTime?.plusHours(1) ?: java.time.LocalTime.MAX
                    val eventEndDateTime = LocalDateTime.of(event.date, eventEndTime)
                    if (eventEndDateTime.isBefore(now)) return@filter false
                }
                
                true
            }
            .distinctBy { "${it.date}_${it.title}" }
            .sortedWith(compareBy({ it.date }, { it.startTime ?: java.time.LocalTime.MIN }))
            .take(limit)
            .map { event ->
                WidgetEvent(event.title, event.date.toEpochDay(), event.startTime?.toString(), event.isAllDay, event.isBirthday, event.age, event.isLongPeriod, event.currentDay, event.totalDays, AlarmUtils.getAlarmTimeString(context, event))
            }
            .toList()
    }

    private suspend fun performUpdate(context: Context, events: List<Festivo>) {
        val (json, prefsMap) = withContext(Dispatchers.Default) {
            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val cleaned = cleanAndFilterEvents(context, events)
            val jsonStr = gson.toJson(cleaned)

            val all = widgetPrefs.all
            val fontVal = try { 
                widgetPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) 
            } catch (_: Exception) { all[WidgetConstants.KEY_WIDGET_FONT_FAMILY]?.toString() } ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY

            val map = mapOf(
                "bg" to try { 
                    widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB) 
                } catch (_: Exception) { (all[WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB },
                
                "event" to try { 
                    widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB) 
                } catch (_: Exception) { (all[WidgetConstants.KEY_WIDGET_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB },
                
                "today" to try { 
                    widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB) 
                } catch (_: Exception) { (all[WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB },
                
                "boost" to try { 
                    widgetPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f) 
                } catch (_: Exception) { (all[WidgetConstants.KEY_WIDGET_TEXT_BOOST] as? Number)?.toFloat() ?: 0f },
                
                "font" to fontVal,
                
                "bold" to try { 
                    widgetPrefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD) 
                } catch (_: Exception) { all[WidgetConstants.KEY_WIDGET_FONT_BOLD]?.toString()?.toBoolean() ?: WidgetConstants.DEFAULT_WIDGET_FONT_BOLD }
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
