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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchScope: SearchScope,
    onSearchScopeChange: (SearchScope) -> Unit,
    searchResults: Map<LocalDate, List<Festivo>>,
    onClose: () -> Unit,
    onEventClick: (Festivo) -> Unit,
    onRefresh: () -> Unit,
    availableCalendars: List<CalendarInfo>
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = rememberLazyListState()
    
    // --- Lógica de Multiselección ---
    var selectedFestivos by remember { mutableStateOf(setOf<Festivo>()) }
    val isSelectionMode = selectedFestivos.isNotEmpty()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isSelectionMode) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(CalendarioTheme.colors.cabecera)
                    .statusBarsPadding()
            ) {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { 
                            Text(
                                text = stringResource(id = R.string.selected_count, selectedFestivos.size), 
                                color = Color.White,
                                fontSize = 20.sp
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedFestivos = emptySet() }) {
                                Icon(Icons.Default.Close, stringResource(id = R.string.close), tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { 
                                IcsHelper.shareEvents(context, selectedFestivos)
                            }) {
                                Icon(Icons.Default.Share, stringResource(id = R.string.share_event), tint = Color.White)
                            }
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, stringResource(id = R.string.delete), tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CalendarioTheme.colors.cabecera)
                    )
                } else {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text(stringResource(id = R.string.search_events_placeholder), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)) },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    errorIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.close_search),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, stringResource(id = R.string.clear_search), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CalendarioTheme.colors.cabecera)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isSelectionMode) {
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
                                SearchScope.MONTH -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").withLocale(Locale.getDefault()))
                                SearchScope.YEAR -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(Locale.getDefault()))
                                SearchScope.ALL -> date.format(DateTimeFormatter.ofPattern("yyyy"))
                            }.replaceFirstChar { it.titlecase(Locale.getDefault()) }

                            Text(
                                text = headerText,
                                modifier = Modifier.fillMaxWidth().background(CalendarioTheme.colors.fondoSecciones).padding(8.dp),
                                fontWeight = FontWeight.Bold,
                                color = CalendarioTheme.colors.textSystem
                            )
                        }
                        
                        items(events) { festivo ->
                            val isSelected = selectedFestivos.contains(festivo)
                            EventRow(
                                festivo = festivo,
                                availableCalendars = availableCalendars,
                                isSelected = isSelected,
                                onEventClick = { clicked ->
                                    if (isSelectionMode) {
                                        selectedFestivos = if (isSelected) selectedFestivos - clicked else selectedFestivos + clicked
                                    } else {
                                        onEventClick(clicked)
                                    }
                                },
                                onLongClick = { target ->
                                    selectedFestivos = selectedFestivos + target
                                },
                                searchScope = searchScope
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Diálogo de Confirmación de Borrado ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(id = R.string.confirm_deletion_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.delete_multiple_confirmation, selectedFestivos.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        val eventsToDelete = selectedFestivos.filter { festivo ->
                            availableCalendars.find { it.id == festivo.calendarId }?.canModify == true
                        }
                        
                        var deletedCount = 0
                        eventsToDelete.forEach { festivo ->
                            try {
                                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, festivo.id)
                                context.contentResolver.delete(deleteUri, null, null)
                                deletedCount++
                            } catch (_: Exception) {
                                // Ignorar errores individuales
                            }
                        }
                        
                        if (deletedCount > 0) {
                            val msg = context.getString(R.string.events_deleted_count, deletedCount)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onRefresh() // Refresca el calendario y la búsqueda
                        }
                        
                        selectedFestivos = emptySet()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(id = R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            },
            containerColor = CalendarioTheme.colors.fondoDialogos
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventRow(
    festivo: Festivo,
    availableCalendars: List<CalendarInfo>,
    isSelected: Boolean,
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

    val itemColor = when {
        esEvento1 -> CalendarioTheme.colors.textEvent1
        esEvento2 -> CalendarioTheme.colors.textEvent2
        esFestivo -> CalendarioTheme.colors.textSundayHoliday
        esCumpleanos -> CalendarioTheme.colors.textBirthday
        else -> CalendarioTheme.colors.textSystem // Eventos normales adaptativos
    }

    val noTitle = stringResource(id = R.string.no_title)
    val allDayEvent = stringResource(id = R.string.all_day_event)
    val baseDesc = if (!festivo.isAllDay && festivo.startTime != null) {
        "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { noTitle }}"
    } else {
        festivo.title.ifEmpty { if (festivo.isAllDay) allDayEvent else "" }
    }

    val descWithAge = if (festivo.age != null && festivo.age > 0) "$baseDesc (${festivo.age})" else baseDesc
    val displayDesc = if (searchScope == SearchScope.MONTH) descWithAge else "${festivo.date.dayOfMonth} - $descWithAge"

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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(festivo)
                }
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableCalendars.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
            Box(
                Modifier.size(10.dp).background(Color(colorInt), CircleShape)
                    .border(0.5.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), CircleShape)
            )
            Spacer(Modifier.size(8.dp))
        }
        
        Text(
            text = displayDesc, 
            color = itemColor, 
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis, 
            modifier = Modifier.weight(1f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (festivo.rrule != null) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }
    }
}
