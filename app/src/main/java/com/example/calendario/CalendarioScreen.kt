package com.example.calendario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CalendarViewMode { MONTHLY, YEARLY }
enum class SearchScope { MONTH, YEAR, ALL }

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()
fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarioScreen(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    eventsByDateExternal: Map<LocalDate, List<Festivo>>,
    availableCalendarsExternal: List<CalendarInfo>,
    selectedCalendarIdsExternal: Set<Long>,
    hasCalendarPermissionExternal: Boolean,
    onRefreshRequest: () -> Unit,
    onCalendarDataUpdated: (Map<LocalDate, List<Festivo>>, List<CalendarInfo>, Set<Long>) -> Unit,
    onPermissionUpdated: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var showDayEventsDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }
    var eventsForDialog by remember { mutableStateOf<List<Festivo>>(emptyList()) }
    var showOptionsScreen by remember { mutableStateOf(false) }
    var showGoToYearDialog by remember { mutableStateOf(false) }
    var showAddEventScreen by remember { mutableStateOf(false) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var eventToEdit by remember { mutableStateOf<Festivo?>(null) }
    var showAllEvents by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var homeClickTrigger by remember { mutableIntStateOf(0) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchScope.ALL) }
    var settingsUpdateTrigger by remember { mutableIntStateOf(0) }

    val todayHighlightColor = remember(settingsUpdateTrigger, isDarkTheme) {
        val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val colorInt = prefs.getInt(AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, Color(0xFFE9E9E9).toArgb())
        val baseColor = Color(colorInt)
        if (isDarkTheme) baseColor.copy(alpha = 0.5f) else baseColor
    }

    val readPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionUpdated(isGranted)
        if (isGranted) {
            showSelectCalendarsDialog = true
        } else {
            Toast.makeText(context, "Permiso de calendario necesario para seleccionar calendarios.", Toast.LENGTH_LONG).show()
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showAddEventScreen = true
            } else {
                Toast.makeText(context, "Permiso para escribir en el calendario es necesario para crear eventos.", Toast.LENGTH_LONG).show()
            }
        }
    )

    val launchAddEditScreenWithPermissionCheck = { date: LocalDate?, event: Festivo? ->
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

    LaunchedEffect(hasCalendarPermissionExternal) {
        if (hasCalendarPermissionExternal) {
            onRefreshRequest()
        }
    }

    val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
    val finalEventsToList = processEventsForDisplay(eventsByDateExternal, currentMonth, today, showAll = if (isCurrentMonthView) showAllEvents else true)

    val searchResults = remember(searchQuery, searchScope, eventsByDateExternal) {
        if (searchQuery.isBlank()) {
            emptyMap()
        } else {
            val allEvents = eventsByDateExternal.values.flatten()
            val scopeFilteredEvents = when (searchScope) {
                SearchScope.MONTH -> allEvents.filter { it.date.year == currentMonth.year && it.date.month == currentMonth.month }
                SearchScope.YEAR -> allEvents.filter { it.date.year == currentMonth.year }
                SearchScope.ALL -> allEvents
            }
            val normalizedQuery = searchQuery.unaccent().lowercase(Locale.getDefault())
            scopeFilteredEvents
                .filter { it.title.unaccent().lowercase(Locale.getDefault()).contains(normalizedQuery) }
                .groupBy { it.date }
                .toSortedMap(compareByDescending { it })
        }
    }

    LaunchedEffect(finalEventsToList, showAllEvents, viewMode, homeClickTrigger) {
        if (viewMode == CalendarViewMode.MONTHLY && finalEventsToList.isNotEmpty()) {
            val targetIndex = if (showAllEvents) {
                val targetDate = if (isCurrentMonthView) today else currentMonth.atDay(1)
                finalEventsToList.indexOfFirst { (date, _) -> date >= targetDate }.takeIf { it != -1 } ?: 0
            } else {
                0
            }
            scope.launch {
                lazyListState.animateScrollToItem(targetIndex)
            }
        }
    }

    if (showAddEventScreen) {
        AddEventScreen(
            onBackPress = { showAddEventScreen = false },
            onSave = {
                showAddEventScreen = false
                scope.launch {
                    delay(1500)
                    onRefreshRequest()
                }
            },
            onDelete = {
                showAddEventScreen = false
                scope.launch {
                    delay(1500)
                    onRefreshRequest()
                }
            },
            editableCalendars = availableCalendarsExternal.filter { it.canModify },
            isDarkTheme = isDarkTheme,
            initialDate = dateForNewEvent,
            eventToEdit = eventToEdit
        )
        return
    }
    
    if (showHelpScreen) {
        HelpScreen(onBackPress = { showHelpScreen = false })
        return
    }

    if (showOptionsScreen) {
        OptionsScreen(
            onBackPress = { 
                showOptionsScreen = false 
                settingsUpdateTrigger++
            },
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle
        )
        return
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar eventos...", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)) },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp),
                                singleLine = true,
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Cerrar búsqueda",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
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
                } else {
                    TopAppBar(
                        title = { Text("Calendario Visual", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menú", tint = MaterialTheme.colorScheme.onPrimary) }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.background(
                                        if (isDarkTheme) AppThemeSetup.DarkColors.dropdownMenuBackground else AppThemeSetup.LightColors.dropdownMenuBackground
                                    )
                                ) {
                                    val dropdownTextColor = if (isDarkTheme) AppThemeSetup.DarkColors.onScreenTextNormal else AppThemeSetup.LightColors.onScreenTextNormal
                                    DropdownMenuItem(
                                        text = { Text("Calendarios", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                        onClick = {
                                            menuExpanded = false
                                            if (hasCalendarPermissionExternal) {
                                                showSelectCalendarsDialog = true
                                            } else {
                                                readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Opciones", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                        onClick = { menuExpanded = false; showOptionsScreen = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                        onClick = { menuExpanded = false; showHelpScreen = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Acerca de", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                        onClick = { menuExpanded = false; showAboutDialog = true }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        if (isSearchActive) {
            Column(modifier = Modifier.padding(paddingValues)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SearchScope.entries.forEach { scope ->
                        val text = when (scope) {
                            SearchScope.MONTH -> "Mes actual"
                            SearchScope.YEAR -> "Año actual"
                            SearchScope.ALL -> "Todos"
                        }
                        if (searchScope == scope) {
                            Button(
                                onClick = { searchScope = scope },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else AppThemeSetup.LightColors.monthlyCalendarEmptyCellBackground,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text(text) }
                        } else {
                            TextButton(onClick = { searchScope = scope }) { Text(text) }
                        }
                    }
                }
                if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se han encontrado resultados")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        searchResults.forEach { (date, events) ->
                            stickyHeader {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy")),
                                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(events) {
                                festivo ->
                                val esCumpleanos = festivo.title.contains("cumpleaños", true) || festivo.title.contains("aniversario", true)
                                val itemColor = when {
                                    esCumpleanos -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemBirthdayText else AppThemeSetup.LightColors.eventListItemBirthdayText
                                    festivo.isFromHolidaySource -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemHolidayText else AppThemeSetup.LightColors.eventListItemHolidayText
                                    else -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemDefaultText else AppThemeSetup.LightColors.eventListItemDefaultText
                                }
                                val displayDesc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { "(Sin título)" }}"
                                else festivo.title.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { launchAddEditScreenWithPermissionCheck(festivo.date, festivo) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    availableCalendarsExternal.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .background(Color(colorInt), CircleShape)
                                                .border(0.5.dp, if (isDarkTheme) AppThemeSetup.DarkColors.dialogCalendarColorIndicatorBorder else AppThemeSetup.LightColors.dialogCalendarColorIndicatorBorder, CircleShape)
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
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (viewMode == CalendarViewMode.MONTHLY) {
                                if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarGridBackground else AppThemeSetup.LightColors.monthlyCalendarGridBackground
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                viewMode = CalendarViewMode.MONTHLY
                                scope.launch {
                                    monthPagerState.animateScrollToPage(initialPage)
                                }
                                homeClickTrigger++
                            },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.Home, contentDescription = "Hoy")
                        }
                        Button(
                            onClick = { 
                                if (viewMode == CalendarViewMode.MONTHLY) {
                                    val targetYearPage = currentMonth.year - startYear.value
                                    scope.launch { yearPagerState.scrollToPage(targetYearPage) }
                                    viewMode = CalendarViewMode.YEARLY
                                } else {
                                    showGoToYearDialog = true
                                } 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                if (viewMode == CalendarViewMode.MONTHLY) "${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}" else "${currentYear.value}",
                                fontSize = 20.sp
                            )
                        }
                        FilledIconButton(
                            onClick = { launchAddEditScreenWithPermissionCheck(null, null) },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Crear evento")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewMode == CalendarViewMode.MONTHLY) {
                        HorizontalPager(
                            state = monthPagerState,
                        ) { page ->
                            val month = startMonth.plusMonths(page.toLong())
                            MonthlyCalendar(
                                currentMonth = month,
                                today = today,
                                eventsByDate = eventsByDateExternal,
                                isDarkTheme = isDarkTheme,
                                onDayClick = { date, events ->
                                    selectedDateForDialog = date
                                    eventsForDialog = events
                                    showDayEventsDialog = true
                                },
                                onEmptyDayClick = { date ->
                                    launchAddEditScreenWithPermissionCheck(date, null)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val titleColor = if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarHeaderText else AppThemeSetup.LightColors.monthlyCalendarHeaderText
                            Text(
                                text = "Eventos de ${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}",
                                fontSize = 18.sp,
                                color = titleColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            if (isCurrentMonthView) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDarkTheme) AppThemeSetup.DarkColors.filterButtonBackground else Color(0xFFC1D7F2))
                                        .clickable { showAllEvents = !showAllEvents }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (showAllEvents) "Todos" else "Pendientes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = titleColor
                                    )
                                }
                            }
                        }

                    } else { // Vista Anual
                        HorizontalPager(
                            state = yearPagerState
                        ) { page ->
                            val year = startYear.plusYears(page.toLong())
                            YearlyCalendar(
                                currentYear = year,
                                today = today,
                                eventsByDate = eventsByDateExternal,
                                isDarkTheme = isDarkTheme,
                                onMonthSelected = { selectedMonth ->
                                    val targetPage = ChronoUnit.MONTHS.between(startMonth, selectedMonth).toInt()
                                    scope.launch { monthPagerState.scrollToPage(targetPage) }
                                    viewMode = CalendarViewMode.MONTHLY
                                }
                            )
                        }
                    }
                }

                if (viewMode == CalendarViewMode.MONTHLY) {
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(MaterialTheme.colorScheme.background)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        if (finalEventsToList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isCurrentMonthView && !showAllEvents) "No hay eventos pendientes para este mes." else "No hay eventos para este mes.",
                                    fontSize = 16.sp,
                                    color = if (isDarkTheme) AppThemeSetup.DarkColors.onScreenTextSecondary else AppThemeSetup.LightColors.onScreenTextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                            ) {
                                itemsIndexed(finalEventsToList, key = { _, (date, festivos) -> date.toString() + festivos.firstOrNull()?.id }) { _, (date, festivos) ->
                                    val isTodayEvents = isCurrentMonthView && date == today
                                    festivos.forEach { festivo ->
                                        val esCumpleanos = festivo.title.contains("cumpleaños", true) || festivo.title.contains("aniversario", true)
                                        val itemColor = when {
                                            esCumpleanos -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemBirthdayText else AppThemeSetup.LightColors.eventListItemBirthdayText
                                            festivo.isFromHolidaySource -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemHolidayText else AppThemeSetup.LightColors.eventListItemHolidayText
                                            else -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemDefaultText else AppThemeSetup.LightColors.eventListItemDefaultText
                                        }
                                        val textColorForToday = if (isTodayEvents) Color.Black else itemColor
                                        val dayNumberColorForToday = if (isTodayEvents) Color.Black else MaterialTheme.colorScheme.onSurface
                                        val refreshIconColor = if (isTodayEvents) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                                        val displayDesc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { "(Sin título)" }}"
                                        else festivo.title.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }

                                        if (displayDesc.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .then(
                                                            if (isTodayEvents) {
                                                                Modifier.background(todayHighlightColor)
                                                            } else {
                                                                Modifier
                                                            }
                                                        )
                                                        .clickable { launchAddEditScreenWithPermissionCheck(festivo.date, festivo) }
                                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            String.format(Locale.getDefault(), "%02d", date.dayOfMonth),
                                                            color = dayNumberColorForToday,
                                                            fontWeight = if (isTodayEvents) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 16.sp
                                                        )
                                                        Text(
                                                            displayDesc,
                                                            color = textColorForToday,
                                                            fontSize = 16.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                    if (festivo.rrule != null) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Evento repetido",
                                                            tint = refreshIconColor,
                                                            modifier = Modifier
                                                                .padding(start = 8.dp)
                                                                .size(16.dp)
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
                }
            }
        }
        
        if (showSelectCalendarsDialog) {
            SelectCalendarsDialog(
                initialSelectedIds = selectedCalendarIdsExternal,
                availableCalendars = availableCalendarsExternal,
                onDismissRequest = { showSelectCalendarsDialog = false }
            ) { newlySelectedIds ->
                showSelectCalendarsDialog = false
                scope.launch {
                    try {
                        val updatedFestivosMap = readFestivosFromCalendarsSuspend(context, newlySelectedIds, availableCalendarsExternal)
                        onCalendarDataUpdated(updatedFestivosMap, availableCalendarsExternal, newlySelectedIds)
                    } catch (e: Exception) {
                        Log.e("CalendarioScreen", "Error aplicando selección de calendarios: ${e.localizedMessage}", e)
                        Toast.makeText(context, "Error al aplicar selección.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                text = { Column { Text("Calendario Visual V1.43", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Asistente IA / Android Studio", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Onso/agosto 2025", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
            )
        }
        
        if (showDayEventsDialog && selectedDateForDialog != null) {
            DayEventsDialog(
                date = selectedDateForDialog!!,
                events = eventsForDialog,
                availableCalendars = availableCalendarsExternal,
                isDarkTheme = isDarkTheme,
                todayHighlightColor = todayHighlightColor,
                onDismissRequest = { 
                    showDayEventsDialog = false
                    selectedDateForDialog = null
                    eventsForDialog = emptyList()
                },
                onAddEventClick = { date ->
                    showDayEventsDialog = false
                    launchAddEditScreenWithPermissionCheck(date, null)
                },
                onEventClick = { event ->
                    showDayEventsDialog = false
                    launchAddEditScreenWithPermissionCheck(event.date, event)
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