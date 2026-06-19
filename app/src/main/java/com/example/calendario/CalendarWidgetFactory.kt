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
            Context.MODE_PRIVATE
        )
        eventCountToShow = try {
            prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        } catch (_: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_EVENT_COUNT] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_EVENT_COUNT
        }

        textBoost = try {
            prefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f)
        } catch (_: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_TEXT_BOOST] as? Number)?.toFloat() ?: 0f
        }
        widgetFontFamily = prefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY
        
        widgetFontBold = try {
            prefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD)
        } catch (_: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_FONT_BOLD] as? Boolean) ?: WidgetConstants.DEFAULT_WIDGET_FONT_BOLD
        }

        widgetEventColor = try {
            prefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
        }

        widgetTodayEventColor = try {
            prefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (prefs.all[WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB
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
        val baseDesc = if (!actualEvent.isAllDay && actualEvent.startTime != null) {
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
        } else {
            // Usamos INVISIBLE en lugar de GONE para que la columna de descripción mantenga su ancho fijo
            views.setViewVisibility(R.id.widget_item_alarm_icon, View.INVISIBLE)
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
        val selectedCalendarIds = loadSelectedCalendarIds(context)
        val allEventsByDateMap = if (selectedCalendarIds.isNotEmpty()) {
            readFestivosFromCalendarsSync(context, selectedCalendarIds)
        } else {
            emptyMap()
        }
        eventsList = processEventsForWidget(allEventsByDateMap, eventCountToShow)
    }
}
