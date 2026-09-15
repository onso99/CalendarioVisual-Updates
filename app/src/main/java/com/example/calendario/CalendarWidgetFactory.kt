package com.example.calendario

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.example.calendario.database.AppDatabase
import com.example.calendario.database.toFestivo
import java.time.LocalDate
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
        eventCountToShow = SettingsManager.getWidgetEventCount(context)
        textBoost = SettingsManager.getWidgetTextBoost(context)
        widgetFontFamily = SettingsManager.getWidgetFontFamily(context)
        widgetFontBold = SettingsManager.isWidgetFontBold(context)
        widgetEventColor = SettingsManager.getWidgetEventColor(context).toArgb()
        widgetTodayEventColor = SettingsManager.getWidgetTodayEventColor(context).toArgb()
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

        val dateOnlyFormatter = AppFormats.DateDayMonth
        views.setTextViewText(R.id.widget_item_date_formatted, applyFontStyles(actualEvent.date.format(dateOnlyFormatter)))

        // --- Description ---
        val timeFormatter = AppFormats.TimeShort
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
        val appActiveIds = SettingsManager.getSelectedCalendarIds(context)
        val widgetSelectedIds = SettingsManager.getWidgetSelectedCalendarIds(context)

        // 1. Verificamos qué IDs de la App existen realmente en el sistema actual
        val availableIds = loadAvailableCalendarsSync(context).map { it.id }.toSet()
        val validAppIds = appActiveIds.filter { it in availableIds }.toSet()
        
        // 2. Si el widget tiene selección propia, la filtramos contra lo que existe
        val validWidgetIds = widgetSelectedIds.filter { it in availableIds }.toSet()

        // 3. Decisión Final de Calendarios
        val effectiveIds = validWidgetIds
            .ifEmpty { validAppIds }
            .ifEmpty { availableIds.take(1).toSet() }

        // 4. CARGA DESDE ROOM (Unificación de fuente de verdad)
        val database = AppDatabase.getDatabase(context)
        val allRoomEvents = database.calendarDao().getAllEventsSync().map { it.toFestivo() }
        
        // Filtramos por calendarios seleccionados
        val filteredEvents = allRoomEvents.filter { event -> 
            effectiveIds.contains(event.calendarId) || event.calendarId == -1L 
        }

        val groupedEvents = filteredEvents.groupBy { it.date }
        eventsList = processEventsForWidget(groupedEvents, eventCountToShow)
    }

}
