package com.example.calendario

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import java.time.ZoneId

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

    fun refreshWithCurrentEvents(context: Context, providedEvents: List<Festivo>? = null) {
        scheduleWidgetRefreshWorker(context)
        scope.launch(Dispatchers.IO) {
            val events = providedEvents ?: try {
                val selectedIds = SettingsManager.getSelectedCalendarIds(context)
                val eventsMap = readFestivosFromCalendarsSync(context, selectedIds)
                eventsMap.values.flatten()
            } catch (_: Exception) {
                val database = AppDatabase.getDatabase(context)
                database.calendarDao().getAllEventsSync().map { it.toFestivo() }
            }
            performUpdate(context, events)
        }
    }

    fun scheduleWidgetRefreshWorker(context: Context) {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_periodic_refresh_work",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (_: Exception) {}
    }

    private fun cleanAndFilterEvents(context: Context, events: List<Festivo>): List<WidgetEvent> {
        val limit = SettingsManager.getWidgetEventCount(context)
        
        // 1. Obtener IDs seleccionados del Widget y activos de la App
        val widgetSelectedIds = SettingsManager.getWidgetSelectedCalendarIds(context)
        val appActiveIds = SettingsManager.getSelectedCalendarIds(context)

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
                val isDismissed = AlarmUtils.isAlarmPassedOrDismissed(context, event)
                WidgetEvent(
                    id = event.id,
                    title = event.title,
                    dateEpochDay = event.date.toEpochDay(),
                    startTimeStr = event.startTime?.toString(),
                    isAllDay = event.isAllDay,
                    isBirthday = event.isBirthday,
                    age = event.age,
                    isLongPeriod = event.isLongPeriod,
                    currentDay = event.currentDay,
                    totalDays = event.totalDays,
                    alarmTimeStr = AlarmUtils.getAlarmTimeString(context, event),
                    isAlarmDismissed = isDismissed
                )
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
            calculateAndScheduleNextRefresh(context, events)
        } catch (_: Exception) { }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun calculateAndScheduleNextRefresh(context: Context, events: List<Festivo>) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }

            val now = LocalDateTime.now()
            val today = LocalDate.now()
            
            val nextExpirationMillis = events.asSequence()
                .filter { it.date.isEqual(today) && !it.isAllDay }
                .mapNotNull { event ->
                    val eventEndTime = event.endTime ?: event.startTime?.plusHours(1)
                    if (eventEndTime != null) {
                        val endDt = LocalDateTime.of(event.date, eventEndTime)
                        if (endDt.isAfter(now)) {
                            endDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else null
                    } else null
                }
                .minOrNull() ?: run {
                    val midnight = today.plusDays(1).atStartOfDay().plusSeconds(1)
                    midnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

            val intent = Intent(context, ModernCalendarWidgetReceiver::class.java).apply {
                action = ModernCalendarWidgetReceiver.ACTION_REFRESH_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                88888,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextExpirationMillis, pendingIntent)
        } catch (_: Exception) {}
    }

    data class WidgetEvent(
        val id: Long = -1L,
        val title: String,
        val dateEpochDay: Long,
        val startTimeStr: String?,
        val isAllDay: Boolean,
        val isBirthday: Boolean,
        val age: Int?,
        val isLongPeriod: Boolean,
        val currentDay: Int,
        val totalDays: Int,
        val alarmTimeStr: String?,
        val isAlarmDismissed: Boolean = false
    )

    /**
     * Fallback mejorado: Lee directamente de Room de forma síncrona
     */
    fun getWidgetEvents(context: Context): List<WidgetEvent> {
        val database = AppDatabase.getDatabase(context)
        val rawEvents = database.calendarDao().getAllEventsSync().map { it.toFestivo() }
        return cleanAndFilterEvents(context, rawEvents)
    }
}
