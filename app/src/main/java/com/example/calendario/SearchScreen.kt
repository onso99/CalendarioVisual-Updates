package com.example.calendario

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchScope: SearchScope,
    onSearchScopeChange: (SearchScope) -> Unit,
    searchResults: Map<LocalDate, List<SearchItem>>,
    onClose: () -> Unit,
    onEventClick: (Festivo) -> Unit,
    onNoteClick: (DailyNote) -> Unit,
    onDeleteNote: (LocalDate) -> Unit, // Nueva acción para Fase 2
    onOpenHolidayManager: (Festivo) -> Unit,
    onRefresh: () -> Unit,
    availableCalendars: List<CalendarInfo>,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = rememberLazyListState()
    
    // --- Lógica de Multiselección Unificada (v3.1.34) ---
    var selectedItems by remember { mutableStateOf(setOf<SearchItem>()) }
    
    // MODELO DRIVE (v3.1.34): Reiniciar selección si cambian los criterios de búsqueda
    LaunchedEffect(searchQuery, searchScope) {
        selectedItems = emptySet()
    }
    
    val isSelectionMode = selectedItems.isNotEmpty()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val selectedFestivos = remember(selectedItems) { 
        selectedItems.filterIsInstance<SearchItem.Event>().map { it.festivo }.toSet() 
    }
    val selectedNotes = remember(selectedItems) { 
        selectedItems.filterIsInstance<SearchItem.Note>().map { it.dailyNote }.toSet() 
    }

    val shareMultipleMessage = stringResource(id = R.string.share_multiple_message, selectedItems.size)
    val calendarEventsSubject = stringResource(id = R.string.calendar_events_subject)
    val shareEventTitle = stringResource(id = R.string.share_event)

    LaunchedEffect(Unit) {
        if (!isSelectionMode) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.search),
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        if (isSelectionMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.selected_count_short, selectedItems.size),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp
                            )
                            IconButton(onClick = { selectedItems = emptySet() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.clear_selection),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            IcsHelper.shareEvents(
                                context = context,
                                events = selectedFestivos,
                                shareMultipleMessage = shareMultipleMessage,
                                calendarEventsSubject = calendarEventsSubject,
                                shareEventTitle = shareEventTitle
                            )
                        }) {
                            Icon(Icons.Default.Share, stringResource(id = R.string.share_event), tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, stringResource(id = R.string.delete), tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CalendarioTheme.colors.cabecera)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CalendarioTheme.colors.settingsBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FASE 1 corregida: Cuadro de búsqueda integrado en el cuerpo, no en la cabecera
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(id = R.string.search_events_placeholder), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, stringResource(id = R.string.clear_search), tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CalendarioTheme.colors.cabecera,
                    unfocusedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f),
                    cursorColor = CalendarioTheme.colors.cabecera,
                    focusedTextColor = CalendarioTheme.colors.textSystem,
                    unfocusedTextColor = CalendarioTheme.colors.textSystem,
                    focusedContainerColor = CalendarioTheme.colors.fondoSecciones,
                    unfocusedContainerColor = CalendarioTheme.colors.fondoSecciones
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val scopeOptions = listOf(stringResource(id = R.string.current_month), stringResource(id = R.string.current_year), stringResource(id = R.string.all))
                scopeOptions.forEachIndexed { index, text ->
                    val scopeValue = SearchScope.entries[index]
                    val isSelected = searchScope == scopeValue
                    TextButton(
                        onClick = { onSearchScopeChange(scopeValue) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isSelected) CalendarioTheme.colors.cabecera.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = CalendarioTheme.colors.textSystem
                        )
                    ) { 
                        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_results_found), color = CalendarioTheme.colors.textSystem)
                }
            } else if (searchQuery.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.type_to_search), color = CalendarioTheme.colors.textSystem)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
                    searchResults.forEach { (date, events) ->
                        stickyHeader {
                            val headerText = when (searchScope) {
                                SearchScope.MONTH -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))
                                SearchScope.YEAR -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                                SearchScope.ALL -> date.format(DateTimeFormatter.ofPattern("yyyy", locale))
                            }.replaceFirstChar { it.titlecase(locale) }

                            Text(
                                text = headerText,
                                modifier = Modifier.fillMaxWidth().background(CalendarioTheme.colors.fondoSecciones).padding(8.dp),
                                fontWeight = FontWeight.Bold,
                                color = CalendarioTheme.colors.textSystem
                            )
                        }
                        
                        items(events, key = { it.adn }) { searchItem ->
                            when (searchItem) {
                                is SearchItem.Event -> {
                                    val festivo = searchItem.festivo
                                    val isSelected = selectedItems.contains(searchItem)
                                    val isLocalHoliday = festivo.calendarId == -1L && festivo.isFromHolidaySource
                                    val calendar = availableCalendars.find { it.id == festivo.calendarId }
                                    val isReadOnlyCalendar = calendar != null && !calendar.canModify
                                    val isSpecial = isLocalHoliday || festivo.isBirthday || festivo.isFromHolidaySource || isReadOnlyCalendar || festivo.isGhost
                                    
                                    EventRow(
                                        festivo = festivo,
                                        availableCalendars = availableCalendars,
                                        isSelected = isSelected,
                                        isSpecial = isSpecial,
                                        onEventClick = { clicked ->
                                            if (isSelectionMode) {
                                                if (!isSpecial) {
                                                    selectedItems = if (isSelected) selectedItems - searchItem else selectedItems + searchItem
                                                }
                                            } else {
                                                if (isLocalHoliday) {
                                                    onOpenHolidayManager(clicked)
                                                } else {
                                                    onEventClick(clicked)
                                                }
                                            }
                                        },
                                        onLongClick = { 
                                            if (!isSpecial) {
                                                selectedItems += searchItem
                                            }
                                        },
                                        searchScope = searchScope
                                    )
                                }
                                is SearchItem.Note -> {
                                    val note = searchItem.dailyNote
                                    val isSelected = selectedItems.contains(searchItem)
                                    NoteRow(
                                        note = note,
                                        isSelected = isSelected,
                                        searchScope = searchScope,
                                        onClick = { 
                                            if (isSelectionMode) {
                                                selectedItems = if (isSelected) selectedItems - searchItem else selectedItems + searchItem
                                            } else {
                                                onNoteClick(note)
                                            }
                                        },
                                        onLongClick = {
                                            selectedItems += searchItem
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Diálogo de Confirmación de Borrado ---
    if (showDeleteConfirmDialog) {
        val deleteMultipleConfirmation = stringResource(id = R.string.delete_multiple_confirmation, selectedItems.size)

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.confirm_deletion_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
            text = { Text(deleteMultipleConfirmation) },
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(id = R.string.delete),
                    onClick = {
                        var deletedCount = 0

                        // 1. Borrar Eventos
                        selectedFestivos.filter { festivo ->
                            availableCalendars.find { it.id == festivo.calendarId }?.canModify == true
                        }.forEach { festivo ->
                            try {
                                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, festivo.id)
                                val rows = context.contentResolver.delete(deleteUri, null, null)
                                if (rows > 0 || festivo.id > 0) {
                                    markEventAsDeleted(context, festivo.id)
                                    removeSeriesFromHistory(context, festivo.id)
                                    deletedCount++
                                }
                            } catch (_: Exception) {}
                        }

                        // 2. Borrar Notas
                        selectedNotes.forEach { note ->
                            onDeleteNote(note.date)
                            deletedCount++
                        }
                        
                        if (deletedCount > 0) {
                            Toast.makeText(context, R.string.holiday_updated_successfully, Toast.LENGTH_SHORT).show()
                            onRefresh()
                        }
                        
                        selectedItems = emptySet()
                        showDeleteConfirmDialog = false
                    },
                    color = Color.Red
                )
            },
            dismissButton = {
                DialogDismissButton(onDismiss = { showDeleteConfirmDialog = false })
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventRow(
    festivo: Festivo,
    availableCalendars: List<CalendarInfo>,
    isSelected: Boolean,
    isSpecial: Boolean,
    onEventClick: (Festivo) -> Unit,
    onLongClick: (Festivo) -> Unit,
    searchScope: SearchScope
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val event1Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "")?.trim() ?: "" }
    val event2Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "")?.trim() ?: "" }

    val normalizedTitle = festivo.title.unaccent().lowercase()
    val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
    val esCumpleanos = festivo.isBirthday && !esFestivo
    val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
    val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

    val neutralColor = CalendarioTheme.colors.textSystem
    
    val eventSpecificColor = when {
        esEvento1 -> CalendarioTheme.colors.textEvent1
        esEvento2 -> CalendarioTheme.colors.textEvent2
        esFestivo -> CalendarioTheme.colors.textSundayHoliday
        esCumpleanos -> CalendarioTheme.colors.textBirthday
        else -> CalendarioTheme.colors.textSystem
    }

    // Si está seleccionado, aseguramos contraste
    val titleColor = if (isSelected) {
        val highlightColor = CalendarioTheme.colors.todayHighlightColor
        val opaqueHighlightInt = ColorUtils.setAlphaComponent(highlightColor.toArgb(), 255)
        val opaqueEventColorInt = ColorUtils.setAlphaComponent(eventSpecificColor.toArgb(), 255)
        if (ColorUtils.calculateContrast(opaqueEventColorInt, opaqueHighlightInt) > 1.5) {
            eventSpecificColor
        } else {
            if (isColorDark(highlightColor, Color.Black)) Color.White else Color.Black
        }
    } else {
        eventSpecificColor
    }

    val noTitle = stringResource(id = R.string.no_title)
    val allDayEvent = stringResource(id = R.string.all_day_event)

    // LÓGICA DE HORA INTELIGENTE (v3.1.34):
    // Primer día -> Hora Inicio | Último día -> Hora Fin | Resto -> Sin hora (Todo el día)
    val displayTime = when {
        festivo.isAllDay -> null
        festivo.currentDay == 1 -> festivo.startTime
        festivo.currentDay == festivo.totalDays -> festivo.endTime
        else -> null
    }
    val timeText = displayTime?.format(DateTimeFormatter.ofPattern("HH:mm"))

    val titleText = festivo.title.ifEmpty { if (festivo.isAllDay) allDayEvent else noTitle }
    val ageText = if (festivo.age != null && festivo.age > 0) " (${festivo.age})" else ""

    val locale = LocalConfiguration.current.locales[0]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, CalendarioTheme.colors.cabecera, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CalendarioTheme.colors.todayHighlightColor else Color.Transparent)
            .combinedClickable(
                onClick = { onEventClick(festivo) },
                onLongClick = {
                    if (!isSpecial) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick(festivo)
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. DÍA (Solo si no es vista de mes)
        if (searchScope != SearchScope.MONTH) {
            Text(
                text = String.format(locale, "%02d", festivo.date.dayOfMonth),
                color = neutralColor,
                fontSize = 16.sp,
                modifier = Modifier.width(26.dp)
            )
        }

        // 2. INDICADOR DE FORMA (Alineado)
        Box(
            modifier = Modifier.width(26.dp), // Aumentado ligeramente para el fantasma
            contentAlignment = Alignment.CenterStart
        ) {
            val cal = availableCalendars.find { it.id == festivo.calendarId }
            val isGhost = festivo.isGhost || (cal == null && festivo.calendarId > 0)
            
            if (isGhost) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ghost_24),
                    contentDescription = null,
                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                val colorToUse = if (festivo.customColor != null) {
                    Color(festivo.customColor)
                } else if (cal != null) {
                    Color(cal.color)
                } else {
                    Color.Transparent
                }

                if (festivo.isLongPeriod && festivo.lane != null) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(colorToUse)
                    )
                } else if (colorToUse != Color.Transparent) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(colorToUse.copy(alpha = 0.6f), CircleShape)
                            .border(0.5.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.width(1.dp))

        // 3. TEXTO (Hora + Título + Progreso)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (timeText != null) {
                Text(
                    text = "$timeText ",
                    color = neutralColor,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }
            
            Text(
                text = titleText + ageText,
                color = titleColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (festivo.isLongPeriod) {
                Text(
                    " (${festivo.currentDay}/${festivo.totalDays})",
                    color = titleColor.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }

        if (festivo.rrule != null) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }

        if (isSpecial) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 8.dp).size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: DailyNote,
    isSelected: Boolean,
    searchScope: SearchScope,
    onClick: (DailyNote) -> Unit,
    onLongClick: (DailyNote) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]
    val neutralColor = CalendarioTheme.colors.textSystem
    var isExpanded by remember { mutableStateOf(false) }
    
    // Almacenamos el índice donde el texto se corta naturalmente en una línea
    var cutIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(isSelected) {
        if (isSelected) isExpanded = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, CalendarioTheme.colors.cabecera, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CalendarioTheme.colors.todayHighlightColor else Color.Transparent)
            .combinedClickable(
                onClick = { 
                    if (isExpanded) isExpanded = false
                    onClick(note) 
                },
                onLongClick = {
                    isExpanded = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(note)
                }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (searchScope != SearchScope.MONTH) {
                Text(
                    text = String.format(locale, "%02d", note.date.dayOfMonth),
                    color = neutralColor,
                    fontSize = 16.sp,
                    modifier = Modifier.width(26.dp)
                )
            }

            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = null,
                    tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // TEXTO SUPERIOR: Lógica de continuidad (v3.1.34)
            // Si está expandida, quitamos los puntos suspensivos (Punto 5)
            val topText = if (isExpanded && cutIndex != -1) {
                note.content.take(cutIndex).replace("\n", " ")
            } else {
                note.content.replace("\n", " ")
            }

            Text(
                text = topText,
                color = neutralColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                onTextLayout = { layoutResult ->
                    if (cutIndex == -1) {
                        val end = layoutResult.getLineEnd(0, visibleEnd = true)
                        val textBeforeCut = note.content.take(end)
                        val lastSpace = textBeforeCut.lastIndexOf(' ')
                        cutIndex = if (lastSpace > 0) lastSpace + 1 else end
                    }
                },
                modifier = Modifier.weight(1f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = "Previsualizar nota",
                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // TEXTO DESPLEGADO: Continúa exactamente donde terminó la primera línea
        if (isExpanded && cutIndex != -1 && cutIndex < note.content.length) {
            Text(
                text = note.content.substring(cutIndex),
                color = neutralColor.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(
                    top = 4.dp, 
                    start = if (searchScope != SearchScope.MONTH) 26.dp else 0.dp,
                    end = 8.dp // Reducido al mínimo para aprovechar el espacio bajo el ojo
                )
            )
        }
    }
}
