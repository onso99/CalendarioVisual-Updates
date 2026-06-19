package com.example.calendario

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
    onRepeatUntilClick: () -> Unit,
    isLongPeriod: Boolean,
    onLongPeriodChange: (Boolean) -> Unit,
    selectedColorInt: Int?,
    onColorSelect: (Int?) -> Unit,
    hasAlarm: Boolean,
    onHasAlarmChange: (Boolean) -> Unit,
    alarmTime: java.time.LocalTime,
    onAlarmTimeClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    Column(modifier = Modifier.padding(16.dp)) {
        // --- First Block ---
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
        ) {
            TextField(
                value = title,
                onValueChange = onTitleChange,
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
                singleLine = true,
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
                    modifier = Modifier.weight(1f),
                    color = CalendarioTheme.colors.textSystem
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(id = R.string.select_calendar),
                    tint = CalendarioTheme.colors.textSystem
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.long_period_switch), modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                Switch(
                    checked = isLongPeriod,
                    onCheckedChange = onLongPeriodChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CalendarioTheme.colors.cabecera,
                        checkedTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.54f)
                    )
                )
            }
            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.all_day_switch), modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                Switch(
                    checked = isAllDay,
                    onCheckedChange = onAllDayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CalendarioTheme.colors.cabecera,
                        checkedTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.54f),
                        uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                        uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                        uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
                    )
                )
            }
            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))

            val fontScale = LocalConfiguration.current.fontScale
            
            // --- INICIO ---
            AdaptiveDateTimeRow(
                label = if (isLongPeriod) stringResource(id = R.string.start) else stringResource(id = R.string.date),
                date = startDate,
                isAllDay = isAllDay,
                onDateClick = onStartDateClick,
                onTimeClick = onStartTimeClick,
                fontScale = fontScale
            )

            if (isLongPeriod || !isAllDay) {
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                // --- FIN ---
                AdaptiveDateTimeRow(
                    label = if (isLongPeriod) stringResource(id = R.string.end) else stringResource(id = R.string.end_time),
                    date = endDate,
                    isAllDay = isAllDay,
                    onDateClick = if (isLongPeriod) onEndDateClick else ({}), // Solo clic en fecha si es largo
                    onTimeClick = onEndTimeClick,
                    fontScale = fontScale,
                    hideDate = !isLongPeriod // Ocultamos fecha si no es periodo largo
                )
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
                    Text(stringResource(id = repetitionRule.displayNameRes), modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(id = R.string.select_repetition),
                        tint = CalendarioTheme.colors.textSystem
                    )
                }

                if (repetitionRule != RepetitionRule.NONE) {
                    HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRepeatUntilClick)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.repeat_until),
                            modifier = Modifier.weight(1f),
                            color = CalendarioTheme.colors.textSystem
                        )
                        Text(
                            text = repeatUntilDate?.format(dateFormatter) ?: stringResource(id = R.string.repeat_indefinite),
                            color = CalendarioTheme.colors.textSystem
                        )
                    }
                }
            } else {
                // --- COLOR SELECTOR (Solo para periodos largos) ---
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(id = R.string.select_color), color = CalendarioTheme.colors.textSystem, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val periodColors = listOf(
                            0xFFE91E63.toInt(), // Rosa
                            0xFF2196F3.toInt(), // Azul
                            0xFFFF9800.toInt(), // Naranja
                            0xFF4CAF50.toInt(), // Verde
                            0xFF9C27B0.toInt(), // Morado
                            0xFF795548.toInt(), // Marrón
                            0xFF607D8B.toInt()  // Gris
                        )
                        periodColors.forEach { colorInt ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorInt), CircleShape)
                                    .border(
                                        width = if (selectedColorInt == colorInt) 3.dp else 1.dp,
                                        color = if (selectedColorInt == colorInt) CalendarioTheme.colors.textSystem else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelect(colorInt) }
                            )
                        }
                    }
                }
            }

            // --- Alarm Section ---
            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.alarm), modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                Switch(
                    checked = hasAlarm,
                    onCheckedChange = onHasAlarmChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CalendarioTheme.colors.cabecera,
                        checkedTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.54f),
                        uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                        uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                        uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
                    )
                )
            }

            if (hasAlarm) {
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAlarmTimeClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.alarm_time),
                        modifier = Modifier.weight(1f),
                        color = CalendarioTheme.colors.textSystem
                    )
                    Text(
                        text = alarmTime.format(timeFormatter),
                        color = CalendarioTheme.colors.textSystem
                    )
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
                Text(stringResource(id = R.string.summary_calendar, selectedCalendar?.displayName ?: "N/A"))

                val summaryFormatter = remember(locale) { DateTimeFormatter.ofPattern("E dd/MM/yyyy", locale) }

                if (isAllDay) {
                    if (startDate.toLocalDate() != endDate.toLocalDate()) {
                        Row {
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text(stringResource(id = R.string.from))
                                Text(stringResource(id = R.string.to))
                            }
                            Column {
                                Text(startDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                                Text(endDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                            }
                        }
                    } else {
                        Text(startDate.format(summaryFormatter).replaceFirstChar { it.titlecase(locale) })
                    }
                    Text(stringResource(id = R.string.all_day_switch))
                } else {
                    val summaryTimeFormatter = remember(locale) { DateTimeFormatter.ofPattern("E dd/MM/yyyy HH:mm", locale) }
                    Text(stringResource(id = R.string.start) + ": " + startDate.format(summaryTimeFormatter).replaceFirstChar { it.titlecase(locale) })
                    Text(stringResource(id = R.string.end) + ": " + endDate.format(summaryTimeFormatter).replaceFirstChar { it.titlecase(locale) })
                }
                if (repetitionRule != RepetitionRule.NONE) {
                    Text(stringResource(id = R.string.repeat_event_title) + ": " + stringResource(id = repetitionRule.displayNameRes))
                    repeatUntilDate?.let {
                        Text(stringResource(id = R.string.repeat_until) + ": " + it.format(summaryFormatter).replaceFirstChar { char -> char.titlecase(locale) })
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
private fun AdaptiveDateTimeRow(
    label: String,
    date: LocalDateTime,
    isAllDay: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    fontScale: Float,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = date.format(timeFormatter),
                    modifier = Modifier
                        .alpha(if (isAllDay) 0.5f else 1f)
                        .clickable(!isAllDay, onClick = onTimeClick),
                    color = CalendarioTheme.colors.textSystem,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                }
                Text(
                    text = date.format(timeFormatter),
                    modifier = Modifier
                        .alpha(if (isAllDay) 0.5f else 1f)
                        .clickable(!isAllDay, onClick = onTimeClick),
                    color = CalendarioTheme.colors.textSystem,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
