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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    val initialEventCount = remember { prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    var eventCountSliderValue by remember { mutableFloatStateOf(initialEventCount.toFloat()) }
    val initialUseLargeFont = remember { prefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    var useLargeFontSwitchState by remember { mutableStateOf(initialUseLargeFont) }

    var eventColor by remember { mutableStateOf(Color(prefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB))) }
    var todayEventColor by remember { mutableStateOf(Color(prefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB))) }
    var showEventColorPalette by remember { mutableStateOf(false) }
    var showTodayEventColorPalette by remember { mutableStateOf(false) }

    val baseEventColors = remember {
        listOf(
            Color(0xFFFFFFFF), Color(0xFFF4F4F4), Color(0xFFD4D4D4), Color(0xFFB4B4B4),
            Color(0xFF949494), Color(0xFF5F5F5F), Color(0xFF000000)
        )
    }
    val baseTodayEventColors = remember {
        listOf(
            Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF80FF80), Color(0xFF00FFFF),
            Color(0xFF952BFF), Color(0xFFFFFFFF), Color(0xFF000000)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Configuración del Widget", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Número de eventos: ${eventCountSliderValue.roundToInt()}", fontSize = 16.sp)
                Slider(
                    value = eventCountSliderValue, onValueChange = { eventCountSliderValue = it },
                    valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                    )
                )
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useLargeFontSwitchState = !useLargeFontSwitchState }
                    .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Letra grande", fontSize = 16.sp)
                    Switch(
                        checked = useLargeFontSwitchState, onCheckedChange = { useLargeFontSwitchState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ColorPickerRow("Color eventos", eventColor, MaterialTheme.colorScheme.onSurfaceVariant) { showEventColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow("Color eventos de hoy", todayEventColor, MaterialTheme.colorScheme.onSurfaceVariant) { showTodayEventColorPalette = true }
            }
        },
        confirmButton = {
            Button(onClick = {
                prefs.edit {
                    putInt(WidgetConstants.KEY_EVENT_COUNT, eventCountSliderValue.roundToInt())
                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, useLargeFontSwitchState)
                    putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, eventColor.toArgb())
                    putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, todayEventColor.toArgb())
                    apply()
                }
                notifyCalendarWidgetsConfigurationChangedMainActivity(context)
                onDismissRequest()
            }) { Text("Guardar", fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
    )

    if (showEventColorPalette) {
        ColorPaletteDialog(
            title = "Color para eventos", colors = baseEventColors, currentlySelectedColor = eventColor,
            onColorSelected = { selectedColor -> eventColor = selectedColor; showEventColorPalette = false },
            onDismiss = { showEventColorPalette = false }
        )
    }
    if (showTodayEventColorPalette) {
        ColorPaletteDialog(
            title = "Color para eventos de hoy", colors = baseTodayEventColors, currentlySelectedColor = todayEventColor,
            onColorSelected = { selectedColor -> todayEventColor = selectedColor; showTodayEventColorPalette = false },
            onDismiss = { showTodayEventColorPalette = false }
        )
    }
}

@Composable
fun ColorPickerRow(label: String, currentColor: Color, textColor: Color, onColorBoxClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f), color = textColor)
        Box(modifier = Modifier
            .size(32.dp)
            .background(currentColor, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onColorBoxClick))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteDialog(
    title: String, colors: List<Color>, currentlySelectedColor: Color,
    onColorSelected: (Color) -> Unit, onDismiss: () -> Unit
) {
    val selectedItemBorderColor = Color.Red

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyRow(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                items(colors) { colorInPalette ->
                    val isSelected = colorInPalette == currentlySelectedColor
                    Box(modifier = Modifier
                        .size(40.dp)
                        .background(colorInPalette, CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) selectedItemBorderColor else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            ),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorInPalette) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
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
    var localAvailableCalendars by remember(availableCalendars) { mutableStateOf(availableCalendars) }
    var currentSelectedIdsInDialog by remember(initialSelectedIds, availableCalendars) {
        mutableStateOf(initialSelectedIds.filter { id -> availableCalendars.any { cal -> cal.id == id } }.toSet())
    }

    LaunchedEffect(availableCalendars, initialSelectedIds) {
        localAvailableCalendars = availableCalendars
        currentSelectedIdsInDialog = initialSelectedIds.filter { id -> availableCalendars.any { cal -> cal.id == id } }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                "Seleccionar Calendarios",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        text = {
            if (localAvailableCalendars.isEmpty()) {
                Text(
                    "No se encontraron calendarios disponibles.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .fillMaxWidth()
                ) {
                    items(localAvailableCalendars, key = { it.id }) { calendar ->
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
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    calendar.displayName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    calendar.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                enabled = localAvailableCalendars.isNotEmpty()
            ) {
                Text("Aplicar", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", fontSize = 16.sp)
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
    isDarkTheme: Boolean,
    onDismissRequest: () -> Unit,
    onAddEventClick: (LocalDate) -> Unit,
    onEventClick: (Festivo) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("E, dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formattedDate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                val desc = if (!festivo.isAllDay && festivo.startTime != null) {
                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty { "(Sin título)" }}"
                } else {
                    festivo.description.ifEmpty { if (festivo.isAllDay) "(Todo el día)" else "" }
                }
                if (desc.isNotBlank()) festivo to desc else null
            }

            if (eventsToDisplay.isEmpty()) {
                Text(
                    "No hay eventos con detalle.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(
                        items = eventsToDisplay,
                        key = { (festivo, _) -> festivo.calendarId.toString() + festivo.description + festivo.startTime.toString() + festivo.date.toString() }
                    ) { (festivo, displayDesc) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onEventClick(festivo) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val itemColor = when {
                                festivo.description.contains("cumpleaños", true) || festivo.description.contains("aniversario", true) ->
                                    if (isDarkTheme) AppThemeSetup.DarkColors.dialogEventBirthdayText else AppThemeSetup.LightColors.dialogEventBirthdayText
                                festivo.isFromHolidaySource ->
                                    if (isDarkTheme) AppThemeSetup.DarkColors.dialogEventHolidayText else AppThemeSetup.LightColors.dialogEventHolidayText
                                else ->
                                    if (isDarkTheme) AppThemeSetup.DarkColors.dialogEventDefaultText else AppThemeSetup.LightColors.dialogEventDefaultText
                            }

                            availableCalendars.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(Color(colorInt), CircleShape)
                                        .border(
                                            0.5.dp,
                                            if (isDarkTheme) AppThemeSetup.DarkColors.dialogCalendarColorIndicatorBorder else AppThemeSetup.LightColors.dialogCalendarColorIndicatorBorder,
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                displayDesc,
                                color = itemColor,
                                fontSize = 16.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar", fontSize = 16.sp)
            }
        }
    )
}