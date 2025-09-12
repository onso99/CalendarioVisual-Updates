package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Asegúrate que WidgetConstants es importable (ej. si está en WidgetConstants.kt)
// import com.example.calendario.WidgetConstants

// Asegúrate que Festivo y FestivoDto son importables o visibles
// (ej. si están en sus propios archivos Festivo.kt, FestivoDto.kt o en un archivo común DataClasses.kt)
// import com.example.calendario.Festivo
// import com.example.calendario.FestivoDto
import com.example.calendario.R // Importante para R.id.widget_list_item_root


class CalendarWidgetFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var eventsList: List<Festivo> = emptyList()
    private var eventCountToShow: Int = WidgetConstants.DEFAULT_EVENT_COUNT
    private var useLargeFontForFactory: Boolean = false // Para la preferencia de fuente grande

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        Log.d("WidgetFactory", "onCreate - Widget ID: $appWidgetId. Cargando ajustes iniciales.")
        loadWidgetSettings()
        // loadCalendarEvents() // Considera si es necesario aquí o solo en onDataSetChanged
    }

    override fun onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged - Widget ID: $appWidgetId. Recargando ajustes y eventos.")
        loadWidgetSettings()
        loadCalendarEvents()
        Log.d("WidgetFactory", "Eventos cargados en onDataSetChanged: ${eventsList.size}, mostrando hasta: $eventCountToShow")
    }

    private fun loadWidgetSettings() {
        val prefs: SharedPreferences = context.getSharedPreferences(
            WidgetConstants.GLOBAL_WIDGET_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        eventCountToShow = prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        useLargeFontForFactory = prefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false)

        Log.d("WidgetFactory", "Configuración del widget cargada: Eventos a mostrar=$eventCountToShow, LetraGrande=$useLargeFontForFactory")
    }

    override fun onDestroy() {
        Log.d("WidgetFactory", "onDestroy - Widget ID: $appWidgetId")
        eventsList = emptyList()
    }

    override fun getCount(): Int {
        val count = eventsList.take(eventCountToShow).size
        Log.d("WidgetFactory", "getCount: $count (Total eventos procesados: ${eventsList.size}, Límite: $eventCountToShow)")
        return count
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= eventsList.take(eventCountToShow).size) {
            Log.w("WidgetFactory", "getViewAt: Posición inválida $position. Mostrando ${eventsList.take(eventCountToShow).size} de ${eventsList.size} eventos.")
            return null
        }

        val actualEvent = eventsList.take(eventCountToShow)[position]
        Log.d("WidgetFactory", "getViewAt($position): Evento - ${actualEvent.description}, Fecha: ${actualEvent.date}")

        val layoutId = if (useLargeFontForFactory) {
            R.layout.widget_list_item_large
        } else {
            R.layout.widget_list_item_normal
        }
        val views = RemoteViews(context.packageName, layoutId)

        // Formato de texto y descripción
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

        // Lógica de color para eventos del día actual
        val today = LocalDate.now()
        val isTodayEvent = actualEvent.date.isEqual(today)

        val defaultTextColor = Color.parseColor("#A9A9A9")
        val todayTextColor = Color.parseColor("#FFC107")

        val currentTextColor = if (isTodayEvent) todayTextColor else defaultTextColor
        views.setTextColor(R.id.widget_item_day_of_week, currentTextColor)
        views.setTextColor(R.id.widget_item_date_formatted, currentTextColor)
        views.setTextColor(R.id.widget_item_description, currentTextColor)

        // ***** INICIO: CÓDIGO MODIFICADO PARA EL PASO 4.1.b *****
        // Configurar el fill-in intent para manejar clics en ítems individuales.
        val fillInIntent = Intent()
        // Opcional: Añade datos específicos del ítem aquí si quieres que MainActivity los reciba.
        // Ejemplo:
        // fillInIntent.putExtra("EVENT_DESCRIPTION_EXTRA", actualEvent.description)
        // fillInIntent.putExtra("EVENT_DATE_EXTRA", actualEvent.date.toString())
        // fillInIntent.putExtra("WIDGET_ITEM_CLICKED_ID_EXTRA", actualEvent.calendarId) // o un ID único del evento
        // fillInIntent.action = "com.example.calendario.ACTION_VIEW_EVENT_DETAILS" // Acción personalizada

        // Asegúrate de que R.id.widget_list_item_root existe en tus layouts de ítem de lista.
        // Este ID será añadido en el siguiente paso (4.1.c) a los XMLs.
        views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)
        Log.d("WidgetFactory", "setOnClickFillInIntent configurado para el ítem en posición $position con R.id.widget_list_item_root")
        // ***** FIN: CÓDIGO MODIFICADO PARA EL PASO 4.1.b *****

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        // Ejemplo: return RemoteViews(context.packageName, R.layout.widget_loading_item)
        return null
    }

    override fun getViewTypeCount(): Int {
        return 2 // Porque tenemos dos layouts diferentes (normal y grande)
    }

    override fun getItemId(position: Int): Long {
        return if (position < eventsList.take(eventCountToShow).size && position >= 0) {
            val event = eventsList.take(eventCountToShow)[position]
            val uniqueString = "${event.date}-${event.startTime}-${event.description}-${event.calendarId}"
            uniqueString.hashCode().toLong()
        } else {
            System.currentTimeMillis() + position // Fallback
        }
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun loadCalendarEvents() {
        val allEventsByDateMap = loadEventsFromPrefs(context)
        val today = LocalDate.now()
        val upcomingEvents = mutableListOf<Festivo>()

        allEventsByDateMap.keys.sorted()
            .filter { date -> !date.isBefore(today) }
            .forEach { date ->
                val eventsOnDate = allEventsByDateMap[date].orEmpty().sortedWith(
                    compareBy(nullsLast()) { it.startTime }
                )
                for (event in eventsOnDate) {
                    val hasMeaningfulDescription = event.description.isNotBlank()
                    val isTimedEvent = !event.isAllDay && event.startTime != null

                    if (date.isEqual(today)) {
                        if (event.isAllDay || (isTimedEvent && event.startTime!!.isAfter(LocalTime.now())) || (hasMeaningfulDescription && !isTimedEvent)) {
                            upcomingEvents.add(event)
                        } else if (isTimedEvent && !event.startTime!!.isAfter(LocalTime.now())) {
                            // Log.d("WidgetFactory", "Evento de hoy omitido (ya pasó): ${event.description}")
                        }
                    } else { // Eventos futuros
                        upcomingEvents.add(event)
                    }
                }
            }
        eventsList = upcomingEvents // Aquí podrías querer aplicar un .take(eventCountToShow * ALGUN_FACTOR_DE_BUFFER) si el filtrado final es muy agresivo
        // o si eventCountToShow se aplica solo en getCount y getViewAt.
        // Actualmente, se filtran todos los eventos futuros y luego se toma el `eventCountToShow` en `getCount` y `getViewAt`.

        Log.d("WidgetFactory", "Eventos futuros procesados para el widget: ${eventsList.size}")
    }

    private fun loadEventsFromPrefs(context: Context): Map<LocalDate, List<Festivo>> {
        val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("events", null)
        if (json == null) {
            Log.d("WidgetFactory", "loadEventsFromPrefs: No hay eventos guardados en SharedPreferences 'events_prefs'.")
            return emptyMap()
        }
        val gson = Gson()
        val type = object : TypeToken<Map<String, List<FestivoDto>>>() {}.type
        val mapFromString: Map<String, List<FestivoDto>> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("WidgetFactory", "loadEventsFromPrefs: Error al deserializar eventos desde SharedPreferences", e)
            prefs.edit().remove("events").apply()
            return emptyMap()
        }

        return mapFromString.mapNotNull { (dateStr, dtoList) ->
            val date = try { LocalDate.parse(dateStr) } catch (e: Exception) {
                Log.e("WidgetFactory", "loadEventsFromPrefs: Error parseando fecha '$dateStr'", e); null
            }
            if (date != null) {
                date to dtoList.map { dto ->
                    Festivo(
                        date = date,
                        description = dto.desc,
                        calendarId = dto.id,
                        isFromHolidaySource = false, // Considera si necesitas persistir esto
                        startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) {
                            Log.e("WidgetFactory", "loadEventsFromPrefs: Error parseando LocalTime '$it'", e); null } },
                        isAllDay = dto.isAllDay
                    )
                }
            } else { null }
        }.toMap().also {
            Log.d("WidgetFactory", "loadEventsFromPrefs: Eventos cargados, ${it.size} días con eventos.")
        }
    }
}
