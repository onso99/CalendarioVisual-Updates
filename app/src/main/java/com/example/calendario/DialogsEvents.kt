package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

enum class DeleteRecurringOption { SINGLE_EVENT, ALL_EVENTS }
enum class EditRecurringOption { SINGLE_EVENT, ALL_EVENTS }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEventsDialog(
    date: LocalDate,
    events: List<Festivo>,
    note: DailyNote?,
    onSaveNote: (String) -> Unit,
    onDeleteNote: () -> Unit,
    availableCalendars: List<CalendarInfo>,
    onDismissRequest: () -> Unit,
    onAddEventClick: (LocalDate) -> Unit,
    onEventClick: (Festivo) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val event1Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "")?.trim() ?: "" }
    val event2Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "")?.trim() ?: "" }
    val locale = LocalConfiguration.current.locales[0]

    val formatter = remember { DateTimeFormatter.ofPattern("E, dd/MM/yyyy", locale) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }
    val isToday = date == LocalDate.now()

    var showNoteField by remember(note) { mutableStateOf(note != null) }
    var noteText by remember(note) { mutableStateOf(note?.content ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val charLimit = 140

    // AUTO-GUARDADO: Si el texto cambia, esperamos 800ms de inactividad y guardamos
    LaunchedEffect(noteText) {
        if (noteText != (note?.content ?: "") && noteText.isNotBlank()) {
            kotlinx.coroutines.delay(800.milliseconds)
            onSaveNote(noteText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = {
            Text(
                formattedDate, 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp, 
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        },
        text = {
            Column {
                // --- 1. EDITOR DE NOTA INTEGRADO (ARRIBA) ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = showNoteField,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = noteText,
                            onValueChange = { if (it.length <= charLimit) noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp, color = CalendarioTheme.colors.textSystem),
                            maxLines = 3,
                            decorationBox = { innerTextField ->
                                if (noteText.isEmpty()) {
                                    Text(stringResource(id = R.string.note_hint), fontSize = 13.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
                                }
                                innerTextField()
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (noteText != (note?.content ?: "")) {
                                    IconButton(onClick = { onSaveNote(noteText) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Check, "Guardar", tint = CalendarioTheme.colors.cabecera)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(text = "${noteText.length}/$charLimit", fontSize = 11.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                // --- 2. SECCIÓN DE CHIPS DE ACCIÓN (DIVISOR) ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isChipSelected = showNoteField || note != null
                    ActionChip(
                        label = stringResource(id = R.string.note_label),
                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                        isSelected = isChipSelected,
                        onClick = { showNoteField = !showNoteField },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChip(
                        label = stringResource(id = R.string.evento),
                        icon = Icons.Default.Add,
                        isSelected = false,
                        onClick = { onAddEventClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // --- 3. LISTA DE EVENTOS ---
                if (events.isNotEmpty()) {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(events) { festivo ->
                            val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
                            val esCumpleanos = festivo.isBirthday && !esFestivo
                            val normalizedTitle = festivo.title.unaccent().lowercase()
                            val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
                            val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())
                            
                            val eventSpecificColor = when {
                                esEvento1 -> CalendarioTheme.colors.textEvent1
                                esEvento2 -> CalendarioTheme.colors.textEvent2
                                esFestivo -> CalendarioTheme.colors.textSundayHoliday
                                esCumpleanos -> CalendarioTheme.colors.textBirthday
                                else -> CalendarioTheme.colors.textSystem
                            }
                            
                            val neutralColor = if (isToday) (if (isColorDark(CalendarioTheme.colors.todayHighlightColor, CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black) else CalendarioTheme.colors.textSystem
                            val titleColor = if (isToday) (if (ColorUtils.calculateContrast(ColorUtils.setAlphaComponent(eventSpecificColor.toArgb(), 255), ColorUtils.setAlphaComponent(CalendarioTheme.colors.todayHighlightColor.toArgb(), 255)) > 1.5) eventSpecificColor else neutralColor) else eventSpecificColor
                            val timeText = if (!festivo.isAllDay && festivo.startTime != null) festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm")) else null
                            
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(if (isToday) Modifier.background(CalendarioTheme.colors.todayHighlightColor) else Modifier).clickable { onEventClick(festivo) }.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 6.dp)) {
                                    val calendarForEvent = availableCalendars.find { it.id == festivo.calendarId }
                                    val colorToUse = if (festivo.customColor != null) Color(festivo.customColor) else if (calendarForEvent != null) Color(calendarForEvent.color) else Color.Transparent
                                    if (festivo.isLongPeriod && festivo.lane != null) Box(Modifier.size(6.dp).background(colorToUse, RoundedCornerShape(1.5.dp)))
                                    else Box(Modifier.size(6.dp).background(Color.Gray.copy(alpha = 0.6f), CircleShape).border(0.5.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.4f), CircleShape))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(buildAnnotatedString {
                                    if (timeText != null) withStyle(SpanStyle(color = neutralColor)) { append("$timeText ") }
                                    withStyle(SpanStyle(color = titleColor)) { append(festivo.title.ifEmpty { stringResource(R.string.no_title) } + (if (festivo.age != null) " (${festivo.age})" else "")) }
                                    if (festivo.isLongPeriod) withStyle(SpanStyle(color = titleColor.copy(alpha = 0.8f), fontSize = 14.sp)) { append(" (${festivo.currentDay}/${festivo.totalDays})") }
                                }, fontSize = 16.sp, maxLines = 10, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.accept),
                onClick = onDismissRequest
            )
        }
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            title = { Text(stringResource(id = R.string.delete_note), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text(stringResource(id = R.string.confirm_delete_note)) },
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(id = R.string.delete),
                    onClick = {
                        onDeleteNote()
                        noteText = ""
                        showNoteField = false
                        showDeleteConfirmation = false
                    },
                    color = Color.Red
                )
            },
            dismissButton = { DialogDismissButton { showDeleteConfirmation = false } }
        )
    }
}

@Composable
fun ActionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) CalendarioTheme.colors.cabecera else Color.Transparent
    val contentColor = if (isSelected) (if (isColorDark(backgroundColor, Color.White)) Color.White else Color.Black) else CalendarioTheme.colors.textSystem
    val borderColor = if (isSelected) Color.Transparent else CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label, 
            color = contentColor, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ReadOnlyEventDialog(onDismissRequest: () -> Unit, festivo: Festivo, calendar: CalendarInfo?, onOpenHolidayManager: (Festivo) -> Unit) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("E, dd MMM yyyy") }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text(festivo.title.ifBlank { stringResource(id = R.string.no_title) }, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = CalendarioTheme.colors.textSystem) },
        text = { SelectionContainer { Column {
            Text(festivo.date.format(dateFormatter).replaceFirstChar(Char::titlecase), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.8f))
            if (!festivo.isAllDay) Text("${festivo.startTime?.format(timeFormatter) ?: "--:--"} - ${festivo.endTime?.format(timeFormatter) ?: "--:--"}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.8f))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(id = R.string.calendar_source, calendar?.displayName ?: "-"), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
        }}},
        confirmButton = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (festivo.isFromHolidaySource) {
                    TextButton(onClick = { onDismissRequest(); onOpenHolidayManager(festivo) }, colors = ButtonDefaults.textButtonColors(contentColor = CalendarioTheme.colors.cabecera)) { 
                        Text(stringResource(id = R.string.holiday_manager)) 
                    }
                    Spacer(Modifier.width(8.dp))
                }
                DialogConfirmButton(text = stringResource(id = R.string.accept), onClick = onDismissRequest)
            }
        }
    )
}

@Composable
fun DeleteRecurringEventDialog(onDismissRequest: () -> Unit, onConfirm: (DeleteRecurringOption) -> Unit) {
    var selectedOption by remember { mutableStateOf<DeleteRecurringOption?>(null) }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.delete_recurring_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { Column { listOf(DeleteRecurringOption.SINGLE_EVENT to stringResource(R.string.delete_single_event_option), DeleteRecurringOption.ALL_EVENTS to stringResource(R.string.delete_all_events_option)).forEach { (opt, txt) ->
            Row(modifier = Modifier.fillMaxWidth().clickable { selectedOption = opt }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = (selectedOption == opt), onClick = { selectedOption = opt }, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem))
                Text(txt, Modifier.padding(start = 8.dp))
            }
        }}},
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.delete),
                onClick = { selectedOption?.let(onConfirm) },
                enabled = selectedOption != null,
                color = Color.Red
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@Composable
fun EditRecurringEventDialog(onDismissRequest: () -> Unit, onConfirm: (EditRecurringOption) -> Unit) {
    var selectedOption by remember { mutableStateOf<EditRecurringOption?>(null) }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.edit_recurring_event_dialog_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { Column { listOf(EditRecurringOption.SINGLE_EVENT to stringResource(R.string.edit_recurring_event_dialog_single_event), EditRecurringOption.ALL_EVENTS to stringResource(R.string.edit_recurring_event_dialog_all_events)).forEach { (opt, txt) ->
            Row(modifier = Modifier.fillMaxWidth().clickable { selectedOption = opt }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = (selectedOption == opt), onClick = { selectedOption = opt }, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem))
                Text(txt, Modifier.padding(start = 8.dp))
            }
        }}},
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.accept),
                onClick = { selectedOption?.let(onConfirm) },
                enabled = selectedOption != null
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@Composable
fun SelectCalendarsDialog(initialSelectedIds: Set<Long>, availableCalendars: List<CalendarInfo>, favoriteCalendarId: Long?, onDismissRequest: () -> Unit, onApplySelection: (Set<Long>) -> Unit, onSetFavorite: (Long) -> Unit) {
    var currentIds by remember(initialSelectedIds) { mutableStateOf(initialSelectedIds) }
    
    // Ordenación estratégica: 1. Favorito, 2. Seleccionados (alfabético), 3. No seleccionados (alfabético)
    val sortedCalendars = remember(availableCalendars, initialSelectedIds, favoriteCalendarId) {
        availableCalendars.sortedWith(
            compareByDescending<CalendarInfo> { it.id == favoriteCalendarId }
                .thenByDescending { initialSelectedIds.contains(it.id) }
                .thenBy { it.displayName }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.calendars), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { 
            LazyColumn(Modifier.heightIn(max = 400.dp).fillMaxWidth()) { 
                items(sortedCalendars) { cal ->
                    val isFavorite = cal.id == favoriteCalendarId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isFavorite) { 
                                val set = currentIds.toMutableSet()
                                if (set.contains(cal.id)) set.remove(cal.id) else set.add(cal.id)
                                currentIds = set 
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentIds.contains(cal.id) || isFavorite, 
                            onCheckedChange = null, 
                            enabled = !isFavorite, 
                            colors = CheckboxDefaults.colors(checkedColor = CalendarioTheme.colors.cabecera)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { 
                            Text(cal.displayName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(cal.accountName, fontSize = 12.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)) 
                        }
                        if (cal.canModify) {
                            IconButton(onClick = { 
                                onSetFavorite(cal.id)
                                val set = currentIds.toMutableSet()
                                set.add(cal.id)
                                currentIds = set 
                            }) { 
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, 
                                    contentDescription = null, 
                                    tint = if (isFavorite) CalendarioTheme.colors.cabecera else Color.Gray
                                ) 
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.apply),
                onClick = { onApplySelection(currentIds) }
            ) 
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@Composable
fun SelectCalendarDialog(calendars: List<CalendarInfo>, currentSelection: CalendarInfo?, onCalendarSelected: (CalendarInfo) -> Unit, onDismissRequest: () -> Unit) {
    var tempSelection by remember { mutableStateOf(currentSelection) }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.select_calendar_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { calendars.forEach { cal ->
            Row(modifier = Modifier.fillMaxWidth().clickable { tempSelection = cal }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = (cal.id == tempSelection?.id), onClick = { tempSelection = cal }, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera))
                Text(cal.displayName, Modifier.padding(start = 8.dp))
            }
        }}},
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.accept),
                onClick = { tempSelection?.let(onCalendarSelected); onDismissRequest() }
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepetitionSelectionDialog(
    currentRule: RepetitionRule,
    currentUntil: LocalDate?,
    currentCount: Int?,
    onConfirm: (RepetitionRule, LocalDate?, Int?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var tempSelection by remember { mutableStateOf(currentRule) }
    var tempUntil by remember { mutableStateOf(currentUntil) }
    var tempCount by remember { mutableStateOf(currentCount?.toString() ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var endMode by remember { 
        mutableIntStateOf(if (currentUntil != null) 1 else if (currentCount != null) 2 else 0)
    }

    LaunchedEffect(endMode) {
        if (endMode == 2) focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.repeat_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                RepetitionRule.entries.forEach { rule ->
                    Row(Modifier.fillMaxWidth().clickable { 
                        tempSelection = rule 
                        val limit = if (rule == RepetitionRule.DAILY) 3 else 2
                        if (tempCount.length > limit) tempCount = tempCount.take(limit)
                    }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (rule == tempSelection), onClick = { 
                            tempSelection = rule 
                            val limit = if (rule == RepetitionRule.DAILY) 3 else 2
                            if (tempCount.length > limit) tempCount = tempCount.take(limit)
                        }, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem))
                        Text(stringResource(id = rule.displayNameRes), Modifier.padding(start = 8.dp), fontSize = 16.sp)
                    }
                }

                val isRepetitionActive = tempSelection != RepetitionRule.NONE
                val activeAlpha = if (isRepetitionActive) 1f else 0.4f
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = isRepetitionActive) { endMode = 0 }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (endMode == 0), onClick = { if (isRepetitionActive) endMode = 0 }, enabled = isRepetitionActive, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.repeat_indefinite), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha))
                }

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = isRepetitionActive) { 
                    endMode = 1
                    if (tempUntil == null) tempUntil = LocalDate.now().plusMonths(1)
                    showDatePicker = true 
                }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (endMode == 1), onClick = { if (isRepetitionActive) { endMode = 1; if (tempUntil == null) tempUntil = LocalDate.now().plusMonths(1); showDatePicker = true } }, enabled = isRepetitionActive, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)))
                    Spacer(Modifier.width(8.dp))
                    val textToShow = if (endMode == 1 && tempUntil != null) tempUntil!!.format(DateTimeFormatter.ofPattern("EEEE, d/MM/yyyy", locale)).replaceFirstChar { it.titlecase(locale) } else stringResource(id = R.string.repeat_on_date)
                    Text(textToShow, fontSize = 16.sp, color = if (endMode == 1) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = isRepetitionActive) { endMode = 2 }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (endMode == 2), onClick = { if (isRepetitionActive) endMode = 2 }, enabled = isRepetitionActive, colors = RadioButtonDefaults.colors(selectedColor = CalendarioTheme.colors.cabecera, unselectedColor = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)))
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.repeat_after), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha))
                        Spacer(Modifier.width(4.dp))
                        TextField(value = tempCount, onValueChange = { if (it.all { c -> c.isDigit() }) { val limit = if (tempSelection == RepetitionRule.DAILY) 3 else 2; if (it.length <= limit) tempCount = it } }, modifier = Modifier.width(75.dp).focusRequester(focusRequester), textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center, color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { val finalUntil = if (endMode == 1) tempUntil else null; val finalCount = if (endMode == 2) tempCount.toIntOrNull() else null; onConfirm(tempSelection, finalUntil, finalCount) }), singleLine = true, enabled = (endMode == 2 && isRepetitionActive),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = CalendarioTheme.colors.cabecera, unfocusedIndicatorColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), disabledIndicatorColor = Color.Transparent))
                    }
                }
            }
        },
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.apply),
                onClick = { 
                    val finalUntil = if (endMode == 1) tempUntil else null
                    val finalCount = if (endMode == 2) tempCount.toIntOrNull() else null
                    onConfirm(tempSelection, finalUntil, finalCount) 
                }
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (tempUntil ?: LocalDate.now()).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        tempUntil = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(id = R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(id = R.string.cancel)) }
            },
            colors = DatePickerDefaults.colors(containerColor = CalendarioTheme.colors.fondoDialogos)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
