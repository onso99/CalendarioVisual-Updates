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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayManagerScreen(
    onBackPress: () -> Unit,
    onRefresh: () -> Unit,
    initialFestivo: Festivo? = null
) {
    val context = LocalContext.current
    var adjustments by remember { mutableStateOf(loadHolidayAdjustments(context)) }
    
    // Internal state for the current edit
    var editingAdjustment by remember { mutableStateOf<HolidayAdjustment?>(null) }
    var currentOriginalEventId by remember { mutableStateOf(initialFestivo?.id) }
    var title by remember { mutableStateOf(initialFestivo?.title ?: "") }
    var date by remember { mutableStateOf(initialFestivo?.date ?: LocalDate.now()) }
    var isHoliday by remember { mutableStateOf(initialFestivo?.isFromHolidaySource ?: true) }
    
    val isFromExistingGoogleEvent = currentOriginalEventId != null && currentOriginalEventId!! >= 0L

    var showDatePicker by remember { mutableStateOf(false) }
    var adjustmentToDelete by remember { mutableStateOf<HolidayAdjustment?>(null) }

    // Logic to store reference values to detect changes
    var refTitle by remember { mutableStateOf(initialFestivo?.title ?: "") }
    var refDate by remember { mutableStateOf(initialFestivo?.date ?: LocalDate.now()) }
    var refIsHoliday by remember { mutableStateOf(initialFestivo?.isFromHolidaySource ?: true) }

    val hasChanges by remember(title, date, isHoliday, refTitle, refDate, refIsHoliday) {
        derivedStateOf {
            title != refTitle || date != refDate || isHoliday != refIsHoliday
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
        
        if (editingAdjustment != null) {
            currentAdjustments.removeAll { it.date == editingAdjustment!!.date && it.originalEventId == editingAdjustment!!.originalEventId && it.title == editingAdjustment!!.title }
        } else if (initialFestivo != null) {
            currentAdjustments.removeAll { 
                (initialFestivo.id >= 0 && it.originalEventId == initialFestivo.id) ||
                (initialFestivo.id < 0 && it.originalEventId == null && it.date == initialFestivo.date && it.title == initialFestivo.title)
            }
        }
        
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
                        focusedTextColor = if (isFromExistingGoogleEvent) Color.Gray else CalendarioTheme.colors.textSystem,
                        unfocusedTextColor = if (isFromExistingGoogleEvent) Color.Gray else CalendarioTheme.colors.textSystem,
                        disabledTextColor = if (isFromExistingGoogleEvent) Color.Gray else CalendarioTheme.colors.textSystem
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
                        color = Color.Gray,
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
                            checkedThumbColor = if (isHoliday) festivoColor else CalendarioTheme.colors.cabecera,
                            checkedTrackColor = (if (isHoliday) festivoColor else CalendarioTheme.colors.cabecera).copy(alpha = 0.54f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- List Section ---
            Text(
                "Festivos locales",
                style = MaterialTheme.typography.titleMedium,
                color = CalendarioTheme.colors.eventListTitleColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
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
                        isClickable = true,
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
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                }) { Text(stringResource(id = R.string.accept)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (adjustmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { adjustmentToDelete = null },
            title = { Text(stringResource(id = R.string.confirm_deletion_title)) },
            text = { Text("¿Deseas eliminar este ajuste? Se restaurará el estado original del calendario.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = adjustmentToDelete
                        if (toDelete != null) {
                            val newList = adjustments.toMutableList()
                            newList.remove(toDelete)
                            saveHolidayAdjustments(context, newList)
                            adjustments = newList
                            
                            // Mostrar Toast con fecha y título
                            val dateStr = toDelete.date.format(DateTimeFormatter.ofPattern("d/M/yy"))
                            val message = context.getString(R.string.holiday_deleted_message, dateStr, toDelete.title)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                            onRefresh()
                            resetForm() // Siempre reseteamos el formulario tras borrar
                        }
                        adjustmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { adjustmentToDelete = null }) { Text(stringResource(id = R.string.cancel)) }
            },
            containerColor = CalendarioTheme.colors.fondoDialogos
        )
    }
}

@Composable
fun HolidayAdjustmentItem(
    adjustment: HolidayAdjustment,
    isClickable: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    festivoColor: Color
) {
    val isGoogle = adjustment.originalEventId != null && adjustment.originalEventId >= 0L
    val isPastYear = adjustment.date.year < LocalDate.now().year
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${adjustment.date.format(DateTimeFormatter.ofPattern("dd/MM/yy"))} ${adjustment.title}",
                fontSize = 14.sp,
                color = when {
                    isGoogle -> Color.Gray
                    isPastYear -> Color.Gray.copy(alpha = 0.6f)
                    adjustment.type == HolidayAdjustmentType.HOLIDAY -> festivoColor
                    else -> CalendarioTheme.colors.textSystem
                },
                fontWeight = if (isPastYear) FontWeight.Normal else FontWeight.Medium,
                fontStyle = if (isPastYear) FontStyle.Italic else FontStyle.Normal
            )
            val sourceLegend = if (isGoogle) stringResource(id = R.string.read_only) else "Gestor de Festivos"
            Text(
                text = "[${stringResource(id = if (adjustment.type == HolidayAdjustmentType.HOLIDAY) R.string.festivo else R.string.laborable)}] - $sourceLegend",
                fontSize = 10.sp,
                color = if (isGoogle || isPastYear || adjustment.type == HolidayAdjustmentType.WORKING_DAY) Color.Gray.copy(alpha = 0.7f) else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
            )
        }
        if (!isGoogle) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (adjustment.type == HolidayAdjustmentType.WORKING_DAY) Color.Gray else festivoColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
