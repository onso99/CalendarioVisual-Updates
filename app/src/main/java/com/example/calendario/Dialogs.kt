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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.blendWithBackground
import com.example.calendario.ui.theme.isColorDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val onFondoDialogos = if(isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

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
    val onFondoDialogos = if(isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

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
    val onFondoDialogos = if(isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

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
                    Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = "Año anterior", modifier = Modifier.size(36.dp))
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
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = "Año siguiente", modifier = Modifier.size(36.dp))
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
    onDismiss: () -> Unit,
    containerColor: Color,
    onContainerColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        title = { Text("Seleccionar Modo", fontWeight = FontWeight.Bold, color = onContainerColor) },
        text = {
            Column {
                ThemeSetting.values().forEach { theme ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onThemeSelected(theme); onDismiss() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = theme.name.lowercase().replaceFirstChar { it.titlecase() }, 
                            modifier = Modifier.weight(1f),
                            color = onContainerColor,
                            fontSize = 16.sp
                        )
                        if (theme == currentTheme) {
                            Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = onContainerColor)
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = onDismiss,
                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { 
                Text("Cancelar")
            }
        }
    )
}