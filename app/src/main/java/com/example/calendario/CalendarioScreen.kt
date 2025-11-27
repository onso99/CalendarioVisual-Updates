package com.example.calendario

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CalendarViewMode { MONTHLY, YEARLY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarioScreen(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onThemeUpdated: () -> Unit,
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
    var showColorThemeScreen by remember { mutableStateOf(false) }
    var showGoToYearDialog by remember { mutableStateOf(false) }
    var showAddEventScreen by remember { mutableStateOf(false) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var eventToEdit by remember { mutableStateOf<Festivo?>(null) }
    var showAllEvents by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
                onThemeUpdated()
            },
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            onColorThemeClick = { 
                showOptionsScreen = false
                showColorThemeScreen = true 
            },
            onThemeUpdated = onThemeUpdated
        )
        return
    }

    if (showColorThemeScreen) {
        ColorThemeScreen(
            onBackPress = { 
                showColorThemeScreen = false
                onThemeUpdated()
            },
            onThemeUpdated = onThemeUpdated,
            isDarkTheme = isDarkTheme
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                } else {
                    Box(modifier = Modifier.height(64.dp)) {
                        TopAppBar(
                            title = {},
                            actions = {
                                IconButton(onClick = { launchAddEditScreenWithPermissionCheck(null, null) }) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Crear evento", tint = MaterialTheme.colorScheme.onPrimary)
                                }
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
                                        modifier = Modifier.background(CalendarioTheme.colors.dropdownMenuBackground)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Calendarios", fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = {
                                                menuExpanded = false
                                                if (hasCalendarPermissionExternal) {
                                                    showSelectCalendarsDialog = true
                                                } else {
                                                    readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = "Calendarios", tint = CalendarioTheme.colors.textSystem) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Opciones", fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = { menuExpanded = false; showOptionsScreen = true },
                                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Opciones", tint = CalendarioTheme.colors.textSystem) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ayuda", fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = { menuExpanded = false; showHelpScreen = true },
                                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = "Ayuda", tint = CalendarioTheme.colors.textSystem) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Acerca de", fontSize = 18.sp, color = CalendarioTheme.colors.textSystem) },
                                            onClick = { menuExpanded = false; showAboutDialog = true },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Acerca de", tint = CalendarioTheme.colors.textSystem) }
                                        )
                                    }
                                }
                            },
                             colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                        // Layered Title Content
                        val showHomeButton = viewMode == CalendarViewMode.MONTHLY && currentMonth != YearMonth.from(today)
                        if (viewMode == CalendarViewMode.YEARLY || showHomeButton) {
                             IconButton(
                                onClick = { 
                                    if (viewMode == CalendarViewMode.YEARLY) {
                                        viewMode = CalendarViewMode.MONTHLY
                                    } else if (showHomeButton) {
                                        scope.launch { monthPagerState.animateScrollToPage(initialPage) }
                                    }
                                }, 
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (viewMode == CalendarViewMode.YEARLY) "Volver a vista mensual" else "Volver al mes actual",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (viewMode == CalendarViewMode.MONTHLY) {
                            Text(
                                text = currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterStart).padding(start = if (showHomeButton) 56.dp else 16.dp)
                            )
                        } 
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
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
                            Text(
                                text = if (viewMode == CalendarViewMode.MONTHLY) "${currentMonth.year}" else "${currentYear.value}",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (viewMode == CalendarViewMode.MONTHLY) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = CalendarioTheme.colors.monthlyCalendarGridBackground,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalPager(
                        state = monthPagerState,
                    ) { page ->
                        val month = startMonth.plusMonths(page.toLong())
                        MonthlyCalendar(
                            currentMonth = month,
                            today = today,
                            eventsByDate = eventsByDateExternal,
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
                        Text(
                            text = "Eventos de ${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}",
                            fontSize = 18.sp,
                            color = CalendarioTheme.colors.eventListTitleColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        if (isCurrentMonthView) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CalendarioTheme.colors.toggleButtonSelectedBackground)
                                    .clickable { showAllEvents = !showAllEvents }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (showAllEvents) "Todos" else "Pendientes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = CalendarioTheme.colors.eventListTitleColor
                                )
                            }
                        }
                    }
                }
                MonthlyEventList(
                    modifier = Modifier.weight(1f),
                    finalEventsToList = finalEventsToList,
                    lazyListState = lazyListState,
                    isCurrentMonthView = isCurrentMonthView,
                    showAllEvents = showAllEvents,
                    today = today,
                    onEventClick = { event -> launchAddEditScreenWithPermissionCheck(event.date, event) }
                )
            } else { // Yearly or Search view
                 if (isSearchActive) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("Búsqueda no implementada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                     }
                 } else {
                     HorizontalPager(
                        state = yearPagerState
                    ) { page ->
                        val year = startYear.plusYears(page.toLong())
                        YearlyCalendar(
                            currentYear = year,
                            today = today,
                            eventsByDate = eventsByDateExternal,
                            onMonthSelected = { selectedMonth ->
                                val targetPage = ChronoUnit.MONTHS.between(startMonth, selectedMonth).toInt()
                                scope.launch { monthPagerState.scrollToPage(targetPage) }
                                viewMode = CalendarViewMode.MONTHLY
                            }
                        )
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
            val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = CalendarioTheme.colors.fondoDialogos,
                title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onFondoDialogos) },
                text = { Column { Text("Calendario Visual V1.5", fontSize = 16.sp, color = onFondoDialogos); Text("Asistente IA / Android Studio", fontSize = 16.sp, color = onFondoDialogos); Text("Onso/noviembre 2025", fontSize = 16.sp, color = onFondoDialogos) } },
                confirmButton = { 
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { 
                        Text("Cerrar", fontSize = 16.sp) 
                    }
                }
            )
        }
        
        if (showDayEventsDialog && selectedDateForDialog != null) {
            DayEventsDialog(
                date = selectedDateForDialog!!,
                events = eventsForDialog,
                availableCalendars = availableCalendarsExternal,
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