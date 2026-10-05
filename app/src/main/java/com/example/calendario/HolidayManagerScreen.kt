package com.example.calendario

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

    AppScreen(
        title = stringResource(id = R.string.holiday_manager_title),
        onBackClick = onBackPress,
        actions = {
            if (editingAdjustment != null || isFromExistingGoogleEvent) {
                IconButton(onClick = { resetForm() }) { Icon(Icons.Default.Close, contentDescription = "Cancelar edición") }
            }
            if (title.isNotBlank() && hasChanges) {
                IconButton(onClick = saveAction) { Icon(Icons.Default.Check, stringResource(id = R.string.save)) }
            }
        },
        scrollable = false 
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            // --- Editor Section ---
            Column(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (!isFromExistingGoogleEvent) title = it },
                    label = { Text(stringResource(id = R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = isFromExistingGoogleEvent,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        if (title.isNotBlank() && !isFromExistingGoogleEvent) {
                            IconButton(onClick = { title = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isFromExistingGoogleEvent) Color.Transparent else CalendarioTheme.colors.cabecera,
                        unfocusedBorderColor = if (isFromExistingGoogleEvent) Color.Transparent else CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                        focusedTextColor = if (isFromExistingGoogleEvent) CalendarioTheme.colors.textSystem.copy(alpha = 0.5f) else CalendarioTheme.colors.textSystem,
                        unfocusedTextColor = if (isFromExistingGoogleEvent) CalendarioTheme.colors.textSystem.copy(alpha = 0.5f) else CalendarioTheme.colors.textSystem,
                        disabledTextColor = if (isFromExistingGoogleEvent) CalendarioTheme.colors.textSystem.copy(alpha = 0.5f) else CalendarioTheme.colors.textSystem
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isFromExistingGoogleEvent) { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(AppFormats.DateFull),
                        modifier = Modifier.weight(1f),
                        color = if (isFromExistingGoogleEvent) Color.Gray else CalendarioTheme.colors.textSystem,
                        fontSize = 18.sp
                    )
                }
                
                if (isFromExistingGoogleEvent) {
                    Text(text = stringResource(id = R.string.read_only), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = if (isHoliday) R.string.festivo else R.string.laborable), modifier = Modifier.weight(1f), color = if (isHoliday) festivoColor else CalendarioTheme.colors.textSystem, fontWeight = FontWeight.Bold)
                    Switch(checked = isHoliday, onCheckedChange = { isHoliday = it })
                }
            }

            // --- SECCIÃ“N LISTADO (Sincronizada v3.2.12.6) ---
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    text = stringResource(id = R.string.local_holidays_label), 
                    modifier = Modifier.weight(1f), 
                    topPadding = 0.dp
                )
                
                IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.size(32.dp).offset(y = (-8).dp)) {
                    Icon(painter = painterResource(id = R.drawable.ic_folder_open_custom), contentDescription = stringResource(id = R.string.cargar_label), tint = CalendarioTheme.colors.cabecera, modifier = Modifier.size(22.dp))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { 
                        CvoHelper.shareHolidaysPackage(context, adjustments)
                    }, 
                    modifier = Modifier.size(32.dp).offset(y = (-8).dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = stringResource(id = R.string.guardar_label), tint = CalendarioTheme.colors.cabecera)
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)
            ) {
                items(adjustments.sortedByDescending { it.date }) { adj ->
                    val isGoogleAdjustment = adj.originalEventId != null && adj.originalEventId >= 0L
                    HolidayAdjustmentItem(
                        adjustment = adj,
                        onDelete = { adjustmentToDelete = adj },
                        onClick = {
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
                        },
                        festivoColor = festivoColor
                    )
                    HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
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
}

@Composable
fun HolidayAdjustmentItem(
    adjustment: HolidayAdjustment,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    festivoColor: Color
) {
    val isGoogle = adjustment.originalEventId != null && adjustment.originalEventId >= 0L
    val isPastYear = adjustment.date.year < LocalDate.now().year
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { AppFormats.dayDateFull(locale) }
    val formattedDate = remember(adjustment.date, locale) { 
        adjustment.date.format(dateFormatter).replaceFirstChar { it.uppercase(locale) } 
    }
    
    val typeLabel = stringResource(id = if (adjustment.type == HolidayAdjustmentType.HOLIDAY) R.string.festivo else R.string.laborable)

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = adjustment.title,
                fontSize = 16.sp,
                color = when {
                    isPastYear -> CalendarioTheme.colors.textSystem.copy(alpha = 0.4f)
                    adjustment.type == HolidayAdjustmentType.HOLIDAY -> festivoColor
                    else -> CalendarioTheme.colors.textSystem
                },
                fontWeight = if (isPastYear) FontWeight.Normal else FontWeight.Medium,
                fontStyle = if (isPastYear) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$formattedDate • $typeLabel",
                fontSize = 13.sp,
                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!isGoogle) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
