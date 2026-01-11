package com.example.calendario

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class RepetitionRule(val rrule: String?, val displayNameRes: Int) {
    NONE(null, R.string.does_not_repeat),
    DAILY("FREQ=DAILY", R.string.every_day),
    WEEKLY("FREQ=WEEKLY", R.string.every_week),
    MONTHLY("FREQ=MONTHLY", R.string.every_month),
    YEARLY("FREQ=YEARLY", R.string.every_year)
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    onBackPress: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    editableCalendars: List<CalendarInfo>,
    initialDate: LocalDate?,
    eventToEdit: Festivo? = null
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(true) }
    var selectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDateTime.now()) }
    var endDate by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var repetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }
    var initialRepetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteRecurringDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }

    // Store initial state to compare for changes
    var initialTitle by remember { mutableStateOf("") }
    var initialIsAllDay by remember { mutableStateOf(true) }
    var initialSelectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var initialStartDate by remember { mutableStateOf(LocalDateTime.now()) }
    var initialEndDate by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }

    LaunchedEffect(key1 = eventToEdit, key2 = editableCalendars) {
        if (eventToEdit != null) {
            title = eventToEdit.title
            isAllDay = eventToEdit.isAllDay
            selectedCalendar = editableCalendars.find { cal -> cal.id == eventToEdit.calendarId }

            startDate = if (eventToEdit.isAllDay) {
                eventToEdit.date.atStartOfDay()
            } else {
                LocalDateTime.of(eventToEdit.date, eventToEdit.startTime ?: LocalTime.now())
            }

            endDate = if (eventToEdit.isAllDay) {
                eventToEdit.date.atStartOfDay()
            } else {
                eventToEdit.endTime?.let { endTime -> LocalDateTime.of(eventToEdit.date, endTime) } ?: startDate.plusHours(1)
            }
            repetitionRule = RepetitionRule.entries.find { rule -> rule.rrule != null && eventToEdit.rrule?.startsWith(rule.rrule) == true } ?: RepetitionRule.NONE
            initialRepetitionRule = repetitionRule

            // Store initial state
            initialTitle = title
            initialIsAllDay = isAllDay
            initialSelectedCalendar = selectedCalendar
            initialStartDate = startDate
            initialEndDate = endDate
            initialRepetitionRule = repetitionRule

        } else {
            val now = LocalDateTime.now()
            val effectiveInitialDateTime = initialDate?.atTime(now.toLocalTime()) ?: now
            startDate = effectiveInitialDateTime
            endDate = effectiveInitialDateTime.plusHours(1)
            selectedCalendar = editableCalendars.find { it.isPrimary } ?: editableCalendars.firstOrNull()

            // Store initial state for new event
            initialTitle = ""
            initialIsAllDay = true
            initialSelectedCalendar = selectedCalendar
            initialStartDate = startDate
            initialEndDate = endDate
            initialRepetitionRule = RepetitionRule.NONE
        }
    }

    val hasChanges by remember {
        derivedStateOf {
            title != initialTitle ||
            isAllDay != initialIsAllDay ||
            selectedCalendar?.id != initialSelectedCalendar?.id ||
            startDate != initialStartDate ||
            endDate != initialEndDate ||
            repetitionRule != initialRepetitionRule
        }
    }

    val backAction = {
        if (hasChanges) {
            showDiscardChangesDialog = true
        } else {
            onBackPress()
        }
    }

    val saveAction = {
        if (eventToEdit != null) {
            updateEvent(
                context = context, eventId = eventToEdit.id, title = title,
                calendarId = selectedCalendar?.id, startDate = startDate,
                endDate = endDate, isAllDay = isAllDay, repetitionRule = repetitionRule
            )
            onSave()
        } else {
            createEvent(
                context = context, title = title, calendarId = selectedCalendar?.id,
                startDate = startDate, endDate = endDate, isAllDay = isAllDay,
                repetitionRule = repetitionRule
            )
            onSave()
        }
    }

    var showStartDatePickerDialog by remember { mutableStateOf(false) }
    var showEndDatePickerDialog by remember { mutableStateOf(false) }
    var showStartTimePickerDialog by remember { mutableStateOf(false) }
    var showEndTimePickerDialog by remember { mutableStateOf(false) }
    var showRepetitionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventToEdit != null) stringResource(id = R.string.edit_event) else stringResource(id = R.string.new_event)) },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    if (hasChanges) {
                        IconButton(onClick = saveAction) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.save))
                        }
                    }
                    if (eventToEdit != null) {
                        IconButton(onClick = {
                            if (eventToEdit.rrule != null) {
                                showDeleteRecurringDialog = true
                            } else {
                                showDeleteDialog = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.delete_event)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CalendarioTheme.colors.cabecera,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- First Block ---
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CalendarioTheme.colors.fondoSecciones)
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
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
                        singleLine = true
                    )
                    HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCalendarDialog = true }
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
                        Text(stringResource(id = R.string.all_day_switch), modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { checked ->
                                isAllDay = checked
                                if (checked) {
                                    startDate = startDate.toLocalDate().atStartOfDay()
                                    endDate = endDate.toLocalDate().atStartOfDay()
                                } else {
                                    val currentTime = LocalTime.now()
                                    startDate = startDate.toLocalDate().atTime(currentTime.hour, currentTime.minute)
                                    endDate = startDate.plusHours(1)
                                }
                            },
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
                    AdaptiveDateTimeRow(
                        label = stringResource(id = R.string.start),
                        date = startDate,
                        isAllDay = isAllDay,
                        onDateClick = { showStartDatePickerDialog = true },
                        onTimeClick = { showStartTimePickerDialog = true },
                        fontScale = fontScale
                    )

                    HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))

                    AdaptiveDateTimeRow(
                        label = stringResource(id = R.string.end),
                        date = endDate,
                        isAllDay = isAllDay,
                        onDateClick = { showEndDatePickerDialog = true },
                        onTimeClick = { showEndTimePickerDialog = true },
                        fontScale = fontScale
                    )

                    HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRepetitionDialog = true }
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
                    val fontScale = LocalConfiguration.current.fontScale
                    val useVerticalLayout = fontScale > 1.1f

                    CompositionLocalProvider(LocalContentColor provides CalendarioTheme.colors.textSystem) {
                        Text(stringResource(id = R.string.summary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(id = R.string.summary_title, title.ifBlank { stringResource(id = R.string.no_title) }))
                        Text(stringResource(id = R.string.summary_calendar, selectedCalendar?.displayName ?: "N/A"))

                        if (isAllDay) {
                            if (startDate.toLocalDate() != endDate.toLocalDate()) {
                                Row {
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Text(stringResource(id = R.string.from))
                                        Text(stringResource(id = R.string.to))
                                    }
                                    Column {
                                        Text(startDate.format(dateFormatter))
                                        Text(endDate.format(dateFormatter))
                                    }
                                }
                            } else {
                                Text(startDate.format(dateFormatter))
                            }
                            Text(stringResource(id = R.string.all_day_switch))
                        } else {
                            val atString = stringResource(id = R.string.at)
                            if (useVerticalLayout) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column {
                                        Text(stringResource(id = R.string.start))
                                        Text(
                                            text = "${startDate.format(dateFormatter)} $atString ${startDate.format(timeFormatter)}",
                                            color = LocalContentColor.current.copy(alpha = 0.8f)
                                        )
                                    }
                                    Column {
                                        Text(stringResource(id = R.string.end))
                                        Text(
                                            text = "${endDate.format(dateFormatter)} $atString ${endDate.format(timeFormatter)}",
                                            color = LocalContentColor.current.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            } else {
                                Row {
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Text(stringResource(id = R.string.start))
                                        Text(stringResource(id = R.string.end))
                                    }
                                    Column {
                                        Text("${startDate.format(dateFormatter)} $atString ${startDate.format(timeFormatter)}")
                                        Text("${endDate.format(dateFormatter)} $atString ${endDate.format(timeFormatter)}")
                                    }
                                }
                            }
                        }
                        if (repetitionRule != RepetitionRule.NONE) {
                            Text(stringResource(id = repetitionRule.displayNameRes))
                        }
                    }
                }
            }
        }
    }

    if (showCalendarDialog) {
        selectedCalendar?.let {
            SelectCalendarDialog(
                calendars = editableCalendars,
                currentSelection = it,
                onCalendarSelected = { newSelection ->
                    selectedCalendar = newSelection
                },
                onDismissRequest = { showCalendarDialog = false }
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                eventToEdit?.id?.let { deleteEvent(context, it) }
                onDelete()
            },
            title = title
        )
    }

    if (showDeleteRecurringDialog) {
        DeleteRecurringEventDialog(
            onDismissRequest = { showDeleteRecurringDialog = false },
            onConfirm = { option ->
                showDeleteRecurringDialog = false
                if (eventToEdit != null) {
                    when (option) {
                        DeleteRecurringOption.SINGLE_EVENT -> {
                            cancelEventInstance(context, eventToEdit)
                            onDelete()
                        }
                        DeleteRecurringOption.ALL_EVENTS -> {
                            deleteEvent(context, eventToEdit.id)
                            onDelete()
                        }
                    }
                }
            }
        )
    }

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.discard_changes_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardChangesDialog = false
                        onBackPress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(id = R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            }
        )
    }

    if (showStartDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartDatePickerDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            startDate = LocalDateTime.of(newLocalDate, startDate.toLocalTime())
                            if (startDate.isAfter(endDate)) {
                                endDate = if (isAllDay) startDate.toLocalDate().atTime(startDate.toLocalTime()) else startDate.plusHours(1)
                            }
                        }
                        showStartDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
                ) { Text(stringResource(id = R.string.accept)) }
            },
            dismissButton = { TextButton(onClick = { showStartDatePickerDialog = false }) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CalendarioTheme.colors.fondoDialogos,
                    titleContentColor = CalendarioTheme.colors.textSystem,
                    headlineContentColor = CalendarioTheme.colors.textSystem,
                    weekdayContentColor = CalendarioTheme.colors.textSystem,
                    dayContentColor = CalendarioTheme.colors.textSystem,
                    selectedDayContentColor = if (isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black,
                    selectedDayContainerColor = CalendarioTheme.colors.cabecera,
                    todayContentColor = CalendarioTheme.colors.cabecera,
                    todayDateBorderColor = CalendarioTheme.colors.cabecera
                )
            )
        }
    }

    if (showEndDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePickerDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            endDate = LocalDateTime.of(newLocalDate, endDate.toLocalTime())
                        }
                        showEndDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
                ) { Text(stringResource(id = R.string.accept)) }
            },
            dismissButton = { TextButton(onClick = { showEndDatePickerDialog = false }) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CalendarioTheme.colors.fondoDialogos,
                    titleContentColor = CalendarioTheme.colors.textSystem,
                    headlineContentColor = CalendarioTheme.colors.textSystem,
                    weekdayContentColor = CalendarioTheme.colors.textSystem,
                    dayContentColor = CalendarioTheme.colors.textSystem,
                    selectedDayContentColor = if (isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black,
                    selectedDayContainerColor = CalendarioTheme.colors.cabecera,
                    todayContentColor = CalendarioTheme.colors.cabecera,
                    todayDateBorderColor = CalendarioTheme.colors.cabecera
                )
            )
        }
    }

    if (showStartTimePickerDialog) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePickerDialog = false },
            onConfirm = { hour, minute ->
                val newTime = LocalTime.of(hour, minute)
                startDate = LocalDateTime.of(startDate.toLocalDate(), newTime)
                if (startDate.isAfter(endDate)) {
                    endDate = startDate.plusHours(1)
                }
                showStartTimePickerDialog = false
            },
            initialHour = startDate.hour,
            initialMinute = startDate.minute
        )
    }

    if (showEndTimePickerDialog) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePickerDialog = false },
            onConfirm = { hour, minute ->
                val newTime = LocalTime.of(hour, minute)
                val newEndDate = LocalDateTime.of(endDate.toLocalDate(), newTime)
                if (newEndDate.isAfter(startDate)) {
                    endDate = newEndDate
                } else {
                    Toast.makeText(context, R.string.end_time_before_start_time_error, Toast.LENGTH_SHORT).show()
                }
                showEndTimePickerDialog = false
            },
            initialHour = endDate.hour,
            initialMinute = endDate.minute
        )
    }

    if (showRepetitionDialog) {
        RepetitionSelectionDialog(
            currentRule = repetitionRule,
            onConfirm = { newRule ->
                repetitionRule = newRule
                showRepetitionDialog = false
            },
            onDismissRequest = { showRepetitionDialog = false }
        )
    }
}

@Composable
private fun AdaptiveDateTimeRow(
    label: String,
    date: LocalDateTime,
    isAllDay: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    fontScale: Float
) {
    val showTwoLines = fontScale > 1.1f

    val dateText = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar(Char::uppercase)} ${date.format(dateFormatter)}"

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
                Text(
                    text = dateText,
                    modifier = Modifier.clickable(onClick = onDateClick),
                    color = CalendarioTheme.colors.textSystem,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                Text(
                    text = dateText,
                    modifier = Modifier.clickable(onClick = onDateClick),
                    color = CalendarioTheme.colors.textSystem,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
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
