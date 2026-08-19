package com.example.calendario

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarWidgetFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var eventsList: List<Festivo> = emptyList()
    private var eventCountToShow: Int = WidgetConstants.DEFAULT_EVENT_COUNT
    private var textBoost: Float = 0f
    private var widgetFontFamily: String = WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY
    private var widgetFontBold: Boolean = WidgetConstants.DEFAULT_WIDGET_FONT_BOLD

    private var widgetEventColor: Int = WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
    private var widgetTodayEventColor: Int = WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB

    override fun onCreate() {
        loadWidgetSettings()
        loadCalendarEvents()
    }

    override fun onDataSetChanged() {
        LogCollector.addLog("FÁBRICA: onDataSetChanged INICIO")
        eventsList = emptyList()
        loadWidgetSettings()
        loadCalendarEvents()
        LogCollector.addLog("FÁBRICA: Carga finalizada (${eventsList.size} eventos)")
    }

    private fun loadWidgetSettings() {
        val prefs: SharedPreferences = context.getSharedPreferences(
            WidgetConstants.GLOBAL_WIDGET_PREFS_NAME,
            Context.MODE_PRIVATE,
        )
        val allPrefs = prefs.all

        eventCountToShow = try {
            prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        } catch (_: ClassCastException) {
            (allPrefs[WidgetConstants.KEY_EVENT_COUNT] as? Number)?.toInt() 
                ?: allPrefs[WidgetConstants.KEY_EVENT_COUNT]?.toString()?.toIntOrNull() 
                ?: WidgetConstants.DEFAULT_EVENT_COUNT
        }

        textBoost = try {
            prefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f)
        } catch (_: ClassCastException) {
            (allPrefs[WidgetConstants.KEY_WIDGET_TEXT_BOOST] as? Number)?.toFloat() 
                ?: allPrefs[WidgetConstants.KEY_WIDGET_TEXT_BOOST]?.toString()?.toFloatOrNull() 
                ?: 0f
        }
        
        widgetFontFamily = try {
            prefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY)
        } catch (_: ClassCastException) {
            allPrefs[WidgetConstants.KEY_WIDGET_FONT_FAMILY]?.toString()
        } ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY
        
        widgetFontBold = try {
            prefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD)
        } catch (_: ClassCastException) {
            val v = allPrefs[WidgetConstants.KEY_WIDGET_FONT_BOLD]
            (v as? Boolean) ?: v?.toString()?.toBooleanStrictOrNull() ?: WidgetConstants.DEFAULT_WIDGET_FONT_BOLD
        }

        widgetEventColor = try {
            prefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (allPrefs[WidgetConstants.KEY_WIDGET_EVENT_COLOR] as? Number)?.toInt() 
                ?: allPrefs[WidgetConstants.KEY_WIDGET_EVENT_COLOR]?.toString()?.toLongOrNull()?.toInt()
                ?: WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
        }

        widgetTodayEventColor = try {
            prefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (allPrefs[WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR] as? Number)?.toInt() 
                ?: allPrefs[WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR]?.toString()?.toLongOrNull()?.toInt()
                ?: WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB
        }
    }

    override fun onDestroy() {
        eventsList = emptyList()
    }

    override fun getCount(): Int = eventsList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if ((position < 0) || (position >= eventsList.size)) return null

        val actualEvent = eventsList[position]

        // --- Layout Selection ---
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

        // --- Text Sizing ---
        val baseMultiplier = if (fontScale <= 1.35f) 1.20f else if (fontScale <= 1.5f) 1.1f else 1.0f
        val boostAmount = if (fontScale <= 1.35f) textBoost * 0.10f else textBoost * 0.05f
        val finalSize = baseTextSize * (baseMultiplier + boostAmount)

        views.setTextViewTextSize(R.id.widget_item_day_of_week, TypedValue.COMPLEX_UNIT_SP, finalSize)
        views.setTextViewTextSize(R.id.widget_item_date_formatted, TypedValue.COMPLEX_UNIT_SP, finalSize)
        views.setTextViewTextSize(R.id.widget_item_description, TypedValue.COMPLEX_UNIT_SP, finalSize)

        // --- Day and Date ---
        val dayOfWeekFullName = actualEvent.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val dayOfWeekFormatted = dayOfWeekFullName.take(3).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        views.setTextViewText(R.id.widget_item_day_of_week, applyFontStyles(dayOfWeekFormatted))

        val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_date_formatted, applyFontStyles(actualEvent.date.format(dateOnlyFormatter)))

        // --- Description ---
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val baseDesc = if (!actualEvent.isAllDay && (actualEvent.startTime != null)) {
            "${actualEvent.startTime.format(timeFormatter)} ${actualEvent.title}"
        } else {
            actualEvent.title
        }
        val agePart = if (actualEvent.age != null && actualEvent.age > 0) " (${actualEvent.age})" else ""
        val progressPart = if (actualEvent.isLongPeriod) " (${actualEvent.currentDay}/${actualEvent.totalDays})" else ""
        
        val fullDesc = "$baseDesc$agePart$progressPart"
        
        views.setTextViewText(R.id.widget_item_description, applyFontStyles(fullDesc))

        // --- Colors and Alarm Icon (Usando INVISIBLE para mantener alineación) ---
        val today = LocalDate.now()
        val isTodayEvent = actualEvent.date.isEqual(today)
        val currentTextColor = if (isTodayEvent) widgetTodayEventColor else widgetEventColor

        views.setTextColor(R.id.widget_item_day_of_week, currentTextColor)
        views.setTextColor(R.id.widget_item_date_formatted, currentTextColor)
        views.setTextColor(R.id.widget_item_description, currentTextColor)

        val showAlarmIcon = AlarmUtils.shouldShowAlarmIcon(context, actualEvent)
        if (showAlarmIcon) {
            views.setViewVisibility(R.id.widget_item_alarm_icon, View.VISIBLE)
            views.setInt(R.id.widget_item_alarm_icon, "setColorFilter", currentTextColor)
            
            // Sincronización de opacidad: Extraemos el Alpha del color del texto para aplicarlo al icono
            val colorAlpha = (currentTextColor shr 24) and 0xFF
            views.setInt(R.id.widget_item_alarm_icon, "setAlpha", colorAlpha)
            
            val alarmTime = AlarmUtils.getAlarmTimeString(context, actualEvent)
            if (alarmTime != null) {
                views.setViewVisibility(R.id.widget_item_alarm_time, View.VISIBLE)
                views.setTextViewText(R.id.widget_item_alarm_time, applyFontStyles(alarmTime))
                views.setTextColor(R.id.widget_item_alarm_time, currentTextColor)
            } else {
                views.setViewVisibility(R.id.widget_item_alarm_time, View.GONE)
            }
        } else {
            // Usamos INVISIBLE para el icono y GONE para el texto
            // Esto reserva el espacio de la campana y mantiene el título alineado
            views.setViewVisibility(R.id.widget_item_alarm_icon, View.INVISIBLE)
            views.setViewVisibility(R.id.widget_item_alarm_time, View.GONE)
        }

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)

        return views
    }

    private fun applyFontStyles(text: String): CharSequence {
        if (widgetFontFamily.isEmpty() && !widgetFontBold) return text
        val spannable = SpannableString(text)
        if (widgetFontFamily.isNotEmpty()) {
            try {
                spannable.setSpan(TypefaceSpan(widgetFontFamily), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } catch (_: Exception) {}
        }
        if (widgetFontBold) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        val appActiveIds = loadSelectedCalendarIds(context)
        
        val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val widgetSelectedIds = try {
            widgetPrefs.getStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, emptySet())
        } catch (_: ClassCastException) {
            // Autosanación: Si el tipo es incorrecto (String en vez de Set), lo ignoramos
            emptySet()
        }?.asSequence()
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet() ?: emptySet()

        // Lógica de Saneamiento:
        // 1. Verificamos qué IDs de la App existen realmente en el sistema actual
        val availableIds = loadAvailableCalendarsSync(context).map { it.id }.toSet()
        val validAppIds = appActiveIds.filter { it in availableIds }.toSet()
        
        // 2. Si el widget tiene selección propia, la filtramos contra lo que existe
        val validWidgetIds = widgetSelectedIds.filter { it in availableIds }.toSet()

        // 3. Decisión Final:
        // Si el widget no tiene selección válida, usamos lo que sea válido en la App.
        // Si nada es válido (post-restauración crítica), usamos el primer ID disponible.
        val effectiveIds = if (validWidgetIds.isNotEmpty()) {
            validWidgetIds
        } else if (validAppIds.isNotEmpty()) {
            validAppIds
        } else {
            availableIds.take(1).toSet()
        }

        val allEventsByDateMap = if (effectiveIds.isNotEmpty()) {
            readFestivosFromCalendarsSync(context, effectiveIds)
        } else {
            emptyMap()
        }
        eventsList = processEventsForWidget(allEventsByDateMap, eventCountToShow)
    }
}

/**
 * Versión síncrona de carga de calendarios para la Fábrica del Widget
 */
private fun loadAvailableCalendarsSync(context: Context): List<CalendarInfo> {
    val calendars = mutableListOf<CalendarInfo>()
    val projection = arrayOf(
        android.provider.CalendarContract.Calendars._ID,
        android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        android.provider.CalendarContract.Calendars.ACCOUNT_NAME,
        android.provider.CalendarContract.Calendars.OWNER_ACCOUNT,
        android.provider.CalendarContract.Calendars.IS_PRIMARY,
        android.provider.CalendarContract.Calendars.CALENDAR_COLOR,
        android.provider.CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
    )

    try {
        context.contentResolver.query(
            android.provider.CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars._ID)
            val nameCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.ACCOUNT_NAME)
            val ownerCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.OWNER_ACCOUNT)
            val primaryCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.IS_PRIMARY)
            val colorCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.CALENDAR_COLOR)
            val accessCol = cursor.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = cursor.getLong(idCol),
                        displayName = cursor.getString(nameCol),
                        accountName = cursor.getString(accCol),
                        ownerAccount = cursor.getString(ownerCol),
                        isPrimary = cursor.getInt(primaryCol) == 1,
                        color = cursor.getInt(colorCol),
                        canModify = cursor.getInt(accessCol) >= android.provider.CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                        accessLevel = cursor.getInt(accessCol),
                        isDeleted = false
                    )
                )
            }
        }
    } catch (_: Exception) {}
    return calendars
}
