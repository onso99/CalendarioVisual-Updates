package com.example.calendario

// Imports necesarios para la Factory
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Log
import android.util.TypedValue
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
    private var textBoost: Float = 0f
    private var widgetFontFamily: String = WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY
    private var widgetFontBold: Boolean = WidgetConstants.DEFAULT_WIDGET_FONT_BOLD

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
        eventCountToShow = try {
            prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        } catch (e: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_EVENT_COUNT] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_EVENT_COUNT
        }

        textBoost = try {
            prefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f)
        } catch (e: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_TEXT_BOOST] as? Number)?.toFloat() ?: 0f
        }
        widgetFontFamily = prefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY
        
        widgetFontBold = try {
            prefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD)
        } catch (e: ClassCastException) {
            val value = prefs.all[WidgetConstants.KEY_WIDGET_FONT_BOLD]
            if (value is Boolean) value else WidgetConstants.DEFAULT_WIDGET_FONT_BOLD
        }

        widgetEventColor = try {
            prefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
        } catch (e: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
        }

        widgetTodayEventColor = try {
            prefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
        } catch (e: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB
        }

        Log.d("WidgetFactory", "Configuración del widget cargada: Eventos a mostrar=$eventCountToShow, AjusteTexto=$textBoost, Fuente=$widgetFontFamily, Bold=$widgetFontBold")
    }

    override fun onDestroy() {
        Log.d("WidgetFactory", "onDestroy - Widget ID: $appWidgetId")
        eventsList = emptyList()
    }

    override fun getCount(): Int {
        return eventsList.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= eventsList.size) {
            Log.w("WidgetFactory", "getViewAt: Posición inválida $position.")
            return null
        }

        val actualEvent = eventsList[position]

        // --- Lógica de Selección de Layout con 3 Niveles ---
        val fontScale = context.resources.configuration.fontScale
        val densityDpi = context.resources.displayMetrics.densityDpi
        val stressFactor = fontScale * (densityDpi / 160f)

        val layoutId: Int
        val baseTextSize: Float

        when {
            stressFactor <= 3.45f -> {
                layoutId = R.layout.widget_list_item_s
                baseTextSize = 12f
            }
            stressFactor <= 4.01f -> {
                layoutId = R.layout.widget_list_item_m
                baseTextSize = 14f
            }
            else -> {
                layoutId = R.layout.widget_list_item_l
                baseTextSize = 17f
            }
        }

        val views = RemoteViews(context.packageName, layoutId)

        // --- Lógica de Multiplicador Inteligente ---
        val baseMultiplier = when {
            fontScale <= 1.35f -> 1.20f // Calibrado
            fontScale <= 1.5f -> 1.1f
            else -> 1.0f
        }
        val boostAmount = when {
            fontScale <= 1.35f -> textBoost * 0.10f // Calibrado
            else -> textBoost * 0.05f
        }
        val finalMultiplier = baseMultiplier + boostAmount
        val finalSize = baseTextSize * finalMultiplier

        views.setTextViewTextSize(R.id.widget_item_day_of_week, TypedValue.COMPLEX_UNIT_SP, finalSize)
        views.setTextViewTextSize(R.id.widget_item_date_formatted, TypedValue.COMPLEX_UNIT_SP, finalSize)
        views.setTextViewTextSize(R.id.widget_item_description, TypedValue.COMPLEX_UNIT_SP, finalSize)

        val eventDate: LocalDate = actualEvent.date
        val dayOfWeekFullName = eventDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val dayOfWeekFormatted = dayOfWeekFullName.take(3).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        views.setTextViewText(R.id.widget_item_day_of_week, applyFontStyles(dayOfWeekFormatted))

        val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_date_formatted, applyFontStyles(actualEvent.date.format(dateOnlyFormatter)))

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val baseDesc = if (!actualEvent.isAllDay && actualEvent.startTime != null) {
            "${actualEvent.startTime.format(timeFormatter)} ${actualEvent.title}"
        } else {
            actualEvent.title
        }
        val displayDescription = if (actualEvent.age != null) "$baseDesc (${actualEvent.age})" else baseDesc
        
        views.setTextViewText(R.id.widget_item_description, applyFontStyles(displayDescription))

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

    private fun applyFontStyles(text: String): CharSequence {
        // Si no se fuerza familia y no hay negrita, devolvemos texto plano
        if (widgetFontFamily.isEmpty() && !widgetFontBold) return text

        val spannable = SpannableString(text)
        
        // 1. Aplicar Familia de Fuente (si no es Sistema)
        if (widgetFontFamily.isNotEmpty()) {
            try {
                spannable.setSpan(
                    TypefaceSpan(widgetFontFamily),
                    0,
                    text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } catch (e: Exception) {
                Log.e("WidgetFactory", "Error aplicando TypefaceSpan: ${e.message}")
            }
        }

        // 2. Aplicar Negrita (si está activa)
        if (widgetFontBold) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int): Long {
        return if (position < eventsList.size && position >= 0) {
            val event = eventsList[position]
            "${event.date}-${event.startTime}-${event.title}-${event.calendarId}-${event.isAllDay}".hashCode().toLong()
        } else {
            System.currentTimeMillis() + position.toLong()
        }
    }

    override fun hasStableIds(): Boolean = true

    private fun loadCalendarEvents() {
        val allEventsByDateMap = loadEventsFromPrefsFromFactory(context)
        eventsList = processEventsForWidget(allEventsByDateMap, eventCountToShow)
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
            } catch (_: Exception) {
                null
            }
            if (date != null) {
                date to dtoList.map { dto ->
                    Festivo(
                        id = -1L,
                        title = dto.title.takeIf { !it.isNullOrBlank() } ?: dto.description.takeIf { !it.isNullOrBlank() } ?: "(Evento guardado)",
                        description = dto.description,
                        date = date,
                        startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (_: Exception) { null } },
                        endTime = dto.endTimeStr?.let { try { LocalTime.parse(it) } catch (_: Exception) { null } },
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
