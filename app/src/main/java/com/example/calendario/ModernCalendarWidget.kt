package com.example.calendario

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle

class ModernCalendarWidget : GlanceAppWidget() {

    // Eliminamos stateDefinition para usar SharedPreferences compartidas (MÃ¡s fiable)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val events = WidgetStateManager.getWidgetEvents(context)
            
            // Log de diagnÃ³stico interno del proceso del Widget
            if (events.isEmpty()) {
                LogCollector.addLog("WIDGET UI: Datos no encontrados en SharedPreferences")
            } else {
                LogCollector.addLog("WIDGET UI: Mostrando ${events.size} eventos")
            }
            
            GlanceTheme {
                WidgetContent(events)
            }
        }
    }

    @Composable
    private fun WidgetContent(events: List<WidgetStateManager.WidgetEvent>) {
        val context = LocalContext.current
        val appPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        
        val bgColorInt = appPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)
        val eventColorInt = appPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
        val textBoost = appPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f)
        val fontFamilyStr = appPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: ""
        val isBold = appPrefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD)

        val widgetFontFamily = when (fontFamilyStr) {
            WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif
            WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace
            WidgetConstants.FONT_FAMILY_CONDENSED -> FontFamily("sans-serif-condensed")
            WidgetConstants.FONT_FAMILY_SANS_SERIF -> FontFamily.SansSerif
            else -> FontFamily.SansSerif
        }
        
        val fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(bgColorInt))
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        ) {
            if (events.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = context.getString(R.string.widget_no_events),
                        style = TextStyle(
                            color = ColorProvider(Color(eventColorInt)),
                            fontSize = (14 + textBoost).sp,
                            fontFamily = widgetFontFamily,
                            fontWeight = fontWeight
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(events) { event ->
                        EventItem(event, textBoost, widgetFontFamily, fontWeight)
                    }
                }
            }
        }
    }

    @Composable
    private fun EventItem(
        event: WidgetStateManager.WidgetEvent,
        textBoost: Float,
        fontFamily: FontFamily,
        fontWeight: FontWeight
    ) {
        val context = LocalContext.current
        val today = LocalDate.now()
        val eventDate = LocalDate.ofEpochDay(event.dateEpochDay)
        val isToday = eventDate.isEqual(today)
        
        val appPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val todayColorInt = appPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
        val eventColorInt = appPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
        
        val colorInt = if (isToday) todayColorInt else eventColorInt
        val colorProvider = ColorProvider(Color(colorInt))
        
        val dateStr = eventDate.format(DateTimeFormatter.ofPattern("dd/MM"))
        val locale = context.resources.configuration.locales[0]
        val dayName = eventDate.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayName,
                modifier = GlanceModifier.width(36.dp),
                style = TextStyle(
                    color = colorProvider,
                    fontSize = (14 + textBoost).sp,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.End
                )
            )

            Text(
                text = dateStr,
                modifier = GlanceModifier.width(55.dp),
                style = TextStyle(
                    color = colorProvider,
                    fontSize = (14 + textBoost).sp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center
                )
            )

            val timePart = event.startTimeStr?.let { "${it.substring(0, 5)} " } ?: ""
            val agePart = event.age?.let { " ($it)" } ?: ""
            val progressPart = if (event.isLongPeriod) " (${event.currentDay}/${event.totalDays})" else ""
            
            Text(
                text = "$timePart${event.title}$agePart$progressPart",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = colorProvider,
                    fontSize = (14 + textBoost).sp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight
                ),
                maxLines = 1
            )

            if (event.alarmTimeStr != null) {
                Spacer(modifier = GlanceModifier.width(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_alarm_bell_outlined),
                        contentDescription = null,
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(colorProvider)
                    )
                    Text(
                        text = event.alarmTimeStr,
                        style = TextStyle(
                            color = colorProvider,
                            fontSize = (12 + textBoost).sp,
                            fontFamily = fontFamily,
                            fontWeight = fontWeight
                        ),
                        modifier = GlanceModifier.padding(start = 1.dp)
                    )
                }
            }
        }
    }
}
