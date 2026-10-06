package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate

@Composable
fun MonthlyEventList(
    modifier: Modifier = Modifier,
    finalEventsToList: List<Pair<LocalDate, List<Festivo>>>,
    lazyListState: LazyListState,
    isCurrentMonthView: Boolean,
    showAllEvents: Boolean,
    today: LocalDate,
    onEventClick: (Festivo) -> Unit,
    onEventLongClick: (Festivo) -> Unit = {},
    selectedEvents: Set<Festivo> = emptySet(),
    availableCalendars: List<CalendarInfo>
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val themeColors = CalendarioTheme.colors
    val event1Keyword = remember(themeColors) { SettingsManager.getEvent1Keyword(context) }
    val event2Keyword = remember(themeColors) { SettingsManager.getEvent2Keyword(context) }
    val normEvent1 = remember(event1Keyword) { event1Keyword.unaccent().lowercase() }
    val normEvent2 = remember(event2Keyword) { event2Keyword.unaccent().lowercase() }

    Box(modifier = modifier) {
        if (finalEventsToList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCurrentMonthView && !showAllEvents) stringResource(id = R.string.no_pending_events_this_month) else stringResource(id = R.string.no_events_this_month),
                    fontSize = 16.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                items(finalEventsToList, key = { (date, _) -> date.toString() }) { (date, festivos) ->
                    val isTodayEvents = isCurrentMonthView && date == today
                    Column {
                        festivos.forEach { festivo ->
                            val eventKey = remember(festivo.id, date, festivo.startTime) {
                                "${festivo.id}_${date}_${festivo.startTime}"
                            }
                            
                            key(eventKey) {
                                val normalizedTitle = remember(festivo.title) { festivo.title.unaccent().lowercase() }
                                val esEvento1 = normEvent1.isNotBlank() && normalizedTitle.contains(normEvent1)
                                val esEvento2 = normEvent2.isNotBlank() && normalizedTitle.contains(normEvent2)
                                val esCumpleanos = festivo.isBirthday
                                val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()

                                // Determinamos el color base según el tipo de evento
                                val eventSpecificColor = when {
                                    esEvento1 -> CalendarioTheme.colors.textEvent1
                                    esEvento2 -> CalendarioTheme.colors.textEvent2
                                    esCumpleanos -> CalendarioTheme.colors.textBirthday
                                    esFestivo -> CalendarioTheme.colors.textSundayHoliday
                                    else -> CalendarioTheme.colors.textSystem // Eventos normales ahora usan textSystem adaptativo
                                }

                                // Color para el día y la hora (neutro)
                                val neutralColor = if (isTodayEvents) {
                                    CalendarioTheme.colors.todayHighlightColor.getContrastColor(MaterialTheme.colorScheme.background)
                                } else {
                                    CalendarioTheme.colors.textSystem
                                }

                                // Color para el tÃ­tulo (especÃ­fico del evento)
                                val titleColor = if (isTodayEvents) {
                                    val highlightColor = CalendarioTheme.colors.todayHighlightColor
                                    val opaqueHighlightInt = ColorUtils.setAlphaComponent(highlightColor.toArgb(), 255)
                                    val opaqueEventColorInt = ColorUtils.setAlphaComponent(eventSpecificColor.toArgb(), 255)
                                    
                                    // Aumentamos la tolerancia: si el evento NO es normal (tiene color especial),
                                    // intentamos mantener su color aunque el contraste sea bajo (umbral 1.2 en lugar de 1.5)
                                    val isSpecialEvent = eventSpecificColor != CalendarioTheme.colors.textSystem
                                    val contrastThreshold = if (isSpecialEvent) 1.2 else 1.5

                                    if (ColorUtils.calculateContrast(opaqueEventColorInt, opaqueHighlightInt) > contrastThreshold) {
                                        eventSpecificColor
                                    } else {
                                        // Si realmente no se lee, en lugar de blanco puro, intentamos una variante clara del mismo color
                                        if (isSpecialEvent) {
                                            val hsl = FloatArray(3)
                                            ColorUtils.colorToHSL(eventSpecificColor.toArgb(), hsl)
                                            hsl[2] = 0.90f // Forzamos mucha luz para que brille sobre el fondo oscuro
                                            Color(ColorUtils.HSLToColor(hsl))
                                        } else {
                                            highlightColor.getContrastColor(MaterialTheme.colorScheme.background)
                                        }
                                    }
                                } else {
                                    eventSpecificColor
                                }
                                
                                val iconColor = if (isTodayEvents) neutralColor else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)

                                val noTitle = stringResource(id = R.string.no_title)
                                val allDayEvent = stringResource(id = R.string.all_day_event)
                                
                                // Separamos la hora del título para colorearlos de forma distinta
                                val timePrefix = if (!festivo.isAllDay && festivo.startTime != null) {
                                    festivo.startTime.format(AppFormats.TimeShort)
                                } else null
                                
                                val titleText = festivo.title.ifEmpty { if (festivo.isAllDay) allDayEvent else noTitle }

                                val ageText = if (festivo.age != null && festivo.age > 0) {
                                    " (${festivo.age})"
                                } else ""

                                if (titleText.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        val isSelected = selectedEvents.contains(festivo)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isSelected) Modifier.border(2.dp, CalendarioTheme.colors.cabecera, RoundedCornerShape(12.dp))
                                                    else Modifier
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .then(
                                                    when {
                                                        isSelected -> Modifier.background(CalendarioTheme.colors.todayHighlightColor)
                                                        isTodayEvents -> Modifier.background(CalendarioTheme.colors.todayHighlightColor)
                                                        else -> Modifier
                                                    }
                                                )
                                                .combinedClickable(
                                                    onClick = { 
                                                        if (selectedEvents.isNotEmpty()) {
                                                            onEventLongClick(festivo)
                                                        } else {
                                                            onEventClick(festivo)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        onEventLongClick(festivo)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    String.format(locale, "%02d", date.dayOfMonth),
                                                    color = neutralColor,
                                                    fontWeight = if (isTodayEvents) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.width(26.dp)
                                                )

                                                // Espacio fijo para el indicador (más reducido)
                                                Box(
                                                    modifier = Modifier.width(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (festivo.isLongPeriod && festivo.lane != null) {
                                                        val cal = availableCalendars.find { it.id == festivo.calendarId }
                                                        val laneColor = if (festivo.customColor != null) {
                                                            Color(festivo.customColor)
                                                        } else if (cal != null) {
                                                            Color(cal.color)
                                                        } else {
                                                            themeColors.textSystem
                                                        }

                                                        Box(
                                                            Modifier
                                                                .width(4.dp)
                                                                .height(10.dp)
                                                                .clip(RoundedCornerShape(1.dp))
                                                                .background(laneColor)
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(1.dp))

                                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                    if (timePrefix != null) {
                                                        Text(
                                                            text = "$timePrefix ",
                                                            color = neutralColor,
                                                            fontSize = 16.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Text(
                                                        text = titleText + ageText,
                                                        color = titleColor,
                                                        fontSize = 16.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (festivo.isLongPeriod) {
                                                        Text(
                                                            " (${festivo.currentDay}/${festivo.totalDays})",
                                                            color = titleColor,
                                                            fontSize = 16.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = CalendarioTheme.colors.cabecera,
                                                        modifier = Modifier.size(18.dp).padding(start = 4.dp)
                                                    )
                                                } else {
                                                    // --- PRIORIDAD VISUAL: Alarma > Repetición (v3.1.64) ---
                                                    val alarmTime = remember(festivo.id, date) { 
                                                        AlarmUtils.getAlarmTimeString(context, festivo) 
                                                    }
                                                    
                                                    if (alarmTime != null) {
                                                        // Caso 1: Tiene Alarma (Dato prioritario)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(start = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Notifications,
                                                                contentDescription = stringResource(id = R.string.alarm),
                                                                tint = iconColor,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = alarmTime,
                                                                color = iconColor,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    } else if (festivo.rrule != null && !festivo.isBirthday) {
                                                        // Caso 2: No tiene alarma pero es repetido -> Mostrar contador (1/10)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        ) {
                                                            val isFinite = festivo.repeatCount != null && festivo.repeatCount > 0
                                                            if (isFinite) {
                                                                Text(
                                                                    text = "${festivo.repeatIndex ?: 1}/${festivo.repeatCount}",
                                                                    color = iconColor,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            } else {
                                                                // Modo Indefinido: Muestra solo el icono (v3.2.08.2)
                                                                Icon(
                                                                    imageVector = Icons.Default.Refresh,
                                                                    contentDescription = stringResource(id = R.string.repeated_event),
                                                                    tint = iconColor,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // MARCADOR DE INCIDENCIA (Far Right v3.2.06)
                                                    if (festivo.hasIncident) {
                                                        Text(
                                                            text = " *",
                                                            color = Color.Red,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 18.sp,
                                                            modifier = Modifier.padding(start = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
