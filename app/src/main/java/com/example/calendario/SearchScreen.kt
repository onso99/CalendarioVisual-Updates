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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import kotlinx.coroutines.launch
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
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(searchResults, searchScope) {
        if (searchResults.isEmpty()) return@LaunchedEffect

        when (searchScope) {
            SearchScope.YEAR -> {
                val currentMonth = YearMonth.now()
                val monthIndex = searchResults.keys.indexOfFirst { YearMonth.from(it) == currentMonth }
                if (monthIndex != -1) {
                    scope.launch {
                        lazyListState.animateScrollToItem(monthIndex * 2) // Each month has a header and items
                    }
                }
            }
            SearchScope.ALL -> {
                val currentYear = LocalDate.now().year
                val yearIndex = searchResults.keys.indexOfFirst { it.year == currentYear }
                if (yearIndex != -1) {
                    val offset = (lazyListState.layoutInfo.viewportSize.height / 2)
                    scope.launch {
                        lazyListState.animateScrollToItem(yearIndex * 2, scrollOffset = -offset)
                    }
                }
            }
            else -> Unit // No specific scroll for MONTH
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(CalendarioTheme.colors.cabecera)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(stringResource(id = R.string.search_events_placeholder), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)) },
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
                                contentDescription = stringResource(id = R.string.close_search),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.clear_search),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CalendarioTheme.colors.cabecera)
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
                val scopeOptions = listOf(stringResource(id = R.string.current_month), stringResource(id = R.string.current_year), stringResource(id = R.string.all))
                val headerColor = CalendarioTheme.colors.cabecera
                val backgroundColor = MaterialTheme.colorScheme.background

                scopeOptions.forEachIndexed { index, text ->
                    val scopeValue = SearchScope.entries[index]
                    val isSelected = searchScope == scopeValue

                    val buttonContainerColor = if (isSelected) {
                        val isBgDark = ColorUtils.calculateLuminance(backgroundColor.toArgb()) < 0.5
                        if (isBgDark) {
                            val isHeaderDark = ColorUtils.calculateLuminance(headerColor.toArgb()) < 0.5
                            if (isHeaderDark) {
                                val hsl = FloatArray(3)
                                ColorUtils.colorToHSL(headerColor.toArgb(), hsl)
                                hsl[2] = (hsl[2] + 0.1f).coerceIn(0f, 1f)
                                Color(ColorUtils.HSLToColor(hsl))
                            } else {
                                headerColor.copy(alpha = 0.2f)
                            }
                        } else {
                            headerColor.copy(alpha = 0.2f)
                        }
                    } else {
                        Color.Transparent
                    }

                    val textColor = if (isSelected) {
                        if (isColorDark(buttonContainerColor, backgroundColor)) Color.White else CalendarioTheme.colors.textSystem
                    } else {
                        CalendarioTheme.colors.textSystem
                    }

                    TextButton(
                        onClick = { onSearchScopeChange(scopeValue) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = buttonContainerColor,
                            contentColor = textColor
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
                                            .background(CalendarioTheme.colors.fondoSecciones)
                                            .padding(8.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = CalendarioTheme.colors.textSystem
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
                                        val titleColor = lerp(
                                            start = CalendarioTheme.colors.cabecera,
                                            stop = CalendarioTheme.colors.textSystem,
                                            fraction = 0.4f
                                        )
                                        Text(
                                            text = yearDate.format(DateTimeFormatter.ofPattern("yyyy")),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = titleColor
                                        )
                                    }
                                }

                                val eventsByMonth = eventsInYear.groupBy { YearMonth.from(it.date) }.toSortedMap()
                                item {
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CalendarioTheme.colors.fondoSecciones)
                                    ) {
                                        eventsByMonth.forEach { (month, eventsInMonth) ->
                                            Text(
                                                text = month.format(DateTimeFormatter.ofPattern("MMMM").withLocale(Locale.getDefault())).replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                                fontWeight = FontWeight.Bold,
                                                color = CalendarioTheme.colors.textSystem,
                                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                                            )
                                            eventsInMonth.forEach { festivo ->
                                                EventRow(festivo, availableCalendars, onEventClick, searchScope)
                                            }
                                            HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f))
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

    val noTitle = stringResource(id = R.string.no_title)
    val allDayEvent = stringResource(id = R.string.all_day_event)
    val baseDesc = if (!festivo.isAllDay && festivo.startTime != null) {
        "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { noTitle }}"
    } else {
        festivo.title.ifEmpty { if (festivo.isAllDay) allDayEvent else "" }
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
                    .border(
                        0.5.dp,
                        CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                        CircleShape
                    )
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(displayDesc, color = itemColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (festivo.rrule != null) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(id = R.string.repeated_event),
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp)
            )
        }
    }
}
