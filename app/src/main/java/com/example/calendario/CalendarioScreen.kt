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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val readPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
        if (isGranted) {
            showSelectCalendarsDialog = true
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
    val finalEventsToList = processEventsForDisplay(uiState.eventsByDate, currentMonth, today, showAll = if (isCurrentMonthView) showAllEvents else true)

    LaunchedEffect(finalEventsToList, showAllEvents, viewMode, isCurrentMonthView) {
        if (viewMode != CalendarViewMode.MONTHLY || finalEventsToList.isEmpty()) return@LaunchedEffect

        val targetIndex = when {
            isCurrentMonthView && showAllEvents ->
                finalEventsToList.indexOfFirst { (date, _) -> date >= today }.takeIf { it != -1 } ?: 0

            isCurrentMonthView && !showAllEvents -> 0

            else -> 0
        }

        scope.launch {
            lazyListState.animateScrollToItem(targetIndex)
        }
    }

    if (showAddEventScreen) {
        AddEventScreen(
            onBackPress = { showAddEventScreen = false },
            onSave = {
                showAddEventScreen = false
                scope.launch {
                    delay(1500)
                    viewModel.refreshData()
                }
            },
            onDelete = {
                showAddEventScreen = false
                scope.launch {
                    delay(1500)
                    viewModel.refreshData()
                }
            },
            editableCalendars = uiState.availableCalendars.filter { it.canModify },
            initialDate = dateForNewEvent,
            eventToEdit = eventToEdit
        )
        return
    }

    if (showHelpScreen) {
        HelpScreen(onBackPress = { showHelpScreen = false })
        return
    }

    if (showColorThemeScreen) {
        ColorThemeScreen(
            onBackPress = { 
                showColorThemeScreen = false 
                onThemeUpdated()
            }
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
                                        modifier = Modifier.background(CalendarioTheme.colors.dropdownMenuBackground)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.calendars), fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = {
                                                menuExpanded = false
                                                if (uiState.hasCalendarPermission) {
                                                    showSelectCalendarsDialog = true
                                                } else {
                                                    readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
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
            containerColor = if (viewMode == CalendarViewMode.YEARLY) CalendarioTheme.colors.settingsBackground else MaterialTheme.colorScheme.background
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
                    val monthlyCalendarGridBrush = when (effectType) {
                        "gradient" -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, CalendarioTheme.colors.monthlyCalendarGridEffect))
                        "sweep" -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, CalendarioTheme.colors.monthlyCalendarGridEffect, CalendarioTheme.colors.monthlyCalendarGridBackground))
                        else -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, CalendarioTheme.colors.monthlyCalendarGridBackground))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = monthlyCalendarGridBrush,
                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            )
                            .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 16.dp),
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
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
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
                            val headerColor = CalendarioTheme.colors.cabecera
                            val backgroundColor = CalendarioTheme.colors.background
                            val buttonContainerColor = if (showAllEvents) {
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

                            val textColor = if (showAllEvents) {
                                if (isColorDark(buttonContainerColor, backgroundColor)) Color.White else Color.Black
                            } else {
                                CalendarioTheme.colors.textSystem
                            }
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
                                    color = textColor
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
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
                                                MaterialTheme.colorScheme.background,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
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
                    onDismissRequest = { showSelectCalendarsDialog = false }
                ) { newlySelectedIds ->
                    showSelectCalendarsDialog = false
                    scope.launch {
                        val updatedFestivosMap = readFestivosFromCalendarsSuspend(context, newlySelectedIds, uiState.availableCalendars)
                        viewModel.updateCalendarData(updatedFestivosMap, uiState.availableCalendars, newlySelectedIds)
                    }
                }
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
                    calendar = uiState.availableCalendars.find { it.id == eventForReadOnlyDialog!!.calendarId }
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