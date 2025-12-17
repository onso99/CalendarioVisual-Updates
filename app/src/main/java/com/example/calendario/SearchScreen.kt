package com.example.calendario

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.YearMonth
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
    availableCalendars: List<CalendarInfo>
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Buscar eventos...", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)) },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cerrar búsqueda",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val scopeOptions = listOf("Mes actual", "Año actual", "Todos")
                scopeOptions.forEachIndexed { index, text ->
                    val scopeValue = SearchScope.entries[index]
                    val isSelected = searchScope == scopeValue

                    TextButton(
                        onClick = { onSearchScopeChange(scopeValue) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) { 
                        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se han encontrado resultados")
                }
            } else if (searchQuery.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Escribe para buscar...")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when (searchScope) {
                        SearchScope.MONTH, SearchScope.YEAR -> {
                            searchResults.forEach { (date, events) ->
                                stickyHeader {
                                    val headerText = if (searchScope == SearchScope.YEAR) {
                                        date.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(Locale.getDefault()))
                                    } else {
                                        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").withLocale(Locale.getDefault()))
                                    }.replaceFirstChar { it.titlecase(Locale.getDefault()) }

                                    Text(
                                        text = headerText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(8.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(events) { festivo ->
                                    EventRow(festivo, availableCalendars, onEventClick, searchScope)
                                }
                            }
                        }
                        SearchScope.ALL -> {
                            searchResults.forEach { (yearDate, eventsInYear) ->
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = yearDate.format(DateTimeFormatter.ofPattern("yyyy")),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                val eventsByMonth = eventsInYear.groupBy { YearMonth.from(it.date) }.toSortedMap()
                                item {
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        eventsByMonth.forEach { (month, eventsInMonth) ->
                                            Text(
                                                text = month.format(DateTimeFormatter.ofPattern("MMMM").withLocale(Locale.getDefault())).replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                                            )
                                            eventsInMonth.forEach { festivo ->
                                                EventRow(festivo, availableCalendars, onEventClick, searchScope)
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    festivo: Festivo,
    availableCalendars: List<CalendarInfo>,
    onEventClick: (Festivo) -> Unit,
    searchScope: SearchScope
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val event1Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "") ?: "" }
    val event2Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "") ?: "" }

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
        else -> CalendarioTheme.colors.textEventDefault
    }

    val baseDesc = if (!festivo.isAllDay && festivo.startTime != null) {
        "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { "(Sin título)" }}"
    } else {
        festivo.title.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }
    }

    val descWithAge = if (festivo.age != null) "$baseDesc (${festivo.age})" else baseDesc

    val displayDesc = when (searchScope) {
        SearchScope.YEAR, SearchScope.ALL -> "${festivo.date.dayOfMonth} - $descWithAge"
        SearchScope.MONTH -> descWithAge
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventClick(festivo) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableCalendars.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(colorInt), CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(displayDesc, color = itemColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (festivo.rrule != null) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Evento repetido",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }
    }
}
