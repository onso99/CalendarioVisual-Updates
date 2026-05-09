package com.example.calendario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

enum class CalendarViewMode { MONTHLY, YEARLY }
enum class SearchScope { MONTH, YEAR, ALL }

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()
fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

@Composable
private fun getActualFirstDayOfWeek(context: Context): DayOfWeek {
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val startOfWeekKey = prefs.getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key) ?: StartOfWeekOption.SYSTEM.key
    return when (StartOfWeekOption.fromKey(startOfWeekKey)) {
        StartOfWeekOption.SYSTEM -> WeekFields.of(Locale.getDefault()).firstDayOfWeek
        StartOfWeekOption.MONDAY -> DayOfWeek.MONDAY
        StartOfWeekOption.SUNDAY -> DayOfWeek.SUNDAY
        StartOfWeekOption.SATURDAY -> DayOfWeek.SATURDAY
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarioScreen(
    themeManager: ThemeManager,
    onThemeUpdated: () -> Unit,
    viewModel: CalendarioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val today = LocalDate.now()
    val startMonth = remember { YearMonth.now().minusYears(100) }
    val initialPage = remember { ChronoUnit.MONTHS.between(startMonth, YearMonth.now()).toInt() }
    val monthPagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })

    val currentMonth by remember { derivedStateOf { startMonth.plusMonths(monthPagerState.currentPage.toLong()) } }

    val startYear = remember { Year.of(1924) }
    val initialYearPage = remember { Year.now().value - startYear.value }
    val yearPagerState = rememberPagerState(initialPage = initialYearPage, pageCount = { 201 })

    val currentYear by remember { derivedStateOf { startYear.plusYears(yearPagerState.currentPage.toLong()) } }

    var menuExpanded by remember { mutableStateOf(false) }
    var showSelectCalendarsDialog by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var showDayEventsDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }
    var eventsForDialog by remember { mutableStateOf<List<Festivo>>(emptyList()) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showColorThemeScreen by remember { mutableStateOf(false) }
    var showGoToYearDialog by remember { mutableStateOf(false) }
    var showAddEventScreen by remember { mutableStateOf(false) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var eventToEdit by remember { mutableStateOf<Festivo?>(null) }
    var showAllEvents by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchScope.YEAR) } // Default to YEAR
    var searchResults by remember { mutableStateOf<Map<LocalDate, List<Festivo>>>(emptyMap()) }
    var showReadOnlyDialog by remember { mutableStateOf(false) }
    var eventForReadOnlyDialog by remember { mutableStateOf<Festivo?>(null) }
    var showHolidayManagerScreen by remember { mutableStateOf(false) }
    var holidayForManager by remember { mutableStateOf<Festivo?>(null) }
    var showWidgetLogScreen by remember { mutableStateOf(false) }

    val readPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
        if (isGranted) {
            scope.launch {
                viewModel.refreshAvailableCalendars()
                showSelectCalendarsDialog = true
            }
        } else {
            Toast.makeText(context, R.string.permission_calendar_select, Toast.LENGTH_LONG).show()
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAddEventScreen = true
        } else {
            Toast.makeText(context, R.string.permission_calendar_write, Toast.LENGTH_LONG).show()
        }
    }

    val launchAddEditScreen = { date: LocalDate?, event: Festivo? ->
        dateForNewEvent = date
        eventToEdit = event
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)) {
            PackageManager.PERMISSION_GRANTED -> {
                showAddEventScreen = true
            }
            else -> {
                writePermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            }
        }
    }

    val onEventClickHandler = { event: Festivo ->
        val calendar = uiState.availableCalendars.find { it.id == event.calendarId }
        if (calendar?.canModify == true) {
            launchAddEditScreen(event.date, event)
        } else {
            eventForReadOnlyDialog = event
            showReadOnlyDialog = true
        }
    }

    LaunchedEffect(uiState.hasCalendarPermission) {
        if (uiState.hasCalendarPermission) {
            viewModel.refreshData()
        }
    }

    LaunchedEffect(uiState.importedEvent) {
        uiState.importedEvent?.let { event ->
            launchAddEditScreen(event.date, event)
            viewModel.consumeImportedEvent()
        }
    }

    LaunchedEffect(searchQuery, searchScope, uiState.eventsByDate) {
        if (searchQuery.isNotBlank()) {
            delay(300) // Debounce
            val allEvents = uiState.eventsByDate.values.flatten()
            val scopeFilteredEvents = when (searchScope) {
                SearchScope.MONTH -> allEvents.filter { it.date.year == currentMonth.year && it.date.month == currentMonth.month }
                SearchScope.YEAR -> allEvents.filter { it.date.year == currentMonth.year }
                SearchScope.ALL -> allEvents
            }
            val normalizedQuery = searchQuery.unaccent().lowercase(Locale.getDefault())

            val groupedEvents = scopeFilteredEvents
                .filter { it.title.unaccent().lowercase(Locale.getDefault()).contains(normalizedQuery) }
                .groupBy {
                    when (searchScope) {
                        SearchScope.MONTH -> it.date
                        SearchScope.YEAR -> it.date.withDayOfMonth(1) // Group by month
                        SearchScope.ALL -> it.date.withDayOfYear(1)   // Group by year
                    }
                }
                .mapValues { (_, events) ->
                    events.sortedWith(compareBy({ it.date }, { it.startTime }))
                }
            searchResults = groupedEvents.toSortedMap(compareByDescending { it })

        } else {
            searchResults = emptyMap()
        }
    }

    val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
    val finalEventsToList = processEventsForDisplay(uiState.eventsByDate, currentMonth.atDay(1), showAll = if (isCurrentMonthView) showAllEvents else true)

    LaunchedEffect(finalEventsToList, showAllEvents, viewMode, isCurrentMonthView) {
        if (viewMode != CalendarViewMode.MONTHLY || finalEventsToList.isEmpty()) return@LaunchedEffect

        val targetIndex = when {
            isCurrentMonthView && !showAllEvents -> 0
            else -> finalEventsToList.indexOfFirst { (date, _) -> date >= today }.takeIf { it != -1 } ?: 0
        }

        scope.launch {
            lazyListState.animateScrollToItem(targetIndex)
        }
    }

    if (showHolidayManagerScreen) {
        HolidayManagerScreen(
            onBackPress = { 
                showHolidayManagerScreen = false 
                holidayForManager = null
            },
            onRefresh = {
                viewModel.refreshData()
            },
            initialFestivo = holidayForManager
        )
        return
    }

    if (showAddEventScreen) {
        val editableCalendars = uiState.availableCalendars.filter { it.canModify }
        val initialCalendar = remember(uiState.favoriteCalendarId, editableCalendars) {
            editableCalendars.find { it.id == uiState.favoriteCalendarId } 
            ?: editableCalendars.find { it.isPrimary && it.accountName.contains("@gmail", ignoreCase = true) }
            ?: editableCalendars.find { it.accountName.contains("@gmail", ignoreCase = true) }
            ?: editableCalendars.firstOrNull()
        }

        AddEventScreen(
            onBackPress = { showAddEventScreen = false },
            onSave = {
                showAddEventScreen = false
            },
            onDelete = {
                showAddEventScreen = false
            },
            editableCalendars = editableCalendars,
            initialDate = dateForNewEvent,
            eventToEdit = eventToEdit,
            initialCalendar = initialCalendar
        )
        return
    }

    if (showHelpScreen) {
        HelpScreen(onBackPress = { showHelpScreen = false })
        return
    }

    if (showColorThemeScreen) {
        ColorThemeScreen(
            onBackPress = { showColorThemeScreen = false },
            onThemeUpdated = onThemeUpdated
        )
        return
    }

    if (showWidgetLogScreen) {
        LogScreen(
            onBack = { showWidgetLogScreen = false }
        )
        return
    }

    if (showSettingsScreen) {
        SettingsScreen(
            onBackPress = {
                showSettingsScreen = false
                onThemeUpdated()
            },
            themeManager = themeManager,
            onColorThemeClick = { showColorThemeScreen = true },
            onHolidayManagerClick = { 
                holidayForManager = null
                showHolidayManagerScreen = true 
            },
            onLogClick = { showWidgetLogScreen = true },
            onRefreshData = {
                viewModel.refreshData()
            },
            onThemeUpdated = onThemeUpdated
        )
        return
    }

    if (isSearchActive) {
        SearchScreen(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            searchScope = searchScope,
            onSearchScopeChange = { searchScope = it },
            searchResults = searchResults,
            onClose = {
                isSearchActive = false
                searchQuery = ""
                searchResults = emptyMap()
            },
            onEventClick = onEventClickHandler,
            onRefresh = { viewModel.refreshData() },
            availableCalendars = uiState.availableCalendars
        )
    } else {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary)
                        .statusBarsPadding()
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                        val showHomeButton = viewMode == CalendarViewMode.MONTHLY && currentMonth != YearMonth.from(today)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Group
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                if (viewMode == CalendarViewMode.YEARLY || showHomeButton) {
                                    IconButton(
                                        onClick = {
                                            if (viewMode == CalendarViewMode.YEARLY) {
                                                scope.launch { monthPagerState.animateScrollToPage(initialPage) }
                                                viewMode = CalendarViewMode.MONTHLY
                                            } else if (showHomeButton) {
                                                scope.launch { monthPagerState.animateScrollToPage(initialPage) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = if (viewMode == CalendarViewMode.YEARLY) stringResource(id = R.string.back_to_monthly_view) else stringResource(id = R.string.back_to_current_month)
                                        )
                                    }
                                }
                                if (viewMode == CalendarViewMode.MONTHLY) {
                                    Text(
                                        text = currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = if (showHomeButton) 0.dp else 12.dp)
                                    )
                                }
                            }

                            // Center Group
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                                    .clickable {
                                        if (viewMode == CalendarViewMode.MONTHLY) {
                                            val targetYearPage = currentMonth.year - startYear.value
                                            scope.launch { yearPagerState.scrollToPage(targetYearPage) }
                                            viewMode = CalendarViewMode.YEARLY
                                        } else {
                                            showGoToYearDialog = true
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                val yearButtonBackgroundColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                val yearButtonTextColor = if (isColorDark(yearButtonBackgroundColor, MaterialTheme.colorScheme.primary)) Color.White else Color.Black
                                Text(
                                    text = if (viewMode == CalendarViewMode.MONTHLY) "${currentMonth.year}" else "${currentYear.value}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = yearButtonTextColor
                                )
                            }

                            // Right Group
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { launchAddEditScreen(null, null) }) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(id = R.string.create_event))
                                }
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(id = R.string.search)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, stringResource(id = R.string.menu)) }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.background(CalendarioTheme.colors.fondoDialogos)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.calendars), fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = {
                                                menuExpanded = false
                                                scope.launch {
                                                    if (uiState.hasCalendarPermission) {
                                                        viewModel.refreshAvailableCalendars()
                                                        showSelectCalendarsDialog = true
                                                    } else {
                                                        readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = stringResource(id = R.string.calendars), tint = CalendarioTheme.colors.textSystem) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.settings), fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = { menuExpanded = false; showSettingsScreen = true },
                                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings), tint = CalendarioTheme.colors.textSystem) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.help), fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = { menuExpanded = false; showHelpScreen = true },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(id = R.string.help), tint = CalendarioTheme.colors.textSystem) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = CalendarioTheme.colors.settingsBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (viewMode == CalendarViewMode.MONTHLY) {
                    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                    val effectType = prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient")
                    
                    val endColor = when (effectType) {
                        "gradient", "radial" -> CalendarioTheme.colors.monthlyCalendarGridEffect
                        else -> CalendarioTheme.colors.monthlyCalendarGridBackground
                    }

                    val monthlyCalendarGridBrush = when (effectType) {
                        "gradient" -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, endColor))
                        "sweep" -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, CalendarioTheme.colors.monthlyCalendarGridEffect, endColor))
                        "radial" -> Brush.radialGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, endColor))
                        else -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, endColor))
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = monthlyCalendarGridBrush
                            )
                            .padding(top = 16.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(
                            state = monthPagerState,
                        ) { page ->
                            val month = startMonth.plusMonths(page.toLong())
                            val startOfWeek = getActualFirstDayOfWeek(context)
                            MonthlyCalendar(
                                currentMonth = month,
                                today = today,
                                eventsByDate = uiState.eventsByDate,
                                onDayClick = { date, events ->
                                    selectedDateForDialog = date
                                    eventsForDialog = events
                                    showDayEventsDialog = true
                                },
                                onEmptyDayClick = { date ->
                                    launchAddEditScreen(date, null)
                                },
                                startOfWeek = startOfWeek
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.events_of_month, currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }),
                                fontSize = 18.sp,
                                color = CalendarioTheme.colors.eventListTitleColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            if (isCurrentMonthView) {
                                val baseColor = when (effectType) {
                                    "gradient", "radial" -> CalendarioTheme.colors.monthlyCalendarGridEffect
                                    else -> CalendarioTheme.colors.monthlyCalendarGridBackground
                                }

                                val buttonContainerColor = run {
                                    val hsl = FloatArray(3)
                                    ColorUtils.colorToHSL(baseColor.toArgb(), hsl)
                                    val isDark = hsl[2] < 0.5f
                                    hsl[2] = if (isDark) {
                                        (hsl[2] + 0.1f).coerceIn(0f, 1f)
                                    } else {
                                        (hsl[2] - 0.1f).coerceIn(0f, 1f)
                                    }
                                    Color(ColorUtils.HSLToColor(hsl))
                                }

                                val textColor = if (isColorDark(buttonContainerColor, baseColor)) Color.White else Color.Black

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(buttonContainerColor)
                                        .clickable { showAllEvents = !showAllEvents }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (showAllEvents) stringResource(id = R.string.all) else stringResource(id = R.string.pending),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(endColor) // Background for the clipping to reveal
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(CalendarioTheme.colors.settingsBackground)
                        ) {
                            MonthlyEventList(
                                modifier = Modifier.fillMaxSize(),
                                finalEventsToList = finalEventsToList,
                                lazyListState = lazyListState,
                                isCurrentMonthView = isCurrentMonthView,
                                showAllEvents = showAllEvents,
                                today = today,
                                onEventClick = onEventClickHandler
                            )

                            val showTopShadow by remember {
                                derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0 }
                            }

                            if (showTopShadow) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .align(Alignment.TopCenter)
                                        .zIndex(1f)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    CalendarioTheme.colors.settingsBackground,
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                } else { // Yearly view
                    val startOfWeek = getActualFirstDayOfWeek(context)
                    val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                    val showWeekNumber = appPrefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false)

                    HorizontalPager(
                        state = yearPagerState
                    ) { page ->
                        val year = startYear.plusYears(page.toLong())
                        YearlyCalendar(
                            currentYear = year,
                            today = today,
                            eventsByDate = uiState.eventsByDate,
                            showWeekNumber = showWeekNumber,
                            startOfWeek = startOfWeek,
                            onMonthSelected = { selectedMonth ->
                                val targetPage = ChronoUnit.MONTHS.between(startMonth, selectedMonth).toInt()
                                scope.launch { monthPagerState.scrollToPage(targetPage) }
                                viewMode = CalendarViewMode.MONTHLY
                            }
                        )
                    }
                }
            }

            if (showSelectCalendarsDialog) {
                SelectCalendarsDialog(
                    initialSelectedIds = uiState.selectedCalendarIds,
                    availableCalendars = uiState.availableCalendars,
                    favoriteCalendarId = uiState.favoriteCalendarId,
                    onDismissRequest = { showSelectCalendarsDialog = false },
                    onApplySelection = { newlySelectedIds ->
                        showSelectCalendarsDialog = false
                        scope.launch {
                            val updatedFestivosMap = readFestivosFromCalendarsSuspend(context, newlySelectedIds)
                            viewModel.updateCalendarData(updatedFestivosMap, uiState.availableCalendars, newlySelectedIds)
                        }
                    },
                    onSetFavorite = viewModel::setFavoriteCalendar
                )
            }

            if (showDayEventsDialog && selectedDateForDialog != null) {
                DayEventsDialog(
                    date = selectedDateForDialog!!,
                    events = eventsForDialog,
                    availableCalendars = uiState.availableCalendars,
                    onDismissRequest = {
                        showDayEventsDialog = false
                        selectedDateForDialog = null
                        eventsForDialog = emptyList()
                    },
                    onAddEventClick = { date ->
                        showDayEventsDialog = false
                        launchAddEditScreen(date, null)
                    },
                    onEventClick = { event ->
                        showDayEventsDialog = false
                        onEventClickHandler(event)
                    }
                )
            }

            if (showReadOnlyDialog && eventForReadOnlyDialog != null) {
                ReadOnlyEventDialog(
                    onDismissRequest = { showReadOnlyDialog = false },
                    festivo = eventForReadOnlyDialog!!,
                    calendar = uiState.availableCalendars.find { it.id == eventForReadOnlyDialog!!.calendarId },
                    onOpenHolidayManager = { festivo ->
                        holidayForManager = festivo
                        showHolidayManagerScreen = true
                    }
                )
            }

            if (showGoToYearDialog) {
                GoToYearDialog(
                    initialYear = currentYear.value,
                    onYearSelected = {
                        val targetYearPage = it - startYear.value
                        scope.launch { yearPagerState.scrollToPage(targetYearPage) }
                    },
                    onDismissRequest = { showGoToYearDialog = false }
                )
            }
        }
    }
}