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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
    searchResults: Map<LocalDate, List<Festivo>>,
    onClose: () -> Unit,
    onEventClick: (Festivo) -> Unit,
    onRefresh: () -> Unit,
    availableCalendars: List<CalendarInfo>,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = rememberLazyListState()
    
    // --- Lógica de Multiselección ---
    var selectedFestivos by remember { mutableStateOf(setOf<Festivo>()) }
    val isSelectionMode = selectedFestivos.isNotEmpty()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val shareMultipleMessage = stringResource(id = R.string.share_multiple_message, selectedFestivos.size)
    val calendarEventsSubject = stringResource(id = R.string.calendar_events_subject)
    val shareEventTitle = stringResource(id = R.string.share_event)

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
                        
                        items(events, key = { it.id.toString() + "_" + it.date.toString() + "_" + it.startTime }) { festivo ->
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
                                    selectedFestivos += target
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
        val deleteMultipleConfirmation = stringResource(id = R.string.delete_multiple_confirmation, selectedFestivos.size)
        val eventsDeletedTemplate = stringResource(id = R.string.events_deleted_count)

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
                            val msg = String.format(locale, eventsDeletedTemplate, deletedCount)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onRefresh() // Refresca el calendario y la búsqueda
                        }
                        
                        selectedFestivos = emptySet()
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

    val timeText = if (!festivo.isAllDay && festivo.startTime != null) {
        festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else null

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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(festivo)
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
            modifier = Modifier.width(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val cal = availableCalendars.find { it.id == festivo.calendarId }
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
    }
}
