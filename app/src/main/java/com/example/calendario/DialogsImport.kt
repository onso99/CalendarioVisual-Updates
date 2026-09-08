package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaImportPreviewDialog(
    events: List<Festivo>,
    notes: List<DailyNote>,
    availableCalendars: List<CalendarInfo>,
    favoriteCalendarId: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val editableCalendars = remember(availableCalendars) { availableCalendars.filter { it.canModify } }
    var selectedCalendar by remember(favoriteCalendarId) { 
        mutableStateOf(editableCalendars.find { it.id == favoriteCalendarId } ?: editableCalendars.firstOrNull()) 
    }
    var showCalendarDropdown by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(R.string.import_agenda_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.import_summary, events.size, notes.size),
                    fontSize = 14.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de Calendario
                Text(stringResource(R.string.select_target_calendar), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CalendarioTheme.colors.cabecera)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CalendarioTheme.colors.fondoSecciones)
                        .clickable { showCalendarDropdown = true }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = selectedCalendar?.let { Color(it.color) } ?: CalendarioTheme.colors.textSystem, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(selectedCalendar?.displayName ?: "", color = CalendarioTheme.colors.textSystem, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
                    }
                    
                    DropdownMenu(
                        expanded = showCalendarDropdown,
                        onDismissRequest = { showCalendarDropdown = false },
                        modifier = Modifier.background(CalendarioTheme.colors.fondoDialogos)
                    ) {
                        editableCalendars.forEach { cal ->
                            DropdownMenuItem(
                                text = { Text(cal.displayName, color = CalendarioTheme.colors.textSystem) },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = Color(cal.color)) },
                                onClick = {
                                    selectedCalendar = cal
                                    showCalendarDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de elementos
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(events) { event ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (event.isLongPeriod) CalendarioTheme.colors.cabecera else Color.Gray))
                            Spacer(Modifier.width(8.dp))
                            Text(event.title, fontSize = 14.sp, color = CalendarioTheme.colors.textSystem, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(event.date.format(dateFormatter), fontSize = 12.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
                        }
                    }
                    items(notes) { note ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.StickyNote2, null, tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(note.content, fontSize = 14.sp, color = CalendarioTheme.colors.textSystem, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(note.date.format(dateFormatter), fontSize = 12.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.import_button),
                onClick = { selectedCalendar?.let { onConfirm(it.id) } },
                enabled = selectedCalendar != null
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismiss) }
    )
}

@Composable
fun HolidayImportPreviewDialog(
    adjustments: List<HolidayAdjustment>,
    onConfirm: (List<HolidayAdjustment>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedItems by remember { mutableStateOf(adjustments.toSet()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy") }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(R.string.holiday_manager_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.import_summary, adjustments.size, 0).replace("0 notas.", ""),
                    fontSize = 14.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(adjustments) { adj ->
                        val isSelected = selectedItems.contains(adj)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedItems = if (isSelected) selectedItems - adj else selectedItems + adj 
                                }
                                .padding(vertical = 4.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { 
                                    selectedItems = if (it) selectedItems + adj else selectedItems - adj 
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CalendarioTheme.colors.cabecera)
                            )
                            
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(
                                    text = adj.title, 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (adj.type == HolidayAdjustmentType.HOLIDAY) CalendarioTheme.colors.textSundayHoliday else CalendarioTheme.colors.textSystem,
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = adj.date.format(dateFormatter).replaceFirstChar { it.titlecase(locale) }, 
                                    fontSize = 12.sp, 
                                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.import_button),
                onClick = { onConfirm(selectedItems.toList()) },
                enabled = selectedItems.isNotEmpty()
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismiss) }
    )
}
