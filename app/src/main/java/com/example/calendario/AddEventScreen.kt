package com.example.calendario

import android.util.Log
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class RepetitionRule(val rrule: String?, val displayName: String) {
    NONE(null, "No se repite"),
    DAILY("FREQ=DAILY", "Cada día"),
    WEEKLY("FREQ=WEEKLY", "Cada semana"),
    MONTHLY("FREQ=MONTHLY", "Cada mes"),
    YEARLY("FREQ=YEARLY", "Cada año")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    onBackPress: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    editableCalendars: List<CalendarInfo>,
    isDarkTheme: Boolean,
    initialDate: LocalDate?,
    eventToEdit: Festivo? = null
) {
    val context = LocalContext.current
    val isEditMode = eventToEdit != null

    var title by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(true) }
    var selectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDateTime.now()) }
    var endDate by remember { mutableStateOf(LocalDateTime.now().plusHours(1)) }
    var repetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteSeriesDialog by remember { mutableStateOf(false)

    }

    LaunchedEffect(key1 = eventToEdit, key2 = editableCalendars) {
        if (isEditMode) {
            Log.d("AddEventScreen_Debug", "RRULE for event '${eventToEdit?.title}': [${eventToEdit?.rrule}]")
            eventToEdit?.let { 
                title = it.title
                isAllDay = it.isAllDay
                selectedCalendar = editableCalendars.find { cal -> cal.id == it.calendarId }
                
                startDate = if (it.isAllDay) {
                    it.date.atStartOfDay()
                } else {
                    LocalDateTime.of(it.date, it.startTime ?: LocalTime.now())
                }

                endDate = if (it.isAllDay) {
                    it.date.atStartOfDay() 
                } else {
                    it.endTime?.let { endTime -> LocalDateTime.of(it.date, endTime) } ?: startDate.plusHours(1)
                }
                repetitionRule = RepetitionRule.entries.find { rule -> rule.rrule != null && it.rrule?.startsWith(rule.rrule) == true } ?: RepetitionRule.NONE
            }
        } else {
            val now = LocalDateTime.now()
            val effectiveInitialDateTime = initialDate?.atTime(now.toLocalTime()) ?: now
            startDate = effectiveInitialDateTime
            endDate = effectiveInitialDateTime.plusHours(1)
            selectedCalendar = editableCalendars.find { it.isPrimary } ?: editableCalendars.firstOrNull()
        }
    }

    var showStartDatePickerDialog by remember { mutableStateOf(false) }
    var showEndDatePickerDialog by remember { mutableStateOf(false) }
    var showStartTimePickerDialog by remember { mutableStateOf(false) }
    var showEndTimePickerDialog by remember { mutableStateOf(false) }
    var showRepetitionDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar evento" else "Nuevo evento", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { 
                            if (eventToEdit?.rrule != null) {
                                showDeleteSeriesDialog = true
                            } else {
                                showDeleteDialog = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Borrar evento",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.background else Color(0xFFE3F2FD)
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCalendarDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCalendar?.displayName ?: "No hay calendarios editables",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Seleccionar calendario",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Second Block ---
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Todo el día", modifier = Modifier.weight(1f))
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
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Inicio")
                        Row {
                            Text(startDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar(Char::uppercase), modifier = Modifier.padding(end = 8.dp))
                            Text(startDate.format(dateFormatter), modifier = Modifier.clickable { showStartDatePickerDialog = true })
                            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                            Text(
                                startDate.format(timeFormatter),
                                modifier = Modifier
                                    .alpha(if (isAllDay) 0.5f else 1f)
                                    .clickable(!isAllDay) { showStartTimePickerDialog = true }
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fin")
                        Row {
                            Text(endDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar(Char::uppercase), modifier = Modifier.padding(end = 8.dp))
                            Text(endDate.format(dateFormatter), modifier = Modifier.clickable { showEndDatePickerDialog = true })
                            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                            Text(
                                endDate.format(timeFormatter),
                                modifier = Modifier
                                    .alpha(if (isAllDay) 0.5f else 1f)
                                    .clickable(!isAllDay) { showEndTimePickerDialog = true }
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRepetitionDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(repetitionRule.displayName, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Seleccionar repetición"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Third Block (Summary) ---
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text("Resumen:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Título: ${title.ifBlank { "(Sin título)" }}")
                    Text("Calendario: ${selectedCalendar?.displayName ?: "N/A"}")
                    if (isAllDay) {
                        Row {
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text("Del:")
                                if (startDate.toLocalDate() != endDate.toLocalDate()) {
                                    Text("Al:")
                                }
                            }
                            Column {
                                Text(startDate.format(dateFormatter))
                                if (startDate.toLocalDate() != endDate.toLocalDate()) {
                                    Text(endDate.format(dateFormatter))
                                }
                            }
                        }
                        Text("Todo el día")
                    } else {
                        Row {
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text("Inicio:")
                                Text("Fin:")
                            }
                            Column {
                                Text("${startDate.format(dateFormatter)} a las ${startDate.format(timeFormatter)}")
                                Text("${endDate.format(dateFormatter)} a las ${endDate.format(timeFormatter)}")
                            }
                        }
                    }
                    if (repetitionRule != RepetitionRule.NONE) {
                        Text(repetitionRule.displayName)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBackPress) {
                    Text("CANCELAR")
                }
                Button(onClick = {
                    if (isEditMode && eventToEdit != null) {
                        val originalStartDate = if (eventToEdit.isAllDay) eventToEdit.date.atStartOfDay() else LocalDateTime.of(eventToEdit.date, eventToEdit.startTime)
                        val originalEndDate = if (eventToEdit.isAllDay) eventToEdit.date.atStartOfDay() else (eventToEdit.endTime?.let { LocalDateTime.of(eventToEdit.date, it) } ?: originalStartDate.plusHours(1))

                        val hasChanges = title != eventToEdit.title ||
                                isAllDay != eventToEdit.isAllDay ||
                                selectedCalendar?.id != eventToEdit.calendarId ||
                                startDate != originalStartDate ||
                                endDate != originalEndDate ||
                                repetitionRule.rrule != eventToEdit.rrule

                        if (hasChanges) {
                            updateEvent(
                                context = context, eventId = eventToEdit.id, title = title,
                                calendarId = selectedCalendar?.id, startDate = startDate,
                                endDate = endDate, isAllDay = isAllDay, repetitionRule = repetitionRule
                            )
                            onSave()
                        } else {
                            Toast.makeText(context, "No hay cambios que guardar", Toast.LENGTH_SHORT).show()
                            onBackPress()
                        }
                    } else {
                        createEvent(
                            context = context, title = title, calendarId = selectedCalendar?.id,
                            startDate = startDate, endDate = endDate, isAllDay = isAllDay,
                            repetitionRule = repetitionRule
                        )
                        onSave()
                    }
                }) {
                    Text(if (isEditMode) "ACTUALIZAR" else "GUARDAR")
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
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres eliminar este evento: \"$title\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        eventToEdit?.id?.let { deleteEvent(context, it) }
                        onDelete()
                    }
                ) { Text("ELIMINAR") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("CANCELAR") } }
        )
    }
    
    if (showDeleteSeriesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSeriesDialog = false },
            title = { Text("Confirmar eliminación de serie") },
            text = {
                Column {
                    Text("Este es un evento repetido. ¿Seguro que quieres eliminar toda la serie: \"$title\"? Esta acción no se puede deshacer.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Para opciones avanzadas (borrar solo este evento o los futuros), utiliza la aplicación de Google Calendar.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSeriesDialog = false
                        eventToEdit?.id?.let { deleteEvent(context, it) }
                        onDelete()
                    }
                ) { Text("ELIMINAR SERIE") }
            },
            dismissButton = { TextButton(onClick = { showDeleteSeriesDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showStartDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            startDate = LocalDateTime.of(newLocalDate, startDate.toLocalTime())
                            if (startDate.isAfter(endDate)) {
                                endDate = if (isAllDay) startDate.toLocalDate().atTime(startDate.toLocalTime()) else startDate.plusHours(1)
                            }
                        }
                        showStartDatePickerDialog = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePickerDialog = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
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
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            endDate = LocalDateTime.of(newLocalDate, endDate.toLocalTime())
                        }
                        showEndDatePickerDialog = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePickerDialog = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showStartTimePickerDialog) {
        val timePickerState = rememberTimePickerState(initialHour = startDate.hour, initialMinute = startDate.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showStartTimePickerDialog = false },
            title = { Text("Seleccionar hora de inicio") },
            text = { TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        startDate = LocalDateTime.of(startDate.toLocalDate(), newTime)
                        if (startDate.isAfter(endDate)) {
                            endDate = startDate.plusHours(1)
                        }
                        showStartTimePickerDialog = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTimePickerDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showEndTimePickerDialog) {
        val timePickerState = rememberTimePickerState(initialHour = endDate.hour, initialMinute = endDate.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showEndTimePickerDialog = false },
            title = { Text("Seleccionar hora de fin") },
            text = { TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val newEndDate = LocalDateTime.of(endDate.toLocalDate(), newTime)
                        if (newEndDate.isAfter(startDate)) {
                            endDate = newEndDate
                        } else {
                            Toast.makeText(context, "La hora de fin no puede ser anterior a la de inicio", Toast.LENGTH_SHORT).show()
                        }
                        showEndTimePickerDialog = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndTimePickerDialog = false }) { Text("Cancelar") } }
        )
    }
    
    if (showRepetitionDialog) {
        var tempSelection by remember { mutableStateOf(repetitionRule) }
        AlertDialog(
            onDismissRequest = { showRepetitionDialog = false },
            title = { Text("Repetir evento") },
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
                                onClick = { tempSelection = rule }
                            )
                            Text(rule.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repetitionRule = tempSelection
                        showRepetitionDialog = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showRepetitionDialog = false }) { Text("Cancelar") } }
        )
    }
}
