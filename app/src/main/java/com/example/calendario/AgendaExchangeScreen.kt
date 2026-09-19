package com.example.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.LooksTwo
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaExchangeScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate,
    onEndDateChange: (LocalDate) -> Unit,
    activeFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    searchResults: Map<LocalDate, List<SearchItem>>,
    onClose: () -> Unit,
    onEventClick: (Festivo) -> Unit, 
    onNoteClick: (DailyNote) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: (Set<SearchItem>) -> Unit,
    onSaveLocalClick: (Set<SearchItem>) -> Unit, // Nuevo para modo pruebas (v3.1.34)
    availableCalendars: List<CalendarInfo>,
) {
    val locale = LocalConfiguration.current.locales[0]
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePickerDialog by remember { mutableStateOf(false) }

    // En esta pantalla la selección ahora es opcional como en búsqueda (v3.1.34)
    var selectedItems by remember { mutableStateOf(setOf<SearchItem>()) }
    val isSelectionMode = selectedItems.isNotEmpty()
    
    // Reiniciar selección si la búsqueda o filtros cambian (Modelo Drive)
    LaunchedEffect(searchQuery, startDate, endDate, activeFilters) {
        selectedItems = emptySet()
    }

    AppScreen(
        title = stringResource(id = R.string.import_agenda_title),
        onBackClick = onClose,
        scrollable = false,
        topBarExtension = {
            if (isSelectionMode) {
                // Contador y aspa prÃ³ximos y centrados (v3.2.15.2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.selected_count_short, selectedItems.size),
                        color = CalendarioTheme.colors.cabecera,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { selectedItems = emptySet() }, 
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, 
                            contentDescription = null, 
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f), 
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        actions = {
            // 1. Acción de Exportar (Compartir Clásico - v3.1.34)
            if (isSelectionMode) {
                IconButton(onClick = { onExportClick(selectedItems) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share, 
                        contentDescription = stringResource(id = R.string.share_event), 
                        tint = Color.White
                    )
                }
            }

            // 2. Acción de Guardar Local (Disquete Outlined) - Modo Pruebas
            if (isSelectionMode) {
                IconButton(onClick = { onSaveLocalClick(selectedItems) }) {
                    Icon(Icons.Outlined.Save, null, tint = Color.White)
                }
            }

            // 3. Acción de Importar (Carpeta Custom) - Siempre en el extremo derecho
            IconButton(onClick = onImportClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_open_custom), 
                    contentDescription = stringResource(id = R.string.cargar_label), 
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Buscador (Sin padding superior extra v3.2.12.2)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(id = R.string.search_events_placeholder), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = CalendarioTheme.colors.cabecera.copy(alpha = 0.7f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
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
                    unfocusedTextColor = CalendarioTheme.colors.textSystem
                )
            )

            // --- FILTROS FASE 3: Rango y Categorías ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Fila 1: Rango de Fechas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dateFmt = AppFormats.DateAbbr
                    
                    TextButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(startDate.format(dateFmt), color = CalendarioTheme.colors.textSystem)
                    }
                    
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                    
                    TextButton(onClick = { showEndDatePickerDialog = true }) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(endDate.format(dateFmt), color = CalendarioTheme.colors.textSystem)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fila 2: Atajos de Categoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterShortcutChip(
                        icon = Icons.Outlined.LooksOne,
                        label = "",
                        isSelected = activeFilters.contains("EVENT1"),
                        color = CalendarioTheme.colors.textEvent1,
                        onClick = { onToggleFilter("EVENT1") }
                    )
                    FilterShortcutChip(
                        icon = Icons.Outlined.LooksTwo,
                        label = "",
                        isSelected = activeFilters.contains("EVENT2"),
                        color = CalendarioTheme.colors.textEvent2,
                        onClick = { onToggleFilter("EVENT2") }
                    )
                    FilterShortcutChip(
                        icon = Icons.Outlined.Cake,
                        label = "",
                        isSelected = activeFilters.contains("BIRTHDAY"),
                        color = CalendarioTheme.colors.textBirthday,
                        onClick = { onToggleFilter("BIRTHDAY") }
                    )
                    FilterShortcutChip(
                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                        label = "",
                        isSelected = activeFilters.contains("NOTE"),
                        color = CalendarioTheme.colors.cabecera,
                        onClick = { onToggleFilter("NOTE") }
                    )
                    
                    Spacer(Modifier.width(4.dp))
                    
                    // BOTÓN SELECCIONAR TODO (v3.1.34: Solo elementos visibles)
                    val visibleItems = remember(searchResults) { searchResults.values.flatten().toSet() }
                    val allVisibleSelected = remember(selectedItems, visibleItems) { 
                        visibleItems.isNotEmpty() && visibleItems.all { it in selectedItems } 
                    }
                    
                    FilterShortcutChip(
                        icon = if (allVisibleSelected) Icons.Default.LibraryAddCheck else Icons.Default.SelectAll,
                        label = "",
                        isSelected = allVisibleSelected,
                        color = CalendarioTheme.colors.cabecera,
                        onClick = {
                            if (allVisibleSelected) {
                                selectedItems = selectedItems - visibleItems
                            } else {
                                selectedItems = selectedItems + visibleItems
                            }
                        }
                    )
                }
            }

            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_results_found), color = CalendarioTheme.colors.textSystem)
                }
            } else {
                val groupedByMonth = remember(searchResults) {
                    searchResults.keys.groupBy { YearMonth.from(it) }
                        .toSortedMap(compareByDescending { it })
                }

                LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
                    groupedByMonth.forEach { (month, daysInMonth) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = CalendarioTheme.colors.fondoSecciones,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = month.format(AppFormats.monthYear(locale)).replaceFirstChar { it.titlecase(locale) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = CalendarioTheme.colors.cabecera
                                )
                            }
                        }

                        daysInMonth.forEach { day ->
                            items(searchResults[day] ?: emptyList(), key = { it.adn }) { searchItem ->
                                when (searchItem) {
                                    is SearchItem.Event -> {
                                        val isSelected = selectedItems.contains(searchItem)
                                        EventRow(
                                            festivo = searchItem.festivo,
                                            availableCalendars = availableCalendars,
                                            isSelected = isSelected,
                                            isSpecial = false,
                                            onEventClick = { clicked ->
                                                if (isSelectionMode) {
                                                    selectedItems = if (isSelected) selectedItems - searchItem else selectedItems + searchItem
                                                } else {
                                                    onEventClick(clicked)
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

    // --- Diálogos de Selección de Fecha ---
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
                // Restricción de Cronología Segura (v3.1.34)
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
private fun FilterShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) color else CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
            if (label.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else CalendarioTheme.colors.textSystem.copy(alpha = 0.4f)
                )
            }
        }
    }
}
