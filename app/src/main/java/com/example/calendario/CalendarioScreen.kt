package com.example.calendario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

enum class CalendarViewMode { MONTHLY, YEARLY }
enum class SearchScope { MONTH, YEAR, ALL }


@Composable
private fun getActualFirstDayOfWeek(context: Context): DayOfWeek {
    val locale = LocalConfiguration.current.locales[0]
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val startOfWeekKey = prefs.getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key) ?: StartOfWeekOption.SYSTEM.key
    return when (StartOfWeekOption.fromKey(startOfWeekKey)) {
        StartOfWeekOption.SYSTEM -> WeekFields.of(locale).firstDayOfWeek
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
    val locale = LocalConfiguration.current.locales[0]
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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
    var searchResults by remember { mutableStateOf<Map<LocalDate, List<SearchItem>>>(emptyMap()) }
    var showReadOnlyDialog by remember { mutableStateOf(false) }
    var eventForReadOnlyDialog by remember { mutableStateOf<Festivo?>(null) }
    var showHolidayManagerScreen by remember { mutableStateOf(false) }
    var showWidgetLogScreen by remember { mutableStateOf(false) }
    var showBackupHistoryScreen by remember { mutableStateOf(false) }
    var showDeleteOrphanDialog by remember { mutableStateOf(false) }
    var eventToDeleteOrphan by remember { mutableStateOf<Festivo?>(null) }
    var holidayForManager by remember { mutableStateOf<Festivo?>(null) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showManageCalendarsScreen by remember { mutableStateOf(false) }
    var showBackupScreen by remember { mutableStateOf(false) }

    // --- LÓGICA DE PUNTO DE PERMISOS (Sincronizada con SettingsScreen) ---
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionsUpdateTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionsUpdateTrigger++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionPointColor by remember(permissionsUpdateTrigger) {
        derivedStateOf {
            val calStatus = PermissionChecker.getCalendarStatus(context)
            val notifStatus = PermissionChecker.getNotificationsStatus(context)
            val alarmStatus = PermissionChecker.getAlarmsStatus(context)
            val driveStatus = PermissionChecker.getGoogleDriveStatus(context)
            val batteryStatus = PermissionChecker.getBatteryOptimizationStatus(context)

            when {
                calStatus == PermissionStatus.DENIED -> Color.Red
                notifStatus == PermissionStatus.DENIED || alarmStatus == PermissionStatus.DENIED || 
                driveStatus == PermissionStatus.DENIED || batteryStatus == PermissionStatus.DENIED -> Color(0xFFFFA500)
                else -> Color.Green
            }
        }
    }

    val calendarPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionResult(allGranted)
        if (allGranted) {
            scope.launch {
                viewModel.refreshAvailableCalendars()
                showSelectCalendarsDialog = true
            }
        } else {
            Toast.makeText(context, R.string.permission_calendar_select, Toast.LENGTH_LONG).show()
        }
    }

    val launchAddEditScreen = { date: LocalDate?, event: Festivo? ->
        dateForNewEvent = date
        eventToEdit = event
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        
        if (hasRead && hasWrite) {
            showAddEventScreen = true
        } else {
            calendarPermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
        }
    }

    val onEventClickHandler = { event: Festivo ->
        val calendar = uiState.availableCalendars.find { it.id == event.calendarId }
        val canEdit = calendar?.canModify == true && !event.isGhost

        if (event.calendarId == -1L && event.isFromHolidaySource) {
            // Es un festivo local (manual) -> Abrir Gestor
            holidayForManager = event
            showHolidayManagerScreen = true
        } else if (event.isGhost) {
            // Es un evento fantasma (está en historial pero no en Google)
            eventForReadOnlyDialog = event
            showReadOnlyDialog = true
        } else if (event.isBirthday && !canEdit) {
            // Es un cumpleaños de SOLO LECTURA (sincronizado de contactos)
            eventForReadOnlyDialog = event
            showReadOnlyDialog = true
        } else if (canEdit) {
            // Es un evento normal o un cumpleaños en un calendario propio -> Editable
            launchAddEditScreen(event.date, event)
        } else {
            // Todo lo demás que no sea editable (calendarios compartidos lectura, etc.)
            eventForReadOnlyDialog = event
            showReadOnlyDialog = true
        }
    }

    val onNoteClickHandler = { note: DailyNote ->
        val date = note.date
        val targetPage = ChronoUnit.MONTHS.between(startMonth, YearMonth.from(date)).toInt()
        scope.launch {
            monthPagerState.scrollToPage(targetPage)
            selectedDateForDialog = date
            showDayEventsDialog = true
            isSearchActive = false
        }
        Unit
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

    LaunchedEffect(searchQuery, searchScope, uiState.eventsByDate, uiState.dailyNotes) {
        if (searchQuery.isNotBlank()) {
            delay(300.milliseconds) // Debounce
            val normalizedQuery = searchQuery.unaccent().lowercase(locale)
            
            // 1. Filtrar Eventos
            val allEvents = uiState.eventsByDate.values.flatten()
            val filteredEvents = allEvents.filter { it.title.unaccent().lowercase(locale).contains(normalizedQuery) }
                .filter { event ->
                    when (searchScope) {
                        SearchScope.MONTH -> event.date.year == currentMonth.year && event.date.month == currentMonth.month
                        SearchScope.YEAR -> event.date.year == currentMonth.year
                        SearchScope.ALL -> true
                    }
                }
                .map { SearchItem.Event(it) }

            // 2. Filtrar Notas
            val filteredNotes = uiState.dailyNotes.values.filter { it.content.unaccent().lowercase(locale).contains(normalizedQuery) }
                .filter { note ->
                    when (searchScope) {
                        SearchScope.MONTH -> note.date.year == currentMonth.year && note.date.month == currentMonth.month
                        SearchScope.YEAR -> note.date.year == currentMonth.year
                        SearchScope.ALL -> true
                    }
                }
                .map { SearchItem.Note(it) }

            // 3. Combinar y Agrupar
            val combined = (filteredEvents + filteredNotes)
            val grouped = combined.groupBy {
                when (searchScope) {
                    SearchScope.MONTH -> it.date
                    SearchScope.YEAR -> it.date.withDayOfMonth(1)
                    SearchScope.ALL -> it.date.withDayOfYear(1)
                }
            }.mapValues { (_, items) ->
                items.sortedWith(compareBy({ it.date }, { (it as? SearchItem.Event)?.festivo?.startTime }))
            }

            searchResults = grouped.toSortedMap(compareByDescending { it })
        } else {
            searchResults = emptyMap()
        }
    }

    val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
    val finalEventsToList = remember(uiState.eventsByDate, currentMonth, showAllEvents) {
        processEventsForDisplay(uiState.eventsByDate, currentMonth.atDay(1), showAll = if (isCurrentMonthView) showAllEvents else true)
    }

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
                viewModel.refreshData()
            },
            onDelete = {
                showAddEventScreen = false
                viewModel.refreshData()
            },
            editableCalendars = editableCalendars,
            initialDate = dateForNewEvent,
            eventToEdit = eventToEdit,
            initialCalendar = initialCalendar,
            eventsByDate = uiState.eventsByDate
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
            onThemeModified = onThemeUpdated
        )
        return
    }

    if (showWidgetLogScreen) {
        LogScreen(
            onBack = { showWidgetLogScreen = false }
        )
        return
    }

    if (showHistoryScreen) {
        HistoryScreen(onBack = { showHistoryScreen = false })
        return
    }

    if (showBackupHistoryScreen) {
        BackupHistoryScreen(onBack = { showBackupHistoryScreen = false })
        return
    }

    if (showBackupScreen) {
        BackupScreen(
            onBackPress = { showBackupScreen = false },
            viewModel = viewModel,
            onHistoryClick = { showBackupHistoryScreen = true },
            onNavigateToDate = { date ->
                val targetPage = ChronoUnit.MONTHS.between(startMonth, YearMonth.from(date)).toInt()
                scope.launch {
                    monthPagerState.scrollToPage(targetPage)
                    selectedDateForDialog = date
                    showDayEventsDialog = true
                }
            }
        )
        return
    }

    if (showManageCalendarsScreen) {
        ManageCalendarsScreen(
            onBackPress = { showManageCalendarsScreen = false },
            availableCalendars = uiState.availableCalendars,
            initialSelectedIds = uiState.selectedCalendarIds,
            favoriteCalendarId = uiState.favoriteCalendarId,
            onApplySelection = { newlySelectedIds ->
                scope.launch {
                    val updatedFestivosMap = readFestivosFromCalendarsSuspend(context, newlySelectedIds)
                    viewModel.updateCalendarData(updatedFestivosMap, uiState.availableCalendars, newlySelectedIds)
                }
            },
            onSetFavorite = viewModel::setFavoriteCalendar
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
            viewModel = viewModel,
            onColorThemeClick = { showColorThemeScreen = true },
            onHistoryClick = { showHistoryScreen = true },
            onLogClick = { showWidgetLogScreen = true },
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
            onNoteClick = onNoteClickHandler,
            onDeleteNote = viewModel::deleteDailyNote,
            onOpenHolidayManager = { clicked ->
                holidayForManager = clicked
                showHolidayManagerScreen = true
            },
            onRefresh = { viewModel.refreshData() },
            availableCalendars = uiState.availableCalendars
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = CalendarioTheme.colors.settingsBackground,
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    Text(
                        stringResource(id = R.string.app_name),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CalendarioTheme.colors.cabecera
                    )
                    
                    val isBgDark = androidx.core.graphics.ColorUtils.calculateLuminance(CalendarioTheme.colors.settingsBackground.toArgb()) < 0.5
                    val selectedAlpha = 0.12f

                    val baseSelectedColor = if (isBgDark) {
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(CalendarioTheme.colors.cabecera.toArgb(), hsl)
                        hsl[2] = (hsl[2] + 0.15f).coerceAtMost(1f) // Aumento del 15% en luminosidad
                        Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
                    } else {
                        CalendarioTheme.colors.cabecera
                    }

                    val drawerItemColors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = baseSelectedColor.copy(alpha = selectedAlpha),
                        selectedIconColor = CalendarioTheme.colors.textSystem,
                        selectedTextColor = CalendarioTheme.colors.textSystem,
                        unselectedIconColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.9f),
                        unselectedTextColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.9f)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.monthly_view)) },
                        selected = viewMode == CalendarViewMode.MONTHLY,
                        onClick = { 
                            viewMode = CalendarViewMode.MONTHLY
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.CalendarMonth, null) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.yearly_view)) },
                        selected = viewMode == CalendarViewMode.YEARLY,
                        onClick = { 
                            viewMode = CalendarViewMode.YEARLY
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.CalendarToday, null) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                    )

                    // --- SECCIÓN: CALENDARIOS (Fase 1 - v3.1.05) ---
                    val favoriteId = uiState.favoriteCalendarId
                    val selectedIds = uiState.selectedCalendarIds
                    val availableCalendars = uiState.availableCalendars

                    // 1. Calendario Favorito (Google Principal) - SIEMPRE ARRIBA
                    val favoriteCalendar = availableCalendars.find { it.id == favoriteId }
                    if (favoriteCalendar != null) {
                        CalendarDrawerItem(
                            calendar = favoriteCalendar,
                            isSelected = selectedIds.contains(favoriteCalendar.id),
                            isFavorite = true,
                            onToggle = { 
                                // REGLA FASE-1 (Mejorada): Al pulsar el favorito, abrimos gestión
                                showManageCalendarsScreen = true
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    // 2. Resto de calendarios VISIBLES (Marcados por el usuario)
                    val otherSelectedCalendars = availableCalendars.filter { 
                        it.id != favoriteId && selectedIds.contains(it.id) 
                    }
                    otherSelectedCalendars.forEach { calendar ->
                        CalendarDrawerItem(
                            calendar = calendar,
                            isSelected = true,
                            isFavorite = false,
                            onToggle = { 
                                val newSet = selectedIds - it
                                viewModel.updateCalendarData(uiState.eventsByDate, availableCalendars, newSet)
                            }
                        )
                    }

                    // 3. Opción "Todos los calendarios" (Almacén)
                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.other_calendars)) },
                        selected = false,
                        onClick = { 
                            showManageCalendarsScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.Event, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                    )

                    // --- SECCIÓN: HERRAMIENTAS (Fase 1.7 - v3.1.06) ---
                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.holiday_manager_title)) },
                        selected = false,
                        onClick = { 
                            showHolidayManagerScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.BeachAccess, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.backup_section_title_label)) },
                        selected = false,
                        onClick = { 
                            showBackupScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_cloud_backup_outlined), contentDescription = null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), modifier = Modifier.size(24.dp)) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                    )

                    // --- SECCIÓN: OTROS (Ajustes, Ayuda) ---
                    NavigationDrawerItem(
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.settings), modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(8.dp)
                                        .background(permissionPointColor, CircleShape)
                                )
                            }
                        },
                        selected = false,
                        onClick = { 
                            showSettingsScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.Settings, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.help)) },
                        selected = false,
                        onClick = { 
                            showHelpScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .statusBarsPadding()
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Group: Menu + Dynamic Title
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, stringResource(id = R.string.menu))
                                    }
                                    
                                    val titleText = remember(currentMonth, currentYear, viewMode, today, locale) {
                                        if (viewMode == CalendarViewMode.YEARLY) {
                                            currentYear.value.toString()
                                        } else {
                                            if (currentMonth.year == today.year) {
                                                currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, locale)
                                                    .replaceFirstChar { it.uppercase(locale) }
                                            } else {
                                                val monthShort = currentMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, locale)
                                                    .replaceFirstChar { it.uppercase(locale) }
                                                "$monthShort ${currentMonth.year}"
                                            }
                                        }
                                    }

                                    Text(
                                        text = titleText,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clickable(enabled = viewMode == CalendarViewMode.YEARLY) {
                                                showGoToYearDialog = true
                                            }
                                    )

                                    val isAtToday = if (viewMode == CalendarViewMode.MONTHLY) {
                                        currentMonth == YearMonth.from(today)
                                    } else {
                                        currentYear == Year.from(today)
                                    }

                                    if (!isAtToday) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (viewMode == CalendarViewMode.MONTHLY) {
                                                        monthPagerState.animateScrollToPage(initialPage)
                                                    } else {
                                                        yearPagerState.animateScrollToPage(initialYearPage)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.padding(start = 12.dp).size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_undo_return),
                                                contentDescription = stringResource(id = R.string.back_to_current_month),
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }

                                // Right Group: Add + Search + More
                                Row(
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
                            "gradient" -> CalendarioTheme.colors.monthlyCalendarGridEffect
                            else -> CalendarioTheme.colors.monthlyCalendarGridBackground
                        }

                        val monthlyCalendarGridBrush = when (effectType) {
                            "gradient" -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, endColor))
                            "sweep" -> Brush.verticalGradient(listOf(CalendarioTheme.colors.monthlyCalendarGridBackground, CalendarioTheme.colors.monthlyCalendarGridEffect, endColor))
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
                                        selectedDateForDialog = date
                                        eventsForDialog = emptyList()
                                        showDayEventsDialog = true
                                    },
                                    startOfWeek = startOfWeek,
                                    availableCalendars = uiState.availableCalendars,
                                    dailyNotes = uiState.dailyNotes,
                                    workingDayDates = uiState.workingDayDates
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            if (isCurrentMonthView) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (showAllEvents) 180f else 0f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "rotateArrow"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { showAllEvents = !showAllEvents }) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardDoubleArrowUp,
                                            contentDescription = if (showAllEvents) stringResource(id = R.string.all) else stringResource(id = R.string.pending),
                                            tint = if (showAllEvents) Color.White else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .graphicsLayer { rotationZ = rotation }
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(28.dp))
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
                                    onEventClick = onEventClickHandler,
                                    availableCalendars = uiState.availableCalendars
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
                                },
                                workingDayDates = uiState.workingDayDates
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS GLOBALES (Disponibles tanto en calendario como en búsqueda) ---
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
            note = uiState.dailyNotes[selectedDateForDialog.toString()],
            onSaveNote = { content ->
                viewModel.saveDailyNote(selectedDateForDialog!!, content)
            },
            onDeleteNote = {
                viewModel.deleteDailyNote(selectedDateForDialog!!)
            },
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
            },
            onRemoveFromHistory = { festivo ->
                eventToDeleteOrphan = festivo
                showDeleteOrphanDialog = true
            }
        )
    }

    if (showDeleteOrphanDialog && eventToDeleteOrphan != null) {
        ConfirmDeleteDialog(
            onDismissRequest = { 
                showDeleteOrphanDialog = false
                eventToDeleteOrphan = null
            },
            onConfirm = {
                val event = eventToDeleteOrphan!!
                viewModel.removeOrphanEvent(event)
                
                // Mostrar Toast unificado
                val fmt = DateTimeFormatter.ofPattern("d/M/yy")
                val dateStr = event.date.format(fmt)
                val displayTitle = if (event.title.length > 60) event.title.take(57) + "..." else event.title
                val message = context.applicationContext.getString(R.string.event_deleted_message, dateStr, displayTitle)
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()

                showDeleteOrphanDialog = false
                eventToDeleteOrphan = null
                showReadOnlyDialog = false // Cerrar también la ficha
            },
            title = eventToDeleteOrphan!!.title,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ghost_24),
                    contentDescription = null,
                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
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

@Composable
private fun CalendarDrawerItem(
    calendar: CalendarInfo,
    isSelected: Boolean,
    isFavorite: Boolean,
    onToggle: (Long) -> Unit
) {
    NavigationDrawerItem(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = calendar.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isFavorite) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.cabecera, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        selected = false,
        onClick = { onToggle(calendar.id) },
        icon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CalendarioTheme.colors.cabecera
                )
            } else {
                Spacer(Modifier.size(24.dp)) // Espacio para mantener alineación si no hay check
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
