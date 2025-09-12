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
        // No es estrictamente necesario llamar a loadCalendarEvents() aquí si onDataSetChanged()
        // siempre se llama después y antes de la primera llamada a getViewAt().
        // Sin embargo, si quieres asegurar datos frescos al crear, puedes descomentarlo.
        // loadCalendarEvents()
    }

    override fun onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged - Widget ID: $appWidgetId. Recargando ajustes y eventos.")
        // 1. Cargar la configuración del widget (número de eventos, tamaño de fuente)
        loadWidgetSettings()

        // 2. Cargar los datos de los eventos
        loadCalendarEvents()
        Log.d("WidgetFactory", "Eventos cargados en onDataSetChanged: ${eventsList.size}, mostrando hasta: $eventCountToShow")
    }

    private fun loadWidgetSettings() {
        val prefs: SharedPreferences = context.getSharedPreferences(
            WidgetConstants.GLOBAL_WIDGET_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        eventCountToShow = prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        useLargeFontForFactory = prefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) // false por defecto

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
            return null // Devuelve null si la posición es inválida
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
            displayDescription = actualEvent.description.ifEmpty { "(Evento)" } // "(Evento todo el día)" podría ser más descriptivo si es allDay
        }
        views.setTextViewText(R.id.widget_item_description, displayDescription)

        // Lógica de color para eventos del día actual
        val today = LocalDate.now()
        val isTodayEvent = actualEvent.date.isEqual(today)

        val defaultTextColor = Color.parseColor("#A9A9A9") // Gris
        val todayTextColor = Color.parseColor("#FFC107")   // Amarillo

        val currentTextColor = if (isTodayEvent) todayTextColor else defaultTextColor
        views.setTextColor(R.id.widget_item_day_of_week, currentTextColor)
        views.setTextColor(R.id.widget_item_date_formatted, currentTextColor)
        views.setTextColor(R.id.widget_item_description, currentTextColor)

        // Configurar el fill-in intent para manejar clics en ítems individuales (opcional)
        // val fillInIntent = Intent()
        // fillInIntent.putExtra("EVENT_DATE", actualEvent.date.toString()) // Ejemplo de dato
        // views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent) // Asume que el layout raíz del item tiene este ID

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        // Puedes retornar un layout de carga simple si la carga de datos es lenta.
        // Ejemplo: return RemoteViews(context.packageName, R.layout.widget_loading_item)
        // Por ahora, null es aceptable si la carga es rápida.
        return null
    }

    override fun getViewTypeCount(): Int {
        // Porque tenemos dos layouts diferentes (normal y grande)
        return 2
    }

    override fun getItemId(position: Int): Long {
        // Es crucial que esto devuelva un ID único y estable para cada ítem en la lista filtrada.
        return if (position < eventsList.take(eventCountToShow).size && position >= 0) {
            val event = eventsList.take(eventCountToShow)[position]
            // Crear un ID basado en las propiedades del evento.
            // Sumar hashCodes puede no ser suficientemente único. Concatenar y luego hashear es mejor.
            val uniqueString = "${event.date}-${event.startTime}-${event.description}-${event.calendarId}"
            uniqueString.hashCode().toLong()
        } else {
            // Fallback, pero debería evitarse llegar aquí si getCount es correcto.
            System.currentTimeMillis() + position // No es estable, pero es un fallback
        }
    }

    override fun hasStableIds(): Boolean {
        // Si getItemId() devuelve IDs que son verdaderamente únicos y no cambian
        // para el mismo ítem lógico a través de diferentes cargas de datos, retorna true.
        return true
    }

    private fun loadCalendarEvents() {
        val allEventsByDateMap = loadEventsFromPrefs(context) // Usa la función de abajo
        val today = LocalDate.now()
        val upcomingEvents = mutableListOf<Festivo>()

        // Procesar eventos de hoy y futuros
        allEventsByDateMap.keys.sorted()
            .filter { date -> !date.isBefore(today) } // Considerar hoy y fechas futuras
            .forEach { date ->
                val eventsOnDate = allEventsByDateMap[date].orEmpty().sortedWith(
                    compareBy(nullsLast()) { it.startTime } // Ordenar por hora
                )
                for (event in eventsOnDate) {
                    val hasMeaningfulDescription = event.description.isNotBlank()
                    val isTimedEvent = !event.isAllDay && event.startTime != null

                    // Lógica de filtrado mejorada
                    if (hasMeaningfulDescription || (isTimedEvent && event.startTime!!.isAfter(LocalTime.now())) || (date.isAfter(today)) || (event.isAllDay && date.isEqual(today))) {
                        // Si es hoy y tiene hora, solo si no ha pasado.
                        // Si es un evento futuro, se añade.
                        // Si es un evento de todo el día para hoy, se añade.
                        // Si es un evento con descripción y no es de hoy con hora pasada, se añade.

                        if (date.isEqual(today) && !event.isAllDay && event.startTime != null) {
                            if (event.startTime.isAfter(LocalTime.now())) {
                                upcomingEvents.add(event)
                            } else {
                                // Opción: podrías querer mostrar eventos de hoy que ya pasaron
                                // Log.d("WidgetFactory", "Evento de hoy omitido (ya pasó): ${event.description}")
                            }
                        } else {
                            upcomingEvents.add(event)
                        }
                    }
                }
            }
        // No es necesario ordenar aquí si ya se ordenó al procesar `allEventsByDateMap.keys.sorted()`
        // y `eventsOnDate.sortedWith(...)`.
        // Si necesitas un ordenamiento global final, aplícalo aquí.
        // upcomingEvents.sortBy { it.date.atTime(it.startTime ?: LocalTime.MIDNIGHT) } // Ejemplo de ordenamiento final

        eventsList = upcomingEvents
        Log.d("WidgetFactory", "Eventos futuros procesados para el widget: ${eventsList.size}")
    }

    // --- COPIA DE loadEventsFromPrefs ---
    // Esta función carga los eventos desde SharedPreferences.
    // Necesita que `Festivo` y `FestivoDto` sean accesibles.
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
            prefs.edit().remove("events").apply() // Limpiar datos corruptos
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
                        isFromHolidaySource = false, // Esta info se pierde/reconstruye al guardar/cargar así.
                        // El widget podría no necesitarla o necesitarías guardarla.
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
