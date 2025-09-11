package com.example.calendario // Asegúrate que este es tu nombre de paquete correcto

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
// import android.os.Build // No se usa directamente en este archivo con las modificaciones actuales
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        // loadCalendarEvents() // Se llama en onDataSetChanged
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

        val views = RemoteViews(context.packageName, R.layout.widget_list_item)

        // --- INICIO DE MODIFICACIONES PARA EL NUEVO FORMATO ---

        // 1. Día de la semana (Ej: "Lun", "Mar")
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        var dayOfWeekStr = actualEvent.date.format(dayOfWeekFormatter)

        // Capitalizar solo la primera letra y asegurarse de que el resto sea minúscula
        if (dayOfWeekStr.isNotEmpty()) {
            dayOfWeekStr = dayOfWeekStr.substring(0, 1).uppercase(Locale.getDefault()) +
                    (if (dayOfWeekStr.length > 1) dayOfWeekStr.substring(1).lowercase(Locale.getDefault()) else "")
            // Opcional: si "EEE" puede generar un punto al final (ej. "lun."), quitarlo:
            // if (dayOfWeekStr.endsWith(".")) {
            //     dayOfWeekStr = dayOfWeekStr.substring(0, dayOfWeekStr.length - 1)
            // }
        }
        views.setTextViewText(R.id.widget_item_day_of_week, dayOfWeekStr)

        // 2. Fecha formateada (dd/MM)
        val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_date_formatted, actualEvent.date.format(dateOnlyFormatter))

        // 3. Descripción del evento (con hora si no es todo el día)
        val displayDescription: String
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm") // Formato de 24 horas

        if (!actualEvent.isAllDay && actualEvent.startTime != null) {
            displayDescription = "${actualEvent.startTime.format(timeFormatter)} ${actualEvent.description.ifEmpty { "(Sin título)" }}"
        } else {
            // Para eventos de todo el día o sin hora, solo la descripción
            displayDescription = actualEvent.description.ifEmpty { "(Evento)" } // Placeholder corto si la descripción está vacía
        }
        views.setTextViewText(R.id.widget_item_description, displayDescription)

        // --- FIN DE MODIFICACIONES PARA EL NUEVO FORMATO ---


        // La plantilla para PendingIntent al hacer clic en un ítem se manejará en el Paso 3
        // si decides implementarla. Por ahora, nos centramos en el formato visual.
        // val fillInIntent = Intent()
        // views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent) // Necesita R.id.widget_item_root en el XML

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null // O un layout de carga simple
    }

    override fun getViewTypeCount(): Int {
        return 1 // Todos los ítems usan el mismo layout
    }

    override fun getItemId(position: Int): Long {
        return if (position < eventsList.take(eventCountToShow).size && position >= 0) {
            val event = eventsList.take(eventCountToShow)[position]
            // Crear un ID más robusto si es posible. Combina campos esenciales.
            (event.date.toEpochDay() +
                    (event.startTime?.toSecondOfDay()?.toLong() ?: 0L) +
                    event.description.hashCode().toLong() +
                    event.calendarId).hashCode().toLong()
        } else {
            position.toLong() // Fallback
        }
    }

    override fun hasStableIds(): Boolean {
        return true // Si getItemId devuelve IDs realmente únicos y estables
    }

    private fun loadEventCountSetting() {
        val prefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        eventCountToShow = prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        Log.d("WidgetFactory", "Configuración de contador de eventos cargada: $eventCountToShow")
    }

    private fun loadCalendarEvents() {
        val allEventsByDate = loadEventsFromPrefs(context) // Asume que esta función es accesible
        val today = LocalDate.now()
        val upcomingEvents = mutableListOf<Festivo>()
        val sortedDates = allEventsByDate.keys.sorted()

        for (date in sortedDates) {
            if (!date.isBefore(today)) {
                val eventsOnDate = allEventsByDate[date].orEmpty().sortedWith(
                    compareBy(nullsLast()) { it.startTime }
                )
                for (event in eventsOnDate) {
                    // La lógica de filtrado de eventos aquí es importante.
                    // Asegúrate que solo añade eventos "significativos" para el widget.
                    // Por ejemplo, eventos con descripción o eventos con hora.

                    // Considerar solo eventos que tienen alguna descripción o no son de todo el día con hora
                    // Esta lógica ya estaba bien:
                    val hasMeaningfulDescription = event.description.isNotBlank() // El placeholder se añade en getViewAt si es necesario
                    val isTimedEvent = !event.isAllDay && event.startTime != null

                    // Si la descripción está completamente vacía Y es un evento de todo el día sin hora,
                    // podríamos optar por no mostrarlo si no es útil.
                    // La lógica actual en getViewAt pone "(Evento)" si está vacío.
                    // Aquí decidimos si AÑADIRLO a la lista de eventos del widget.

                    // Si el evento es solo un placeholder como "(Evento todo el día)" y no queremos mostrarlo
                    // a menos que tenga una descripción real, aquí se podría filtrar.
                    // Por ahora, lo mantenemos como estaba, confiando en que getViewAt lo formateará.

                    if (hasMeaningfulDescription || isTimedEvent || (event.isAllDay && event.description.isNotEmpty())) { // Ligeramente ajustado para asegurar que eventos todo el día sin descripción pero que tuvieran un título originalmente sí se consideren
                        if (date == today && event.startTime != null && !event.isAllDay) {
                            if (event.startTime.isAfter(LocalTime.now())) {
                                upcomingEvents.add(event)
                            }
                        } else {
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

