package com.example.calendario

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayManagerScreen(
    onBackPress: () -> Unit,
    onRefresh: () -> Unit,
    initialFestivo: Festivo? = null,
) {
    val context = LocalContext.current
    var adjustments by remember { mutableStateOf(loadHolidayAdjustments(context)) }
    
    // Internal state for the current edit
    var editingAdjustment by remember { mutableStateOf<HolidayAdjustment?>(null) }
    var currentOriginalEventId by remember { mutableStateOf(initialFestivo?.id) }
    var title by remember { mutableStateOf(initialFestivo?.title ?: "") }
    var date by remember { mutableStateOf(initialFestivo?.date ?: LocalDate.now()) }
    var isHoliday by remember { mutableStateOf(initialFestivo?.isFromHolidaySource ?: true) }
    
    val isFromExistingGoogleEvent = (currentOriginalEventId != null) && (currentOriginalEventId!! >= 0L)

    var showDatePicker by remember { mutableStateOf(value = false) }
    var adjustmentToDelete by remember { mutableStateOf<HolidayAdjustment?>(null) }
    
    // Logic to store reference values to detect changes
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
        val currentAdjustments = loadHolidayAdjustments(context).toMutableList()
        
        // UN DÍA, UN ESTADO: Eliminamos cualquier ajuste previo para esta fecha (manual o de Google)
        currentAdjustments.removeAll { it.date == date }
        
        val shouldAdd = if (isFromExistingGoogleEvent) !isHoliday else true

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
        
        saveHolidayAdjustments(context, currentAdjustments)
        adjustments = currentAdjustments
        resetForm()
        
        Toast.makeText(context, R.string.holiday_updated_successfully, Toast.LENGTH_SHORT).show()
        onRefresh() 
    }

    // Atenuamos el rojo solo para esta pantalla si es el rojo puro del modo claro
    val festivoColor = if (CalendarioTheme.colors.textSundayHoliday == Color(0xFFFF0000)) Color(0xFFD32F2F) else CalendarioTheme.colors.textSundayHoliday

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.holiday_manager_title)) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back)) } },
                actions = {
                    if (editingAdjustment != null || isFromExistingGoogleEvent) {
                        IconButton(onClick = { resetForm() }) { Icon(Icons.Default.Close, contentDescription = "Cancelar edición") }
                    }
                    if (title.isNotBlank() && hasChanges) {
                        IconButton(onClick = saveAction) { Icon(Icons.Default.Check, stringResource(id = R.string.save)) }
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // --- Editor Section ---
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
                    .padding(16.dp)
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
                        text = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        modifier = Modifier.weight(1f),
                        color = if (isFromExistingGoogleEvent) Color.Gray else CalendarioTheme.colors.textSystem,
                        fontSize = 18.sp
                    )
                }
                
                if (isFromExistingGoogleEvent) {
                    Text(
                        text = stringResource(id = R.string.read_only),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(id = if (isHoliday) R.string.festivo else R.string.laborable),
                        modifier = Modifier.weight(1f),
                        color = if (isHoliday) festivoColor else CalendarioTheme.colors.textSystem,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isHoliday,
                        onCheckedChange = { isHoliday = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = festivoColor,
                            checkedTrackColor = festivoColor.copy(alpha = 0.54f),
                            uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                            uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- List Section ---
            SectionTitle(text = stringResource(id = R.string.local_holidays_label))
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
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
                                // Template mode (only for manual holidays)
                                date = adj.date.withYear(LocalDate.now().year)
                                isHoliday = true
                                currentOriginalEventId = null
                                editingAdjustment = null
                                showDatePicker = true
                            } else {
                                // Edit mode (for current year or Google events)
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
            dismissButton = {
                DialogDismissButton { showDatePicker = false }
            },
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

    if (adjustmentToDelete != null) {
        val toDelete = adjustmentToDelete!!
        val dateStr = toDelete.date.format(DateTimeFormatter.ofPattern("d/M/yy"))
        val holidayDeletedMsg = stringResource(id = R.string.holiday_deleted_message, dateStr, toDelete.title)

        AlertDialog(
            onDismissRequest = { adjustmentToDelete = null },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.confirm_deletion_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
            text = { Text(stringResource(id = R.string.confirm_delete_adjustment)) },
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(id = R.string.delete),
                    onClick = {
                        val newList = adjustments.toMutableList()
                        newList.remove(toDelete)
                        saveHolidayAdjustments(context, newList)
                        adjustments = newList
                        
                        Toast.makeText(context, holidayDeletedMsg, Toast.LENGTH_SHORT).show()

                        onRefresh()
                        resetForm()
                        adjustmentToDelete = null
                    },
                    color = Color.Red
                )
            },
            dismissButton = {
                DialogDismissButton(onDismiss = { adjustmentToDelete = null })
            }
        )
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val secondaryTextColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
            Text(
                text = "${adjustment.date.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} ${adjustment.title}",
                fontSize = 14.sp,
                color = when {
                    isGoogle -> secondaryTextColor
                    isPastYear -> secondaryTextColor.copy(alpha = 0.4f)
                    adjustment.type == HolidayAdjustmentType.HOLIDAY -> festivoColor
                    else -> CalendarioTheme.colors.textSystem
                },
                fontWeight = if (isPastYear) FontWeight.Normal else FontWeight.Medium,
                fontStyle = if (isPastYear) FontStyle.Italic else FontStyle.Normal
            )
            val sourceLegend = if (isGoogle) stringResource(id = R.string.read_only) else stringResource(id = R.string.holiday_manager_legend)
            Text(
                text = "[${stringResource(id = if (adjustment.type == HolidayAdjustmentType.HOLIDAY) R.string.festivo else R.string.laborable)}] - $sourceLegend",
                fontSize = 10.sp,
                color = secondaryTextColor
            )
        }
        if (!isGoogle) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red, // Unificado a Rojo para consistencia de acciÃ³n
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
