package com.example.calendario // Asegúrate que este es tu nombre de paquete correcto

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences // Necesario para PreferenceManager.getDefaultSharedPreferences
import android.graphics.Color // Necesario para Color.parseColor
import android.preference.PreferenceManager // O androidx.preference.PreferenceManager si lo usas consistentemente
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Asumo que tu clase Festivo tiene una propiedad 'date' de tipo LocalDate
// data class Festivo(val date: LocalDate, val startTime: LocalTime?, val description: String, val isAllDay: Boolean, val calendarId: Long /*, otros campos */)

class CalendarWidgetFactory(
    private val context: Context,
    private val intent: Intent // El intent que inició el servicio, contiene el appWidgetId
) : RemoteViewsService.RemoteViewsFactory {

    private var eventsList: List<Festivo> = emptyList()
    private var eventCountToShow: Int = WidgetConstants.DEFAULT_EVENT_COUNT
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        Log.d("WidgetFactory", "onCreate - Widget ID: $appWidgetId")
        loadEventCountSetting()
        // loadCalendarEvents() // Se llama en onDataSetChanged, que también se llama después de onCreate
    }

    override fun onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged - Widget ID: $appWidgetId")
        loadEventCountSetting() // Recargar por si cambió desde la app
        loadCalendarEvents()
        Log.d("WidgetFactory", "Eventos cargados: ${eventsList.size}, mostrando: $eventCountToShow")
    }

    override fun onDestroy() {
        Log.d("WidgetFactory", "onDestroy - Widget ID: $appWidgetId")
        eventsList = emptyList()
    }

    override fun getCount(): Int {
        val count = eventsList.take(eventCountToShow).size
        Log.d("WidgetFactory", "getCount: $count (de ${eventsList.size} eventos totales, límite $eventCountToShow)")
        return count
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= eventsList.take(eventCountToShow).size) {
            Log.w("WidgetFactory", "getViewAt: Posición inválida $position, eventos a mostrar ${eventsList.take(eventCountToShow).size}")
            return null
        }

        val actualEvent = eventsList.take(eventCountToShow)[position]
        Log.d("WidgetFactory", "getViewAt($position): Evento - ${actualEvent.description}")

        // --- INICIO: LEER PREFERENCIA Y ELEGIR LAYOUT ---
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // Asegúrate que "font_size_large_preference_key" es la misma clave que usaste en calendar_widget_preferences.xml
        val useLargeFont = prefs.getBoolean("font_size_large_preference_key", false)

        val layoutId = if (useLargeFont) {
            R.layout.widget_list_item_large
        } else {
            R.layout.widget_list_item_normal
        }
        // --- FIN: LEER PREFERENCIA Y ELEGIR LAYOUT ---

        val views = RemoteViews(context.packageName, layoutId) // Usa el layoutId determinado

        // --- FORMATO DE TEXTO Y DESCRIPCIÓN (como lo tenías) ---
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        var dayOfWeekStr = actualEvent.date.format(dayOfWeekFormatter)
        if (dayOfWeekStr.isNotEmpty()) {
            dayOfWeekStr = dayOfWeekStr.substring(0, 1).uppercase(Locale.getDefault()) +
                    (if (dayOfWeekStr.length > 1) dayOfWeekStr.substring(1).lowercase(Locale.getDefault()) else "")
        }
        views.setTextViewText(R.id.widget_item_day_of_week, dayOfWeekStr)

        val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_date_formatted, actualEvent.date.format(dateOnlyFormatter))

        val displayDescription: String
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        if (!actualEvent.isAllDay && actualEvent.startTime != null) {
            displayDescription = "${actualEvent.startTime.format(timeFormatter)} ${actualEvent.description.ifEmpty { "(Sin título)" }}"
        } else {
            displayDescription = actualEvent.description.ifEmpty { "(Evento)" }
        }
        views.setTextViewText(R.id.widget_item_description, displayDescription)
        // --- FIN FORMATO DE TEXTO ---

        // --- ★★★ INICIO: LÓGICA DE COLOR PARA EVENTOS DEL DÍA ACTUAL (OPCIÓN 1) ★★★ ---
        val today = LocalDate.now()
        val isTodayEvent = actualEvent.date.isEqual(today) // Asume que actualEvent.date es LocalDate

        val defaultTextColor = Color.parseColor("#A9A9A9") // Tu color de texto gris por defecto
        val todayTextColor = Color.parseColor("#FFC107")   // Amarillo para eventos de hoy

        if (isTodayEvent) {
            views.setTextColor(R.id.widget_item_day_of_week, todayTextColor)
            views.setTextColor(R.id.widget_item_date_formatted, todayTextColor)
            views.setTextColor(R.id.widget_item_description, todayTextColor)
        } else {
            // MUY IMPORTANTE: Restablecer al color por defecto para ítems que NO son de hoy
            views.setTextColor(R.id.widget_item_day_of_week, defaultTextColor)
            views.setTextColor(R.id.widget_item_date_formatted, defaultTextColor)
            views.setTextColor(R.id.widget_item_description, defaultTextColor)
        }
        // --- ★★★ FIN: LÓGICA DE COLOR ★★★ ---

        // Opcional: Configurar PendingIntent para clics en ítems
        // val fillInIntent = Intent()
        // fillInIntent.putExtra("event_id", actualEvent.calendarId) // o algún otro identificador único
        // views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent) // Necesita un R.id.widget_item_root en el XML del ítem

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null // O un layout de carga simple
    }

    override fun getViewTypeCount(): Int {
        return 2 // Porque ahora tenemos dos tipos de layout: normal y grande
    }

    override fun getItemId(position: Int): Long {
        return if (position < eventsList.take(eventCountToShow).size && position >= 0) {
            val event = eventsList.take(eventCountToShow)[position]
            // Un ID más robusto, usando campos del evento
            (event.date.toEpochDay() +
                    (event.startTime?.toSecondOfDay()?.toLong() ?: 0L) +
                    event.description.hashCode().toLong() +
                    event.calendarId // Asumiendo que calendarId es parte de Festivo y es un Long
                    ).hashCode().toLong() // Aplicar otro hashCode para asegurar que el resultado final sea Long
        } else {
            position.toLong() // Fallback, no ideal pero asegura un Long
        }
    }

    override fun hasStableIds(): Boolean {
        return true // Si getItemId devuelve IDs realmente únicos y estables
    }

    private fun loadEventCountSetting() {
        val prefs: SharedPreferences = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        eventCountToShow = prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        Log.d("WidgetFactory", "Configuración de contador de eventos cargada: $eventCountToShow")
    }

    private fun loadCalendarEvents() {
        val allEventsByDate = loadEventsFromPrefs(context) // Asume que esta función es accesible y devuelve Map<LocalDate, List<Festivo>>
        val today = LocalDate.now()
        val upcomingEvents = mutableListOf<Festivo>()
        val sortedDates = allEventsByDate.keys.sorted()

        for (date in sortedDates) {
            if (!date.isBefore(today)) { // Considerar hoy y fechas futuras
                val eventsOnDate = allEventsByDate[date].orEmpty().sortedWith(
                    compareBy(nullsLast()) { it.startTime } // Ordenar por hora, eventos de todo el día (null startTime) podrían ir al principio o final según nullsLast/nullsFirst
                )
                for (event in eventsOnDate) {
                    val hasMeaningfulDescription = event.description.isNotBlank()
                    val isTimedEvent = !event.isAllDay && event.startTime != null

                    // Lógica de filtrado para añadir al widget
                    // Esta lógica parece razonable: queremos eventos con descripción, o eventos con hora,
                    // o eventos de todo el día que originalmente tuvieran un título (isNotEmpty description)
                    if (hasMeaningfulDescription || isTimedEvent || (event.isAllDay && event.description.isNotEmpty())) {
                        if (date.isEqual(today) && event.startTime != null && !event.isAllDay) {
                            // Para eventos de hoy CON hora específica, solo añadir si la hora aún no ha pasado
                            if (event.startTime.isAfter(LocalTime.now())) {
                                upcomingEvents.add(event)
                            }
                        } else {
                            // Para eventos futuros, o eventos de hoy que son de todo el día, o eventos de hoy cuya hora ya pasó (pero queremos mostrarlo igual)
                            upcomingEvents.add(event)
                        }
                    }
                }
            }
        }
        eventsList = upcomingEvents
        Log.d("WidgetFactory", "Eventos futuros procesados para el widget: ${eventsList.size}")
    }
}
