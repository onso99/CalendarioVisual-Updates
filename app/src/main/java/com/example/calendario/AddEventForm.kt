package com.example.calendario

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
    repeatUntilDate: java.time.LocalDate?,
    repeatCount: Int?,
    isLongPeriod: Boolean,
    onLongPeriodChange: (Boolean) -> Unit,
    selectedColorInt: Int?,
    onColorSelect: (Int?) -> Unit,
    onPaletteClick: () -> Unit,
    hasAlarm: Boolean,
    onHasAlarmChange: (Boolean) -> Unit,
    alarmTime: java.time.LocalTime,
    onAlarmTimeClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    Column {
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
                    text = stringResource(id = R.string.calendar_label),
                    color = CalendarioTheme.colors.textSystem,
                )
                
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = selectedCalendar?.displayName ?: stringResource(id = R.string.no_editable_calendars),
                    color = CalendarioTheme.colors.textSystem,
                    fontWeight = if (selectedCalendar != null) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.End,
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
            // Fila de Chips: Periodo Largo y Todo el dÃ­a
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chip: Periodo Largo
                EventModeChip(
                    text = stringResource(id = R.string.long_period_switch),
                    iconResId = R.drawable.ic_long_period_24,
                    isSelected = isLongPeriod,
                    onClick = { onLongPeriodChange(!isLongPeriod) },
                    modifier = Modifier.weight(1f)
                )

                // Chip: Todo el dÃ­a
                EventModeChip(
                    text = stringResource(id = R.string.all_day_switch),
                    iconResId = R.drawable.ic_all_day_24,
                    isSelected = isAllDay,
                    onClick = { onAllDayChange(!isAllDay) },
                    enabled = !isLongPeriod, // Bloqueado si es periodo largo
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))

            val fontScale = LocalConfiguration.current.fontScale
            
            if (isLongPeriod) {
                // --- VISTA PARA PERIODOS LARGOS: FILAS SEPARADAS ---
                AdaptiveDateTimeRow(
                    label = stringResource(id = R.string.start),
                    date = startDate,
                    onDateClick = onStartDateClick,
                    onTimeClick = onStartTimeClick,
                    fontScale = fontScale
                )
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                AdaptiveDateTimeRow(
                    label = stringResource(id = R.string.end),
                    date = endDate,
                    onDateClick = onEndDateClick,
                    onTimeClick = onEndTimeClick,
                    fontScale = fontScale
                )
            } else {
                // --- VISTA SOLICITADA PARA EVENTOS NORMALES: MODO COMPACTO ---
                // Fila 1: Fecha
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStartDateClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.date), color = CalendarioTheme.colors.textSystem, modifier = Modifier.weight(1f))
                    val dateText = remember(startDate, locale) {
                        "${startDate.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }} ${startDate.format(dateFormatter)}"
                    }
                    Text(text = dateText, color = CalendarioTheme.colors.textSystem, fontWeight = FontWeight.Medium)
                }

                if (!isAllDay) {
                    HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                    // Fila 2: Inicio y Fin compartidos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bloque Inicio
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onStartTimeClick)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(id = R.string.start), color = CalendarioTheme.colors.textSystem)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = startDate.format(timeFormatter), 
                                color = CalendarioTheme.colors.textSystem, 
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                        
                        // Icono Reloj Separador
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock_custom_24),
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                            modifier = Modifier.size(26.dp)
                        )

                        // Bloque Fin
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onEndTimeClick)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.end_time), 
                                color = CalendarioTheme.colors.textSystem,
                                modifier = Modifier.padding(start = 20.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = endDate.format(timeFormatter), color = CalendarioTheme.colors.textSystem, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (!isLongPeriod) {
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRepetitionClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val repetitionText = stringResource(id = repetitionRule.displayNameRes)
                    val countSuffix = if (repeatCount != null && repeatCount > 0) " ($repeatCount)" else ""
                    
                    Text(
                        text = stringResource(id = R.string.select_repetition),
                        color = CalendarioTheme.colors.textSystem,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$repetitionText$countSuffix",
                        color = CalendarioTheme.colors.textSystem,
                        fontWeight = if (repetitionRule != RepetitionRule.NONE) FontWeight.Medium else FontWeight.Normal,
                        textAlign = TextAlign.End
                    )
                }
            } else {
                // --- COLOR SELECTOR (Solo para periodos largos) ---
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                Column(modifier = Modifier.padding(16.dp)) {
                    val periodColors = listOf(
                        0xFFE91E63.toInt(), // Rosa
                        0xFF2196F3.toInt(), // Azul
                        0xFFFF9800.toInt(), // Naranja
                        0xFF4CAF50.toInt(), // Verde
                        0xFF9C27B0.toInt(), // Morado
                        0xFF795548.toInt(), // MarrÃ³n
                        0xFF607D8B.toInt(), // Gris
                        0xFF009688.toInt()  // Cyan
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (i in 0..3) {
                                    ColorCircle(
                                        colorInt = periodColors[i],
                                        isSelected = selectedColorInt == periodColors[i],
                                        onClick = { onColorSelect(periodColors[i]) }
                                    )
                                    ColorCircle(
                                        colorInt = periodColors[i + 4],
                                        isSelected = selectedColorInt == periodColors[i + 4],
                                        onClick = { onColorSelect(periodColors[i + 4]) }
                                    )
                                }
                            }
                        }

                        // Separador sutil
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .width(1.dp)
                                .height(64.dp)
                                .background(CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
                        )

                        // Quinta Columna: Paleta (Selector personalizado)
                        val isCustomColor = selectedColorInt != null && selectedColorInt !in periodColors
                        Box(
                            modifier = Modifier
                                .size(36.dp) // Un poco más grande para destacar
                                .clip(CircleShape)
                                .background(if (isCustomColor) Color(selectedColorInt) else Color.Transparent)
                                .border(
                                    width = if (isCustomColor) 3.dp else 1.dp,
                                    color = if (isCustomColor) CalendarioTheme.colors.textSystem else CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { onPaletteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isCustomColor) {
                                    Color(selectedColorInt).getContrastColor(Color.White)
                                } else {
                                    CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
            }

            // --- Alarm Section (Nuevo diseño compacto) ---
            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAlarmTimeClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm), 
                        modifier = Modifier.weight(1f), 
                        color = CalendarioTheme.colors.textSystem
                    )
                    
                    if (hasAlarm) {
                        // Botón X para eliminar
                        IconButton(
                            onClick = { onHasAlarmChange(false) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Valor de la hora (v3.1.34)
                        Text(
                            text = alarmTime.format(timeFormatter),
                            color = CalendarioTheme.colors.textSystem,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Flecha indicadora (v3.1.34: Siempre visible como en otras secciones)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Third Block (Summary) ---
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalContentColor provides CalendarioTheme.colors.textSystem) {
                Text(stringResource(id = R.string.summary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.summary_title, title.ifBlank { stringResource(id = R.string.no_title) }))
                val calendarName = selectedCalendar?.displayName ?: stringResource(id = R.string.not_applicable)
                Text(stringResource(id = R.string.summary_calendar, calendarName))

                val summaryFormatter = remember(locale) { AppFormats.dayDateAbbr(locale) }
    val timeOnlyFormatter = remember(locale) { AppFormats.TimeShort }
                
                // FASE 3: LÓGICA DE DÍA ÚNICO: Si no es periodo largo, el resumen ignora el salto de fecha técnico
                val isSameDay = !isLongPeriod || (startDate.toLocalDate() == endDate.toLocalDate())

                if (isAllDay) {
                    if (!isSameDay) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.from) + ": ", fontWeight = FontWeight.Medium)
                            Text(startDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.to) + ": ", fontWeight = FontWeight.Medium)
                            Text(endDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.start) + ": ", fontWeight = FontWeight.Medium)
                            Text(startDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                        }
                    }
                    Text(stringResource(id = R.string.all_day_switch), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp)
                } else {
                    if (isSameDay) {
                        // MISMO DÍA: Fecha una vez, horas a la derecha
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.start) + ": ", fontWeight = FontWeight.Medium)
                            Text(startDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                            Spacer(modifier = Modifier.weight(1f))
                            Text(startDate.format(timeOnlyFormatter), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(endDate.format(timeOnlyFormatter), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // DÍAS DISTINTOS: Fecha y hora en cada línea
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.start) + ": ", fontWeight = FontWeight.Medium)
                            Text(startDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                            Spacer(modifier = Modifier.weight(1f))
                            Text(startDate.format(timeOnlyFormatter), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.end) + ": ", fontWeight = FontWeight.Medium)
                            Text(endDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                            Spacer(modifier = Modifier.weight(1f))
                            Text(endDate.format(timeOnlyFormatter), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (repetitionRule != RepetitionRule.NONE) {
                    Text(stringResource(id = R.string.repeat_event_title) + ": " + stringResource(id = repetitionRule.displayNameRes))
                    if (repeatUntilDate != null) {
                        Text(stringResource(id = R.string.repeat_until) + ": " + repeatUntilDate.format(summaryFormatter).replaceFirstChar { char -> char.titlecase(locale) })
                    } else if (repeatCount != null && repeatCount > 0) {
                        Text(stringResource(id = R.string.repeat_until) + ": " + repeatCount + " " + stringResource(id = R.string.repeat_after).lowercase(locale))
                    }
                }
                if (hasAlarm) {
                    Text(stringResource(id = R.string.alarm) + ": " + alarmTime.format(timeFormatter))
                }
            }
        }
    }
}

@Composable
private fun EventModeChip(
    text: String,
    iconResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // AnimaciÃ³n de color para el flash parpadeante
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> CalendarioTheme.colors.cabecera.copy(alpha = 0.28f)
            isSelected -> CalendarioTheme.colors.cabecera.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = if (isPressed) snap() else tween(durationMillis = 400),
        label = "chipFlash"
    )

    val contentColor = when {
        !enabled -> CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
        isSelected -> CalendarioTheme.colors.cabecera
        else -> CalendarioTheme.colors.textSystem
    }

    val borderColor = when {
        !enabled -> CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
        isSelected -> CalendarioTheme.colors.cabecera
        else -> CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
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
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color(colorInt), CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) CalendarioTheme.colors.textSystem else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun AdaptiveDateTimeRow(
    label: String,
    date: LocalDateTime,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    fontScale: Float,
    isAllDay: Boolean = true,
    hideDate: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val showTwoLines = fontScale > 1.1f
    val dateText = remember(date, locale) {
        "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }} ${date.format(dateFormatter)}"
    }

    if (showTwoLines) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(label, color = CalendarioTheme.colors.textSystem)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!hideDate) {
                    Text(
                        text = dateText,
                        modifier = Modifier.clickable(onClick = onDateClick),
                        color = CalendarioTheme.colors.textSystem,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (!isAllDay) {
                    Text(
                        text = date.format(timeFormatter),
                        modifier = Modifier.clickable(onClick = onTimeClick),
                        color = CalendarioTheme.colors.textSystem,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = CalendarioTheme.colors.textSystem, modifier = Modifier.weight(0.25f))
            Row(modifier = Modifier.weight(0.75f), horizontalArrangement = Arrangement.End) {
                if (!hideDate) {
                    Text(
                        text = dateText,
                        modifier = Modifier.clickable(onClick = onDateClick),
                        color = CalendarioTheme.colors.textSystem,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isAllDay) {
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
                
                if (!isAllDay) {
                    Text(
                        text = date.format(timeFormatter),
                        modifier = Modifier.clickable(onClick = onTimeClick),
                        color = CalendarioTheme.colors.textSystem,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
