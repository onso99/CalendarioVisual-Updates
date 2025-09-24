package com.example.calendario

// Imports necesarios para la Factory
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
// import android.graphics.Color // No es necesario si obtenemos ARGB de constantes
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.ZoneId
import java.util.Locale

// Imports para tus clases/objetos definidos en otros archivos
import com.example.calendario.WidgetConstants
import com.example.calendario.Festivo
import com.example.calendario.FestivoDto
// import com.example.calendario.R // R se resuelve automáticamente


class CalendarWidgetFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var eventsList: List<Festivo> = emptyList()
    private var eventCountToShow: Int = WidgetConstants.DEFAULT_EVENT_COUNT
    private var useLargeFontForFactory: Boolean = false

    // --- ★★★ VARIABLES MIEMBRO PARA COLORES CARGADOS/POR DEFECTO ★★★ ---
    private var widgetEventColor: Int = WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
    private var widgetTodayEventColor: Int = WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB
    // --- FIN DE VARIABLES MIEMBRO ---

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        Log.d("WidgetFactory", "onCreate - Widget ID: $appWidgetId. Cargando ajustes iniciales y eventos.")
        loadWidgetSettings() // Esto ahora cargará los colores también
        loadCalendarEvents()
    }

    override fun onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged - Widget ID: $appWidgetId. Recargando ajustes y eventos.")
        loadWidgetSettings() // Esto ahora cargará los colores también
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

        // --- ★★★ LEER COLORES DE PREFERENCIAS ★★★ ---
        widgetEventColor = prefs.getInt(
            WidgetConstants.KEY_WIDGET_EVENT_COLOR,
            WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB // Fallback a constante
        )
        widgetTodayEventColor = prefs.getInt(
            WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR,
            WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB // Fallback a constante
        )
        // --- FIN DE LEER COLORES ---

        Log.d("WidgetFactory", "Configuración del widget cargada: Eventos a mostrar=$eventCountToShow, LetraGrande=$useLargeFontForFactory, ColorEvento=0x${Integer.toHexString(widgetEventColor)}, ColorHoy=0x${Integer.toHexString(widgetTodayEventColor)}")
    }

    override fun onDestroy() {
        Log.d("WidgetFactory", "onDestroy - Widget ID: $appWidgetId")
        eventsList = emptyList()
    }

    override fun getCount(): Int {
        val count = eventsList.take(eventCountToShow).size
        // Log.d("WidgetFactory", "getCount: $count (Total eventos procesados: ${eventsList.size}, Límite: $eventCountToShow)") // Puede ser muy verboso
        return count
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= eventsList.take(eventCountToShow).size) {
            Log.w("WidgetFactory", "getViewAt: Posición inválida $position.")
            return null
        }

        val actualEvent = eventsList.take(eventCountToShow)[position]
        // Log.d("WidgetFactory", "getViewAt($position): Evento - ${actualEvent.description}, Fecha: ${actualEvent.date}") // Puede ser verboso

        val layoutId = if (useLargeFontForFactory) {
            R.layout.widget_list_item_large
        } else {
            R.layout.widget_list_item_normal
        }
        val views = RemoteViews(context.packageName, layoutId)

        val eventDate: LocalDate = actualEvent.date
        val dayOfWeekShortOriginal = eventDate.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
        val dayOfWeekFormatted: String = if (dayOfWeekShortOriginal.length >= 2) {
            dayOfWeekShortOriginal.substring(0, 1).uppercase(Locale.getDefault()) +
                    dayOfWeekShortOriginal.substring(1, 2).lowercase(Locale.getDefault())
        } else if (dayOfWeekShortOriginal.isNotEmpty()) {
            dayOfWeekShortOriginal.uppercase(Locale.getDefault())
        } else {
            ""
        }
        views.setTextViewText(R.id.widget_item_day_of_week, dayOfWeekFormatted)

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

        val today = LocalDate.now()
        val isTodayEvent = actualEvent.date.isEqual(today)

        // --- ★★★ USAR COLORES CARGADOS/POR DEFECTO DE LAS VARIABLES MIEMBRO ★★★ ---
        val currentTextColor = if (isTodayEvent) widgetTodayEventColor else widgetEventColor
        // --- FIN DE USAR COLORES CARGADOS ---

        views.setTextColor(R.id.widget_item_day_of_week, currentTextColor)
        views.setTextColor(R.id.widget_item_date_formatted, currentTextColor)
        views.setTextColor(R.id.widget_item_description, currentTextColor)

        // Configurar el intent de relleno para clics en elementos de la lista
        val fillInIntent = Intent().apply {
            // Puedes añadir extras aquí si quieres pasar datos específicos al PendingIntent de la plantilla en CalendarAppWidgetProvider
            // Por ejemplo, para abrir la app en una fecha específica o mostrar detalles del evento.
            // putExtra("EVENT_ID_OR_DATE", actualEvent.date.toString()) // Ejemplo
        }
        views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        // Puedes retornar un RemoteViews aquí si quieres una vista de carga personalizada mientras se cargan los datos.
        // Por ahora, null está bien.
        return null
    }

    override fun getViewTypeCount(): Int {
        return 2 // Porque usas widget_list_item_large y widget_list_item_normal
    }

    override fun getItemId(position: Int): Long {
        return if (position < eventsList.take(eventCountToShow).size && position >= 0) {
            val event = eventsList.take(eventCountToShow)[position]
            // Crear un ID más robusto si es posible. El hashCode puede tener colisiones,
            // pero para una lista pequeña y con estos campos suele ser suficiente.
            // Considerar una combinación de `event.date.toEpochDay()` y otros identificadores si es necesario.
            "${event.date}-${event.startTime}-${event.description}-${event.calendarId}-${event.isAllDay}".hashCode().toLong()
        } else {
            // Fallback para posiciones inválidas, aunque idealmente no debería llegar aquí si getCount() es correcto.
            System.currentTimeMillis() + position.toLong() // No es ideal, pero es un fallback
        }
    }

    override fun hasStableIds(): Boolean {
        // Devuelve true porque getItemId devuelve IDs únicos y consistentes para los mismos elementos.
        return true
    }


    private fun loadCalendarEvents() {
        val allEventsByDateMap = loadEventsFromPrefsFromFactory(context)
        val today = LocalDate.now()
        val nowTime = LocalTime.now()
        val upcomingEvents = mutableListOf<Festivo>()

        Log.d("WidgetFactory", "Iniciando loadCalendarEvents. Hoy: $today, Hora Actual: $nowTime")
        // Log.d("WidgetFactoryDebug", "Mapa de eventos cargado de prefs tiene ${allEventsByDateMap.size} días con eventos.") // Eliminado/Comentado

        allEventsByDateMap.keys.sorted()
            .forEach { date ->
                val eventsOnDate = allEventsByDateMap[date].orEmpty()

                for (event in eventsOnDate) {
                    if (date.isBefore(today)) {
                        continue
                    }

                    if (date.isEqual(today)) {
                        if (!event.isAllDay && event.startTime != null) {
                            if (event.endTime != null) {
                                // --- LOGS DE DEPURACIÓN DETALLADOS ELIMINADOS DE AQUÍ ---
                                if (nowTime.isBefore(event.endTime)) {
                                    upcomingEvents.add(event)
                                } else {
                                    // Este log es útil, así que lo conservamos:
                                    Log.d("WidgetFactory", "Evento de hoy OMITIDO (endTime ${event.endTime} ya pasó a las $nowTime): ${event.description}")
                                }
                            } else { // Evento de hoy con startTime pero SIN endTime
                                // --- LOGS DE DEPURACIÓN DETALLADOS ELIMINADOS DE AQUÍ ---
                                var addedSinEndTime = false // Para el log de omisión
                                if (nowTime.isBefore(event.startTime.plusMinutes(1))) {
                                    upcomingEvents.add(event)
                                    addedSinEndTime = true
                                } else if (event.startTime.isBefore(nowTime) && event.startTime.plusHours(1).isAfter(nowTime)) {
                                    upcomingEvents.add(event)
                                    addedSinEndTime = true
                                } else if (!nowTime.isAfter(event.startTime)) {
                                    upcomingEvents.add(event)
                                    addedSinEndTime = true
                                }

                                if (!addedSinEndTime) {
                                    // Este log también puede ser útil:
                                    Log.d("WidgetFactory", "Evento de hoy (sin endTime) OMITIDO (startTime ${event.startTime} no cumple criterio actual para mostrarse): ${event.description}")
                                }
                            }
                        } else { // Evento de hoy de todo el día o sin hora de inicio
                            // --- LOGS DE DEPURACIÓN DETALLADOS ELIMINADOS DE AQUÍ ---
                            upcomingEvents.add(event)
                        }
                    } else { // Eventos de días FUTUROS
                        upcomingEvents.add(event)
                    }
                }
            }

        eventsList = upcomingEvents.sortedWith(
            compareBy<Festivo> { it.date }
                .thenByDescending { it.isAllDay }
                .thenBy(nullsLast()) { it.startTime }
        )

        Log.d("WidgetFactory", "Eventos procesados para el widget: ${eventsList.size}. Mostrando hasta: $eventCountToShow")
        // El log para mostrar la lista final puede ser útil para depuración futura, así que lo comento:
        // eventsList.take(eventCountToShow).forEachIndexed { index, festivo ->
        //     Log.d("WidgetFactory", "  Lista Final Widget [$index]: ${festivo.description} - ${festivo.date} ${festivo.startTime ?: ""} (TodoDia: ${festivo.isAllDay})")
        // }
    }



    // Renombrado para evitar confusión con la función global, pero es la misma lógica.
    // Podrías mover esta lógica a CalendarDataUtils.kt si no está ya allí y es compartida.
    private fun loadEventsFromPrefsFromFactory(context: Context): Map<LocalDate, List<Festivo>> {
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
            return emptyMap() // Podrías también limpiar prefs aquí si están corruptas.
        }

        return mapFromString.mapNotNull { (dateStr, dtoList) ->
            val date = try { LocalDate.parse(dateStr) } catch (e: Exception) {
                Log.e("WidgetFactory", "loadEventsFromPrefs: Error parseando fecha '$dateStr'", e); null
            }
            if (date != null) {
                date to dtoList.map { dto ->
                    Festivo(
                        id = -1L,
                        title = dto.desc.takeIf { it.isNotBlank() }?.take(40)?.trim() ?: "(Evento widget)",
                        description = dto.desc,
                        date = date,
                        calendarId = dto.id,
                        isFromHolidaySource = false, // Considera cómo manejar esto si es importante para el widget
                        startTime = dto.startTimeStr?.let {
                            try { LocalTime.parse(it) } catch (e: Exception) {
                                Log.e("WidgetFactory", "loadEventsFromPrefsFromFactory: Error parseando startTime '$it' para fecha $dateStr", e); null
                            }
                        },
                        endTime = dto.endTimeStr?.let { // <<< --- AÑADIR ESTA LÍNEA
                            try { LocalTime.parse(it) } catch (e: Exception) {
                                Log.e("WidgetFactory", "loadEventsFromPrefsFromFactory: Error parseando endTime '$it' para fecha $dateStr", e); null
                            }
                        },
                        isAllDay = dto.isAllDay
                    )
                }
            } else { null }
        }.toMap().also {
            // Log.d("WidgetFactory", "loadEventsFromPrefs: Eventos cargados, ${it.size} días con eventos.")
        }
    }


}
