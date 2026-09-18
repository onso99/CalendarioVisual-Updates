package com.example.calendario

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
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
    val event1Keyword = remember { SettingsManager.getEvent1Keyword(context) }
    val event2Keyword = remember { SettingsManager.getEvent2Keyword(context) }
    val locale = LocalConfiguration.current.locales[0]

    // Formato: Dia dd/mm/aa (ej: Lun 22/05/26)
    val formatter = remember { AppFormats.dayDateAbbr(locale) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }
    val isToday = date == LocalDate.now()

    var showNoteField by remember(note) { mutableStateOf(note != null) }
    var noteText by remember(note) { mutableStateOf(note?.content ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val charLimit = 200

    LaunchedEffect(noteText) {
        if (noteText != (note?.content ?: "") && noteText.isNotBlank()) {
            delay(800.milliseconds)
            onSaveNote(noteText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. TÍTULO (Izquierda) - Ahora tiene todo el espacio
                Text(
                    text = formattedDate, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 20.sp, 
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                // 2. ICONO EVENTO (+) - Se mantiene arriba a la derecha
                val circleColor = CalendarioTheme.colors.cabecera
                val contentColor = circleColor.getContrastColor(Color.White)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .clickable { onAddEventClick(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.new_event),
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        text = {
            Column {
                // --- 1. LISTA DE EVENTOS ---
                if (events.isNotEmpty()) {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(events, key = { it.adn }) { festivo ->
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
                            
                            val neutralColor = if (isToday) CalendarioTheme.colors.todayHighlightColor.getContrastColor(CalendarioTheme.colors.fondoDialogos) else CalendarioTheme.colors.textSystem
                            val titleColor = if (isToday) (if (ColorUtils.calculateContrast(ColorUtils.setAlphaComponent(eventSpecificColor.toArgb(), 255), ColorUtils.setAlphaComponent(CalendarioTheme.colors.todayHighlightColor.toArgb(), 255)) > 1.5) eventSpecificColor else neutralColor) else eventSpecificColor
                            
                            // LÓGICA DE HORA INTELIGENTE (v3.1.34):
                            // Primer día -> Hora Inicio | Último día -> Hora Fin | Resto -> Sin hora (Todo el día)
                            val displayTime = when {
                                festivo.isAllDay -> null
                                festivo.currentDay == 1 -> festivo.startTime
                                festivo.currentDay == festivo.totalDays -> festivo.endTime
                                else -> null
                            }
                            val timeText = displayTime?.format(AppFormats.TimeShort)
                            
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(if (isToday) Modifier.background(CalendarioTheme.colors.todayHighlightColor) else Modifier).clickable { onEventClick(festivo) }.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 8.dp, start = 2.dp)) {
                                    val calendarForEvent = availableCalendars.find { it.id == festivo.calendarId }
                                    val colorToUse = if (festivo.customColor != null) Color(festivo.customColor) else if (calendarForEvent != null) Color(calendarForEvent.color) else Color.Transparent
                                    if (festivo.isLongPeriod && festivo.lane != null) Box(Modifier.size(6.dp).background(colorToUse, RoundedCornerShape(1.5.dp)))
                                    else Box(Modifier.size(6.dp).background(Color.Gray.copy(alpha = 0.6f), CircleShape).border(0.5.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.4f), CircleShape))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(buildAnnotatedString {
                                    if (timeText != null) withStyle(SpanStyle(color = neutralColor)) { append("$timeText ") }
                                    withStyle(SpanStyle(color = titleColor)) { append(festivo.title.ifEmpty { stringResource(R.string.no_title) }) }
                                    if (festivo.age != null) withStyle(SpanStyle(color = titleColor)) { append(" (${festivo.age})") }
                                    if (festivo.isLongPeriod) withStyle(SpanStyle(color = titleColor.copy(alpha = 0.8f), fontSize = 14.sp)) { append(" (${festivo.currentDay}/${festivo.totalDays})") }
                                }, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // ICONO ALARMA (v3.1.64 - Outlined y mayor)
                                    if (AlarmUtils.shouldShowAlarmIcon(context, festivo)) {
                                        Icon(
                                            imageVector = Icons.Outlined.NotificationsActive,
                                            contentDescription = null,
                                            tint = titleColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                        )
                                    }

                                    // MARCADOR DE INCIDENCIA (v3.2.06)
                                    if (festivo.hasIncident) {
                                        Text(
                                            text = " *",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // --- 2. EDITOR DE NOTA INTEGRADO (Debajo de eventos v3.1.64) ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = showNoteField,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = noteText,
                            onValueChange = { if (it.length <= charLimit) noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp, color = CalendarioTheme.colors.textSystem),
                            maxLines = 4,
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
                            IconButton(
                                onClick = { 
                                    if (noteText.isBlank()) showNoteField = false 
                                    else showDeleteConfirmation = true 
                                }, 
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, stringResource(id = R.string.delete), tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                            
                            // Contador de caracteres (v3.1.64)
                            Text(
                                text = "${noteText.length}/$charLimit", 
                                fontSize = 11.sp, 
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ICONO NOTA (Esquina inferior izquierda v3.1.64)
                val hasNote = note != null || noteText.isNotBlank()
                val noteIconAlpha = if (showNoteField || hasNote) 1f else 0.4f
                
                IconButton(
                    onClick = { showNoteField = !showNoteField },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                        contentDescription = stringResource(id = R.string.note_label),
                        tint = CalendarioTheme.colors.cabecera.copy(alpha = noteIconAlpha),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // BOTÓN ACEPTAR (Derecha)
                DialogConfirmButton(
                    text = stringResource(id = R.string.accept),
                    onClick = onDismissRequest
                )
            }
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
            dismissButton = { DialogDismissButton(onDismiss = { showDeleteConfirmation = false }) }
        )
    }
}


@Composable
fun ReadOnlyEventDialog(
    onDismissRequest: () -> Unit, 
    festivo: Festivo, 
    calendar: CalendarInfo?, 
    onOpenHolidayManager: (Festivo) -> Unit,
    onRemoveFromHistory: (Festivo) -> Unit // Nueva acción
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember { AppFormats.TimeShort }
    val dateFormatter = remember { AppFormats.dayDateAbbr(locale) }
    
    // Lógica de color original del evento (v3.1.34)
    val event1Keyword = remember { SettingsManager.getEvent1Keyword(context) }
    val event2Keyword = remember { SettingsManager.getEvent2Keyword(context) }

    val normalizedTitle = festivo.title.unaccent().lowercase()
    val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
    val esCumpleanos = festivo.isBirthday && !esFestivo
    val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
    val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

    val eventColor = when {
        esEvento1 -> CalendarioTheme.colors.textEvent1
        esEvento2 -> CalendarioTheme.colors.textEvent2
        esFestivo -> CalendarioTheme.colors.textSundayHoliday
        esCumpleanos -> CalendarioTheme.colors.textBirthday
        else -> CalendarioTheme.colors.textSystem
    }

    AlertDialog(
        onDismissRequest = onDismissRequest, 
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { 
            // LA FECHA COMO CABECERA ABREVIADA (v3.1.34)
            Text(
                text = festivo.date.format(dateFormatter).replaceFirstChar { it.titlecase(locale) }, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp,
                color = CalendarioTheme.colors.cabecera 
            ) 
        },
        text = { 
            SelectionContainer { 
                Column {
                    // EL TÍTULO DEL EVENTO CON SU COLOR ORIGINAL (v3.1.34: Incluye edad para cumpleaños)
                    val ageText = if (festivo.age != null && festivo.age > 0) " (${festivo.age})" else ""
                    Text(
                        text = (festivo.title.ifBlank { stringResource(id = R.string.no_title) }) + ageText,
                        fontSize = 16.sp,
                        color = eventColor
                    )
                    
                    Spacer(Modifier.height(8.dp))

                    if (!festivo.isAllDay) {
                        Text(
                            text = "${festivo.startTime?.format(timeFormatter) ?: "--:--"} - ${festivo.endTime?.format(timeFormatter) ?: "--:--"}", 
                            fontSize = 14.sp, 
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.all_day_switch),
                            fontSize = 14.sp, 
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (festivo.isGhost || (calendar == null && festivo.calendarId > 0)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ghost_24),
                                contentDescription = null,
                                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp).padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.calendar_source, calendar?.displayName ?: "-"), 
                            fontSize = 14.sp, 
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                        )
                    }
                    if (calendar?.canModify == false) {
                        Text(
                            text = stringResource(id = R.string.calendar_read_only_error),
                            fontSize = 12.sp,
                            color = CalendarioTheme.colors.textSundayHoliday.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (festivo.isFromHolidaySource) {
                    TextButton(onClick = { onDismissRequest(); onOpenHolidayManager(festivo) }, colors = ButtonDefaults.textButtonColors(contentColor = CalendarioTheme.colors.cabecera)) { 
                        Text(stringResource(id = R.string.holiday_manager_title)) 
                    }
                    Spacer(Modifier.width(8.dp))
                }
                DialogConfirmButton(text = stringResource(id = R.string.accept), onClick = onDismissRequest)
            }
        },
        dismissButton = {
            if (festivo.isGhost || (calendar == null && festivo.calendarId > 0)) {
                // Es un evento huérfano o fantasma
                TextButton(
                    onClick = { 
                        onDismissRequest()
                        onRemoveFromHistory(festivo) 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(id = R.string.delete)) // Usamos "Eliminar"
                }
            }
        }
    )
}

@Composable
fun DeleteRecurringEventDialog(onDismissRequest: () -> Unit, onConfirm: (DeleteRecurringOption) -> Unit) {
    var selectedOption by remember { mutableStateOf<DeleteRecurringOption?>(null) }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.delete_recurring_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { 
            Column { 
                listOf(
                    DeleteRecurringOption.SINGLE_EVENT to stringResource(R.string.delete_single_event_option), 
                    DeleteRecurringOption.ALL_EVENTS to stringResource(R.string.delete_all_events_option)
                ).forEach { (opt, txt) ->
                    val isSelected = selectedOption == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = opt }
                            .padding(vertical = 12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = txt, 
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            color = CalendarioTheme.colors.textSystem
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check, 
                                contentDescription = null, 
                                tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos)
                            )
                        }
                    }
                }
            }
        },
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
        text = { 
            Column { 
                listOf(
                    EditRecurringOption.SINGLE_EVENT to stringResource(R.string.edit_recurring_event_dialog_single_event), 
                    EditRecurringOption.ALL_EVENTS to stringResource(R.string.edit_recurring_event_dialog_all_events)
                ).forEach { (opt, txt) ->
                    val isSelected = selectedOption == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = opt }
                            .padding(vertical = 12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = txt, 
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            color = CalendarioTheme.colors.textSystem
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check, 
                                contentDescription = null, 
                                tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos)
                            )
                        }
                    }
                }
            }
        },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectCalendarsDialog(initialSelectedIds: Set<Long>, availableCalendars: List<CalendarInfo>, favoriteCalendarId: Long?, onDismissRequest: () -> Unit, onApplySelection: (Set<Long>) -> Unit, onSetFavorite: (Long) -> Unit) {
    var currentIds by remember(initialSelectedIds) { mutableStateOf(initialSelectedIds) }
    var currentFavoriteId by remember(favoriteCalendarId) { mutableStateOf(favoriteCalendarId) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    
    val hintMsg = stringResource(id = R.string.hint_long_press_favorite)
    val favUpdatedMsg = stringResource(id = R.string.favorite_updated)
    val readOnlyMsg = stringResource(id = R.string.calendar_read_only_error)

    // Inicialización de seguridad del favorito si no existe
    LaunchedEffect(Unit) {
        if (currentFavoriteId == null) {
            findBestCalendarCandidate(availableCalendars)?.let { candidate ->
                onSetFavorite(candidate.id)
                currentFavoriteId = candidate.id
                currentIds = currentIds + candidate.id
            }
        }
        // Mensaje de ayuda inicial
        infoMessage = hintMsg
        delay(5000.milliseconds)
        infoMessage = null
    }

    // Efecto para limpiar mensajes de error tras 5 segundos
    LaunchedEffect(infoMessage) {
        if (infoMessage != null && infoMessage != hintMsg) {
            delay(5000.milliseconds)
            infoMessage = null
        }
    }
    
    val sortedCalendars = remember(availableCalendars, currentIds, currentFavoriteId) {
        availableCalendars.sortedWith(
            compareByDescending<CalendarInfo> { it.id == currentFavoriteId }
                .thenByDescending { currentIds.contains(it.id) }
                .thenBy { it.displayName }
        )
    }

    // Control de transparencia y persistencia del mensaje
    val messageAlpha by animateFloatAsState(
        targetValue = if (infoMessage != null) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    var lastKnownMessage by remember { mutableStateOf("") }
    if (infoMessage != null) lastKnownMessage = infoMessage!!

    AlertDialog(
        onDismissRequest = onDismissRequest, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.calendars), fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Start)
                
                // Mensaje fijo que solo cambia opacidad
                Text(
                    text = lastKnownMessage,
                    fontSize = 12.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f * messageAlpha),
                    modifier = Modifier.offset(y = (-16).dp) // Pegado casi al título
                )
            }
        },
        text = { 
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .fillMaxWidth()
                    .drawBehind { /* Solo para asegurar el renderizado */ }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val offsetPx = 48.dp.roundToPx()
                        // Reportamos un alto menor al real para que los botones de abajo suban
                        layout(placeable.width, placeable.height - offsetPx) {
                            placeable.placeRelative(0, -offsetPx)
                        }
                    }
            ) {
                items(sortedCalendars) { cal ->
                    val isFavorite = cal.id == currentFavoriteId
                    val isSelected = currentIds.contains(cal.id) || isFavorite
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { 
                                    if (!isFavorite) {
                                        val set = currentIds.toMutableSet()
                                        if (set.contains(cal.id)) set.remove(cal.id) else set.add(cal.id)
                                        currentIds = set 
                                    }
                                },
                                onLongClick = {
                                    if (cal.canModify) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSetFavorite(cal.id)
                                        currentFavoriteId = cal.id
                                        currentIds = currentIds + cal.id // El favorito debe estar seleccionado
                                        infoMessage = favUpdatedMsg
                                    } else {
                                        infoMessage = readOnlyMsg
                                    }
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // El lado izquierdo queda limpio sin Checkbox físico
                        Column(Modifier.weight(1f)) { 
                            Text(
                                text = cal.displayName, 
                                fontWeight = if (isFavorite) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = CalendarioTheme.colors.textSystem
                            )
                            Text(
                                text = cal.accountName, 
                                fontSize = 12.sp,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        }
                        
                        // Icono dinámico a la derecha (Mutante)
                        Box(
                            modifier = Modifier.size(32.dp).offset(y = (-2).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = CalendarioTheme.colors.cabecera,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.apply),
                onConfirm = { onApplySelection(currentIds) },
                onDismiss = onDismissRequest
            )
        }
    )
}

@Composable
fun SelectWidgetCalendarsDialog(
    appActiveCalendars: List<CalendarInfo>,
    initialSelectedIds: Set<Long>,
    currentFavoriteId: Long?,
    onApply: (Set<Long>) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentIds by remember { mutableStateOf(initialSelectedIds) }
    var showError by remember { mutableStateOf(false) }

    // Efecto para limpiar el mensaje de error tras 5 segundos, igual que en el diálogo global
    LaunchedEffect(showError) {
        if (showError) {
            delay(5000.milliseconds)
            showError = false
        }
    }

    val sortedCalendars = remember(appActiveCalendars, currentIds, currentFavoriteId) {
        appActiveCalendars.sortedWith(
            compareByDescending<CalendarInfo> { it.id == currentFavoriteId }
                .thenByDescending { currentIds.contains(it.id) }
                .thenBy { it.displayName }
        )
    }

    val errorAlpha by animateFloatAsState(
        targetValue = if (showError) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "errorAlpha"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.calendars),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start
                )
                // Mensaje fijo que solo cambia opacidad para no mover la lista
                Text(
                    text = stringResource(id = R.string.widget_min_calendar_error),
                    fontSize = 12.sp,
                    color = Color.Red.copy(alpha = errorAlpha),
                    modifier = Modifier.offset(y = (-16).dp) // Imitamos el offset del diálogo global
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val offsetPx = 48.dp.roundToPx() 
                        // ASEGURAMOS QUE EL ALTO NUNCA SEA NEGATIVO (Evita el crash Size out of range)
                        val layoutHeight = (placeable.height - offsetPx).coerceAtLeast(0)
                        layout(placeable.width, layoutHeight) {
                            placeable.placeRelative(0, -offsetPx)
                        }
                    }
            ) {
                items(sortedCalendars) { cal ->
                    val isSelected = currentIds.contains(cal.id)
                    val isFavorite = cal.id == currentFavoriteId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isSelected) {
                                    if (currentIds.size > 1) {
                                        currentIds = currentIds - cal.id
                                        showError = false
                                    } else {
                                        showError = true
                                    }
                                } else {
                                    currentIds = currentIds + cal.id
                                    showError = false
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = cal.displayName,
                                // SÓLO el favorito de la App destaca en seminegrita
                                fontWeight = if (isFavorite) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 15.sp,
                                color = CalendarioTheme.colors.textSystem,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = cal.accountName,
                                fontSize = 12.sp,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.apply),
                onConfirm = { onApply(currentIds) },
                onDismiss = onDismissRequest
            )
        }
    )
}

@Composable
fun SelectCalendarDialog(calendars: List<CalendarInfo>, currentSelection: CalendarInfo?, onCalendarSelected: (CalendarInfo) -> Unit, onDismissRequest: () -> Unit) {
    var tempSelection by remember { mutableStateOf(currentSelection) }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.select_calendar_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { calendars.forEach { cal ->
            val isSelected = cal.id == tempSelection?.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tempSelection = cal }
                    .padding(vertical = 12.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cal.displayName, 
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = CalendarioTheme.colors.textSystem,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = null, 
                        tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos)
                    )
                }
            }
        }}},
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.accept),
                onConfirm = {
                    tempSelection?.let { onCalendarSelected(it) }
                    onDismissRequest()
                },
                onDismiss = onDismissRequest
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepetitionSelectionDialog(
    currentRule: RepetitionRule,
    currentUntil: LocalDate?,
    currentCount: Int?,
    currentDays: Set<DayOfWeek>,
    onConfirm: (RepetitionRule, LocalDate?, Int?, Set<DayOfWeek>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var tempSelection by remember { mutableStateOf(currentRule) }
    var tempUntil by remember { mutableStateOf(currentUntil) }
    var tempCount by remember { mutableStateOf(currentCount?.toString() ?: "") }
    var tempDays by remember { mutableStateOf(currentDays) }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var endMode by remember { 
        mutableIntStateOf(
            when {
                currentUntil != null -> 1
                currentCount != null -> 2
                currentRule != RepetitionRule.NONE -> 0 // Existente indefinida (v3.2.08.2)
                else -> 2 // Nuevo evento: Repeticiones por defecto (v3.2.08.1)
            }
        )
    }

    LaunchedEffect(endMode) {
        if (endMode == 2) focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.repeat_event_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = {
            Column {
                RepetitionRule.entries.forEach { rule ->
                    val isSelected = rule == tempSelection
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    tempSelection = rule 
                                    val limit = if (rule == RepetitionRule.DAILY) 3 else 2
                                    if (tempCount.length > limit) tempCount = tempCount.take(limit)
                                }
                                .padding(vertical = 10.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = rule.displayNameRes), 
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                color = CalendarioTheme.colors.textSystem
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check, 
                                    contentDescription = null, 
                                    tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos)
                                )
                            }
                        }
                        
                        // --- Fila de días para Cada semana (v3.2.08) ---
                        if (rule == RepetitionRule.WEEKLY && tempSelection == RepetitionRule.WEEKLY) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp, top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val days = listOf(
                                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                                )
                                days.forEach { day ->
                                    val isDaySelected = tempDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDaySelected) CalendarioTheme.colors.cabecera else Color.Transparent)
                                            .border(1.dp, if (isDaySelected) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), CircleShape)
                                            .clickable {
                                                tempDays = if (isDaySelected) {
                                                    if (tempDays.size > 1) tempDays - day else tempDays
                                                } else {
                                                    tempDays + day
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.getDisplayName(java.time.format.TextStyle.NARROW, locale).uppercase(locale),
                                            color = if (isDaySelected) CalendarioTheme.colors.cabecera.getContrastColor(Color.White) else CalendarioTheme.colors.textSystem,
                                            fontSize = 12.sp,
                                            fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val isRepetitionActive = tempSelection != RepetitionRule.NONE
                val activeAlpha = if (isRepetitionActive) 1f else 0.4f
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
                Spacer(Modifier.height(6.dp))

                Spacer(Modifier.height(6.dp))

                // 1. REPETICIONES (Ahora primera opciÃ³n por defecto v3.2.08)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isRepetitionActive) { endMode = 2 }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.repeat_after), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha))
                        Spacer(Modifier.width(8.dp))
                        
                        val underlineColor = if (endMode == 2 && isRepetitionActive) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                        val cursorBrush = SolidColor(CalendarioTheme.colors.cabecera)
                        
                        androidx.compose.foundation.text.BasicTextField(
                            value = tempCount, 
                            onValueChange = { if (it.all { c -> c.isDigit() }) { val limit = if (tempSelection == RepetitionRule.DAILY) 3 else 2; if (it.length <= limit) tempCount = it } }, 
                            modifier = Modifier
                                .width(50.dp)
                                .focusRequester(focusRequester)
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val y = size.height - strokeWidth / 2
                                    drawLine(
                                        color = underlineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                },
                            textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center, color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), 
                            keyboardActions = KeyboardActions(onDone = { 
                                val finalUntil = if (endMode == 1) tempUntil else null
                                val finalCount = if (endMode == 2) tempCount.toIntOrNull() else null
                                onConfirm(tempSelection, finalUntil, finalCount, tempDays) 
                            }), 
                            singleLine = true, 
                            enabled = (endMode == 2 && isRepetitionActive),
                            cursorBrush = cursorBrush
                        )
                    }
                    if (endMode == 2 && isRepetitionActive) {
                        Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos))
                    }
                }

                // 2. HASTA LA FECHA
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isRepetitionActive) { 
                            endMode = 1
                            if (tempUntil == null) tempUntil = LocalDate.now().plusMonths(1)
                            showDatePicker = true 
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val textToShow = if (endMode == 1 && tempUntil != null) tempUntil!!.format(AppFormats.dayDateFull(locale)).replaceFirstChar { it.titlecase(locale) } else stringResource(id = R.string.repeat_on_date)
                    Text(
                        text = textToShow, 
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp, 
                        color = if (endMode == 1) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha), 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    if (endMode == 1 && isRepetitionActive) {
                        Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos))
                    }
                }

                // 3. INDEFINIDAMENTE
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isRepetitionActive) { endMode = 0 }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.repeat_indefinite), 
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp, 
                        color = CalendarioTheme.colors.textSystem.copy(alpha = activeAlpha)
                    )
                    if (endMode == 0 && isRepetitionActive) {
                        Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos))
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
                    onConfirm(tempSelection, finalUntil, finalCount, tempDays) 
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
                DialogConfirmButton(text = stringResource(id = R.string.apply), onClick = {
                    datePickerState.selectedDateMillis?.let {
                        tempUntil = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
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
                    selectedDayContentColor = CalendarioTheme.colors.cabecera.getContrastColor(CalendarioTheme.colors.fondoDialogos),
                    selectedDayContainerColor = CalendarioTheme.colors.cabecera,
                    todayContentColor = CalendarioTheme.colors.cabecera,
                    todayDateBorderColor = CalendarioTheme.colors.cabecera
                )
            )
        }
    }
}
