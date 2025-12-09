package com.example.calendario

// Imports necesarios para la Factory
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var eventsList: List<Festivo> = emptyList()
    private var eventCountToShow: Int = WidgetConstants.DEFAULT_EVENT_COUNT
    private var useLargeFontForFactory: Boolean = false

    private var widgetEventColor: Int = WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
    private var widgetTodayEventColor: Int = WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        Log.d("WidgetFactory", "onCreate - Widget ID: $appWidgetId. Cargando ajustes iniciales y eventos.")
        loadWidgetSettings()
        loadCalendarEvents()
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

        widgetEventColor = prefs.getInt(
            WidgetConstants.KEY_WIDGET_EVENT_COLOR,
            WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
        )
        widgetTodayEventColor = prefs.getInt(
            WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR,
            WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB
        )

        Log.d("WidgetFactory", "Configuración del widget cargada: Eventos a mostrar=$eventCountToShow, LetraGrande=$useLargeFontForFactory, ColorEvento=0x${Integer.toHexString(widgetEventColor)}, ColorHoy=0x${Integer.toHexString(widgetTodayEventColor)}")
    }

    override fun onDestroy() {
        Log.d("WidgetFactory", "onDestroy - Widget ID: $appWidgetId")
        eventsList = emptyList()
    }

    override fun getCount(): Int {
        return eventsList.take(eventCountToShow).size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= eventsList.take(eventCountToShow).size) {
            Log.w("WidgetFactory", "getViewAt: Posición inválida $position.")
            return null
        }

        val actualEvent = eventsList.take(eventCountToShow)[position]

        val layoutId = if (useLargeFontForFactory) {
            R.layout.widget_list_item_large
        } else {
            R.layout.widget_list_item_normal
        }
        val views = RemoteViews(context.packageName, layoutId)

        val eventDate: LocalDate = actualEvent.date
        val dayOfWeekFullName = eventDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val dayOfWeekFormatted = dayOfWeekFullName.take(3).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        views.setTextViewText(R.id.widget_item_day_of_week, dayOfWeekFormatted)

        val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_date_formatted, actualEvent.date.format(dateOnlyFormatter))

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val baseDesc = if (!actualEvent.isAllDay && actualEvent.startTime != null) {
            "${actualEvent.startTime.format(timeFormatter)} ${actualEvent.title}"
        } else {
            actualEvent.title
        }
        val displayDescription = if (actualEvent.age != null) "$baseDesc (${actualEvent.age})" else baseDesc
        
        views.setTextViewText(R.id.widget_item_description, displayDescription)

        val today = LocalDate.now()
        val isTodayEvent = actualEvent.date.isEqual(today)

        val currentTextColor = if (isTodayEvent) widgetTodayEventColor else widgetEventColor

        views.setTextColor(R.id.widget_item_day_of_week, currentTextColor)
        views.setTextColor(R.id.widget_item_date_formatted, currentTextColor)
        views.setTextColor(R.id.widget_item_description, currentTextColor)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long {
        return if (position < eventsList.take(eventCountToShow).size && position >= 0) {
            val event = eventsList.take(eventCountToShow)[position]
            "${event.date}-${event.startTime}-${event.title}-${event.calendarId}-${event.isAllDay}".hashCode().toLong()
        } else {
            System.currentTimeMillis() + position.toLong()
        }
    }

    override fun hasStableIds(): Boolean = true

    private fun loadCalendarEvents() {
        val allEventsByDateMap = loadEventsFromPrefsFromFactory(context)
        val today = LocalDate.now()
        val nowTime = LocalTime.now()
        val upcomingEvents = mutableListOf<Festivo>()

        Log.d("WidgetFactory", "Iniciando loadCalendarEvents. Hoy: $today, Hora Actual: $nowTime")

        allEventsByDateMap.keys.sorted().forEach { date ->
            val eventsOnDate = allEventsByDateMap[date].orEmpty()

            for (event in eventsOnDate) {
                if (date.isBefore(today)) continue

                if (date.isEqual(today)) {
                    if (!event.isAllDay && event.startTime != null) {
                        if (event.endTime != null) {
                            if (nowTime.isBefore(event.endTime)) {
                                upcomingEvents.add(event)
                            } else {
                                Log.d("WidgetFactory", "Evento de hoy OMITIDO (endTime ${event.endTime} ya pasó a las $nowTime): ${event.title}")
                            }
                        } else {
                            var addedSinEndTime = false
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
                                Log.d("WidgetFactory", "Evento de hoy (sin endTime) OMITIDO (startTime ${event.startTime} no cumple criterio): ${event.title}")
                            }
                        }
                    } else {
                        upcomingEvents.add(event)
                    }
                } else {
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
    }

    private fun loadEventsFromPrefsFromFactory(context: Context): Map<LocalDate, List<Festivo>> {
        val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("events", null)
        if (json == null) {
            Log.d("WidgetFactory", "No hay eventos guardados en SharedPreferences para el factory.")
            return emptyMap()
        }
        val gson = Gson()
        val type = object : TypeToken<Map<String, List<FestivoDto>>>() {}.type
        val mapFromString: Map<String, List<FestivoDto>> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("WidgetFactory", "Error al deserializar eventos desde SharedPreferences en el factory", e)
            return emptyMap()
        }

        return mapFromString.mapNotNull { (dateStr, dtoList) ->
            val date = try {
                LocalDate.parse(dateStr)
            } catch (e: Exception) {
                null
            }
            if (date != null) {
                date to dtoList.map { dto ->
                    Festivo(
                        id = -1L,
                        title = dto.title.takeIf { !it.isNullOrBlank() } ?: dto.description.takeIf { !it.isNullOrBlank() } ?: "(Evento guardado)",
                        description = dto.description,
                        date = date,
                        startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } },
                        endTime = dto.endTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } },
                        isAllDay = dto.isAllDay,
                        calendarId = dto.id,
                        isFromHolidaySource = false,
                        rrule = dto.rrule,
                        age = dto.age
                    )
                }
            } else {
                null
            }
        }.toMap()
    }
}
