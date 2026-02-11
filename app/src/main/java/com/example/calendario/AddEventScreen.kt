package com.example.calendario

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding

enum class RepetitionRule(val rrule: String?, val displayNameRes: Int) {
    NONE(null, R.string.does_not_repeat),
    DAILY("FREQ=DAILY", R.string.every_day),
    WEEKLY("FREQ=WEEKLY", R.string.every_week),
    MONTHLY("FREQ=MONTHLY", R.string.every_month),
    YEARLY("FREQ=YEARLY", R.string.every_year)
}

enum class SaveEventError {
    NO_PERMISSION,
    NO_CALENDAR_SELECTED,
    TITLE_EMPTY,
    END_BEFORE_START
}

private fun validateEventData(
    context: Context,
    title: String,
    selectedCalendar: CalendarInfo?,
    startDate: LocalDateTime,
    endDate: LocalDateTime
): SaveEventError? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return SaveEventError.NO_PERMISSION
    }
    if (title.isBlank()) {
        return SaveEventError.TITLE_EMPTY
    }
    if (selectedCalendar == null) {
        return SaveEventError.NO_CALENDAR_SELECTED
    }
    if (endDate.isBefore(startDate)) {
        return SaveEventError.END_BEFORE_START
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    onBackPress: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    editableCalendars: List<CalendarInfo>,
    initialDate: LocalDate?,
    eventToEdit: Festivo? = null,
    initialCalendar: CalendarInfo?
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(true) }
    var selectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDateTime.now()) }
    var endDate by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var repetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteRecurringDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<SaveEventError?>(null) }
    var localEventToEdit by remember { mutableStateOf(eventToEdit) }
    var isCopying by remember { mutableStateOf(false) }

    var initialTitle by remember { mutableStateOf("") }
    var initialIsAllDay by remember { mutableStateOf(true) }
    var initialSelectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var initialStartDate by remember { mutableStateOf(LocalDateTime.now()) }
    var initialEndDate by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var initialRepetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }

    LaunchedEffect(key1 = localEventToEdit, key2 = editableCalendars) {
        if (localEventToEdit != null && !isCopying) {
            title = localEventToEdit!!.title
            isAllDay = localEventToEdit!!.isAllDay
            selectedCalendar = editableCalendars.find { it.id == localEventToEdit!!.calendarId }
            startDate = if (localEventToEdit!!.isAllDay) localEventToEdit!!.date.atStartOfDay() else LocalDateTime.of(localEventToEdit!!.date, localEventToEdit!!.startTime ?: LocalTime.now())
            endDate = if (localEventToEdit!!.isAllDay) localEventToEdit!!.date.atStartOfDay() else localEventToEdit!!.endTime?.let { LocalDateTime.of(localEventToEdit!!.date, it) } ?: startDate.plusHours(1)
            repetitionRule = RepetitionRule.entries.find { it.rrule != null && localEventToEdit!!.rrule?.startsWith(it.rrule) == true } ?: RepetitionRule.NONE

            initialTitle = title
            initialIsAllDay = isAllDay
            initialSelectedCalendar = selectedCalendar
            initialStartDate = startDate
            initialEndDate = endDate
            initialRepetitionRule = repetitionRule
        } else if (isCopying) {
            isCopying = false
        } else {
            val now = LocalDateTime.now()
            val effectiveInitialDateTime = initialDate?.atTime(now.toLocalTime()) ?: now
            title = ""
            isAllDay = true
            startDate = effectiveInitialDateTime
            endDate = effectiveInitialDateTime.plusHours(1)
            selectedCalendar = initialCalendar
            repetitionRule = RepetitionRule.NONE

            initialTitle = ""
            initialIsAllDay = true
            initialSelectedCalendar = selectedCalendar
            initialStartDate = startDate
            initialEndDate = endDate
            initialRepetitionRule = RepetitionRule.NONE
        }
    }

    val hasChanges by remember(title, isAllDay, selectedCalendar, startDate, endDate, repetitionRule) {
        derivedStateOf {
            title != initialTitle ||
            isAllDay != initialIsAllDay ||
            selectedCalendar?.id != initialSelectedCalendar?.id ||
            startDate != initialStartDate ||
            endDate != initialEndDate ||
            repetitionRule != initialRepetitionRule
        }
    }

    val backAction = { if (hasChanges) showDiscardChangesDialog = true else onBackPress() }

    val saveAction = {
        val error = validateEventData(context, title, selectedCalendar, startDate, endDate)
        if (error != null) {
            saveError = error
        } else {
            val success = if (localEventToEdit != null) {
                updateEvent(context, localEventToEdit!!.id, title, selectedCalendar?.id, startDate, endDate, isAllDay, repetitionRule)
            } else {
                createEvent(context, title, selectedCalendar?.id, startDate, endDate, isAllDay, repetitionRule)
            }
            if (success) onSave()
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
                title = { Text(if (localEventToEdit != null) stringResource(id = R.string.edit_event) else stringResource(id = R.string.new_event)) },
                navigationIcon = { IconButton(onClick = backAction) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back)) } },
                actions = {
                    if (hasChanges) {
                        IconButton(onClick = saveAction) { Icon(Icons.Default.Check, stringResource(id = R.string.save)) }
                    }
                    localEventToEdit?.let {
                        IconButton(onClick = {
                            isCopying = true
                            val today = LocalDate.now()
                            val originalStartDate = startDate
                            if (startDate.toLocalDate().isBefore(today)) {
                                val duration = Duration.between(startDate, endDate)
                                val newStartDate = LocalDateTime.of(today, startDate.toLocalTime())
                                startDate = newStartDate
                                endDate = newStartDate.plus(duration)
                            }
                            initialTitle = title
                            initialIsAllDay = isAllDay
                            initialSelectedCalendar = selectedCalendar
                            initialStartDate = originalStartDate 
                            initialEndDate = endDate
                            initialRepetitionRule = repetitionRule
                            localEventToEdit = null
                        }) { Icon(Icons.Default.ContentCopy, null) }
                        
                        IconButton(onClick = {
                            if (it.rrule != null) showDeleteRecurringDialog = true else showDeleteDialog = true
                        }) { Icon(Icons.Default.Delete, stringResource(id = R.string.delete_event)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CalendarioTheme.colors.cabecera, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            AddEventForm(
                title = title, onTitleChange = { title = it },
                selectedCalendar = selectedCalendar, onCalendarClick = { showCalendarDialog = true },
                isAllDay = isAllDay, onAllDayChange = { isAllDay = it },
                startDate = startDate, onStartDateClick = { showStartDatePickerDialog = true }, onStartTimeClick = { showStartTimePickerDialog = true },
                endDate = endDate, onEndDateClick = { showEndDatePickerDialog = true }, onEndTimeClick = { showEndTimePickerDialog = true },
                repetitionRule = repetitionRule, onRepetitionClick = { showRepetitionDialog = true }
            )
        }
    }

    if (showCalendarDialog && selectedCalendar != null) {
        SelectCalendarDialog(calendars = editableCalendars, currentSelection = selectedCalendar, onCalendarSelected = { selectedCalendar = it }, onDismissRequest = { showCalendarDialog = false })
    }

    saveError?.let { error ->
        val (errorTitle, errorText, showSettingsButton) = when (error) {
            SaveEventError.NO_PERMISSION -> Triple(stringResource(R.string.permission_denied_title), stringResource(R.string.permission_denied_text), true)
            SaveEventError.NO_CALENDAR_SELECTED -> Triple(stringResource(R.string.error), stringResource(R.string.no_calendar_selected_error), false)
            SaveEventError.TITLE_EMPTY -> Triple(stringResource(R.string.error), stringResource(R.string.title_empty_error), false)
            SaveEventError.END_BEFORE_START -> Triple(stringResource(R.string.error), stringResource(R.string.end_time_before_start_time_error), false)
        }
        AlertDialog(onDismissRequest = { saveError = null }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(errorTitle, fontWeight = FontWeight.Bold) }, text = { Text(errorText) },
            confirmButton = { Button(onClick = { saveError = null }, colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)) { Text(stringResource(R.string.accept)) } },
            dismissButton = { if (showSettingsButton) { TextButton(onClick = { saveError = null; context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) }) { Text(stringResource(R.string.go_to_settings), color = CalendarioTheme.colors.textSystem) } } }
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(onDismissRequest = { showDeleteDialog = false }, onConfirm = { showDeleteDialog = false; localEventToEdit?.id?.let { deleteEvent(context, it) }; onDelete() }, title = title)
    }

    if (showDeleteRecurringDialog) {
        DeleteRecurringEventDialog(onDismissRequest = { showDeleteRecurringDialog = false }, onConfirm = { option ->
            showDeleteRecurringDialog = false
            localEventToEdit?.let { event ->
                when (option) {
                    DeleteRecurringOption.SINGLE_EVENT -> { cancelEventInstance(context, event); onDelete() }
                    DeleteRecurringOption.ALL_EVENTS -> { deleteEvent(context, event.id); onDelete() }
                }
            }
        })
    }

    if (showDiscardChangesDialog) {
        AlertDialog(onDismissRequest = { showDiscardChangesDialog = false }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(R.string.discard_changes_title), fontWeight = FontWeight.Bold) }, text = { Text(stringResource(R.string.discard_changes_confirmation)) },
            confirmButton = { Button(onClick = { showDiscardChangesDialog = false; onBackPress() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(stringResource(R.string.discard)) } },
            dismissButton = { TextButton(onClick = { showDiscardChangesDialog = false }) { Text(stringResource(R.string.cancel), color = CalendarioTheme.colors.textSystem) } }
        )
    }

    if (showStartDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showStartDatePickerDialog = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val duration = Duration.between(startDate, endDate)
                        startDate = LocalDateTime.of(newLocalDate, startDate.toLocalTime())
                        endDate = if (isCopying || localEventToEdit == null) startDate.plus(duration) else startDate.plusHours(1)
                    }
                    showStartDatePickerDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = { TextButton(onClick = { showStartDatePickerDialog = false }) { Text(stringResource(R.string.cancel), color = CalendarioTheme.colors.textSystem) } },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, headlineContentColor = CalendarioTheme.colors.textSystem, weekdayContentColor = CalendarioTheme.colors.textSystem, dayContentColor = CalendarioTheme.colors.textSystem, selectedDayContentColor = if (isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black, selectedDayContainerColor = CalendarioTheme.colors.cabecera, todayContentColor = CalendarioTheme.colors.cabecera, todayDateBorderColor = CalendarioTheme.colors.cabecera))
        }
    }

    if (showEndDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        })
        DatePickerDialog(onDismissRequest = { showEndDatePickerDialog = false },
            confirmButton = { Button(onClick = { datePickerState.selectedDateMillis?.let { endDate = LocalDateTime.of(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(), endDate.toLocalTime()) }; showEndDatePickerDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)) { Text(stringResource(R.string.accept)) } },
            dismissButton = { TextButton(onClick = { showEndDatePickerDialog = false }) { Text(stringResource(R.string.cancel), color = CalendarioTheme.colors.textSystem) } },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, headlineContentColor = CalendarioTheme.colors.textSystem, weekdayContentColor = CalendarioTheme.colors.textSystem, dayContentColor = CalendarioTheme.colors.textSystem, selectedDayContentColor = if (isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black, selectedDayContainerColor = CalendarioTheme.colors.cabecera, todayContentColor = CalendarioTheme.colors.cabecera, todayDateBorderColor = CalendarioTheme.colors.cabecera))
        }
    }

    if (showStartTimePickerDialog) {
        TimePickerDialog(onDismissRequest = { showStartTimePickerDialog = false }, onConfirm = { hour, minute ->
            val newTime = LocalTime.of(hour, minute)
            startDate = LocalDateTime.of(startDate.toLocalDate(), newTime)
            if (startDate.isAfter(endDate)) endDate = startDate.plusHours(1)
            showStartTimePickerDialog = false
        }, initialHour = startDate.hour, initialMinute = startDate.minute)
    }

    if (showEndTimePickerDialog) {
        TimePickerDialog(onDismissRequest = { showEndTimePickerDialog = false }, onConfirm = { hour, minute ->
            val newTime = LocalTime.of(hour, minute)
            val newEndDate = LocalDateTime.of(endDate.toLocalDate(), newTime)
            if (newEndDate.isAfter(startDate)) endDate = newEndDate else Toast.makeText(context, R.string.end_time_before_start_time_error, Toast.LENGTH_SHORT).show()
            showEndTimePickerDialog = false
        }, initialHour = endDate.hour, initialMinute = endDate.minute)
    }

    if (showRepetitionDialog) {
        RepetitionSelectionDialog(currentRule = repetitionRule, onConfirm = { repetitionRule = it; showRepetitionDialog = false }, onDismissRequest = { showRepetitionDialog = false })
    }
}