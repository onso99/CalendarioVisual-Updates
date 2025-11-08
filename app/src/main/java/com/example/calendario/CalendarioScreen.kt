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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CalendarViewMode { MONTHLY, YEARLY }

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
    var showHelpDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var showDayEventsDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }
    var eventsForDialog by remember { mutableStateOf<List<Festivo>>(emptyList()) }
    var showWidgetConfigDialog by remember { mutableStateOf(false) }
    var showGoToYearDialog by remember { mutableStateOf(false) }
    var showAddEventScreen by remember { mutableStateOf(false) }

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

    LaunchedEffect(hasCalendarPermissionExternal) {
        if (hasCalendarPermissionExternal) {
            onRefreshRequest()
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
            editableCalendars = availableCalendarsExternal.filter { it.canModify },
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
                TopAppBar(
                    title = { Text("Calendario Visual", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { onThemeToggle(!isDarkTheme) }) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                                contentDescription = "Cambiar Tema",
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
                                            readPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Widget", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                    onClick = { menuExpanded = false; showWidgetConfigDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                    onClick = { menuExpanded = false; showHelpDialog = true }
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
            val listTitleText = if (isCurrentMonthView) "Eventos pendientes de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}" else "Eventos de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"
            val finalEventsToList = processEventsForDisplay(eventsByDateExternal, currentMonth, today)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (viewMode == CalendarViewMode.MONTHLY && !isDarkTheme) AppThemeSetup.LightColors.monthlyCalendarGridBackground
                        else if (viewMode == CalendarViewMode.MONTHLY && isDarkTheme) AppThemeSetup.DarkColors.upperSectionBackground
                        else Color.Transparent
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
                            if (viewMode == CalendarViewMode.MONTHLY) "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}" else "${currentYear.value}",
                            fontSize = 20.sp
                        )
                    }
                    FilledIconButton(
                        onClick = { 
                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    showAddEventScreen = true
                                }
                                else -> {
                                    writePermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
                                }
                            }
                        },
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
                    ) {
                        val month = startMonth.plusMonths(it.toLong())
                        MonthlyCalendar(
                            currentMonth = month,
                            today = today,
                            eventsByDate = eventsByDateExternal,
                            isDarkTheme = isDarkTheme,
                            onDayClick = { date, events ->
                                selectedDateForDialog = date
                                eventsForDialog = events
                                showDayEventsDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = listTitleText,
                        fontSize = 18.sp,
                        color = if (isDarkTheme) AppThemeSetup.DarkColors.eventListTitleColor else AppThemeSetup.LightColors.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
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
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (finalEventsToList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isCurrentMonthView) "No hay eventos pendientes para este mes." else "No hay eventos para este mes.",
                                fontSize = 16.sp,
                                color = if (isDarkTheme) AppThemeSetup.DarkColors.onScreenTextSecondary else AppThemeSetup.LightColors.onScreenTextSecondary
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                        ) {
                            finalEventsToList.forEach { (date, festivos) ->
                                val isTodayEvents = isCurrentMonthView && date == today
                                festivos.forEach { festivo ->
                                    val esCumpleanos = festivo.description.contains("cumpleaños", true) || festivo.description.contains("aniversario", true)
                                    val itemColor = when {
                                        esCumpleanos -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemBirthdayText else AppThemeSetup.LightColors.eventListItemBirthdayText
                                        festivo.isFromHolidaySource -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemHolidayText else AppThemeSetup.LightColors.eventListItemHolidayText
                                        else -> if (isDarkTheme) AppThemeSetup.DarkColors.eventListItemDefaultText else AppThemeSetup.LightColors.eventListItemDefaultText
                                    }
                                    val fontWeightNum = if (isTodayEvents) FontWeight.Bold else FontWeight.Normal
                                    val displayDesc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty { "(Sin título)" }}"
                                    else festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }

                                    if (displayDesc.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                String.format("%02d:", date.dayOfMonth),
                                                color = itemColor,
                                                fontWeight = fontWeightNum,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                displayDesc,
                                                color = itemColor,
                                                fontWeight = fontWeightNum,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 4.dp)
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
        
        if (showSelectCalendarsDialog) {
            SelectCalendarsDialog(
                initialSelectedIds = selectedCalendarIdsExternal,
                availableCalendars = availableCalendarsExternal,
                isDarkTheme = isDarkTheme,
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
                text = { Column { Text("Calendario Visual V1.38", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Asistente IA / Android Studio", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Onso/agosto 2025", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
            )
        }
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text("Ayuda", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                text = {
                    Column {
                        Text("- Toca el nombre del mes/año para cambiar entre vista mensual y anual.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- Usa las flechas para navegar al mes/año anterior o siguiente.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- Pulsa sobre un día con eventos para ver el detalle de las citas.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- El icono 'Sol' alterna entre modo claro y oscuro.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- El widget muestra los eventos pendientes y se puede configurar en los ajustes.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
            )
        }

        if (showDayEventsDialog && selectedDateForDialog != null) {
            DayEventsDialog(
                date = selectedDateForDialog!!,
                events = eventsForDialog,
                availableCalendars = availableCalendarsExternal,
                isDarkTheme = isDarkTheme,
                onDismissRequest = { showDayEventsDialog = false; selectedDateForDialog = null; eventsForDialog = emptyList() }
            )
        }
        if (showWidgetConfigDialog) {
            WidgetConfigScreen(
                isDarkTheme = isDarkTheme,
                onDismissRequest = { showWidgetConfigDialog = false }
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