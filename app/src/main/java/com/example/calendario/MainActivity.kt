package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.roundToInt

// --- Definiciones de Colores y Constantes de Tema Directamente en este Archivo ---
object AppThemeSetup {
    const val APP_SETTINGS_PREFS_NAME = "app_settings_prefs_internal"
    const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled_internal"

    private val baseAppPrimaryColor = Color(0xFF2196F3)
    private val baseAppOnPrimaryColor = Color.White

    object LightColors {
        val primary = baseAppPrimaryColor
        val onPrimary = baseAppOnPrimaryColor
        val background = Color(0xFFFAFAFA)
        val surface = Color.White
        val onBackground = Color.Black
        val onSurface = Color.Black
        val error = Color.Red
        val onError = Color.White
        val screenBackground = background
        val onScreenTextNormal = onBackground
        val onScreenTextSecondary = Color.DarkGray
        val topAppBarBackground = primary
        val topAppBarContent = onPrimary
        val dropdownMenuBackground = Color.White
        val navigationButtonBackground = Color(0xFFffbb77)
        val navigationButtonContent = Color.Black
        val eventListTitleBackground = Color(0xFFEAEAEA)
        val eventListTitleColor = primary
        val eventListItemHolidayText = error
        val eventListItemBirthdayText = Color(0xFF0000FF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = Color(0xFFF1F7FE)
        val monthlyCalendarGridBorder = Color(0xFFCCCCCC)
        val monthlyCalendarDayCellBackground = Color.White
        val monthlyCalendarDayCellBorder = Color(0xFFCCCCCC)
        val monthlyCalendarTodayCellBorder = primary
        val monthlyCalendarHeaderBackground = Color(0xFFADD1FA)
        val monthlyCalendarHeaderText = Color.Black
        val monthlyCalendarDayNumberNormal = Color.Black
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error.copy(alpha = 0.7f)
        val monthlyCalendarEventIndicator = primary
        val miniMonthBackground = Color(0xFFF0F0F0)
        val miniMonthBorder = Color(0xFFDCDCDC)
        val miniMonthHeaderBackground = Color(0xFFE0E0E0)
        val miniMonthHeaderText = Color.DarkGray
        val miniMonthDayNumberNormal = Color.Black.copy(alpha = 0.9f)
        val miniMonthDayNumberHoliday = error
        val miniMonthDayNumberSunday = error.copy(alpha = 0.7f)
        val miniMonthTodayHighlightText = Color.Blue.copy(alpha = 0.9f)
        val miniMonthTodayHighlightBackground = primary.copy(alpha = 0.15f)
        val dialogEventHolidayText = error
        val dialogEventBirthdayText = eventListItemBirthdayText
        val dialogEventDefaultText = onScreenTextNormal
        val dialogCalendarColorIndicatorBorder = Color.DarkGray.copy(alpha = 0.5f)
    }

    object DarkColors {
        val primary = Color(0xFF0D47A1)
        val onPrimary = Color.White
        val background = Color(0xFF121212)
        val surface = Color(0xFF1E1E1E)
        val onBackground = Color(0xFFE0E0E0)
        val onSurface = Color(0xFFE0E0E0)
        val error = Color(0xFFFF8A80)
        val onError = Color.Black
        val screenBackground = background
        val onScreenTextNormal = onBackground
        val onScreenTextSecondary = Color(0xFFA0A0A0)
        val topAppBarBackground = primary
        val topAppBarContent = onPrimary
        val dropdownMenuBackground = Color(0xFF2C2C2C)
        val navigationButtonBackground = Color(0xFFB87333)
        val navigationButtonContent = Color.White
        val eventListTitleBackground = Color(0xFF303030)
        val eventListTitleColor = Color(0xFF64B5F6)
        val eventListItemHolidayText = error
        val eventListItemBirthdayText = Color(0xFFAECBFF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = Color(0xFF1E1E1E)
        val monthlyCalendarGridBorder = Color(0xFF424242)
        val monthlyCalendarDayCellBackground = Color(0xFF2C2C2C)
        val monthlyCalendarDayCellBorder = Color(0xFF424242)
        val monthlyCalendarTodayCellBorder = primary
        val monthlyCalendarHeaderBackground = Color(0xFF0D47A1)
        val monthlyCalendarHeaderText = onPrimary
        val monthlyCalendarDayNumberNormal = onSurface
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error.copy(alpha = 0.7f)
        val monthlyCalendarEventIndicator = Color(0xFF64B5F6)
        val miniMonthBackground = Color(0xFF2A2A2A)
        val miniMonthBorder = Color(0xFF404040)
        val miniMonthHeaderBackground = Color(0xFF333333)
        val miniMonthHeaderText = Color(0xFFB0B0B0)
        val miniMonthDayNumberNormal = onSurface.copy(alpha = 0.9f)
        val miniMonthDayNumberHoliday = error
        val miniMonthDayNumberSunday = error.copy(alpha = 0.7f)
        val miniMonthTodayHighlightText = Color(0xFFAECBFF)
        val miniMonthTodayHighlightBackground = primary.copy(alpha = 0.20f)
        val dialogEventHolidayText = error
        val dialogEventBirthdayText = eventListItemBirthdayText
        val dialogEventDefaultText = onScreenTextNormal
        val dialogCalendarColorIndicatorBorder = Color(0xFFA0A0A0).copy(alpha = 0.5f)
    }
}
// --- FIN DEFINICIONES DE COLORES ---

class MainActivity : ComponentActivity() {

    private var eventsByDateState by mutableStateOf<Map<LocalDate, List<Festivo>>>(emptyMap())
    private var availableCalendarsState by mutableStateOf<List<CalendarInfo>>(emptyList())
    private var selectedCalendarIdsState by mutableStateOf<Set<Long>>(emptySet())
    private var hasCalendarPermissionState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        eventsByDateState = loadEventsFromPrefs(this)
        selectedCalendarIdsState = loadSelectedCalendarIds(this)
        hasCalendarPermissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

        val appPrefs = getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val systemIsDark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val initialDarkTheme = appPrefs.getBoolean(AppThemeSetup.KEY_DARK_THEME_ENABLED, systemIsDark)

        setContent {
            var isDarkThemeEnabled by remember { mutableStateOf(initialDarkTheme) }

            val toggleTheme: (Boolean) -> Unit = { newThemeState ->
                isDarkThemeEnabled = newThemeState
                appPrefs.edit {
                    putBoolean(AppThemeSetup.KEY_DARK_THEME_ENABLED, newThemeState)
                    apply()
                }
            }

            CalendarioApp(
                isDarkTheme = isDarkThemeEnabled,
                onThemeToggle = toggleTheme,
                initialEventsByDate = eventsByDateState,
                initialAvailableCalendars = availableCalendarsState,
                initialSelectedCalendarIds = selectedCalendarIdsState,
                initialHasPermission = hasCalendarPermissionState,
                onRefreshRequest = {
                    refreshDataFromCalendarProviderAndUpdateStates()
                },
                onCalendarDataUpdated = { newEvents, newAvailable, newSelectedIds ->
                    eventsByDateState = newEvents
                    availableCalendarsState = newAvailable
                    selectedCalendarIdsState = newSelectedIds
                    saveEventsToPrefs(this, newEvents)
                    saveSelectedCalendarIds(this, newSelectedIds)
                    notifyCalendarWidgetsDataChangedMainActivity(this)
                },
                onPermissionUpdated = { newPermissionState ->
                    hasCalendarPermissionState = newPermissionState
                    if (newPermissionState) {
                        refreshDataFromCalendarProviderAndUpdateStates()
                    } else {
                        eventsByDateState = emptyMap()
                        availableCalendarsState = emptyList()
                        saveEventsToPrefs(this, emptyMap())
                        notifyCalendarWidgetsDataChangedMainActivity(this)
                    }
                }
            )
        }

        if (hasCalendarPermissionState) {
            refreshDataFromCalendarProviderAndUpdateStates()
        }
    }

    override fun onResume() {
        super.onResume()
        hasCalendarPermissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (hasCalendarPermissionState) {
            refreshDataFromCalendarProviderAndUpdateStates()
        } else {
            eventsByDateState = emptyMap()
            availableCalendarsState = emptyList()
        }
    }

    private fun refreshDataFromCalendarProviderAndUpdateStates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            hasCalendarPermissionState = false
            eventsByDateState = emptyMap()
            availableCalendarsState = emptyList()
            saveEventsToPrefs(this, emptyMap())
            notifyCalendarWidgetsDataChangedMainActivity(this)
            return
        }
        if (!hasCalendarPermissionState) hasCalendarPermissionState = true

        lifecycleScope.launch {
            try {
                val currentSelectedIds = loadSelectedCalendarIds(this@MainActivity)
                val freshAvailableCalendars = loadAvailableCalendarsSuspend(this@MainActivity)
                availableCalendarsState = freshAvailableCalendars

                val validSelectedIds = currentSelectedIds.filter { sid -> freshAvailableCalendars.any { cal -> cal.id == sid } }.toSet()
                if (validSelectedIds != selectedCalendarIdsState) {
                    selectedCalendarIdsState = validSelectedIds
                    saveSelectedCalendarIds(this@MainActivity, validSelectedIds)
                }

                val freshEventsMap = if (selectedCalendarIdsState.isNotEmpty() || freshAvailableCalendars.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(this@MainActivity, selectedCalendarIdsState, freshAvailableCalendars)
                } else {
                    emptyMap()
                }
                eventsByDateState = freshEventsMap
                saveEventsToPrefs(this@MainActivity, freshEventsMap)
                notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error refrescando datos del calendario", e)
                Toast.makeText(this@MainActivity, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()
                eventsByDateState = emptyMap()
                availableCalendarsState = emptyList()
                saveEventsToPrefs(this@MainActivity, emptyMap())
                notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity)
            }
        }
    }
}

fun notifyCalendarWidgetsConfigurationChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar widgets por CAMBIO DE CONFIGURACIÓN.")
    }
}

fun notifyCalendarWidgetsDataChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar datos de EVENTOS en widgets.")
    }
}

@Composable
fun CalendarioApp(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    initialEventsByDate: Map<LocalDate, List<Festivo>>,
    initialAvailableCalendars: List<CalendarInfo>,
    initialSelectedCalendarIds: Set<Long>,
    initialHasPermission: Boolean,
    onRefreshRequest: () -> Unit,
    onCalendarDataUpdated: (Map<LocalDate, List<Festivo>>, List<CalendarInfo>, Set<Long>) -> Unit,
    onPermissionUpdated: (Boolean) -> Unit
) {
    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = AppThemeSetup.DarkColors.primary,
            onPrimary = AppThemeSetup.DarkColors.onPrimary,
            background = AppThemeSetup.DarkColors.background,
            surface = AppThemeSetup.DarkColors.surface,
            onBackground = AppThemeSetup.DarkColors.onBackground,
            onSurface = AppThemeSetup.DarkColors.onSurface,
            error = AppThemeSetup.DarkColors.error,
            onError = AppThemeSetup.DarkColors.onError,
            primaryContainer = AppThemeSetup.DarkColors.navigationButtonBackground,
            onPrimaryContainer = AppThemeSetup.DarkColors.navigationButtonContent,
            surfaceVariant = AppThemeSetup.DarkColors.dropdownMenuBackground,
            onSurfaceVariant = AppThemeSetup.DarkColors.onScreenTextNormal,
            outline = AppThemeSetup.DarkColors.monthlyCalendarGridBorder
        )
    } else {
        lightColorScheme(
            primary = AppThemeSetup.LightColors.primary,
            onPrimary = AppThemeSetup.LightColors.onPrimary,
            background = AppThemeSetup.LightColors.background,
            surface = AppThemeSetup.LightColors.surface,
            onBackground = AppThemeSetup.LightColors.onBackground,
            onSurface = AppThemeSetup.LightColors.onSurface,
            error = AppThemeSetup.LightColors.error,
            onError = AppThemeSetup.LightColors.onError,
            primaryContainer = AppThemeSetup.LightColors.navigationButtonBackground,
            onPrimaryContainer = AppThemeSetup.LightColors.navigationButtonContent,
            surfaceVariant = AppThemeSetup.LightColors.dropdownMenuBackground,
            onSurfaceVariant = AppThemeSetup.LightColors.onScreenTextNormal,
            outline = AppThemeSetup.LightColors.monthlyCalendarGridBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        CalendarioScreen(
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            eventsByDateExternal = initialEventsByDate,
            availableCalendarsExternal = initialAvailableCalendars,
            selectedCalendarIdsExternal = initialSelectedCalendarIds,
            hasCalendarPermissionExternal = initialHasPermission,
            onRefreshRequest = onRefreshRequest,
            onCalendarDataUpdated = onCalendarDataUpdated,
            onPermissionUpdated = onPermissionUpdated
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentYear by remember { mutableStateOf(Year.now()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSelectCalendarsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    val today = LocalDate.now()
    var showDayEventsDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }
    var eventsForDialog by remember { mutableStateOf<List<Festivo>>(emptyList()) }
    var showWidgetConfigDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        onPermissionUpdated(isGranted)
    }

    LaunchedEffect(hasCalendarPermissionExternal) {
        if (hasCalendarPermissionExternal) onRefreshRequest()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()) {
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
                                    text = { Text("Calendarios", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor ) },
                                    onClick = { menuExpanded = false; if (hasCalendarPermissionExternal) showSelectCalendarsDialog = true else requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR) })
                                DropdownMenuItem(
                                    text = { Text("Widget", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                    onClick = { menuExpanded = false; showWidgetConfigDialog = true })
                                DropdownMenuItem(
                                    text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                    onClick = { menuExpanded = false; showHelpDialog = true })
                                DropdownMenuItem(
                                    text = { Text("Acerca de", fontSize = 18.sp, modifier = Modifier.padding(8.dp), color = dropdownTextColor) },
                                    onClick = { menuExpanded = false; showAboutDialog = true })
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
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(onClick = { if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.minusMonths(1) else currentYear = currentYear.minusYears(1) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Icon(Icons.Filled.ArrowBack, "Anterior") }
                Button(
                    onClick = { if (viewMode == CalendarViewMode.MONTHLY) { currentYear = Year.of(currentMonth.year); viewMode = CalendarViewMode.YEARLY } else { currentMonth = YearMonth.now(); viewMode = CalendarViewMode.MONTHLY } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) { Text(if (viewMode == CalendarViewMode.MONTHLY) "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}" else "${currentYear.value}", fontSize = 20.sp) }
                FilledIconButton(onClick = { if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.plusMonths(1) else currentYear = currentYear.plusYears(1) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Icon(Icons.Filled.ArrowForward, "Siguiente") }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                if (viewMode == CalendarViewMode.MONTHLY) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MonthlyCalendar(
                            currentMonth = currentMonth,
                            today = today,
                            eventsByDate = eventsByDateExternal,
                            isDarkTheme = isDarkTheme,
                            onDayClick = { date, events ->
                                selectedDateForDialog = date
                                eventsForDialog = events
                                showDayEventsDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val finalEventsToList = processEventsForDisplay(eventsByDateExternal, currentMonth, today)
                        val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
                        val listTitleText = if (isCurrentMonthView) "Eventos pendientes de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}" else "Eventos de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"

                        Text(
                            listTitleText,
                            fontSize = 18.sp,
                            color = if (isDarkTheme) AppThemeSetup.DarkColors.eventListTitleColor else AppThemeSetup.LightColors.eventListTitleColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDarkTheme) AppThemeSetup.DarkColors.eventListTitleBackground else AppThemeSetup.LightColors.eventListTitleBackground,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp, horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (finalEventsToList.isEmpty()) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    if (isCurrentMonthView) "No hay eventos pendientes para este mes." else "No hay eventos para este mes.",
                                    fontSize = 16.sp,
                                    color = if (isDarkTheme) AppThemeSetup.DarkColors.onScreenTextSecondary else AppThemeSetup.LightColors.onScreenTextSecondary
                                )
                            }
                        } else {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp)) {
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
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text(String.format("%02d:", date.dayOfMonth), color = itemColor, fontWeight = fontWeightNum, fontSize = 16.sp)
                                                Text(displayDesc, color = itemColor, fontWeight = fontWeightNum, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    YearlyCalendar(
                        currentYear = currentYear,
                        today = today,
                        eventsByDate = eventsByDateExternal,
                        isDarkTheme = isDarkTheme,
                        onMonthSelected = { selectedMonth ->
                            currentMonth = selectedMonth
                            viewMode = CalendarViewMode.MONTHLY
                        }
                    )
                }
            }

            if (showSelectCalendarsDialog) {
                SelectCalendarsDialog(
                    isDarkTheme = isDarkTheme,
                    onDismissRequest = { showSelectCalendarsDialog = false }
                ) { ids ->
                    scope.launch {
                        try {
                            val avail = if (availableCalendarsExternal.isNotEmpty()) availableCalendarsExternal else loadAvailableCalendarsSuspend(context)
                            val fest = readFestivosFromCalendarsSuspend(context, ids, avail)
                            onCalendarDataUpdated(fest, avail, ids)
                        } catch (e: Exception) {
                            Log.e("CalendarioScreen", "Error aplicando selección: ${e.localizedMessage}", e); Toast.makeText(context, "Error.", Toast.LENGTH_SHORT).show()
                        } finally {
                            showSelectCalendarsDialog = false
                        }
                    }
                }
            }
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = { Column { Text("Calendario Visual V1.32", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Asistente IA / Android Studio", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Onso/agosto 2025", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
                )
            }
            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    title = { Text("Ayuda", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = { Column { Text("Botón mes alterna mensual/anual.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Flechas navegan mes/año.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Pulsar día con eventos muestra lista.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
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
        }
    }
}

enum class CalendarViewMode { MONTHLY, YEARLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(isDarkTheme: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    val initialEventCount = remember { prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    var eventCountSliderValue by remember { mutableFloatStateOf(initialEventCount.toFloat()) }
    val initialUseLargeFont = remember { prefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    var useLargeFontSwitchState by remember { mutableStateOf(initialUseLargeFont) }

    var eventColor by remember { mutableStateOf(Color(prefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB))) }
    var todayEventColor by remember { mutableStateOf(Color(prefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB))) }
    var showEventColorPalette by remember { mutableStateOf(false) }
    var showTodayEventColorPalette by remember { mutableStateOf(false) }

    val baseEventColors = remember {
        listOf(
            Color(0xFFFFFFFF), Color(0xFFF4F4F4), Color(0xFFD4D4D4), Color(0xFFB4B4B4),
            Color(0xFF949494), Color(0xFF5F5F5F), Color(0xFF000000)
        )
    }
    val baseTodayEventColors = remember {
        listOf(
            Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF80FF80), Color(0xFF00FFFF),
            Color(0xFF952BFF), Color(0xFFFFFFFF), Color(0xFF000000)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Configuración del Widget", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Número de eventos: ${eventCountSliderValue.roundToInt()}", fontSize = 16.sp)
                Slider(
                    value = eventCountSliderValue, onValueChange = { eventCountSliderValue = it },
                    valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                    )
                )
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useLargeFontSwitchState = !useLargeFontSwitchState }
                    .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Letra grande", fontSize = 16.sp)
                    Switch(
                        checked = useLargeFontSwitchState, onCheckedChange = { useLargeFontSwitchState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ColorPickerRow("Color eventos", eventColor, MaterialTheme.colorScheme.onSurfaceVariant) { showEventColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow("Color eventos de hoy", todayEventColor, MaterialTheme.colorScheme.onSurfaceVariant) { showTodayEventColorPalette = true }
            }
        },
        confirmButton = {
            Button(onClick = {
                prefs.edit {
                    putInt(WidgetConstants.KEY_EVENT_COUNT, eventCountSliderValue.roundToInt())
                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, useLargeFontSwitchState)
                    putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, eventColor.toArgb())
                    putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, todayEventColor.toArgb())
                    apply()
                }
                notifyCalendarWidgetsConfigurationChangedMainActivity(context)
                onDismissRequest()
            }) { Text("Guardar", fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
    )

    if (showEventColorPalette) {
        ColorPaletteDialog(
            title = "Color para eventos", colors = baseEventColors, currentlySelectedColor = eventColor,
            isDarkTheme = isDarkTheme,
            onColorSelected = { selectedColor -> eventColor = selectedColor; showEventColorPalette = false },
            onDismiss = { showEventColorPalette = false }
        )
    }
    if (showTodayEventColorPalette) {
        ColorPaletteDialog(
            title = "Color para eventos de hoy", colors = baseTodayEventColors, currentlySelectedColor = todayEventColor,
            isDarkTheme = isDarkTheme,
            onColorSelected = { selectedColor -> todayEventColor = selectedColor; showTodayEventColorPalette = false },
            onDismiss = { showTodayEventColorPalette = false }
        )
    }
}

@Composable
fun ColorPickerRow(label: String, currentColor: Color, textColor: Color, onColorBoxClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f), color = textColor)
        Box(modifier = Modifier
            .size(32.dp)
            .background(currentColor, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onColorBoxClick))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteDialog(
    title: String, colors: List<Color>, currentlySelectedColor: Color,
    isDarkTheme: Boolean,
    onColorSelected: (Color) -> Unit, onDismiss: () -> Unit
) {
    val selectedItemBorderColor = if (isDarkTheme) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyRow(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                items(colors) { colorInPalette ->
                    val isSelected = colorInPalette == currentlySelectedColor
                    Box(modifier = Modifier
                        .size(40.dp)
                        .background(colorInPalette, CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) selectedItemBorderColor else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            ),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorInPalette) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCalendarsDialog(isDarkTheme: Boolean, onDismissRequest: () -> Unit, onApplySelection: (selectedIds: Set<Long>) -> Unit) {
    val context = LocalContext.current
    var localAvailableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var currentSelectedIdsInDialog by remember { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(Unit) {
        try {
            val calendars = loadAvailableCalendarsSuspend(context)
            localAvailableCalendars = calendars
            currentSelectedIdsInDialog = loadSelectedCalendarIds(context).filter { id -> calendars.any { cal -> cal.id == id } }.toSet()
        } catch (e: Exception) { Log.e("SelectCalendarsDialog", "Error cargando calendarios: ${e.localizedMessage}", e); localAvailableCalendars = emptyList() }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Seleccionar Calendarios", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        text = {
            if (localAvailableCalendars.isEmpty()) { Text("No se encontraron calendarios.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else {
                LazyColumn(modifier = Modifier
                    .heightIn(max = 400.dp)
                    .fillMaxWidth()) {
                    items(localAvailableCalendars, key = { it.id }) { calendar ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSet =
                                    currentSelectedIdsInDialog.toMutableSet(); if (newSet.contains(
                                    calendar.id
                                )
                            ) newSet.remove(calendar.id) else newSet.add(calendar.id); currentSelectedIdsInDialog =
                                newSet
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = currentSelectedIdsInDialog.contains(calendar.id),
                                onCheckedChange = { isChecked -> val newSet = currentSelectedIdsInDialog.toMutableSet(); if (isChecked) newSet.add(calendar.id) else newSet.remove(calendar.id); currentSelectedIdsInDialog = newSet },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(calendar.displayName, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(calendar.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onApplySelection(currentSelectedIdsInDialog) }, enabled = localAvailableCalendars.isNotEmpty()) { Text("Aplicar", fontSize = 16.sp) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
    )
}

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    isDarkTheme: Boolean,
    onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val cells = mutableListOf<@Composable () -> Unit>()

    val colorBordeDiaActual = if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarTodayCellBorder else AppThemeSetup.LightColors.monthlyCalendarTodayCellBorder

    daysOfWeek.forEach { day ->
        cells.add {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarHeaderBackground else AppThemeSetup.LightColors.monthlyCalendarHeaderBackground)
                    .border(
                        1.dp,
                        if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarGridBorder else AppThemeSetup.LightColors.monthlyCalendarGridBorder
                    ),
                Alignment.Center
            ) {
                Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarHeaderText else AppThemeSetup.LightColors.monthlyCalendarHeaderText)
            }
        }
    }
    repeat(firstDayOfWeek) {
        cells.add {
            Box(Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground else AppThemeSetup.LightColors.monthlyCalendarDayCellBackground)
                .border(
                    1.dp,
                    if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBorder else AppThemeSetup.LightColors.monthlyCalendarDayCellBorder
                ))
        }
    }

    (1..daysInMonth).forEach { dayNum ->
        val thisDate = currentMonth.atDay(dayNum)
        val isToday = thisDate == today
        val dayEvents = eventsByDate[thisDate].orEmpty()
        val dayEventsConAlgunaInfo = dayEvents.any { it.description.ifEmpty { if (it.isAllDay) "(Todo el día)" else "" }.isNotBlank() }
        val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
        val isSundayNonHoliday = thisDate.dayOfWeek == java.time.DayOfWeek.SUNDAY && !isHoliday
        val hasOtherEventsPoint = dayEvents.any { !it.isFromHolidaySource && (it.startTime != null && !it.isAllDay || it.description.isNotBlank()) }

        val fontWeightNum = if (isHoliday || isToday) FontWeight.Bold else FontWeight.Normal
        val colorNum = when {
            isHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberHoliday else AppThemeSetup.LightColors.monthlyCalendarDayNumberHoliday
            isSundayNonHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberSunday else AppThemeSetup.LightColors.monthlyCalendarDayNumberSunday
            else -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberNormal else AppThemeSetup.LightColors.monthlyCalendarDayNumberNormal
        }
        val currentDayCellBorderColor = if (isToday) colorBordeDiaActual else (if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBorder else AppThemeSetup.LightColors.monthlyCalendarDayCellBorder)

        cells.add {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground else AppThemeSetup.LightColors.monthlyCalendarDayCellBackground)
                    .border(
                        if (isToday) 2.dp else 1.dp,
                        currentDayCellBorderColor,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = dayEventsConAlgunaInfo) {
                        onDayClick(
                            thisDate,
                            dayEvents.filter {
                                it.description.ifEmpty { if (it.isAllDay) "(Todo el día)" else "" }
                                    .isNotBlank()
                            })
                    }
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("$dayNum", fontWeight = fontWeightNum, color = colorNum, fontSize = 22.sp)
                    if (hasOtherEventsPoint) {
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier
                            .size(6.dp)
                            .background(
                                if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarEventIndicator else AppThemeSetup.LightColors.monthlyCalendarEventIndicator,
                                CircleShape
                            ))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
    repeat((7 - cells.size % 7) % 7) {
        cells.add {
            Box(Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground else AppThemeSetup.LightColors.monthlyCalendarDayCellBackground)
                .border(
                    1.dp,
                    if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBorder else AppThemeSetup.LightColors.monthlyCalendarDayCellBorder
                ))
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(
                if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarGridBackground else AppThemeSetup.LightColors.monthlyCalendarGridBackground,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarGridBorder else AppThemeSetup.LightColors.monthlyCalendarGridBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        Column { cells.chunked(7).forEach { weekCells -> Row(Modifier.fillMaxWidth()) { weekCells.forEach { cell -> Box(Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(1.dp), Alignment.Center) { cell() } } } } }
    }
}

@Composable
fun YearlyCalendar(
    currentYear: Year,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    isDarkTheme: Boolean,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(currentYear.value, it) }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        months.chunked(3).forEachIndexed { rowIndex, monthRow ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                monthRow.forEach { month ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onMonthSelected(month) },
                        Alignment.Center
                    ) {
                        MiniMonthCalendar(
                            month = month,
                            today = today,
                            eventsByDate = eventsByDate,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                repeat(3 - monthRow.size) { Spacer(Modifier
                    .weight(1f)
                    .aspectRatio(1f)) }
            }
            if (rowIndex < months.chunked(3).size - 1) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun MiniMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val daysOfWeekShort = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = month.lengthOfMonth()

    val compactTextStyle = LocalTextStyle.current.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val monthNameFontSize = 12.sp
    val dayHeadersFontSize = 8.sp
    val dayNumberFontSize = 9.sp

    Column(
        modifier
            .background(
                if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthBackground else AppThemeSetup.LightColors.miniMonthBackground,
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthBorder else AppThemeSetup.LightColors.miniMonthBorder,
                RoundedCornerShape(4.dp)
            )
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar(Char::titlecase),
            fontSize = monthNameFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthHeaderText else AppThemeSetup.LightColors.miniMonthHeaderText,
            style = compactTextStyle.copy(lineHeight = monthNameFontSize * 0.95f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthHeaderBackground else AppThemeSetup.LightColors.miniMonthHeaderBackground)
                .padding(vertical = 2.dp),
            Arrangement.SpaceAround
        ) {
            daysOfWeekShort.forEach {
                Box(Modifier.weight(1f), Alignment.Center) {
                    Text(
                        it,
                        fontSize = dayHeadersFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthHeaderText else AppThemeSetup.LightColors.miniMonthHeaderText,
                        style = compactTextStyle.copy(lineHeight = dayHeadersFontSize * 0.95f)
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            val dayCellsData = remember(month) {
                List(6 * 7) {
                    val day = it - firstDayOfWeekIndex + 1
                    if (day in 1..daysInMonth) month.atDay(day) else null
                }
            }
            dayCellsData.chunked(7).forEach { weekDates ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    Arrangement.SpaceAround,
                    Alignment.CenterVertically
                ) {
                    weekDates.forEach { date ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            Alignment.Center
                        ) {
                            if (date != null) {
                                val isToday = date == today
                                val isHoliday = eventsByDate[date]?.any { it.isFromHolidaySource && it.description.isNotBlank() } == true
                                val isSundayNonHoliday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY && !isHoliday

                                val textColor = when {
                                    isToday && !isHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthTodayHighlightText else AppThemeSetup.LightColors.miniMonthTodayHighlightText
                                    isHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthDayNumberHoliday else AppThemeSetup.LightColors.miniMonthDayNumberHoliday
                                    isSundayNonHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthDayNumberSunday else AppThemeSetup.LightColors.miniMonthDayNumberSunday
                                    else -> if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthDayNumberNormal else AppThemeSetup.LightColors.miniMonthDayNumberNormal
                                }
                                val fontWeightText = if (isHoliday || (isToday && !isHoliday)) FontWeight.Bold else FontWeight.Normal

                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    if (isToday && !isHoliday) {
                                        Box(
                                            Modifier
                                                .size((dayNumberFontSize.value * 2.1f).dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isDarkTheme) AppThemeSetup.DarkColors.miniMonthTodayHighlightBackground else AppThemeSetup.LightColors.miniMonthTodayHighlightBackground)
                                        )
                                    }
                                    Text(
                                        "${date.dayOfMonth}",
                                        fontSize = dayNumberFontSize,
                                        fontWeight = fontWeightText,
                                        color = textColor,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        style = compactTextStyle.copy(lineHeight = dayNumberFontSize * 0.95f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEventsDialog(
    date: LocalDate,
    events: List<Festivo>,
    availableCalendars: List<CalendarInfo>,
    isDarkTheme: Boolean, // Parámetro para saber el tema actual
    onDismissRequest: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault()) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant, // Color de fondo del diálogo, tomado del tema general
        title = {
            Text(
                formattedDate,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Color del título, tomado del tema general
            )
        },
        text = {
            val eventsToDisplay = events.mapNotNull { festivo ->
                val desc = if (!festivo.isAllDay && festivo.startTime != null) {
                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty { "(Sin título)" }}"
                } else {
                    festivo.description.ifEmpty { if (festivo.isAllDay) "(Todo el día)" else "" }
                }
                if (desc.isNotBlank()) festivo to desc else null
            }

            if (eventsToDisplay.isEmpty()) {
                Text(
                    "No hay eventos con detalle.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Color del texto, tomado del tema
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) { // Altura máxima para el LazyColumn
                    items(
                        items = eventsToDisplay,
                        key = { (festivo, _) -> festivo.calendarId.toString() + festivo.description + festivo.startTime.toString() + festivo.date.toString() }
                    ) { (festivo, displayDesc) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Determinar el color del texto del evento basado en el tema actual
                            val itemColor = when {
                                festivo.description.contains("cumpleaños", true) || festivo.description.contains("aniversario", true) ->
                                    if (isDarkTheme) AppThemeSetup.DarkColors.dialogEventBirthdayText else AppThemeSetup.LightColors.dialogEventBirthdayText
                                festivo.isFromHolidaySource ->
                                    if (isDarkTheme) AppThemeSetup.DarkColors.dialogEventHolidayText else AppThemeSetup.LightColors.dialogEventHolidayText
                                else ->
                                    if (isDarkTheme) AppThemeSetup.DarkColors.dialogEventDefaultText else AppThemeSetup.LightColors.dialogEventDefaultText
                            }

                            // Muestra el color del calendario si está disponible
                            availableCalendars.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(Color(colorInt), CircleShape)
                                        .border(
                                            0.5.dp,
                                            if (isDarkTheme) AppThemeSetup.DarkColors.dialogCalendarColorIndicatorBorder else AppThemeSetup.LightColors.dialogCalendarColorIndicatorBorder,
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                displayDesc,
                                color = itemColor, // Usar el color determinado
                                fontSize = 16.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar", fontSize = 16.sp) // El color del texto del botón lo toma MaterialTheme
            }
        }
    )
}