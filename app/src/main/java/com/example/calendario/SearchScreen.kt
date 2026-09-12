package com.example.calendario

import android.content.ContentUris
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
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
                                SearchScope.MONTH -> date.format(AppFormats.dayDateFull(locale))
                                SearchScope.YEAR -> date.format(AppFormats.monthYear(locale))
                                SearchScope.ALL -> date.format(AppFormats.yearOnly(locale))
                            }.replaceFirstChar { it.titlecase(locale) }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = CalendarioTheme.colors.fondoSecciones,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = headerText,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = CalendarioTheme.colors.cabecera
                                )
                            }
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
