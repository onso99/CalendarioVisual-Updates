package com.example.calendario

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.core.content.ContextCompat
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private fun processAlarmForEvent(
    context: Context,
    eventId: Long,
    hasAlarm: Boolean,
    alarmTime: LocalTime,
    startDate: LocalDateTime,
    title: String,
    isAllDay: Boolean,
    selectedCalendarId: Long?,
    repetitionRule: RepetitionRule,
    repeatUntil: LocalDate?
) {
    if (hasAlarm) {
        val finalRrule = if (repetitionRule != RepetitionRule.NONE && repeatUntil != null) {
            val untilStr = repeatUntil.format(AppFormats.IcsDateTime)
            "${repetitionRule.rrule};UNTIL=$untilStr"
        } else repetitionRule.rrule

        // PUNTO CLAVE: Si es todo el día, el punto de referencia es la medianoche
        val cleanStartTime = if (isAllDay) LocalTime.MIDNIGHT else startDate.toLocalTime().withSecond(0).withNano(0)
        val cleanAlarmTime = alarmTime.withSecond(0).withNano(0)
        
        val offset = Duration.between(cleanAlarmTime, cleanStartTime).toMinutes().toInt()
        AlarmUtils.saveAlarmSetting(context, eventId, offset)
        val tempFestivo = Festivo(
            id = eventId,
            title = title,
            description = null,
            date = startDate.toLocalDate(),
            startTime = if (isAllDay) null else startDate.toLocalTime(),
            endTime = null,
            isAllDay = isAllDay,
            calendarId = selectedCalendarId ?: -1,
            isFromHolidaySource = false,
            rrule = finalRrule
        )
        AlarmUtils.scheduleAlarm(context, tempFestivo)
    } else {
        AlarmUtils.saveAlarmSetting(context, eventId, null)
        AlarmUtils.cancelAlarm(context, eventId, startDate.toLocalDate())
    }
}

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
    END_BEFORE_START,
    LANES_FULL
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

private fun isLaneAvailable(
    eventsByDate: Map<LocalDate, List<Festivo>>,
    startDate: LocalDate,
    endDate: LocalDate,
    excludeEventId: Long?
): LocalDate? {
    var current = startDate
    while (!current.isAfter(endDate)) {
        val count = eventsByDate[current]?.count { it.isLongPeriod && it.lane != null && it.id != excludeEventId } ?: 0
        if (count >= 5) return current
        current = current.plusDays(1)
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
    initialCalendar: CalendarInfo?,
    eventsByDate: Map<LocalDate, List<Festivo>>
) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val defaultAlarmOffset = remember { appPrefs.getInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, 30) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    var title by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }
    var selectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDateTime.now()) }
    var endDate by remember { mutableStateOf(LocalDateTime.now().plusMinutes(30)) }
    var repetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }
    var repeatUntilDate by remember { mutableStateOf<LocalDate?>(null) }
    var repeatCount by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteRecurringDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<SaveEventError?>(null) }
    var conflictingDate by remember { mutableStateOf<LocalDate?>(null) }
    var localEventToEdit by remember { mutableStateOf(eventToEdit) }
    var isCopying by remember { mutableStateOf(false) }
    var showEditRecurringDialog by remember { mutableStateOf(false) }

    var hasAlarm by remember { mutableStateOf(false) }
    var alarmTime by remember { mutableStateOf(LocalTime.now()) }
    var showAlarmTimePickerDialog by remember { mutableStateOf(false) }

    var initialTitle by remember { mutableStateOf("") }
    var initialIsAllDay by remember { mutableStateOf(true) }
    var initialSelectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }
    var initialStartDate by remember { mutableStateOf(LocalDateTime.now()) }
    var initialEndDate by remember { mutableStateOf(LocalDateTime.now().plusMinutes(30)) }
    var initialRepetitionRule by remember { mutableStateOf(RepetitionRule.NONE) }
    var initialRepeatUntilDate by remember { mutableStateOf<LocalDate?>(null) }
    var initialHasAlarm by remember { mutableStateOf(false) }
    var initialAlarmTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var isLongPeriod by remember { mutableStateOf(false) }
    var selectedColorInt by remember { mutableStateOf<Int?>(null) }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = localEventToEdit, key2 = editableCalendars) {
        if (localEventToEdit != null && !isCopying) {
            isLongPeriod = localEventToEdit!!.isLongPeriod
            title = localEventToEdit!!.title
            // REGLA DE HIERRO (v3.1.34): Al editar, si es periodo largo forzamos Todo el día
            isAllDay = localEventToEdit!!.isAllDay || isLongPeriod
            selectedCalendar = editableCalendars.find { it.id == localEventToEdit!!.calendarId }
            
            if (localEventToEdit!!.fullStartMillis != null && localEventToEdit!!.fullEndMillis != null) {
                // Recuperamos el rango completo real del evento. 
                // IMPORTANTE: Los eventos "All Day" se almacenan en UTC por estándar de Android.
                val zoneId = if (localEventToEdit!!.isAllDay) ZoneId.of("UTC") else ZoneId.systemDefault()
                startDate = Instant.ofEpochMilli(localEventToEdit!!.fullStartMillis!!).atZone(zoneId).toLocalDateTime()
                endDate = Instant.ofEpochMilli(localEventToEdit!!.fullEndMillis!!).atZone(zoneId).toLocalDateTime()
                
                // Ajuste visual para el fin de eventos Todo el día (Android guarda el día siguiente a las 00:00)
                if (localEventToEdit!!.isAllDay && endDate.isAfter(startDate)) {
                    endDate = endDate.minusDays(1)
                }
            } else {
                startDate = (if (localEventToEdit!!.isAllDay) localEventToEdit!!.date.atStartOfDay() else LocalDateTime.of(localEventToEdit!!.date, localEventToEdit!!.startTime ?: LocalTime.now()))
                    .withSecond(0).withNano(0)
                endDate = (if (localEventToEdit!!.isAllDay) localEventToEdit!!.date.atStartOfDay() else localEventToEdit!!.endTime?.let { LocalDateTime.of(localEventToEdit!!.date, it) } ?: startDate.plusHours(1))
                    .withSecond(0).withNano(0)
            }
            
            isLongPeriod = localEventToEdit!!.isLongPeriod
            selectedColorInt = localEventToEdit!!.customColor
            repeatCount = localEventToEdit!!.repeatCount
                
            repetitionRule = RepetitionRule.entries.find { it.rrule != null && localEventToEdit!!.rrule?.startsWith(it.rrule) == true } ?: RepetitionRule.NONE
            
            // Extraer UNTIL de la RRULE si existe
            repeatUntilDate = localEventToEdit!!.rrule?.let { rrule ->
                if (rrule.contains("UNTIL=")) {
                    val untilPart = rrule.substringAfter("UNTIL=").substringBefore(";")
                    try {
                        // Formato esperado: yyyyMMddT...Z o yyyyMMdd
                        LocalDate.parse(untilPart.take(8), AppFormats.IcsDate)
                    } catch (_: Exception) { null }
                } else null
            }

            val offset = AlarmUtils.getAlarmOffset(context, localEventToEdit!!.id)
            hasAlarm = offset != null
            
            alarmTime = if (offset != null) {
                // SINCRO INTELIGENTE (v3.1.34): Usamos 'isAllDay' (forzado para largos) para calcular la hora real
                if (isAllDay) LocalTime.MIDNIGHT.minusMinutes(offset.toLong())
                else startDate.toLocalTime().minusMinutes(offset.toLong())
            } else {
                if (isAllDay) LocalTime.of(9, 0)
                else startDate.toLocalTime().minusMinutes(defaultAlarmOffset.toLong())
            }

            initialTitle = title
            initialIsAllDay = isAllDay
            initialSelectedCalendar = selectedCalendar
            initialStartDate = startDate
            initialEndDate = endDate
            initialRepetitionRule = repetitionRule
            initialRepeatUntilDate = repeatUntilDate
            initialHasAlarm = hasAlarm
            initialAlarmTime = alarmTime
        } else if (isCopying) {
            isCopying = false
        } else {
            val now = LocalDateTime.now().withSecond(0).withNano(0)
            val effectiveInitialDateTime = initialDate?.atTime(now.toLocalTime()) ?: now
            title = ""
            isAllDay = true // Por defecto: Todo el día
            startDate = effectiveInitialDateTime
            endDate = effectiveInitialDateTime.plusMinutes(30) // Margen de 30 min (v3.0.24)
            selectedCalendar = initialCalendar
            repetitionRule = RepetitionRule.NONE
            repeatUntilDate = null
            repeatCount = null
            hasAlarm = false
            // Alarma a las 09:00 AM por defecto para Todo el día
            alarmTime = LocalTime.of(9, 0)

            initialTitle = ""
            initialIsAllDay = true
            initialStartDate = startDate
            initialEndDate = endDate
            initialRepetitionRule = repetitionRule
            initialRepeatUntilDate = null
        }
    }

    val hasChanges by remember(title, isAllDay, selectedCalendar, startDate, endDate, repetitionRule, repeatUntilDate, repeatCount, isLongPeriod, selectedColorInt, hasAlarm, alarmTime) {
        derivedStateOf {
            title != initialTitle ||
            isAllDay != initialIsAllDay ||
            selectedCalendar?.id != initialSelectedCalendar?.id ||
            startDate != initialStartDate ||
            endDate != initialEndDate ||
            repetitionRule != initialRepetitionRule ||
            repeatUntilDate != initialRepeatUntilDate ||
            repeatCount != localEventToEdit?.repeatCount ||
            isLongPeriod != (localEventToEdit?.isLongPeriod ?: false) ||
            selectedColorInt != localEventToEdit?.customColor ||
            hasAlarm != initialHasAlarm ||
            (hasAlarm && alarmTime != initialAlarmTime)
        }
    }

    val backAction = { if (hasChanges) showDiscardChangesDialog = true else onBackPress() }

    val saveAction = {
        if (localEventToEdit?.rrule != null && localEventToEdit?.id != 0L && hasChanges) {
            showEditRecurringDialog = true
        } else {
            val error = validateEventData(context, title, selectedCalendar, startDate, endDate)
            if (error != null) {
                saveError = error
            } else if (isLongPeriod && isLaneAvailable(eventsByDate, startDate.toLocalDate(), endDate.toLocalDate(), localEventToEdit?.id).also { conflictingDate = it } != null) {
                saveError = SaveEventError.LANES_FULL
            } else {
                val createdEventId = if (localEventToEdit != null && localEventToEdit!!.id != 0L) {
                    updateEvent(context, localEventToEdit!!.id, title, selectedCalendar?.id, startDate, endDate, isAllDay, repetitionRule, repeatUntilDate, repeatCount, selectedColorInt, isLongPeriod)
                } else {
                    createEvent(context, title, selectedCalendar?.id, startDate, endDate, isAllDay, repetitionRule, repeatUntilDate, repeatCount, selectedColorInt, isLongPeriod)
                }

                if (createdEventId != null) {
                    LogCollector.addLog("ALARMA: Procesando tras guardar evento $createdEventId")
                    processAlarmForEvent(context, createdEventId, hasAlarm, alarmTime, startDate, title, isAllDay, selectedCalendar?.id, repetitionRule, repeatUntilDate)
                    onSave()
                }
            }
        }
    }

    var showStartDatePickerDialog by remember { mutableStateOf(false) }
    var showEndDatePickerDialog by remember { mutableStateOf(false) }
    var showStartTimePickerDialog by remember { mutableStateOf(false) }
    var showEndTimePickerDialog by remember { mutableStateOf(false) }
    var showRepeatUntilDatePickerDialog by remember { mutableStateOf(false) }
    var showRepetitionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            val isImported = localEventToEdit?.id == 0L
            TopAppBar(
                title = { Text(if (localEventToEdit != null && !isImported) stringResource(id = R.string.edit_event) else stringResource(id = R.string.new_event)) },
                navigationIcon = { IconButton(onClick = backAction) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back)) } },
                actions = {
                    if (hasChanges || isImported) {
                        IconButton(onClick = saveAction) { Icon(Icons.Default.Check, stringResource(id = R.string.save)) }
                    }
                    localEventToEdit?.let { event ->
                        if (!isImported) {
                            if (!hasChanges) {
                                IconButton(onClick = { IcsHelper.shareEvent(context, event) }) {
                                    Icon(Icons.Default.Share, stringResource(id = R.string.share_event))
                                }
                            }
                            IconButton(onClick = {
                                val currentEvent = localEventToEdit ?: return@IconButton
                                isCopying = true 
                                
                                // 1. Preparar nuevas fechas (Hoy + duración original)
                                val today = LocalDate.now()
                                val duration = Duration.between(startDate, endDate)
                                val newStartDate = LocalDateTime.of(today, startDate.toLocalTime())
                                
                                startDate = newStartDate
                                endDate = newStartDate.plus(duration)
                                
                                // 2. Protección de Calendario: Si el original es de solo lectura, usamos el favorito
                                val originalCalendar = editableCalendars.find { it.id == currentEvent.calendarId }
                                if (originalCalendar?.canModify == false) {
                                    selectedCalendar = initialCalendar // El calendario inicial es el favorito/por defecto
                                }
                                
                                // 3. Soltar el ancla del evento anterior para que sea uno NUEVO
                                localEventToEdit = null
                            }) { Icon(Icons.Default.ContentCopy, stringResource(id = R.string.copy_event)) }
                            
                            IconButton(onClick = {
                                if (event.rrule != null) showDeleteRecurringDialog = true else showDeleteDialog = true
                            }) { Icon(Icons.Default.Delete, stringResource(id = R.string.delete_event)) }
                        }
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
                isAllDay = isAllDay, onAllDayChange = { newValue -> 
                    isAllDay = newValue
                    if (!newValue) {
                        // FASE 2: LÓGICA DE HORA SUGERIDA AL QUITAR "TODO EL DÍA"
                        val today = LocalDate.now()
                        val eventStartDate = startDate.toLocalDate()
                        
                        val startT = if (eventStartDate == today) {
                            LocalTime.now().withSecond(0).withNano(0)
                        } else {
                            LocalTime.of(9, 0)
                        }
                        
                        startDate = LocalDateTime.of(eventStartDate, startT)
                        
                        endDate = if (!isLongPeriod) {
                            // Si no es periodo largo, el fin es simplemente +30 min desde el inicio
                            startDate.plusMinutes(30)
                        } else {
                            // Si es periodo largo, mantenemos la fecha de fin pero ponemos hora lógica
                            val calculatedEnd = LocalDateTime.of(endDate.toLocalDate(), startT.plusMinutes(30))
                            if (calculatedEnd.isBefore(startDate)) startDate.plusMinutes(30) else calculatedEnd
                        }
                    }
                },
                startDate = startDate, onStartDateClick = { showStartDatePickerDialog = true }, onStartTimeClick = { showStartTimePickerDialog = true },
                endDate = endDate, onEndDateClick = { showEndDatePickerDialog = true }, onEndTimeClick = { showEndTimePickerDialog = true },
                repetitionRule = repetitionRule, onRepetitionClick = { showRepetitionDialog = true },
                repeatUntilDate = repeatUntilDate,
                repeatCount = repeatCount,
                isLongPeriod = isLongPeriod,
                onLongPeriodChange = { 
                    isLongPeriod = it 
                    if (it) {
                        repetitionRule = RepetitionRule.NONE
                        isAllDay = true // REGLA: Periodo largo SIEMPRE es todo el día
                        if (endDate.toLocalDate() == startDate.toLocalDate()) {
                            endDate = endDate.plusDays(1)
                        }
                        // Si no hay color elegido, ponemos el Azul Especial por defecto
                        if (selectedColorInt == null) {
                            selectedColorInt = 0xFF4C58D8.toInt()
                        }
                    } else {
                        // Al desactivar, el evento se encoge a un solo día para evitar errores
                        endDate = startDate.plusMinutes(30)
                    }
                },
                selectedColorInt = selectedColorInt,
                onColorSelect = { selectedColorInt = it },
                onPaletteClick = { showCustomColorPicker = true },
                hasAlarm = hasAlarm, 
                onHasAlarmChange = { 
                    hasAlarm = it
                    if (it) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        
                        alarmTime = if (isAllDay) {
                            // Para todo el día, sugerimos las 09:00 AM
                            LocalTime.of(9, 0)
                        } else {
                            // Para eventos con hora, aplicamos la anticipación configurada
                            val calculatedTime = startDate.toLocalTime().minusMinutes(defaultAlarmOffset.toLong())
                            val now = LocalTime.now()
                            // Lógica Conservadora: Si la anticipación ya pasó, sugerimos la hora de inicio
                            if (calculatedTime.isBefore(now) && startDate.toLocalDate().isEqual(LocalDate.now())) {
                                startDate.toLocalTime().withSecond(0).withNano(0)
                            } else {
                                calculatedTime.withSecond(0).withNano(0)
                            }
                        }
                        
                        showAlarmTimePickerDialog = true
                    }
                },
                alarmTime = alarmTime,
                onAlarmTimeClick = { 
                    if (!hasAlarm) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        alarmTime = if (isAllDay) {
                            LocalTime.of(9, 0)
                        } else {
                            val calculatedTime = startDate.toLocalTime().minusMinutes(defaultAlarmOffset.toLong())
                            val now = LocalTime.now()
                            // Lógica Conservadora: Si la anticipación ya pasó, sugerimos la hora de inicio
                            if (calculatedTime.isBefore(now) && startDate.toLocalDate().isEqual(LocalDate.now())) {
                                startDate.toLocalTime().withSecond(0).withNano(0)
                            } else {
                                calculatedTime.withSecond(0).withNano(0)
                            }
                        }
                    }
                    showAlarmTimePickerDialog = true 
                }
            )
        }
    }

    if (showCalendarDialog) {
        SelectCalendarDialog(
            calendars = editableCalendars,
            currentSelection = selectedCalendar,
            onCalendarSelected = { 
                selectedCalendar = it
                showCalendarDialog = false 
            },
            onDismissRequest = { showCalendarDialog = false }
        )
    }

    if (showEditRecurringDialog) {
        EditRecurringEventDialog(
            onDismissRequest = { showEditRecurringDialog = false },
            onConfirm = { option ->
                showEditRecurringDialog = false
                val error = validateEventData(context, title, selectedCalendar, startDate, endDate)
                if (error != null) {
                    saveError = error
                    return@EditRecurringEventDialog
                }

                when (option) {
                    EditRecurringOption.SINGLE_EVENT -> {
                        val createdId = localEventToEdit?.let { 
                            updateSingleEventInSeries(context, it, title, startDate, endDate, isAllDay)
                        }
                        if (createdId != null) {
                            processAlarmForEvent(context, createdId, hasAlarm, alarmTime, startDate, title, isAllDay, selectedCalendar?.id, repetitionRule, repeatUntilDate)
                            onSave()
                        }
                    }
                    EditRecurringOption.ALL_EVENTS -> {
                        val success = updateEvent(context, localEventToEdit!!.id, title, selectedCalendar?.id, startDate, endDate, isAllDay, repetitionRule, repeatUntilDate, repeatCount, selectedColorInt)
                        if (success != null) {
                            processAlarmForEvent(context, localEventToEdit!!.id, hasAlarm, alarmTime, startDate, title, isAllDay, selectedCalendar?.id, repetitionRule, repeatUntilDate)
                            onSave()
                        }
                    }
                }
            }
        )
    }

    saveError?.let { error ->
        val (errorTitle, errorText, showSettingsButton) = when (error) {
            SaveEventError.NO_PERMISSION -> Triple(stringResource(R.string.permission_denied_title), stringResource(R.string.permission_denied_text), true)
            SaveEventError.NO_CALENDAR_SELECTED -> Triple(stringResource(R.string.error), stringResource(R.string.no_calendar_selected_error), false)
            SaveEventError.TITLE_EMPTY -> Triple(stringResource(R.string.error), stringResource(R.string.title_empty_error), false)
            SaveEventError.END_BEFORE_START -> Triple(stringResource(R.string.error), stringResource(R.string.end_time_before_start_time_error), false)
            SaveEventError.LANES_FULL -> {
                val dateStr = conflictingDate?.format(AppFormats.DateFull) ?: ""
                Triple(stringResource(R.string.error), stringResource(R.string.lanes_full_error, dateStr), false)
            }
        }
        AlertDialog(
            onDismissRequest = { saveError = null }, 
            containerColor = CalendarioTheme.colors.fondoDialogos, 
            titleContentColor = CalendarioTheme.colors.textSystem, 
            textContentColor = CalendarioTheme.colors.textSystem, 
            title = { Text(errorTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
            text = { Text(errorText) },
            confirmButton = { 
                DialogConfirmButton(
                    text = stringResource(R.string.accept),
                    onClick = { saveError = null }
                )
            },
            dismissButton = { 
                if (showSettingsButton) { 
                    DialogDismissButton(text = stringResource(R.string.go_to_settings)) { 
                        saveError = null
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) 
                    } 
                } 
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onDismissRequest = { showDeleteDialog = false }, 
            onConfirm = { 
                showDeleteDialog = false
                localEventToEdit?.let { deleteEvent(context, it) }
                onDelete() 
            }, 
            title = title,
            icon = if (localEventToEdit?.isGhost == true) {
                {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ghost_24),
                        contentDescription = null,
                        tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else null
        )
    }

    if (showDeleteRecurringDialog) {
        DeleteRecurringEventDialog(onDismissRequest = { showDeleteRecurringDialog = false }, onConfirm = { option ->
            showDeleteRecurringDialog = false
            localEventToEdit?.let { event ->
                when (option) {
                    DeleteRecurringOption.SINGLE_EVENT -> { cancelEventInstance(context, event); onDelete() }
                    DeleteRecurringOption.ALL_EVENTS -> { deleteEvent(context, event); onDelete() }
                }
            }
        })
    }

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false }, 
            containerColor = CalendarioTheme.colors.fondoDialogos, 
            titleContentColor = CalendarioTheme.colors.textSystem, 
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(R.string.discard_changes_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
            text = { Text(stringResource(R.string.discard_changes_confirmation)) },
            confirmButton = { 
                DialogConfirmButton(
                    text = stringResource(id = R.string.discard),
                    onClick = { showDiscardChangesDialog = false; onBackPress() },
                    color = Color.Red
                )
            },
            dismissButton = { DialogDismissButton(onDismiss = { showDiscardChangesDialog = false }) }
        )
    }

    if (showStartDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showStartDatePickerDialog = false },
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(R.string.accept),
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            val duration = Duration.between(startDate, endDate)
                            startDate = LocalDateTime.of(newLocalDate, startDate.toLocalTime())
                            endDate = startDate.plus(duration)
                        }
                        showStartDatePickerDialog = false
                    }
                )
            },
            dismissButton = { DialogDismissButton(onDismiss = { showStartDatePickerDialog = false }) },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, headlineContentColor = CalendarioTheme.colors.textSystem, weekdayContentColor = CalendarioTheme.colors.textSystem, dayContentColor = CalendarioTheme.colors.textSystem, selectedDayContentColor = CalendarioTheme.colors.cabecera.getContrastColor(CalendarioTheme.colors.fondoDialogos), selectedDayContainerColor = CalendarioTheme.colors.cabecera, todayContentColor = CalendarioTheme.colors.cabecera, todayDateBorderColor = CalendarioTheme.colors.cabecera))
        }
    }

    if (showEndDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        })
        DatePickerDialog(onDismissRequest = { showEndDatePickerDialog = false },
            confirmButton = { 
                DialogConfirmButton(
                    text = stringResource(R.string.accept),
                    onClick = { 
                        datePickerState.selectedDateMillis?.let { 
                            val selectedLocalDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                            
                            // AUTO-DETECCIÓN DE PERIODO (v3.1.34):
                            // Si el usuario elige la misma fecha, deja de ser periodo largo automáticamente
                            if (isLongPeriod && (selectedLocalDate == startDate.toLocalDate())) {
                                isLongPeriod = false
                                // Forzamos margen de seguridad de 30 min por si desactiva 'Todo el día'
                                endDate = startDate.plusMinutes(30)
                            } else {
                                endDate = LocalDateTime.of(selectedLocalDate, endDate.toLocalTime())
                            }
                        }
                        showEndDatePickerDialog = false 
                    }
                )
            },
            dismissButton = { DialogDismissButton(onDismiss = { showEndDatePickerDialog = false }) },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, headlineContentColor = CalendarioTheme.colors.textSystem, weekdayContentColor = CalendarioTheme.colors.textSystem, dayContentColor = CalendarioTheme.colors.textSystem, selectedDayContentColor = CalendarioTheme.colors.cabecera.getContrastColor(CalendarioTheme.colors.fondoDialogos), selectedDayContainerColor = CalendarioTheme.colors.cabecera, todayContentColor = CalendarioTheme.colors.cabecera, todayDateBorderColor = CalendarioTheme.colors.cabecera))
        }
    }

    if (showStartTimePickerDialog) {
        TimePickerDialog(onDismissRequest = { showStartTimePickerDialog = false }, onConfirm = { hour, minute ->
            val oldDuration = Duration.between(startDate, endDate)
            val newTime = LocalTime.of(hour, minute)
            val newStartDate = LocalDateTime.of(startDate.toLocalDate(), newTime)
            
            // LÓGICA DE MOVIMIENTO DE BLOQUE: Al cambiar el inicio, desplazamos el fin para mantener la duración
            startDate = newStartDate
            endDate = newStartDate.plus(oldDuration)
            
            showStartTimePickerDialog = false
        }, initialHour = startDate.hour, initialMinute = startDate.minute)
    }

    if (showEndTimePickerDialog) {
        TimePickerDialog(onDismissRequest = { showEndTimePickerDialog = false }, onConfirm = { hour, minute ->
            val newTime = LocalTime.of(hour, minute)
            
            if (!isLongPeriod) {
                // DIRECCIÓN INTELIGENTE: Si la hora de fin es después de la de inicio -> Mismo día.
                // Si la hora de fin es antes de la de inicio -> Día siguiente (Cruza medianoche).
                endDate = if (newTime.isAfter(startDate.toLocalTime())) {
                    LocalDateTime.of(startDate.toLocalDate(), newTime)
                } else {
                    LocalDateTime.of(startDate.toLocalDate().plusDays(1), newTime)
                }
            } else {
                val newEndDate = LocalDateTime.of(endDate.toLocalDate(), newTime)
                if (newEndDate.isAfter(startDate)) {
                    endDate = newEndDate
                } else {
                    context.showToast(R.string.end_time_before_start_time_error)
                }
            }
            showEndTimePickerDialog = false
        }, initialHour = endDate.hour, initialMinute = endDate.minute)
    }

    if (showRepeatUntilDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = (repeatUntilDate ?: endDate.toLocalDate()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        })
        DatePickerDialog(onDismissRequest = { showRepeatUntilDatePickerDialog = false },
            confirmButton = { 
                DialogConfirmButton(
                    text = stringResource(id = R.string.accept),
                    onClick = { 
                        datePickerState.selectedDateMillis?.let { 
                            repeatUntilDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() 
                        }
                        showRepeatUntilDatePickerDialog = false 
                    }
                )
            },
            dismissButton = { 
                Row {
                    DialogDismissButton(text = stringResource(id = R.string.repeat_indefinite)) { 
                        repeatUntilDate = null
                        showRepeatUntilDatePickerDialog = false 
                    }
                    DialogDismissButton(onDismiss = { showRepeatUntilDatePickerDialog = false })
                }
            },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, headlineContentColor = CalendarioTheme.colors.textSystem, weekdayContentColor = CalendarioTheme.colors.textSystem, dayContentColor = CalendarioTheme.colors.textSystem, selectedDayContentColor = CalendarioTheme.colors.cabecera.getContrastColor(CalendarioTheme.colors.fondoDialogos), selectedDayContainerColor = CalendarioTheme.colors.cabecera, todayContentColor = CalendarioTheme.colors.cabecera, todayDateBorderColor = CalendarioTheme.colors.cabecera))
        }
    }

    if (showRepetitionDialog) {
        RepetitionSelectionDialog(
            currentRule = repetitionRule,
            currentUntil = repeatUntilDate,
            currentCount = repeatCount,
            onConfirm = { rule, until, count ->
                repetitionRule = rule
                repeatUntilDate = until
                repeatCount = count
                showRepetitionDialog = false
            },
            onDismissRequest = { showRepetitionDialog = false }
        )
    }

    if (showAlarmTimePickerDialog) {
        TimePickerDialog(onDismissRequest = { showAlarmTimePickerDialog = false }, onConfirm = { hour, minute ->
            alarmTime = LocalTime.of(hour, minute)
            hasAlarm = true // Aseguramos que la alarma se active al confirmar la hora
            showAlarmTimePickerDialog = false
        }, initialHour = alarmTime.hour, initialMinute = alarmTime.minute)
    }

    if (showCustomColorPicker) {
        AdvancedColorPickerDialog(
            initialColor = if (selectedColorInt != null) Color(selectedColorInt!!) else Color.Blue,
            onDismissRequest = { showCustomColorPicker = false },
            onColorConfirm = { color ->
                selectedColorInt = color.toArgb()
                showCustomColorPicker = false
            }
        )
    }
}
