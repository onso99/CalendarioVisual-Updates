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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val formatter = remember { DateTimeFormatter.ofPattern("E, dd/MM/yyyy", Locale.getDefault()) }
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
            val eventsToDisplay = events.mapNotNull { festivo ->
                val baseTitle = if (!festivo.isAllDay && festivo.startTime != null) {
                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { noTitle }}"
                } else {
                    festivo.title.ifEmpty { if (festivo.isAllDay) allDay else "" }
                }
                val title = if (festivo.age != null) "$baseTitle (${festivo.age})" else baseTitle
                if (title.isNotBlank()) festivo to title else null
            }

            if (eventsToDisplay.isEmpty()) {
                Text(stringResource(id = R.string.no_detailed_events), fontSize = 16.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(eventsToDisplay, key = { (festivo, _) -> festivo.id.toString() + festivo.title + festivo.startTime.toString() }) { (festivo, displayTitle) ->
                        val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
                        val esCumpleanos = festivo.isBirthday && !esFestivo
                        val normalizedTitle = festivo.title.unaccent().lowercase()
                        val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
                        val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

                        val itemColor = if (isToday) {
                            if (isColorDark(CalendarioTheme.colors.todayHighlightColor, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
                        } else {
                            when {
                                esEvento1 -> CalendarioTheme.colors.textEvent1
                                esEvento2 -> CalendarioTheme.colors.textEvent2
                                esFestivo -> CalendarioTheme.colors.textSundayHoliday
                                esCumpleanos -> CalendarioTheme.colors.textBirthday
                                else -> CalendarioTheme.colors.textSystem
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .then(if (isToday) Modifier.background(CalendarioTheme.colors.todayHighlightColor) else Modifier)
                                .clickable { onEventClick(festivo) }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                            
                            Spacer(Modifier.width(10.dp))

                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    displayTitle,
                                    color = itemColor,
                                    fontSize = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (festivo.isLongPeriod) {
                                    Text(
                                        " (${festivo.currentDay}/${festivo.totalDays})",
                                        color = itemColor.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
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
    onConfirm: (RepetitionRule) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tempSelection by remember { mutableStateOf(currentRule) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.repeat_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                RepetitionRule.entries.forEach { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempSelection = rule },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (rule == tempSelection),
                            onClick = { tempSelection = rule },
                            colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem)
                        )
                        Text(stringResource(id = rule.displayNameRes), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempSelection) },
                colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
            ) { Text(stringResource(id = R.string.accept)) }
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
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
