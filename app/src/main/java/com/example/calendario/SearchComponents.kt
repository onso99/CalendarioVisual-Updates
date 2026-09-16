package com.example.calendario

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme

/**
 * Componente visual para una fila de evento en las pantallas de búsqueda y exportación.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EventRow(
    festivo: Festivo,
    availableCalendars: List<CalendarInfo>,
    isSelected: Boolean,
    isSpecial: Boolean,
    onEventClick: (Festivo) -> Unit,
    onLongClick: (Festivo) -> Unit,
    searchScope: SearchScope
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val event1Keyword = remember { SettingsManager.getEvent1Keyword(context) }
    val event2Keyword = remember { SettingsManager.getEvent2Keyword(context) }

    val normalizedTitle = festivo.title.unaccent().lowercase()
    val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
    val esCumpleanos = festivo.isBirthday && !esFestivo
    val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
    val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

    val neutralColor = CalendarioTheme.colors.textSystem
    
    val eventSpecificColor = when {
        esEvento1 -> CalendarioTheme.colors.textEvent1
        esEvento2 -> CalendarioTheme.colors.textEvent2
        esFestivo -> CalendarioTheme.colors.textSundayHoliday
        esCumpleanos -> CalendarioTheme.colors.textBirthday
        else -> CalendarioTheme.colors.textSystem
    }

    // Si está seleccionado, aseguramos contraste
    val titleColor = if (isSelected) {
        val highlightColor = CalendarioTheme.colors.todayHighlightColor
        val opaqueHighlightInt = ColorUtils.setAlphaComponent(highlightColor.toArgb(), 255)
        val opaqueEventColorInt = ColorUtils.setAlphaComponent(eventSpecificColor.toArgb(), 255)
        if (ColorUtils.calculateContrast(opaqueEventColorInt, opaqueHighlightInt) > 1.5) {
            eventSpecificColor
        } else {
            highlightColor.getContrastColor(Color.Black)
        }
    } else {
        eventSpecificColor
    }

    val noTitle = stringResource(id = R.string.no_title)
    val allDayEvent = stringResource(id = R.string.all_day_event)

    val displayTime = when {
        festivo.isAllDay -> null
        festivo.currentDay == 1 -> festivo.startTime
        festivo.currentDay == festivo.totalDays -> festivo.endTime
        else -> null
    }
    val timeText = displayTime?.format(AppFormats.TimeShort)

    val titleText = festivo.title.ifEmpty { if (festivo.isAllDay) allDayEvent else noTitle }
    val ageText = if (festivo.age != null && festivo.age > 0) " (${festivo.age})" else ""

    val locale = LocalConfiguration.current.locales[0]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, CalendarioTheme.colors.cabecera, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CalendarioTheme.colors.todayHighlightColor else Color.Transparent)
            .combinedClickable(
                onClick = { onEventClick(festivo) },
                onLongClick = {
                    if (!isSpecial) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick(festivo)
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchScope != SearchScope.MONTH) {
            Text(
                text = String.format(locale, "%02d", festivo.date.dayOfMonth),
                color = neutralColor,
                fontSize = 16.sp,
                modifier = Modifier.width(26.dp)
            )
        }

        Box(
            modifier = Modifier.width(26.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val cal = availableCalendars.find { it.id == festivo.calendarId }
            val isGhost = festivo.isGhost || (cal == null && festivo.calendarId > 0)
            
            if (isGhost) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ghost_24),
                    contentDescription = null,
                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                val colorToUse = if (festivo.customColor != null) {
                    Color(festivo.customColor)
                } else if (cal != null) {
                    Color(cal.color)
                } else {
                    Color.Transparent
                }

                if (festivo.isLongPeriod && festivo.lane != null) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(colorToUse)
                    )
                } else if (colorToUse != Color.Transparent) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(colorToUse.copy(alpha = 0.6f), CircleShape)
                            .border(0.5.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.width(1.dp))

        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (timeText != null) {
                Text(
                    text = "$timeText ",
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
                    color = titleColor.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }

        // --- PRIORIDAD VISUAL: Alarma > Repetición (v3.1.64) ---
        val alarmTime = remember(festivo.id, festivo.date) { AlarmUtils.getAlarmTimeString(context, festivo) }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alarmTime != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = alarmTime,
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                }
            } else if (festivo.rrule != null && !festivo.isBirthday) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    val isFinite = festivo.repeatCount != null && festivo.repeatCount > 0
                    
                    if (isFinite) {
                        // Modo Compacto: "1/10" (Consistente con EventList v3.1.64)
                        Text(
                            text = "${festivo.repeatIndex ?: 1}/${festivo.repeatCount}",
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        // Modo Indefinido
                        if (festivo.repeatIndex != null && festivo.repeatIndex > 0) {
                            Text(
                                text = festivo.repeatIndex.toString(),
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
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
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (isSpecial) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 8.dp).size(14.dp)
            )
        }
    }
}

/**
 * Componente visual para una fila de nota en las pantallas de búsqueda y exportación.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoteRow(
    note: DailyNote,
    isSelected: Boolean,
    searchScope: SearchScope,
    onClick: (DailyNote) -> Unit,
    onLongClick: (DailyNote) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]
    val neutralColor = CalendarioTheme.colors.textSystem
    var isExpanded by remember { mutableStateOf(false) }
    
    var cutIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(isSelected) {
        if (isSelected) isExpanded = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, CalendarioTheme.colors.cabecera, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CalendarioTheme.colors.todayHighlightColor else Color.Transparent)
            .combinedClickable(
                onClick = { 
                    if (isExpanded) isExpanded = false
                    onClick(note) 
                },
                onLongClick = {
                    isExpanded = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(note)
                }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (searchScope != SearchScope.MONTH) {
                Text(
                    text = String.format(locale, "%02d", note.date.dayOfMonth),
                    color = neutralColor,
                    fontSize = 16.sp,
                    modifier = Modifier.width(26.dp)
                )
            }

            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = null,
                    tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            val topText = if (isExpanded && cutIndex != -1) {
                note.content.take(cutIndex).replace("\n", " ")
            } else {
                note.content.replace("\n", " ")
            }

            Text(
                text = topText,
                color = neutralColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                onTextLayout = { layoutResult ->
                    if (cutIndex == -1) {
                        val end = layoutResult.getLineEnd(0, visibleEnd = true)
                        val textBeforeCut = note.content.take(end)
                        val lastSpace = textBeforeCut.lastIndexOf(' ')
                        cutIndex = if (lastSpace > 0) lastSpace + 1 else end
                    }
                },
                modifier = Modifier.weight(1f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = "Previsualizar nota",
                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (isExpanded && cutIndex != -1 && cutIndex < note.content.length) {
            Text(
                text = note.content.substring(cutIndex),
                color = neutralColor.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(
                    top = 4.dp, 
                    start = if (searchScope != SearchScope.MONTH) 26.dp else 0.dp,
                    end = 8.dp
                )
            )
        }
    }
}
