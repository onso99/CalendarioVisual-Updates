package com.example.calendario

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.blendWithBackground
import com.example.calendario.ui.theme.isColorDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class DeleteRecurringOption {
    SINGLE_EVENT,
    ALL_EVENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteRecurringEventDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (DeleteRecurringOption) -> Unit
) {
    var selectedOption by remember { mutableStateOf<DeleteRecurringOption?>(null) }
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Eliminar evento recurrente", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                val options = listOf(
                    DeleteRecurringOption.SINGLE_EVENT to "Eliminar solo este evento",
                    DeleteRecurringOption.ALL_EVENTS to "Eliminar todos los eventos de la serie"
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
                            onClick = { selectedOption = option }
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
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("ELIMINAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("CANCELAR", color = onFondoDialogos)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCalendarsDialog(
    initialSelectedIds: Set<Long>,
    availableCalendars: List<CalendarInfo>,
    onDismissRequest: () -> Unit,
    onApplySelection: (selectedIds: Set<Long>) -> Unit
) {
    var currentSelectedIdsInDialog by remember(initialSelectedIds, availableCalendars) {
        mutableStateOf(initialSelectedIds.filter { id -> availableCalendars.any { cal -> cal.id == id } }.toSet())
    }
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = {
            Text(
                "Seleccionar Calendarios",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            if (availableCalendars.isEmpty()) {
                Text("No se encontraron calendarios disponibles.", fontSize = 16.sp)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .fillMaxWidth()
                ) {
                    items(availableCalendars, key = { it.id }) { calendar ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    val newSet = currentSelectedIdsInDialog.toMutableSet()
                                    if (newSet.contains(calendar.id)) {
                                        newSet.remove(calendar.id)
                                    } else {
                                        newSet.add(calendar.id)
                                    }
                                    currentSelectedIdsInDialog = newSet
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentSelectedIdsInDialog.contains(calendar.id),
                                onCheckedChange = { isChecked ->
                                    val newSet = currentSelectedIdsInDialog.toMutableSet()
                                    if (isChecked) {
                                        newSet.add(calendar.id)
                                    } else {
                                        newSet.remove(calendar.id)
                                    }
                                    currentSelectedIdsInDialog = newSet
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = onFondoDialogos.copy(alpha = 0.6f),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    calendar.displayName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Text(
                                    calendar.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onFondoDialogos.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Aplicar", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", fontSize = 16.sp, color = onFondoDialogos)
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
    val event1Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "") ?: "" }
    val event2Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "") ?: "" }

    val formatter = remember { DateTimeFormatter.ofPattern("E, dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }
    val isToday = date == LocalDate.now()
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir evento"
                    )
                }
            }
        },
        text = {
            val eventsToDisplay = events.mapNotNull { festivo ->
                val baseTitle = if (!festivo.isAllDay && festivo.startTime != null) {
                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { "(Sin título)" }}"
                } else {
                    festivo.title.ifEmpty { if (festivo.isAllDay) "(Todo el día)" else "" }
                }
                val title = if (festivo.age != null) "$baseTitle (${festivo.age})" else baseTitle
                if (title.isNotBlank()) festivo to title else null
            }

            if (eventsToDisplay.isEmpty()) {
                Text("No hay eventos con detalle.", fontSize = 16.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) { 
                    items(eventsToDisplay, key = { (festivo, _) -> festivo.id.toString() + festivo.title + festivo.startTime.toString() }) { (festivo, displayTitle) ->
                        val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
                        val esCumpleanos = festivo.isBirthday && !esFestivo
                        val normalizedTitle = festivo.title.unaccent().lowercase()
                        val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
                        val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

                        val itemColor = if (isToday) {
                            val highlightColor = CalendarioTheme.colors.todayHighlightColor
                            val backgroundColor = CalendarioTheme.colors.fondoDialogos
                            val finalBlendedColor = blendWithBackground(highlightColor, backgroundColor)
                            if (isColorDark(finalBlendedColor)) Color.White else Color.Black
                        } else {
                            when {
                                esEvento1 -> CalendarioTheme.colors.textEvent1
                                esEvento2 -> CalendarioTheme.colors.textEvent2
                                esFestivo -> CalendarioTheme.colors.textSundayHoliday
                                esCumpleanos -> CalendarioTheme.colors.textBirthday
                                else -> onFondoDialogos
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
                            availableCalendars.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(Color(colorInt), CircleShape)
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                displayTitle,
                                color = itemColor,
                                fontSize = 16.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
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
                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cerrar", fontSize = 16.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoToYearDialog(
    initialYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var year by remember { mutableStateOf(initialYear.toString()) }
    val minYear = 1924
    val maxYear = 2124
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Selección de Año", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { 
                    val currentYear = year.toIntOrNull() ?: initialYear
                    val newYear = (currentYear - 1).coerceIn(minYear, maxYear)
                    year = newYear.toString()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = "Año anterior", modifier = Modifier.size(36.dp), tint = onFondoDialogos)
                }
                OutlinedTextField(
                    value = year,
                    onValueChange = { 
                        val newText = it.filter { char -> char.isDigit() }.take(4)
                        year = newText
                        if (newText.length == 4) {
                            val newYear = newText.toInt().coerceIn(minYear, maxYear)
                            year = newYear.toString()
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp).padding(horizontal = 8.dp),
                    textStyle = TextStyle(textAlign = TextAlign.Center)
                )
                IconButton(onClick = { 
                    val currentYear = year.toIntOrNull() ?: initialYear
                    val newYear = (currentYear + 1).coerceIn(minYear, maxYear)
                    year = newYear.toString()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = "Año siguiente", modifier = Modifier.size(36.dp), tint = onFondoDialogos)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedYear = year.toIntOrNull()?.coerceIn(minYear, maxYear) ?: initialYear
                    onYearSelected(selectedYear)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = onFondoDialogos)
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeSetting, 
    onThemeSelected: (ThemeSetting) -> Unit, 
    onDismiss: () -> Unit
) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Seleccionar Modo", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ThemeSetting.values().forEach { theme ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onThemeSelected(theme); onDismiss() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = theme.displayName, 
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        if (theme == currentTheme) {
                            Icon(Icons.Default.Check, contentDescription = "Seleccionado")
                        }
                    }
                }
            }
        },
        confirmButton = { 
            TextButton(
                onClick = onDismiss,
            ) { 
                Text("Cancelar", color = onFondoDialogos)
            }
        }
    )
}

data class CompatibilityDialogInfo(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

@Composable
fun RestoreDefaultColorsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Restaurar Colores", fontWeight = FontWeight.Bold) },
        text = { Text("¿Estás seguro de que quieres restaurar todos los colores a sus valores por defecto? Las palabras clave no se verán afectadas.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Restaurar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = onFondoDialogos) } }
    )
}

@Composable
fun CompatibilityAlertDialog(
    info: CompatibilityDialogInfo,
    onDismiss: () -> Unit
) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text(info.title, fontWeight = FontWeight.Bold) },
        text = { Text(info.message) },
        confirmButton = {
            Button(
                onClick = {
                    info.onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Continuar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = onFondoDialogos) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String
) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Confirmar eliminación", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = { Text("¿Seguro que quieres eliminar este evento: \"$title\"?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("ELIMINAR") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("CANCELAR", color = onFondoDialogos) } }
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
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Repetir evento", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            Button(
                onClick = { onConfirm(tempSelection) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", color = onFondoDialogos) } }
    )
}

@Composable
fun AboutDialog(onDismissRequest: () -> Unit) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = { 
            Column {
                Text("Calendario Visual V1.7.4", fontSize = 16.sp)
                Text("Asistente IA / Android Studio", fontSize = 16.sp)
                Text("Onso/noviembre 2025", fontSize = 16.sp)
            } 
        },
        confirmButton = { 
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { 
                Text("Cerrar", fontSize = 16.sp) 
            }
        }
    )
}

@Composable
fun SelectCalendarDialog(
    calendars: List<CalendarInfo>,
    currentSelection: CalendarInfo,
    onCalendarSelected: (CalendarInfo) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tempSelection by remember { mutableStateOf(currentSelection) }
    val sortedCalendars = remember(calendars) {
        calendars.sortedByDescending { it.isPrimary }
    }
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Seleccionar Calendario") },
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
                            selected = (calendar.id == tempSelection.id),
                            onClick = { tempSelection = calendar }
                        )
                        Text(text = calendar.displayName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onCalendarSelected(tempSelection)
                onDismissRequest()
            }) {
                Text("Aceptar", color = onFondoDialogos)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = onFondoDialogos)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordColorPickerDialog(
    label: String,
    initialColor: Color,
    initialKeyword: String,
    onDismissRequest: () -> Unit,
    onConfirm: (Color, String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var keyword by remember { mutableStateOf(initialKeyword) }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(label, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Color del evento:")
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(selectedColor, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Palabra clave") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedColor, keyword) }) {
                Text("GUARDAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("CANCELAR")
            }
        }
    )

    if (showColorPicker) {
        AdvancedColorPickerDialog(
            initialColor = selectedColor,
            onDismissRequest = { showColorPicker = false },
            onColorConfirm = { color ->
                selectedColor = color
                showColorPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorConfirm: (Color) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var currentColor by remember(initialColor) { mutableStateOf(initialColor) }
    var isHexError by remember { mutableStateOf(false) }

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(currentColor.toArgb(), hsl)
    val lightness = hsl[2]

    var hexCode by remember(currentColor) {
        mutableStateOf(String.format("#%08X", currentColor.toArgb()))
    }

    fun updateColorFromHex(newHex: String) {
        if (newHex.length == 9 || newHex.length == 7) { // Support ARGB and RGB
            try {
                val colorToParse = if (newHex.length == 7) newHex.replace("#", "#FF") else newHex
                currentColor = Color(colorToParse.toColorInt())
                isHexError = false
            } catch (_: IllegalArgumentException) { 
                isHexError = true
            }
        } else {
            isHexError = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text("Seleccionar Color", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth().height(60.dp).border(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(initialColor))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if(isHexError) initialColor else currentColor))
                }
                Spacer(Modifier.height(16.dp))

                ColorSlider(label = "A", value = currentColor.alpha * 255, onValueChange = { currentColor = currentColor.copy(alpha = it / 255f) })
                ColorSlider(label = "R", value = currentColor.red * 255, onValueChange = { currentColor = currentColor.copy(red = it / 255f) })
                ColorSlider(label = "G", value = currentColor.green * 255, onValueChange = { currentColor = currentColor.copy(green = it / 255f) })
                ColorSlider(label = "B", value = currentColor.blue * 255, onValueChange = { currentColor = currentColor.copy(blue = it / 255f) })
                ColorSlider(label = "L", value = lightness * 100, onValueChange = { newLightnessValue ->
                    ColorUtils.colorToHSL(currentColor.toArgb(), hsl)
                    hsl[2] = newLightnessValue / 100f
                    val newColorInt = ColorUtils.HSLToColor(hsl)
                    currentColor = Color(newColorInt).copy(alpha = currentColor.alpha)
                    isHexError = false
                }, valueRange = 0f..100f)

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hexCode,
                        onValueChange = { 
                            val newHexUncapped = if (it.startsWith("#")) it else "#$it"
                            hexCode = newHexUncapped.take(9)
                            updateColorFromHex(hexCode)
                        },
                        label = { Text("Hex (ARGB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if(!isHexError) onColorConfirm(currentColor) }),
                        modifier = Modifier.weight(1f),
                        isError = isHexError
                    )

                    IconButton(onClick = { hexCode = "#"; isHexError = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar")
                    }

                    IconButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(hexCode))
                        Toast.makeText(context, "Copiado: $hexCode", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar color")
                    }

                    IconButton(onClick = { 
                        clipboardManager.getText()?.text?.let { 
                            val pasted = it.take(9)
                            hexCode = if (pasted.startsWith("#")) pasted else "#$pasted"
                            updateColorFromHex(hexCode)
                        } 
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Pegar color")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if(!isHexError) onColorConfirm(currentColor) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float> = 0f..255f) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.weight(1f))
        Text(value.roundToInt().toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = onFondoDialogos,
        textContentColor = onFondoDialogos,
        title = { Text("Seleccionar hora", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = { TimePicker(state = rememberTimePickerState(), modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}
