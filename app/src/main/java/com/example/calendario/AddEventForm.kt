package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val dateFormatter: DateTimeFormatter = AppFormats.DateFull
private val timeFormatter: DateTimeFormatter = AppFormats.TimeShort

@Composable
fun AddEventForm(
    title: String,
    onTitleChange: (String) -> Unit,
    selectedCalendar: CalendarInfo?,
    onCalendarClick: () -> Unit,
    isAllDay: Boolean,
    onAllDayChange: (Boolean) -> Unit,
    startDate: LocalDateTime,
    onStartDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    endDate: LocalDateTime,
    onEndDateClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    repetitionRule: RepetitionRule,
    onRepetitionClick: () -> Unit,
    repeatCount: Int?,
    isLongPeriod: Boolean,
    onLongPeriodChange: (Boolean) -> Unit,
    selectedColorInt: Int?,
    onColorSelect: (Int?) -> Unit,
    onPaletteClick: () -> Unit,
    hasAlarm: Boolean,
    onHasAlarmChange: (Boolean) -> Unit,
    alarmTime: java.time.LocalTime,
    onAlarmTimeClick: () -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRepetitionClear: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val rowHeight = 50.dp

    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isInitialized = true
    }

    Column(modifier = modifier) {
        // --- First Block ---
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
        ) {
            TextField(
                value = title,
                onValueChange = { if (it.length <= 120) onTitleChange(it) },
                placeholder = { Text(stringResource(id = R.string.title), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedTextColor = CalendarioTheme.colors.textSystem,
                    unfocusedTextColor = CalendarioTheme.colors.textSystem
                ),
                singleLine = false,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCalendarClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCalendar?.displayName ?: stringResource(id = R.string.no_editable_calendars),
                    color = CalendarioTheme.colors.textSystem,
                    fontWeight = if (selectedCalendar != null) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Second Block ---
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
        ) {
            // Fila de Chips: Periodo Largo y Todo el día
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventModeChip(
                    text = stringResource(id = R.string.long_period_switch),
                    isSelected = isLongPeriod,
                    showLanes = true,
                    laneColorInt = selectedColorInt,
                    onClick = { onLongPeriodChange(!isLongPeriod) },
                    modifier = Modifier.weight(1f).height(38.dp)
                )

                EventModeChip(
                    text = stringResource(id = R.string.all_day_switch),
                    isSelected = isAllDay,
                    showLanes = false,
                    onClick = { onAllDayChange(!isAllDay) },
                    enabled = !isLongPeriod,
                    modifier = Modifier.weight(1f).height(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val startDateText = remember(startDate, locale) {
                "${startDate.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }} ${startDate.format(dateFormatter)}"
            }
            val endDateText = remember(endDate, locale) {
                "${endDate.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }} ${endDate.format(dateFormatter)}"
            }

            // Fila 1: Fecha de Inicio + Hora de Inicio (si no es Todo el día)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clickable(onClick = onStartDateClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = startDateText, 
                    color = CalendarioTheme.colors.textSystem, 
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (!isAllDay) {
                    Text(
                        text = startDate.format(timeFormatter), 
                        color = CalendarioTheme.colors.textSystem, 
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onStartTimeClick)
                    )
                }
            }

            // Fila 2: Fecha de Fin + Hora de Fin (si no es Todo el día)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clickable(onClick = onEndDateClick)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = endDateText, 
                    color = CalendarioTheme.colors.textSystem, 
                    fontWeight = FontWeight.Medium, 
                    modifier = Modifier.weight(1f)
                )
                if (!isAllDay) {
                    Text(
                        text = endDate.format(timeFormatter), 
                        color = CalendarioTheme.colors.textSystem, 
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onEndTimeClick)
                    )
                }
            }

            @Composable
            fun RepetitionRow() {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable(onClick = onRepetitionClick)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.repeat_event_title),
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )

                    if (repetitionRule != RepetitionRule.NONE) {
                        val repetitionText = stringResource(id = repetitionRule.displayNameRes)
                        val countSuffix = if (repeatCount != null && repeatCount > 0) " ($repeatCount)" else ""
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onRepetitionClear,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$repetitionText$countSuffix",
                                color = CalendarioTheme.colors.textSystem,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.does_not_repeat),
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            @Composable
            fun ColorSelectorRow() {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val periodColors = listOf(
                        CalendarioTheme.colors.cabecera.toArgb(),
                        0xFFE91E63.toInt(), // Rosa
                        0xFF2196F3.toInt(), // Azul
                        0xFFFF9800.toInt(), // Naranja
                        0xFF4CAF50.toInt(), // Verde
                        0xFF9C27B0.toInt(), // Morado
                        0xFF795548.toInt(), // Marrón
                        0xFFFF53FF.toInt()  // Nuevo color
                    )
                    
                    for (i in 0..7) {
                        ColorCircle(
                            colorInt = periodColors[i],
                            isSelected = selectedColorInt == periodColors[i],
                            onClick = { onColorSelect(periodColors[i]) }
                        )
                    }

                    val isCustomColor = selectedColorInt != null && selectedColorInt !in periodColors
                    IconButton(
                        onClick = onPaletteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = if (isCustomColor) Color(selectedColorInt) else CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (!isLongPeriod) {
                RepetitionRow()
            } else {
                ColorSelectorRow()
            }

            // --- Alarm Section ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clickable(onClick = {
                        if (!hasAlarm) {
                            onHasAlarmChange(true)
                        } else {
                            onAlarmTimeClick()
                        }
                    })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm),
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasAlarm) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { onHasAlarmChange(false) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = alarmTime.format(timeFormatter),
                                color = CalendarioTheme.colors.textSystem,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.no_alarm_label),
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }

                // Aviso de Alarma Posterior (Solo para eventos con hora)
                if (hasAlarm && !isAllDay) {
                    val alarmDateTime = LocalDateTime.of(startDate.toLocalDate(), alarmTime)
                    // Si la alarma parece estar antes del inicio pero el inicio es muy tarde (ej: 23:00) 
                    // y la alarma muy pronto (ej: 01:00), asumimos que la alarma es del día siguiente.
                    val finalAlarmDateTime = if (alarmTime.isBefore(startDate.toLocalTime()) && startDate.hour > 20 && alarmTime.hour < 6) {
                        alarmDateTime.plusDays(1)
                    } else {
                        alarmDateTime
                    }

                    val isAfterEnd = finalAlarmDateTime.isAfter(endDate)
                    val isAfterStart = finalAlarmDateTime.isAfter(startDate)
                    
                    if (isAfterStart) {
                        val warningColor = CalendarioTheme.colors.settingsBackground.getContrastColor(Color.White).let {
                            if (it == Color.White) Color(0xFFFFA500) else Color(0xFFC45100)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = warningColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = if (isAfterEnd) R.string.alarm_after_end_warning else R.string.alarm_after_start_warning),
                                color = warningColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Third Block (Description) ---
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            TextField(
                value = description,
                onValueChange = { if (it.length <= 1000) onDescriptionChange(it) },
                placeholder = { Text(stringResource(id = R.string.description), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    color = CalendarioTheme.colors.textSystem,
                    fontSize = 16.sp
                ),
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EventModeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLanes: Boolean = false,
    laneColorInt: Int? = null,
    enabled: Boolean = true
) {
    val brandColor = CalendarioTheme.colors.cabecera
    val chipBg = when {
        !enabled -> Color.Transparent
        isSelected -> brandColor.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val chipBorder = when {
        !enabled -> CalendarioTheme.colors.textSystem.copy(alpha = 0.08f)
        isSelected -> brandColor
        else -> CalendarioTheme.colors.textSystem.copy(alpha = 0.15f)
    }
    val contentColor = when {
        !enabled -> CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
        isSelected -> brandColor
        else -> CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
    }

    val laneColor = if (laneColorInt != null) Color(laneColorInt) else contentColor

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = chipBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (showLanes && isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(8.dp)
                        .height(3.dp)
                        .background(laneColor, RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 1.dp, bottomEnd = 1.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(8.dp)
                        .height(3.dp)
                        .background(laneColor, RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                )
            }

            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ColorCircle(
    colorInt: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val displayColor = remember(colorInt, isDark) {
        if (!isDark) Color(colorInt)
        else {
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(colorInt, hsl)
            hsl[2] = (hsl[2] + 0.12f).coerceAtMost(0.85f)
            Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        }
    }

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(displayColor)
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) CalendarioTheme.colors.textSystem else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}


