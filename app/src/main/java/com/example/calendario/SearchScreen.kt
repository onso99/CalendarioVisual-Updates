package com.example.calendario

import android.content.ContentUris
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AssistantPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate,
    onEndDateChange: (LocalDate) -> Unit,
    activeFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    timeShortcut: String?,
    onApplyShortcut: (String) -> Unit,
    searchResults: Map<LocalDate, List<SearchItem>>,
    onClose: () -> Unit,
    onEventClick: (Festivo) -> Unit,
    onNoteClick: (DailyNote) -> Unit,
    onDeleteNote: (LocalDate) -> Unit, 
    onOpenHolidayManager: (Festivo) -> Unit, 
    onSaveLocalClick: (Set<SearchItem>) -> Unit, 
    onRefresh: () -> Unit,
    availableCalendars: List<CalendarInfo>,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    
    var selectedItems by remember { mutableStateOf(setOf<SearchItem>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePickerDialog by remember { mutableStateOf(false) }

    // Reiniciar selección si cambian los criterios
    LaunchedEffect(searchQuery, startDate, endDate, activeFilters) {
        selectedItems = emptySet()
    }
    
    // --- LÓGICA DE AUTO-SCROLL AL "HOY" ---
    val today = LocalDate.now()
    LaunchedEffect(searchResults) {
        if (searchResults.isNotEmpty()) {
            val targetDate = searchResults.keys.filter { it >= today }.minByOrNull { it }
                ?: searchResults.keys.maxByOrNull { it }
            
            if (targetDate != null) {
                val groupedByMonth = searchResults.keys.groupBy { YearMonth.from(it) }
                    .toSortedMap(compareByDescending { it })
                
                var targetIndex = 0
                outer@for ((month, days) in groupedByMonth) {
                    targetIndex += 1
                    val isTargetMonth = month == YearMonth.from(targetDate)
                    if (isTargetMonth) {
                        for (day in days) {
                            if (day == targetDate) break@outer
                            targetIndex += searchResults[day]?.size ?: 0
                        }
                        break@outer
                    }
                    days.forEach { d -> targetIndex += searchResults[d]?.size ?: 0 }
                }
                
                if (targetIndex > 0) {
                    delay(150.milliseconds)
                    lazyListState.scrollToItem((targetIndex - 1).coerceAtLeast(0)) 
                }
            }
        }
    }
    
    val isSelectionMode = selectedItems.isNotEmpty()

    val selectedFestivos = remember(selectedItems) { 
        selectedItems.filterIsInstance<SearchItem.Event>().map { it.festivo }.toSet() 
    }
    val selectedNotes = remember(selectedItems) { 
        selectedItems.filterIsInstance<SearchItem.Note>().map { it.dailyNote }.toSet() 
    }

    LaunchedEffect(Unit) {
        if (!isSelectionMode) focusRequester.requestFocus()
    }

    AppScreen(
        title = stringResource(id = R.string.search),
        onBackClick = onClose,
        scrollable = false, 
        selectionCount = selectedItems.size, 
        onClearSelection = { selectedItems = emptySet() },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = { 
                    val events = selectedItems.filterIsInstance<SearchItem.Event>().map { it.festivo }
                    val notes = selectedItems.filterIsInstance<SearchItem.Note>().map { it.dailyNote }
                    CvoHelper.shareAgendaPackage(context, events, notes)
                }) {
                    Icon(Icons.Outlined.Share, null, tint = Color.White)
                }
                IconButton(onClick = { onSaveLocalClick(selectedItems) }) {
                    Icon(Icons.Outlined.Save, null, tint = Color.White)
                }
                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                    Icon(Icons.Outlined.Delete, stringResource(id = R.string.delete), tint = Color.White)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 0.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(id = R.string.search_events_placeholder), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.7f)) },
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
                    focusedContainerColor = CalendarioTheme.colors.fondoSecciones,
                    unfocusedContainerColor = CalendarioTheme.colors.fondoSecciones,
                    focusedTextColor = CalendarioTheme.colors.textSystem,
                    unfocusedTextColor = CalendarioTheme.colors.textSystem,
                    cursorColor = CalendarioTheme.colors.cabecera
                )
            )

            // 2. Filtros
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchFilterChip(
                    icon = Icons.Outlined.AssistantPhoto,
                    isSelected = activeFilters.contains("EVENT"),
                    onClick = { onToggleFilter("EVENT") }
                )
                SearchFilterChip(
                    icon = Icons.AutoMirrored.Outlined.StickyNote2,
                    isSelected = activeFilters.contains("NOTE"),
                    onClick = { onToggleFilter("NOTE") }
                )
                
                Spacer(Modifier.width(4.dp))
                HorizontalDivider(modifier = Modifier.width(1.dp).height(24.dp), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
                Spacer(Modifier.width(4.dp))

                SearchFilterChip(
                    painter = painterResource(id = R.drawable.ic_search_month), 
                    isSelected = timeShortcut == "MONTH",
                    onClick = { onApplyShortcut("MONTH") }
                )
                SearchFilterChip(
                    painter = painterResource(id = R.drawable.ic_search_year), 
                    isSelected = timeShortcut == "YEAR",
                    onClick = { onApplyShortcut("YEAR") }
                )
                SearchFilterChip(
                    icon = Icons.Default.AllInclusive,
                    isSelected = timeShortcut == "ALWAYS",
                    onClick = { onApplyShortcut("ALWAYS") }
                )

                Spacer(Modifier.weight(1f))

                val visibleItems = remember(searchResults) { searchResults.values.flatten().toSet() }
                val allVisibleSelected = remember(selectedItems, visibleItems) { 
                    visibleItems.isNotEmpty() && visibleItems.all { it in selectedItems } 
                }
                
                SearchFilterChip(
                    icon = if (allVisibleSelected) Icons.Default.LibraryAddCheck else Icons.Default.SelectAll,
                    isSelected = allVisibleSelected,
                    onClick = {
                        selectedItems = if (allVisibleSelected) {
                            selectedItems - visibleItems
                        } else {
                            selectedItems + visibleItems
                        }
                    }
                )
            }

            // 3. Rango de Fechas
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val isAlways = timeShortcut == "ALWAYS"
                val dateAlpha = if (isAlways) 0.3f else 1f
                val dateFmt = AppFormats.DateAbbr
                
                TextButton(onClick = { showStartDatePicker = true }, enabled = !isAlways) {
                    Text(startDate.format(dateFmt), color = CalendarioTheme.colors.textSystem.copy(alpha = dateAlpha), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f * dateAlpha), modifier = Modifier.size(14.dp))
                TextButton(onClick = { showEndDatePickerDialog = true }, enabled = !isAlways) {
                    Text(endDate.format(dateFmt), color = CalendarioTheme.colors.textSystem.copy(alpha = dateAlpha), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) stringResource(id = R.string.type_to_search) else stringResource(id = R.string.no_results_found),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                    )
                }
            } else {
                val groupedByMonth = remember(searchResults) {
                    searchResults.keys.groupBy { YearMonth.from(it) }
                        .toSortedMap(compareByDescending { it })
                }
                val mostFutureMonth = groupedByMonth.keys.firstOrNull()

                LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
                    groupedByMonth.forEach { (month, daysInMonth) ->
                        stickyHeader {
                            val isNotMostFuture = month != mostFutureMonth
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = CalendarioTheme.colors.fondoSecciones,
                                shadowElevation = 1.dp
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = month.format(AppFormats.monthYear(locale)).replaceFirstChar { it.titlecase(locale) },
                                        fontWeight = FontWeight.Bold,
                                        color = CalendarioTheme.colors.cabecera,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isNotMostFuture) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        daysInMonth.forEach { day ->
                            items(searchResults[day] ?: emptyList(), key = { it.adn }) { searchItem ->
                                when (searchItem) {
                                    is SearchItem.Event -> {
                                        val festivo = searchItem.festivo
                                        val isSelected = selectedItems.contains(searchItem)
                                        val isLocalHoliday = festivo.calendarId == -1L && festivo.isFromHolidaySource
                                        EventRow(
                                            festivo = festivo,
                                            availableCalendars = availableCalendars,
                                            isSelected = isSelected,
                                            isSpecial = isLocalHoliday,
                                            onEventClick = { clicked ->
                                                if (isSelectionMode) {
                                                    selectedItems = if (isSelected) selectedItems - searchItem else selectedItems + searchItem
                                                } else {
                                                    if (isLocalHoliday) onOpenHolidayManager(clicked)
                                                    else onEventClick(clicked)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedItems += searchItem
                                                }
                                            },
                                            searchScope = SearchScope.YEAR
                                        )
                                    }
                                    is SearchItem.Note -> {
                                        val isSelected = selectedItems.contains(searchItem)
                                        NoteRow(
                                            note = searchItem.dailyNote,
                                            isSelected = isSelected,
                                            searchScope = SearchScope.YEAR,
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedItems = if (isSelected) selectedItems - searchItem else selectedItems + searchItem
                                                } else {
                                                    onNoteClick(searchItem.dailyNote)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedItems += searchItem
                                                }
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
    }

    if (showDeleteConfirmDialog) {
        AppDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = stringResource(id = R.string.confirm_deletion_title),
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(id = R.string.delete),
                    onClick = {
                        var deletedCount = 0
                        selectedFestivos.filter { festivo ->
                            availableCalendars.find { it.id == festivo.calendarId }?.canModify == true
                        }.forEach { festivo ->
                            try {
                                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, festivo.id)
                                val rows = context.contentResolver.delete(deleteUri, null, null)
                                if (rows > 0 || festivo.id > 0) {
                                    SettingsManager.markEventAsDeleted(context, festivo.id)
                                    removeSeriesFromHistory(context, festivo.id)
                                    deletedCount++
                                }
                            } catch (_: Exception) {}
                        }
                        selectedNotes.forEach { note ->
                            onDeleteNote(note.date)
                            deletedCount++
                        }
                        if (deletedCount > 0) {
                            val msg = context.applicationContext.getString(R.string.elements_deleted_count, deletedCount)
                            context.showToast(msg)
                            onRefresh()
                        }
                        selectedItems = emptySet()
                        showDeleteConfirmDialog = false
                    },
                    color = Color.Red
                )
            },
            dismissButton = { DialogDismissButton(onDismiss = { showDeleteConfirmDialog = false }) }
        ) {
            Text(stringResource(id = R.string.delete_multiple_confirmation, selectedItems.size))
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onStartDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showStartDatePicker = false
                }) { Text(stringResource(id = R.string.accept)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val startMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    return utcTimeMillis >= startMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onEndDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showEndDatePickerDialog = false
                }) { Text(stringResource(id = R.string.accept)) }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun SearchFilterChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val brandColor = CalendarioTheme.colors.cabecera
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) brandColor.copy(alpha = 0.12f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) brandColor else CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
        ),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) brandColor else CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            } else if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = if (isSelected) brandColor else CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
