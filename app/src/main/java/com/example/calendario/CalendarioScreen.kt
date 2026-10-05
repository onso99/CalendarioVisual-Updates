package com.example.calendario

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import kotlin.time.Duration.Companion.milliseconds

enum class CalendarViewMode { MONTHLY, YEARLY }


@Composable
private fun getActualFirstDayOfWeek(context: Context): DayOfWeek {
    val locale = LocalConfiguration.current.locales[0]
    return when (SettingsManager.getStartOfWeek(context)) {
        StartOfWeekOption.SYSTEM -> WeekFields.of(locale).firstDayOfWeek
        StartOfWeekOption.MONDAY -> DayOfWeek.MONDAY
        StartOfWeekOption.SUNDAY -> DayOfWeek.SUNDAY
        StartOfWeekOption.SATURDAY -> DayOfWeek.SATURDAY
    }
}

@Composable
fun CalendarioScreen(
    themeManager: ThemeManager,
    onThemeUpdated: () -> Unit,
    viewModel: CalendarioViewModel,
    darkTheme: Boolean
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
    var updateCheckTrigger by remember { mutableIntStateOf(0) }
    var showStartupRecoveryDialog by remember { mutableStateOf(SettingsManager.hasStartupCrashFlag(context)) }
    var lastCrashMessage by remember { mutableStateOf(SettingsManager.getLastCrashMessage(context)) }
    var isRepairingDatabase by remember { mutableStateOf(false) }
    
    var showCleaningDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    
    var isScanningCleaning by remember { mutableStateOf(false) }
    var cleaningStatusMessage by remember { mutableStateOf<String?>(null) }
    val analyzingDataMsg = stringResource(id = R.string.analyzing_data)
    val analysisFinishedMsg = stringResource(id = R.string.analysis_finished)

    var isSyncingDrive by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    val syncingMsg = "Sincronizando con Drive..."
    val syncFinishedMsg = "Sincronización completada"

    LaunchedEffect(isScanningCleaning) {
        if (isScanningCleaning) {
            cleaningStatusMessage = analyzingDataMsg
            viewModel.refreshData {
                isScanningCleaning = false
                cleaningStatusMessage = analysisFinishedMsg
            }
        }
    }

    LaunchedEffect(isSyncingDrive) {
        if (isSyncingDrive) {
            syncStatusMessage = syncingMsg
            viewModel.syncHistoryToDrive(context) { result ->
                isSyncingDrive = false
                syncStatusMessage = if (result.success) syncFinishedMsg else "Error en la sincronización"
            }
        }
    }

    LaunchedEffect(cleaningStatusMessage) {
        if (cleaningStatusMessage == analysisFinishedMsg) {
            delay(2000.milliseconds)
            cleaningStatusMessage = null
        }
    }

    // --- Comprobación Silenciosa Semanal de Actualizaciones ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val lastCheck = SettingsManager.getLastUpdateCheckTime(context)
            val now = System.currentTimeMillis()
            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000L
            if (now - lastCheck > sevenDaysMillis) {
                UpdateManager.checkLatestRelease(context)
                updateCheckTrigger++
            }
        }
    }

    LaunchedEffect(syncStatusMessage) {
        if (syncStatusMessage == syncFinishedMsg || syncStatusMessage?.startsWith("Error") == true) {
            delay(2000.milliseconds)
            syncStatusMessage = null
        }
    }
    
    var itemsToExportLocally by remember { mutableStateOf<Set<SearchItem>>(emptySet()) }

    // --- MANEJO DE PANTALLAS (Fase 4 - Optimización) ---
    if (showHelpScreen) {
        HelpScreen(onBackPress = { showHelpScreen = false })
        return
    }

    if (showHistoryScreen) {
        HistoryScreen(onBack = { showHistoryScreen = false })
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionsUpdateTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionsUpdateTrigger++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionPointColor by remember(permissionsUpdateTrigger) {
        derivedStateOf {
            PermissionChecker.getOverallPermissionPointColor(context)
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
            context.showToast(R.string.permission_calendar_select, Toast.LENGTH_LONG)
        }
    }

    val exportCvoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val events = itemsToExportLocally.filterIsInstance<SearchItem.Event>().map { it.festivo }
                    val notes = itemsToExportLocally.filterIsInstance<SearchItem.Note>().map { it.dailyNote }
                    val json = CvoHelper.generateAgendaJson(events, notes)
                    context.contentResolver.openOutputStream(selectedUri)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        context.showToast(R.string.file_saved_successfully)
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        context.showToast("Error al guardar localmente")
                    }
                }
            }
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

    val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
    val finalEventsToList = remember(uiState.eventsByDate, currentMonth, showAllEvents) {
        processEventsForDisplay(uiState.eventsByDate, currentMonth.atDay(1), showAll = !isCurrentMonthView || showAllEvents)
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

    // --- DIÁLOGOS GLOBALES (Reubicados para visibilidad sobre cualquier pantalla v3.1.34) ---
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
                val fmt = AppFormats.DateAbbr
                val dateStr = event.date.format(fmt)
                val displayTitle = if (event.title.length > 60) event.title.take(57) + "..." else event.title
                val message = context.applicationContext.getString(R.string.event_deleted_message, dateStr, displayTitle)
                context.showToast(message)

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

    if (uiState.showAgendaImportPreview) {
        AgendaImportPreviewDialog(
            events = uiState.agendaImportEvents,
            notes = uiState.agendaImportNotes,
            availableCalendars = uiState.availableCalendars,
            favoriteCalendarId = uiState.favoriteCalendarId,
            onConfirm = viewModel::applyAgendaImport,
            onDismiss = viewModel::cancelAgendaImport
        )
    }

    if (uiState.showHolidayImportPreview) {
        HolidayImportPreviewDialog(
            adjustments = uiState.holidayImportItems,
            onConfirm = viewModel::applyHolidayImport,
            onDismiss = viewModel::cancelHolidayImport
        )
    }

    if (showSyncDialog) {
        SyncDriveDialog(
            isSyncing = isSyncingDrive,
            statusMessage = syncStatusMessage,
            onSync = { isSyncingDrive = true },
            onDismiss = { showSyncDialog = false }
        )
    }

    if (showCleaningDialog) {
        CleaningAssistantDialog(
            uiState = uiState,
            isScanning = isScanningCleaning,
            statusMessage = cleaningStatusMessage,
            onScan = { isScanningCleaning = true },
            onDelete = viewModel::deleteCleaningCandidate,
            onDeleteAll = viewModel::deleteAllCleaningCandidates,
            onNavigateToDate = { date ->
                showCleaningDialog = false
                val targetPage = ChronoUnit.MONTHS.between(startMonth, YearMonth.from(date)).toInt()
                scope.launch { monthPagerState.scrollToPage(targetPage) }
            },
            onDismiss = { showCleaningDialog = false }
        )
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
            initialFestivo = holidayForManager,
            viewModel = viewModel // Pasamos el ViewModel para unificar importación (v3.1.34)
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

    if (showColorThemeScreen) {
        ColorThemeScreen(
            onBackPress = { showColorThemeScreen = false },
            onThemeModified = onThemeUpdated,
            darkTheme = darkTheme
        )
        return
    }

    if (showWidgetLogScreen) {
        LogScreen(
            onBack = { showWidgetLogScreen = false }
        )
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
            onHistoryClick = { showBackupHistoryScreen = true }
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
            searchQuery = uiState.mainSearchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            startDate = uiState.mainSearchStartDate,
            onStartDateChange = viewModel::updateSearchStartDate,
            endDate = uiState.mainSearchEndDate,
            onEndDateChange = viewModel::updateSearchEndDate,
            activeFilters = uiState.mainSearchFilters,
            onToggleFilter = viewModel::toggleSearchFilter,
            timeShortcut = uiState.mainSearchTimeShortcut,
            onApplyShortcut = viewModel::applySearchTimeShortcut,
            searchResults = uiState.mainSearchResults,
            onClose = { isSearchActive = false },
            onEventClick = { clicked -> 
                isSearchActive = false
                onEventClickHandler(clicked)
            },
            onNoteClick = { note -> 
                selectedDateForDialog = LocalDate.parse(note.dateStr)
                showDayEventsDialog = true
                isSearchActive = false 
            },
            onDeleteNote = viewModel::deleteDailyNote,
            onOpenHolidayManager = { clicked ->
                isSearchActive = false
                onEventClickHandler(clicked)
            },
            onSaveLocalClick = { selectedSet ->
                itemsToExportLocally = selectedSet
                exportCvoLauncher.launch("AgendaVisual.cvo")
            },
            onRefresh = { viewModel.refreshData() },
            availableCalendars = uiState.availableCalendars
        )
    } else {
        val density = LocalDensity.current
        val containerWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
        val maxDrawerWidth = containerWidth * 0.85f
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.widthIn(max = maxDrawerWidth),
                    drawerContainerColor = CalendarioTheme.colors.settingsBackground,
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    val iconColor = CalendarioTheme.colors.settingsBackground.getContrastColor(Color.White).copy(alpha = 0.6f)
                    val brandColor = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.settingsBackground)

                    Text(
                        stringResource(id = R.string.app_name),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
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
                        icon = { Icon(Icons.Outlined.CalendarMonth, null, tint = iconColor) },
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
                        icon = { Icon(Icons.Outlined.CalendarToday, null, tint = iconColor) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                    )

                    // --- SECCIÓN: CALENDARIO (Adaptativa) ---
                    val favoriteId = uiState.favoriteCalendarId
                    val selectedIds = uiState.selectedCalendarIds
                    val availableCalendars = uiState.availableCalendars

                    val favoriteCalendar = availableCalendars.find { it.id == favoriteId && selectedIds.contains(it.id) }
                        ?: availableCalendars.find { selectedIds.contains(it.id) }

                    if (favoriteCalendar != null) {
                        CalendarDrawerItem(
                            calendar = favoriteCalendar,
                            iconColor = iconColor,
                            onToggle = { 
                                showManageCalendarsScreen = true
                                scope.launch { drawerState.close() }
                            }
                        )
                    } else {
                        NavigationDrawerItem(
                            label = { Text(stringResource(id = R.string.other_calendars)) },
                            selected = false,
                            onClick = { 
                                showManageCalendarsScreen = true
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(painter = painterResource(id = R.drawable.ic_select_window_custom), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp)) },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

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
                        icon = { Icon(Icons.Outlined.Celebration, null, tint = iconColor) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.data_center_screen_title)) },
                        selected = false,
                        onClick = { 
                            showBackupScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(painter = painterResource(id = R.drawable.ic_cloud_backup_outlined), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp)) },
                        colors = drawerItemColors,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                    )

                    val isUpdateAvailable = remember(updateCheckTrigger, showSettingsScreen) { SettingsManager.isUpdateAvailable(context) }
                    val updatePointColor = Color(0xFF2196F3)

                    // --- SECCIÓN: OTROS (Ajustes, Ayuda) ---
                    NavigationDrawerItem(
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.settings), modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isUpdateAvailable) {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .size(8.dp)
                                                .background(updatePointColor, CircleShape)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(8.dp)
                                            .background(permissionPointColor, CircleShape)
                                    )
                                }
                            }
                        },
                        selected = false,
                        onClick = { 
                            showSettingsScreen = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Outlined.Settings, null, tint = iconColor) },
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
                        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = iconColor) },
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
                            .background(CalendarioTheme.colors.cabecera)
                            .statusBarsPadding()
                    ) {
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
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
                                        color = Color.White,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clickable(enabled = viewMode == CalendarViewMode.YEARLY) {
                                                showGoToYearDialog = true
                                            }
                                    )

                                    val isAtToday = if (viewMode == CalendarViewMode.MONTHLY) currentMonth == YearMonth.from(today) else currentYear == Year.from(today)

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
                                                tint = Color.White
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
                        val effectType = SettingsManager.getMonthlyEffectType(context)
                        
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
                                LaunchedEffect(month) {
                                    viewModel.checkAndRefreshForAnchorDate(month.atDay(1))
                                }
                                val startOfWeek = getActualFirstDayOfWeek(context)
                                MonthlyCalendar(
                                    currentMonth = month,
                                    today = today,
                                    eventsByDate = uiState.eventsByDate,
                                    onDayClick = { date, events ->
                                        viewModel.checkAndRefreshForAnchorDate(date)
                                        selectedDateForDialog = date
                                        eventsForDialog = events
                                        showDayEventsDialog = true
                                    },
                                    onEmptyDayClick = { date ->
                                        viewModel.checkAndRefreshForAnchorDate(date)
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
                        val showWeekNumber = SettingsManager.isWeekNumberVisible(context)

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

    if (showStartupRecoveryDialog) {
        StartupRecoveryDialog(
            errorMessage = lastCrashMessage,
            isRepairing = isRepairingDatabase,
            onConfirmRepair = {
                if (!isRepairingDatabase) {
                    isRepairingDatabase = true
                    viewModel.repairDatabaseAndResync(context) { success ->
                        isRepairingDatabase = false
                        showStartupRecoveryDialog = false
                        if (success) {
                            context.showToast(R.string.repair_data_success)
                        } else {
                            context.showToast(R.string.error)
                        }
                    }
                }
            },
            onDismissRequest = {
                if (!isRepairingDatabase) {
                    SettingsManager.clearStartupCrashFlag(context)
                    showStartupRecoveryDialog = false
                }
            }
        )
    }
}

@Composable
private fun CalendarDrawerItem(
    calendar: CalendarInfo,
    iconColor: Color,
    onToggle: (Long) -> Unit
) {
    val context = LocalContext.current
    val brandColor = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.settingsBackground)
    val signedInEmail = SettingsManager.getGoogleAccountEmail(context)
    val isMatchingAccount = !signedInEmail.isNullOrBlank() && calendar.accountName.equals(signedInEmail, ignoreCase = true)
    val googlePhotoUrl = if (isMatchingAccount) SettingsManager.getGoogleAccountPhotoUrl(context) else null

    NavigationDrawerItem(
        label = {
            Text(
                text = calendar.displayName,
                color = brandColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        selected = false,
        onClick = { onToggle(calendar.id) },
        icon = {
            if (!googlePhotoUrl.isNullOrBlank()) {
                UserGoogleAvatar(
                    photoUrl = googlePhotoUrl,
                    size = 24.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = iconColor
                )
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
