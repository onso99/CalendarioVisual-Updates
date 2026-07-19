package com.example.calendario

import android.annotation.SuppressLint
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
import androidx.glance.currentState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle

class ModernCalendarWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            
            // 1. Extraer Eventos
            val json = prefs[WidgetStateManager.KEY_WIDGET_DATA] ?: ""
            val events = try { Gson().fromJson(json, Array<WidgetStateManager.WidgetEvent>::class.java).toList() } catch (_: Exception) { emptyList() }
            
            // 2. Extraer Ajustes Visuales del Estado (Ya no leemos SharedPreferences aquÃ­)
            val bgColor = Color(prefs[WidgetStateManager.KEY_BG_COLOR] ?: WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)
            val eventColor = Color(prefs[WidgetStateManager.KEY_EVENT_COLOR] ?: WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
            val todayColor = Color(prefs[WidgetStateManager.KEY_TODAY_COLOR] ?: WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
            val textBoost = prefs[WidgetStateManager.KEY_TEXT_BOOST] ?: 0f
            val fontFamilyStr = prefs[WidgetStateManager.KEY_FONT_FAMILY] ?: ""
            val isBold = prefs[WidgetStateManager.KEY_FONT_BOLD] ?: false

            LogCollector.addLog("WIDGET UI: Renderizando ${events.size} ev con ajustes nativos")
            
            GlanceTheme {
                WidgetLayout(events, bgColor, eventColor, todayColor, textBoost, fontFamilyStr, isBold)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun WidgetLayout(
        events: List<WidgetStateManager.WidgetEvent>,
        bgColor: Color,
        eventColor: Color,
        todayColor: Color,
        textBoost: Float,
        fontFamilyStr: String,
        isBold: Boolean
    ) {
        val context = LocalContext.current
        val widgetFontFamily = when (fontFamilyStr) {
            WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif
            WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace
            WidgetConstants.FONT_FAMILY_CONDENSED -> FontFamily("sans-serif-condensed")
            else -> FontFamily.SansSerif
        }
        val fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal

        val clickAction = actionStartActivity(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        Box(modifier = GlanceModifier.fillMaxSize().background(bgColor).clickable(clickAction)) {
            if (events.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize().clickable(clickAction), contentAlignment = Alignment.Center) {
                    Text(text = context.getString(R.string.widget_no_events), style = TextStyle(color = ColorProvider(eventColor), fontSize = (14 + textBoost).sp, fontFamily = widgetFontFamily, fontWeight = fontWeight))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(events) { event ->
                        EventRow(event, eventColor, todayColor, textBoost, widgetFontFamily, fontWeight, clickAction)
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun EventRow(
        event: WidgetStateManager.WidgetEvent,
        defaultColor: Color,
        todayColor: Color,
        textBoost: Float,
        fontFamily: FontFamily,
        fontWeight: FontWeight,
        clickAction: androidx.glance.action.Action
    ) {
        val eventDate = LocalDate.ofEpochDay(event.dateEpochDay)
        val isToday = eventDate.isEqual(LocalDate.now())
        val color = if (isToday) todayColor else defaultColor
        val colorProvider = ColorProvider(color)
        
        val dateStr = eventDate.format(DateTimeFormatter.ofPattern("dd/MM"))
        val locale = LocalContext.current.resources.configuration.locales[0]
        val dayName = eventDate.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }

        Row(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 1.dp).clickable(clickAction), verticalAlignment = Alignment.CenterVertically) {
            Text(text = dayName, modifier = GlanceModifier.width(36.dp), style = TextStyle(color = colorProvider, fontSize = (14 + textBoost).sp, fontWeight = fontWeight, fontFamily = fontFamily, textAlign = TextAlign.End))
            Text(text = dateStr, modifier = GlanceModifier.width(55.dp), style = TextStyle(color = colorProvider, fontSize = (14 + textBoost).sp, fontFamily = fontFamily, fontWeight = fontWeight, textAlign = TextAlign.Center))
            val timePart = event.startTimeStr?.let { "${it.substring(0, 5)} " } ?: ""
            val agePart = event.age?.let { " ($it)" } ?: ""
            Text(text = "$timePart${event.title}$agePart", modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = colorProvider, fontSize = (14 + textBoost).sp, fontFamily = fontFamily, fontWeight = fontWeight), maxLines = 1)
            if (event.alarmTimeStr != null) {
                Spacer(modifier = GlanceModifier.width(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = ImageProvider(R.drawable.ic_alarm_bell_outlined), contentDescription = null, modifier = GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(colorProvider))
                    Text(text = event.alarmTimeStr, style = TextStyle(color = colorProvider, fontSize = (12 + textBoost).sp, fontFamily = fontFamily, fontWeight = fontWeight), modifier = GlanceModifier.padding(start = 1.dp))
                }
            }
        }
    }
}
