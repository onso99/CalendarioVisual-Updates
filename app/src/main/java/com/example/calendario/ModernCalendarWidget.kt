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
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.height
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
    
    // Activamos el modo exacto para recibir la altura real del widget en pantalla
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            
            // 1. Extraer Eventos con ESCUDO DE MIGRACIÓN
            // Si el estado está corrupto o es de una versión vieja, el catch evita que la App se cierre.
            val events = try {
                val json = prefs[WidgetStateManager.KEY_WIDGET_DATA] ?: ""
                if (json.isNotEmpty()) {
                    Gson().fromJson(json, Array<WidgetStateManager.WidgetEvent>::class.java).toList()
                } else {
                    WidgetStateManager.getWidgetEvents(context)
                }
            } catch (_: Exception) {
                WidgetStateManager.getWidgetEvents(context)
            }
            
            val appPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            
            // 2. Extraer Ajustes Visuales con Try-Catch individual para máxima seguridad
            val bgColor = try { Color(prefs[WidgetStateManager.KEY_BG_COLOR] ?: appPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)) } catch(_:Exception) { Color(WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB) }
            val eventColor = try { Color(prefs[WidgetStateManager.KEY_EVENT_COLOR] ?: appPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) } catch(_:Exception) { Color(WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB) }
            val todayColor = try { Color(prefs[WidgetStateManager.KEY_TODAY_COLOR] ?: appPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) } catch(_:Exception) { Color(WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB) }
            val textBoost = try { prefs[WidgetStateManager.KEY_TEXT_BOOST] ?: appPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f) } catch(_:Exception) { 0f }
            val fontFamilyStr = try { prefs[WidgetStateManager.KEY_FONT_FAMILY] ?: appPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: "" } catch(_:Exception) { "" }
            val isBold = try { prefs[WidgetStateManager.KEY_FONT_BOLD] ?: appPrefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD) } catch(_:Exception) { false }

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
        val widgetSize = LocalSize.current // Contiene la altura real (gracias a SizeMode.Exact)
        
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
                        EventRow(event, eventColor, todayColor, textBoost, fontFamilyStr, widgetFontFamily, fontWeight, clickAction, bgColor)
                    }
                    
                    // RELLENO MATEMÁTICO REAL: Rellenamos el hueco con precisión para capturar clics.
                    item {
                        val estimatedRowHeight = 25f + textBoost 
                        val totalContentHeight = (events.size * estimatedRowHeight) + 8f 
                        val remainingHeight = (widgetSize.height.value - totalContentHeight).coerceAtLeast(0f)
                        
                        if (remainingHeight > 5f) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(remainingHeight.dp)
                                    .clickable(clickAction)
                            ) {}
                        }
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
        fontFamilyStr: String,
        fontFamily: FontFamily,
        fontWeight: FontWeight,
        clickAction: androidx.glance.action.Action,
        bgColor: Color
    ) {
        val eventDate = LocalDate.ofEpochDay(event.dateEpochDay)
        val isToday = eventDate.isEqual(LocalDate.now())
        val color = if (isToday) todayColor else defaultColor
        val colorProvider = ColorProvider(color)
        
        val dateStr = eventDate.format(DateTimeFormatter.ofPattern("dd/MM"))
        val locale = LocalContext.current.resources.configuration.locales[0]
        val dayName = eventDate.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }

        val baseFontSize = 14f + textBoost
        val (dayF, dateF, gapF) = when(fontFamilyStr) {
            WidgetConstants.FONT_FAMILY_CONDENSED -> Triple(0.92f, 0.90f, 0.40f)
            WidgetConstants.FONT_FAMILY_MONOSPACE -> Triple(0.95f, 1.15f, 0.01f)
            WidgetConstants.FONT_FAMILY_SERIF -> Triple(1.02f, 1.0f, 0.45f)
            else -> Triple(1.08f, 1.0f, 0.45f)
        }

        val dayWidth = (baseFontSize * 3.0f * dayF).dp
        val dateWidth = (baseFontSize * 4.1f * dateF).dp
        val columnGap = (baseFontSize * 0.16f * gapF).dp

        Row(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp).clickable(clickAction), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dayName, 
                modifier = GlanceModifier.width(dayWidth), 
                style = TextStyle(color = colorProvider, fontSize = baseFontSize.sp, fontWeight = fontWeight, fontFamily = fontFamily, textAlign = TextAlign.End),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(columnGap))
            Text(
                text = dateStr, 
                modifier = GlanceModifier.width(dateWidth), 
                style = TextStyle(color = colorProvider, fontSize = baseFontSize.sp, fontFamily = fontFamily, fontWeight = fontWeight, textAlign = TextAlign.Center),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(columnGap * 1.4f))
            val timePart = event.startTimeStr?.let { "${it.substring(0, 5)} " } ?: ""
            val agePart = event.age?.let { " ($it)" } ?: ""
            val progressPart = if (event.isLongPeriod) " (${event.currentDay}/${event.totalDays})" else ""
            
            Text(text = "$timePart${event.title}$agePart$progressPart", modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = colorProvider, fontSize = baseFontSize.sp, fontFamily = fontFamily, fontWeight = fontWeight), maxLines = 1)
            
            if (event.alarmTimeStr != null) {
                Spacer(modifier = GlanceModifier.width(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ALGORITMO DE OPACIDAD ADAPTATIVA
                    // Compensamos la delgadez del icono con una curva no lineal.
                    val rawAlpha = color.alpha
                    val adjustedAlpha = if (rawAlpha > 0f) {
                        (rawAlpha * 0.7f + 0.3f).coerceAtMost(1f)
                    } else 0f
                    
                    val mixedColor = Color(
                        red = color.red * adjustedAlpha + bgColor.red * (1f - adjustedAlpha),
                        green = color.green * adjustedAlpha + bgColor.green * (1f - adjustedAlpha),
                        blue = color.blue * adjustedAlpha + bgColor.blue * (1f - adjustedAlpha),
                        alpha = 1f
                    )
                    
                    Image(
                        provider = ImageProvider(R.drawable.ic_alarm_bell_outlined), 
                        contentDescription = null, 
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(mixedColor))
                    )
                    Text(
                        text = event.alarmTimeStr, 
                        style = TextStyle(
                            color = colorProvider,
                            // ESCALADO PROPORCIONAL: 20% más pequeño para jerarquía visual.
                            fontSize = (baseFontSize * 0.80f).sp,
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
