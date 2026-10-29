package com.example.calendario

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HolidayManagerScreen(
    onBackPress: () -> Unit,
    onRefresh: () -> Unit,
    initialFestivo: Festivo? = null,
    viewModel: CalendarioViewModel
) {
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    var adjustments by remember { mutableStateOf(SettingsManager.getHolidayAdjustments(context)) }
    
    LaunchedEffect(uiState.workingDayDates) {
        adjustments = SettingsManager.getHolidayAdjustments(context)
    }
    
    var editingAdjustment by remember { mutableStateOf<HolidayAdjustment?>(null) }
    var currentOriginalEventId by remember { mutableStateOf(initialFestivo?.id) }
    var title by remember { mutableStateOf(initialFestivo?.title ?: "") }
    var date by remember { mutableStateOf(initialFestivo?.date ?: LocalDate.now()) }
    var isHoliday by remember { mutableStateOf(initialFestivo?.isFromHolidaySource ?: true) }
    
    val isFromExistingGoogleEvent = (currentOriginalEventId != null) && (currentOriginalEventId!! >= 0L)

    var showDatePicker by remember { mutableStateOf(value = false) }
    var adjustmentToDelete by remember { mutableStateOf<HolidayAdjustment?>(null) }
    var showAgendaContextWarning by remember { mutableStateOf(false) } 
    var selectedAdjustments by remember { mutableStateOf(setOf<HolidayAdjustment>()) }
    var itemsToExport by remember { mutableStateOf(emptyList<HolidayAdjustment>()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    val exportCvoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            CvoHelper.exportHolidaysPackageToUri(context, it, itemsToExport)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.processExternalCvo(it, autoShowAgendaPreview = false) { success, error, isAgenda ->
                if (success) {
                    if (isAgenda) {
                        showAgendaContextWarning = true
                    }
                } else if (error != null) {
                    context.showToast(error)
                }
            }
        }
    }
    
    var refTitle by remember { mutableStateOf(initialFestivo?.title ?: "") }
    var refDate by remember { mutableStateOf(initialFestivo?.date ?: LocalDate.now()) }
    var refIsHoliday by remember { mutableStateOf(initialFestivo?.isFromHolidaySource ?: true) }

    val hasChanges by remember(title, date, isHoliday, refTitle, refDate, refIsHoliday) {
        derivedStateOf {
            (title != refTitle) || (date != refDate) || (isHoliday != refIsHoliday)
        }
    }

    val resetForm = {
        title = ""
        date = LocalDate.now()
        isHoliday = true
        currentOriginalEventId = null
        editingAdjustment = null
        refTitle = ""
        refDate = LocalDate.now()
        refIsHoliday = true
    }

    val saveAction = {
        val currentAdjustments = SettingsManager.getHolidayAdjustments(context).toMutableList()
        currentAdjustments.removeAll { it.date == date }
        
        val shouldAdd = !isFromExistingGoogleEvent || !isHoliday
        if (shouldAdd) {
            currentAdjustments.add(
                HolidayAdjustment(
                    date = date,
                    type = if (isHoliday) HolidayAdjustmentType.HOLIDAY else HolidayAdjustmentType.WORKING_DAY,
                    title = title,
                    originalEventId = if (isFromExistingGoogleEvent) currentOriginalEventId else null
                )
            )
        }
        
        SettingsManager.saveHolidayAdjustments(context, currentAdjustments)
        adjustments = currentAdjustments
        resetForm()
        context.showToast(R.string.holiday_updated_successfully)
        onRefresh() 
    }

    val festivoColor = if (CalendarioTheme.colors.textSundayHoliday == Color(0xFFFF0000)) Color(0xFFD32F2F) else CalendarioTheme.colors.textSundayHoliday
    val isSelectionMode = selectedAdjustments.isNotEmpty()
    val allSelected = selectedAdjustments.size == adjustments.size && adjustments.isNotEmpty()

    AppScreen(
        title = stringResource(id = R.string.holiday_manager_title),
        onBackClick = onBackPress,
        selectionCount = selectedAdjustments.size,
        onClearSelection = { selectedAdjustments = emptySet() },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = {
                    selectedAdjustments = if (allSelected) emptySet() else adjustments.toSet()
                }) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.LibraryAddCheck else Icons.Default.SelectAll,
                        contentDescription = "Seleccionar todo",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    CvoHelper.shareHolidaysPackage(context, selectedAdjustments.toList())
                }) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = "Compartir", tint = Color.White)
                }
                val defaultExportFilename = stringResource(id = R.string.export_filename_local_holidays)
                IconButton(onClick = {
                    itemsToExport = selectedAdjustments.toList()
                    exportCvoLauncher.launch("$defaultExportFilename.cvo")
                }) {
                    Icon(painter = painterResource(id = R.drawable.ic_save_outlined), contentDescription = "Guardar", tint = Color.White)
                }
                IconButton(onClick = { showDeleteSelectedDialog = true }) {
                    Icon(painter = painterResource(id = R.drawable.ic_delete_outlined), contentDescription = "Borrar", tint = Color.White)
                }
            } else {
                if (!hasChanges) {
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(painter = painterResource(id = R.drawable.ic_folder_open_custom), contentDescription = stringResource(id = R.string.cargar_label))
                    }
                }
                if (hasChanges && (editingAdjustment != null || isFromExistingGoogleEvent)) {
                    IconButton(onClick = { resetForm() }) { Icon(Icons.Default.Close, contentDescription = "Cancelar edición") }
                }
                if (title.isNotBlank() && hasChanges) {
                    IconButton(onClick = saveAction) { Icon(Icons.Default.Check, stringResource(id = R.string.save)) }
                }
            }
        },
        scrollable = false 
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            // --- Editor Section (Estilo Tarjeta Unificada AddEventForm) ---
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                TextField(
                    value = title,
                    onValueChange = { if (!isFromExistingGoogleEvent) title = it },
                    placeholder = { Text(stringResource(id = R.string.title), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = isFromExistingGoogleEvent,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedTextColor = if (isFromExistingGoogleEvent) CalendarioTheme.colors.textSystem.copy(alpha = 0.5f) else CalendarioTheme.colors.textSystem,
                        unfocusedTextColor = if (isFromExistingGoogleEvent) CalendarioTheme.colors.textSystem.copy(alpha = 0.5f) else CalendarioTheme.colors.textSystem
                    ),
                    trailingIcon = {
                        if (title.isNotBlank() && !isFromExistingGoogleEvent) {
                            IconButton(onClick = { title = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                            }
                        }
                    }
                )

                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isFromExistingGoogleEvent) { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val locale = LocalConfiguration.current.locales[0]
                    val dateFormatter = remember(locale) { AppFormats.dayDateAbbr(locale) }
                    val dateText = remember(date, locale) {
                        date.format(dateFormatter).replaceFirstChar { it.uppercase(locale) }
                    }
                    Text(
                        text = dateText,
                        modifier = Modifier.weight(1f),
                        color = if (isFromExistingGoogleEvent) Color.Gray else CalendarioTheme.colors.textSystem,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                
                if (isFromExistingGoogleEvent) {
                    Text(text = stringResource(id = R.string.read_only), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
                }

                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = if (isHoliday) R.string.festivo else R.string.laborable), 
                        modifier = Modifier.weight(1f), 
                        color = if (isHoliday) festivoColor else CalendarioTheme.colors.textSystem, 
                        fontWeight = FontWeight.Bold
                    )
                    Switch(checked = isHoliday, onCheckedChange = { isHoliday = it })
                }
            }

            // --- SECCIÓN LISTADO ---
            SectionTitle(
                text = stringResource(id = R.string.local_holidays_label), 
                modifier = Modifier.fillMaxWidth()
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(adjustments.sortedByDescending { it.date }) { adj ->
                    val isGoogleAdjustment = adj.originalEventId != null
                    val isSelected = selectedAdjustments.contains(adj)
                    val isEditing = editingAdjustment == adj

                    val calendarName = remember(adj, uiState.eventsByDate, uiState.availableCalendars) {
                        if (isGoogleAdjustment) {
                            val matchingEvent = uiState.eventsByDate.values.flatten().find { it.id == adj.originalEventId }
                            val cal = uiState.availableCalendars.find { it.id == matchingEvent?.calendarId }
                            cal?.displayName ?: "Google Calendar"
                        } else {
                            null
                        }
                    }

                    HolidayAdjustmentItem(
                        adjustment = adj,
                        isSelected = isSelected,
                        isEditing = isEditing,
                        calendarName = calendarName,
                        onSelectToggle = {
                            selectedAdjustments = if (isSelected) {
                                selectedAdjustments - adj
                            } else {
                                selectedAdjustments + adj
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedAdjustments = if (isSelected) {
                                    selectedAdjustments - adj
                                } else {
                                    selectedAdjustments + adj
                                }
                            } else {
                                title = adj.title
                                val isPastYear = adj.date.year < LocalDate.now().year
                                if (isPastYear && !isGoogleAdjustment) {
                                    date = adj.date.withYear(LocalDate.now().year)
                                    isHoliday = true
                                    currentOriginalEventId = null
                                    editingAdjustment = null
                                    showDatePicker = true
                                } else {
                                    date = adj.date
                                    isHoliday = adj.type == HolidayAdjustmentType.HOLIDAY
                                    currentOriginalEventId = adj.originalEventId
                                    editingAdjustment = adj
                                }
                                refTitle = title
                                refDate = date
                                refIsHoliday = isHoliday
                            }
                        },
                        festivoColor = festivoColor
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                DialogConfirmButton(text = stringResource(id = R.string.accept), onClick = {
                    datePickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                })
            },
            dismissButton = { DialogDismissButton { showDatePicker = false } },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, headlineContentColor = CalendarioTheme.colors.textSystem, weekdayContentColor = CalendarioTheme.colors.textSystem, dayContentColor = CalendarioTheme.colors.textSystem, selectedDayContentColor = CalendarioTheme.colors.cabecera.getContrastColor(CalendarioTheme.colors.fondoDialogos), selectedDayContainerColor = CalendarioTheme.colors.cabecera, todayContentColor = CalendarioTheme.colors.cabecera, todayDateBorderColor = CalendarioTheme.colors.cabecera))
        }
    }

    if (adjustmentToDelete != null) {
        val toDelete = adjustmentToDelete!!
        val dateStr = toDelete.date.format(AppFormats.DateAbbr)
        val holidayDeletedMsg = stringResource(id = R.string.holiday_deleted_message, dateStr, toDelete.title)

        AppDialog(
            onDismissRequest = { adjustmentToDelete = null },
            title = stringResource(id = R.string.confirm_deletion_title),
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(id = R.string.delete),
                    onClick = {
                        val newList = adjustments.toMutableList()
                        newList.remove(toDelete)
                        SettingsManager.saveHolidayAdjustments(context, newList)
                        adjustments = newList
                        context.showToast(holidayDeletedMsg)
                        onRefresh()
                        resetForm()
                        adjustmentToDelete = null
                    },
                    color = Color.Red
                )
            },
            dismissButton = { DialogDismissButton(onDismiss = { adjustmentToDelete = null }) }
        ) {
            Text(stringResource(id = R.string.confirm_delete_adjustment))
        }
    }

    if (showAgendaContextWarning) {
        AppDialog(
            onDismissRequest = { showAgendaContextWarning = false },
            title = stringResource(R.string.agenda_file_detected_title),
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(R.string.continue_button),
                    onClick = {
                        showAgendaContextWarning = false
                        viewModel.showAgendaImportPreview()
                    }
                )
            },
            dismissButton = {
                DialogDismissButton(
                    text = stringResource(R.string.cancel),
                    onDismiss = { showAgendaContextWarning = false }
                )
            }
        ) {
            Text(stringResource(R.string.agenda_file_detected_message))
        }
    }

    if (showDeleteSelectedDialog && selectedAdjustments.isNotEmpty()) {
        ConfirmDeleteDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            onConfirm = {
                val newList = adjustments.toMutableList()
                newList.removeAll(selectedAdjustments)
                SettingsManager.saveHolidayAdjustments(context, newList)
                adjustments = newList
                selectedAdjustments = emptySet()
                showDeleteSelectedDialog = false
                context.showToast(R.string.holiday_updated_successfully)
                onRefresh()
                resetForm()
            },
            title = if (selectedAdjustments.size == 1) selectedAdjustments.first().title else "${selectedAdjustments.size} festivos"
        )
    }
}

@Composable
fun HolidayAdjustmentItem(
    adjustment: HolidayAdjustment,
    isSelected: Boolean,
    isEditing: Boolean = false,
    calendarName: String? = null,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit,
    festivoColor: Color
) {
    val isPastYear = adjustment.date.year < LocalDate.now().year
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { AppFormats.dayDateAbbr(locale) }
    val formattedDate = remember(adjustment.date, locale) { 
        adjustment.date.format(dateFormatter).replaceFirstChar { it.uppercase(locale) } 
    }
    
    val itemBg = when {
        isSelected -> CalendarioTheme.colors.todayHighlightColor
        isEditing -> CalendarioTheme.colors.fondoSecciones.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    
    val itemBorder = if (isSelected) Modifier.border(1.dp, CalendarioTheme.colors.cabecera, RoundedCornerShape(12.dp)) else Modifier

    val subText = if (!calendarName.isNullOrBlank()) {
        "$formattedDate • $calendarName"
    } else {
        formattedDate
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(itemBg)
            .then(itemBorder)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelectToggle
            )
            .padding(vertical = 6.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((-2).dp)
        ) {
            Text(
                text = adjustment.title,
                fontSize = 16.sp,
                color = when {
                    isPastYear -> CalendarioTheme.colors.textSystem.copy(alpha = 0.4f)
                    adjustment.type == HolidayAdjustmentType.HOLIDAY -> festivoColor
                    else -> CalendarioTheme.colors.textSystem
                },
                fontWeight = if (isSelected || isEditing) FontWeight.Bold else if (isPastYear) FontWeight.Normal else FontWeight.Medium,
                fontStyle = if (isPastYear) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subText,
                fontSize = 12.sp,
                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CalendarioTheme.colors.cabecera,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
