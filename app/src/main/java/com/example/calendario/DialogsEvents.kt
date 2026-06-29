package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DeleteRecurringOption {
    SINGLE_EVENT,
    ALL_EVENTS
}

enum class EditRecurringOption {
    SINGLE_EVENT,
    ALL_EVENTS
}

@Composable
private fun DialogDismissButton(onDismiss: () -> Unit) {
    TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = CalendarioTheme.colors.textSystem)
    ) {
        Text(stringResource(id = R.string.cancel))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadOnlyEventDialog(
    onDismissRequest: () -> Unit,
    festivo: Festivo,
    calendar: CalendarInfo?,
    onOpenHolidayManager: (Festivo) -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("E, dd MMM yyyy") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = {
            Text(
                festivo.title.ifBlank { stringResource(id = R.string.no_title) },
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = CalendarioTheme.colors.textSystem
            )
        },
        text = {
            SelectionContainer {
                Column {
                    Text(festivo.date.format(dateFormatter).replaceFirstChar(Char::titlecase), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    if (!festivo.isAllDay) {
                        val startTime = festivo.startTime?.format(timeFormatter) ?: "--:--"
                        val endTime = festivo.endTime?.format(timeFormatter) ?: "--:--"
                        Text("$startTime - $endTime", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.8f))
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    val sourceText = calendar?.displayName
                        ?: if (festivo.calendarId == -2L || festivo.id == -2L) {
                            "Gestor de Festivos"
                        } else {
                            "-"
                        }
                    
                    Text(
                        stringResource(id = R.string.calendar_source, sourceText),
                        fontSize = 16.sp, 
                        color = CalendarioTheme.colors.textSystem
                    )
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (festivo.isFromHolidaySource) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onOpenHolidayManager(festivo)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = CalendarioTheme.colors.cabecera)
                    ) {
                        Text(stringResource(id = R.string.holiday_manager))
                    }
                }
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
                ) {
                    Text(stringResource(id = R.string.accept))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteRecurringEventDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (DeleteRecurringOption) -> Unit
) {
    var selectedOption by remember { mutableStateOf<DeleteRecurringOption?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.delete_recurring_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                val options = listOf(
                    DeleteRecurringOption.SINGLE_EVENT to stringResource(id = R.string.delete_single_event_option),
                    DeleteRecurringOption.ALL_EVENTS to stringResource(id = R.string.delete_all_events_option)
                )
                options.forEach { (option, text) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == option),
                            onClick = { selectedOption = option },
                            colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem)
                        )
                        Text(text, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedOption?.let(onConfirm) },
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(id = R.string.delete))
            }
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringEventDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (EditRecurringOption) -> Unit
) {
    var selectedOption by remember { mutableStateOf<EditRecurringOption?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.edit_recurring_event_dialog_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                val options = listOf(
                    EditRecurringOption.SINGLE_EVENT to stringResource(id = R.string.edit_recurring_event_dialog_single_event),
                    EditRecurringOption.ALL_EVENTS to stringResource(id = R.string.edit_recurring_event_dialog_all_events)
                )
                options.forEach { (option, text) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == option),
                            onClick = { selectedOption = option },
                            colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem)
                        )
                        Text(text, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedOption?.let(onConfirm) },
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
            ) {
                Text(stringResource(id = R.string.accept))
            }
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCalendarsDialog(
    initialSelectedIds: Set<Long>,
    availableCalendars: List<CalendarInfo>,
    favoriteCalendarId: Long?,
    onDismissRequest: () -> Unit,
    onApplySelection: (selectedIds: Set<Long>) -> Unit,
    onSetFavorite: (Long) -> Unit
) {
    var currentSelectedIdsInDialog by remember(initialSelectedIds, availableCalendars) {
        mutableStateOf(initialSelectedIds.filter { id -> availableCalendars.any { cal -> cal.id == id } }.toSet())
    }

    val sortedCalendars = remember(availableCalendars) { // Key is now only availableCalendars
        availableCalendars.sortedWith(
            compareBy<CalendarInfo> { calendar ->
                // This logic runs only ONCE
                when {
                    calendar.isPrimary && calendar.canModify && calendar.accountName.contains("com.google", ignoreCase = true) -> 0
                    calendar.canModify && calendar.accountName.contains("com.google", ignoreCase = true) -> 1
                    calendar.isPrimary && calendar.canModify -> 2
                    calendar.canModify -> 3
                    else -> 4
                }
            }.thenBy { it.displayName }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = {
            Text(
                stringResource(id = R.string.select_calendars_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            if (sortedCalendars.isEmpty()) {
                Text(stringResource(id = R.string.no_calendars_found), fontSize = 16.sp)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .fillMaxWidth()
                ) {
                    items(sortedCalendars, key = { it.id }) { calendar ->
                        val isFavorite = calendar.id == favoriteCalendarId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isFavorite) {
                                    val newSet = currentSelectedIdsInDialog.toMutableSet()
                                    if (newSet.contains(calendar.id)) {
                                        if (!isFavorite) { // Prevent unchecking the favorite calendar
                                            newSet.remove(calendar.id)
                                        }
                                    } else {
                                        newSet.add(calendar.id)
                                    }
                                    currentSelectedIdsInDialog = newSet
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentSelectedIdsInDialog.contains(calendar.id) || isFavorite,
                                onCheckedChange = null, // Checkbox is controlled by the Row's clickable
                                enabled = !isFavorite,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CalendarioTheme.colors.cabecera,
                                    uncheckedColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                                    disabledCheckedColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.5f),
                                    checkmarkColor = if(isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    calendar.displayName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    calendar.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                if (!calendar.canModify) {
                                    Text(
                                        stringResource(id = R.string.read_only),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (calendar.canModify) {
                                IconButton(onClick = { 
                                    onSetFavorite(calendar.id)
                                    if (!currentSelectedIdsInDialog.contains(calendar.id)) {
                                        val newSet = currentSelectedIdsInDialog.toMutableSet()
                                        newSet.add(calendar.id)
                                        currentSelectedIdsInDialog = newSet
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = if (isFavorite) stringResource(id = R.string.favorite_calendar_marked) else stringResource(id = R.string.mark_as_favorite_calendar),
                                        tint = if (isFavorite) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplySelection(currentSelectedIdsInDialog) },
                enabled = availableCalendars.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
            ) {
                Text(stringResource(id = R.string.apply), fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, colors = ButtonDefaults.textButtonColors(contentColor = CalendarioTheme.colors.textSystem)) {
                Text(stringResource(id = R.string.cancel), fontSize = 16.sp)
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEventsDialog(
    date: LocalDate,
    events: List<Festivo>,
    availableCalendars: List<CalendarInfo>,
    onDismissRequest: () -> Unit,
    onAddEventClick: (LocalDate) -> Unit,
    onEventClick: (Festivo) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val event1Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "")?.trim() ?: "" }
    val event2Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "")?.trim() ?: "" }
    val locale = LocalConfiguration.current.locales[0]

    val formatter = remember { DateTimeFormatter.ofPattern("E, dd/MM/yyyy", locale) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }
    val isToday = date == LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formattedDate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                FilledIconButton(
                    onClick = { onAddEventClick(date) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CalendarioTheme.colors.cabecera,
                        contentColor = if (isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add_event)
                    )
                }
            }
        },
        text = {
            val noTitle = stringResource(id = R.string.no_title)
            val allDay = stringResource(id = R.string.all_day)
            
            if (events.isEmpty()) {
                Text(stringResource(id = R.string.no_detailed_events), fontSize = 16.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(events, key = { festivo -> festivo.id.toString() + festivo.title + festivo.startTime.toString() + festivo.date.toString() }) { festivo ->
                        val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
                        val esCumpleanos = festivo.isBirthday && !esFestivo
                        val normalizedTitle = festivo.title.unaccent().lowercase()
                        val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
                        val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

                        // Determinamos el color base según el tipo de evento
                        val eventSpecificColor = when {
                            esEvento1 -> CalendarioTheme.colors.textEvent1
                            esEvento2 -> CalendarioTheme.colors.textEvent2
                            esFestivo -> CalendarioTheme.colors.textSundayHoliday
                            esCumpleanos -> CalendarioTheme.colors.textBirthday
                            else -> CalendarioTheme.colors.textSystem
                        }

                        val neutralColor = if (isToday) {
                            if (isColorDark(CalendarioTheme.colors.todayHighlightColor, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
                        } else {
                            CalendarioTheme.colors.textSystem
                        }

                        val titleColor = if (isToday) {
                            val highlightColor = CalendarioTheme.colors.todayHighlightColor
                            val opaqueHighlightInt = ColorUtils.setAlphaComponent(highlightColor.toArgb(), 255)
                            val opaqueEventColorInt = ColorUtils.setAlphaComponent(eventSpecificColor.toArgb(), 255)
                            if (ColorUtils.calculateContrast(opaqueEventColorInt, opaqueHighlightInt) > 1.5) {
                                eventSpecificColor
                            } else {
                                neutralColor
                            }
                        } else {
                            eventSpecificColor
                        }

                        val timeText = if (!festivo.isAllDay && festivo.startTime != null) {
                            festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        } else null

                        val titleText = festivo.title.ifEmpty { if (festivo.isAllDay) allDay else noTitle }
                        val ageText = if (festivo.age != null) " (${festivo.age})" else ""

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .then(if (isToday) Modifier.background(CalendarioTheme.colors.todayHighlightColor) else Modifier)
                                .clickable { onEventClick(festivo) }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.Top // Alineación superior para flujo continuo
                        ) {
                            val calendarForEvent = availableCalendars.find { it.id == festivo.calendarId }
                            val colorToUse = if (festivo.customColor != null) {
                                Color(festivo.customColor)
                            } else if (calendarForEvent != null) {
                                Color(calendarForEvent.color ?: 0xFFFFFFFF.toInt())
                            } else if (festivo.calendarId == -2L || festivo.id == -2L) {
                                Color.White
                            } else {
                                Color.Transparent
                            }

                            // Icono alineado a la parte superior con un pequeño margen para que cuadre con la primera línea
                            Box(modifier = Modifier.padding(top = 6.dp)) {
                                if (festivo.isLongPeriod && festivo.lane != null) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .background(colorToUse, RoundedCornerShape(1.5.dp))
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .background(Color.Gray.copy(alpha = 0.6f), CircleShape)
                                            .border(
                                                0.5.dp,
                                                CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))

                            // Texto unificado con AnnotatedString para flujo continuo
                            val annotatedString = buildAnnotatedString {
                                // 1. Hora (Color Neutro)
                                if (timeText != null) {
                                    withStyle(SpanStyle(color = neutralColor)) {
                                        append("$timeText ")
                                    }
                                }
                                // 2. Título + Edad (Color del Evento)
                                withStyle(SpanStyle(color = titleColor)) {
                                    append(titleText + ageText)
                                }
                                // 3. Progreso (Color del Evento atenuado)
                                if (festivo.isLongPeriod) {
                                    withStyle(SpanStyle(color = titleColor.copy(alpha = 0.8f), fontSize = 14.sp)) {
                                        append(" (${festivo.currentDay}/${festivo.totalDays})")
                                    }
                                }
                            }

                            Text(
                                text = annotatedString,
                                fontSize = 16.sp,
                                maxLines = 10, // Permitimos flujo libre si es extraordinariamente largo
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
            ) {
                Text(stringResource(id = R.string.close), fontSize = 16.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepetitionSelectionDialog(
    currentRule: RepetitionRule,
    currentUntil: LocalDate?,
    currentCount: Int?,
    onConfirm: (RepetitionRule, LocalDate?, Int?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var tempSelection by remember { mutableStateOf(currentRule) }
    var tempUntil by remember { mutableStateOf(currentUntil) }
    var tempCount by remember { mutableStateOf(currentCount?.toString() ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // 0: Indefinidamente, 1: En una fecha, 2: Tras X veces
    var endMode by remember { 
        mutableIntStateOf(if (currentUntil != null) 1 else if (currentCount != null) 2 else 0)
    }

    LaunchedEffect(endMode) {
        if (endMode == 2) {
            focusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.repeat_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                // --- SECCIÓN 1: REGLA DE REPETICIÓN ---
                RepetitionRule.entries.forEach { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                tempSelection = rule 
                                // Ajuste dinámico de dígitos si cambiamos de regla
                                val limit = if (rule == RepetitionRule.DAILY) 3 else 2
                                if (tempCount.length > limit) {
                                    tempCount = tempCount.take(limit)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (rule == tempSelection),
                            onClick = { 
                                tempSelection = rule 
                                val limit = if (rule == RepetitionRule.DAILY) 3 else 2
                                if (tempCount.length > limit) {
                                    tempCount = tempCount.take(limit)
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem)
                        )
                        Text(stringResource(id = rule.displayNameRes), modifier = Modifier.padding(start = 8.dp), fontSize = 16.sp)
                    }
                }

                // --- SECCIÓN 2: FINALIZACIÓN (Visible siempre, inactiva si NONE) ---
                val isRepetitionActive = tempSelection != RepetitionRule.NONE
                val activeAlpha = if (isRepetitionActive) 1f else 0.4f

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                // Opción 0: Indefinidamente
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isRepetitionActive) { endMode = 0 }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (endMode == 0),
                        onClick = { if (isRepetitionActive) endMode = 0 },
                        enabled = isRepetitionActive,
                        colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.repeat_indefinite), 
                        fontSize = 16.sp,
                        color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)
                    )
                }

                // Opción 1: Hasta la fecha
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isRepetitionActive) { 
                            endMode = 1
                            if (tempUntil == null) tempUntil = LocalDate.now().plusMonths(1)
                            showDatePicker = true 
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (endMode == 1),
                        onClick = { 
                            if (isRepetitionActive) {
                                endMode = 1
                                if (tempUntil == null) tempUntil = LocalDate.now().plusMonths(1)
                                showDatePicker = true 
                            }
                        },
                        enabled = isRepetitionActive,
                        colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    
                    val textToShow = if (endMode == 1 && tempUntil != null) {
                        val formatter = DateTimeFormatter.ofPattern("EEEE, d/MM/yyyy", locale)
                        tempUntil!!.format(formatter).replaceFirstChar { it.titlecase(locale) }
                    } else {
                        stringResource(id = R.string.repeat_on_date)
                    }

                    Text(
                        text = textToShow,
                        fontSize = 16.sp,
                        color = if (endMode == 1) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Opción 2: Repeticiones X
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isRepetitionActive) { endMode = 2 }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (endMode == 2),
                        onClick = { if (isRepetitionActive) endMode = 2 },
                        enabled = isRepetitionActive,
                        colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(id = R.string.repeat_after), 
                            fontSize = 16.sp,
                            color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)
                        )
                        Spacer(Modifier.width(4.dp))
                        TextField(
                            value = tempCount,
                            onValueChange = { newValue ->
                                if (newValue.all { char -> char.isDigit() }) {
                                    val maxLength = if (tempSelection == RepetitionRule.DAILY) 3 else 2
                                    if (newValue.length <= maxLength) {
                                        tempCount = newValue
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(75.dp)
                                .focusRequester(focusRequester), 
                            textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center, color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val finalUntil = if (endMode == 1) tempUntil else null
                                val finalCount = if (endMode == 2) tempCount.toIntOrNull() else null
                                onConfirm(tempSelection, finalUntil, finalCount)
                            }),
                            singleLine = true,
                            enabled = (endMode == 2 && isRepetitionActive),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = CalendarioTheme.colors.cabecera,
                                unfocusedIndicatorColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalUntil = if (endMode == 1) tempUntil else null
                    val finalCount = if (endMode == 2) tempCount.toIntOrNull() else null
                    onConfirm(tempSelection, finalUntil, finalCount) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
            ) { Text(stringResource(id = R.string.accept)) }
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (tempUntil ?: LocalDate.now()).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        tempUntil = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(id = R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(id = R.string.cancel)) }
            },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SelectCalendarDialog(
    calendars: List<CalendarInfo>,
    currentSelection: CalendarInfo?,
    onCalendarSelected: (CalendarInfo) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tempSelection by remember { mutableStateOf(currentSelection) }
    val sortedCalendars = remember(calendars) {
        calendars.sortedByDescending { it.isPrimary }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.select_calendar_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                sortedCalendars.forEach { calendar ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempSelection = calendar }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (calendar.id == tempSelection?.id),
                            onClick = { tempSelection = calendar },
                            colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem)
                        )
                        Text(text = calendar.displayName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                tempSelection?.let(onCalendarSelected)
                onDismissRequest()
            }, colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)) {
                Text(stringResource(id = R.string.accept))
            }
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}
