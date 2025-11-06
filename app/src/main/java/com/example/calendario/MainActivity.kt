package com.example.calendario

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import java.util.Locale
import kotlin.math.roundToInt


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
        val dropdownMenuBackground = Color.White
        val navigationButtonBackground = Color(0xFFffbb77)
        val navigationButtonContent = Color.Black
        val eventListTitleColor = primary
        val eventListItemHolidayText = error
        val eventListItemBirthdayText = Color(0xFF0000FF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = Color(0xFFF4F4FF)
        val monthlyCalendarGridBorder = Color(0xFFCCCCCC)
        val monthlyCalendarDayCellBackground = Color.White
        val monthlyCalendarEmptyCellBackground = Color(0xFFF0F0F0)
        val monthlyCalendarDayCellBorder = Color(0xFFCCCCCC)
        val monthlyCalendarTodayCellBorder = primary
        val monthlyCalendarHeaderBackground = Color(0xFFADD1FA)
        val monthlyCalendarHeaderText = Color.Black
        val monthlyCalendarDayNumberNormal = Color.Black
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error.copy(alpha = 0.7f)
        val monthlyCalendarDayNumberGhost = Color.Gray.copy(alpha = 0.5f) // NUEVO
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
        val primary = Color(0xFF2173ed)
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
        val dropdownMenuBackground = Color(0xFF2C2C2C)
        val navigationButtonBackground = Color(0xFFB87333)
        val navigationButtonContent = Color.White
        val eventListTitleColor = Color(0xFFD28C45)
        val upperSectionBackground = Color(0xFF252525)            // MODIFICADO: Nuevo color de fondo
        val eventListItemHolidayText = error
        val eventListItemBirthdayText = Color(0xFFAECBFF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = upperSectionBackground    // MODIFICADO: Para usar el nuevo color
        val monthlyCalendarGridBorder = Color(0xFF424242)
        val monthlyCalendarDayCellBackground = Color(0xFF555555)
        val monthlyCalendarEmptyCellBackground = Color(0xFF353535)
        val monthlyCalendarDayCellBorder = Color(0xFF424242)
        val monthlyCalendarTodayCellBorder = eventListTitleColor
        val monthlyCalendarHeaderBackground = Color(0xFF0D47A1)
        val monthlyCalendarHeaderText = Color(0xFFAAD7FF)
        val monthlyCalendarDayNumberNormal = onSurface
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error.copy(alpha = 0.7f)
        val monthlyCalendarDayNumberGhost = Color.Gray.copy(alpha = 0.4f)
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

class MainActivity : ComponentActivity() {

    private var eventsByDateState by mutableStateOf<Map<LocalDate, List<Festivo>>>(emptyMap())
    private var availableCalendarsState by mutableStateOf<List<CalendarInfo>>(emptyList())
    private var selectedCalendarIdsState by mutableStateOf<Set<Long>>(emptySet())
    private var hasCalendarPermissionState by mutableStateOf(false)
    private var initialDarkThemeLoadedState by mutableStateOf(false)


    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        lifecycleScope.launch {
            val context = this@MainActivity
            eventsByDateState = loadEventsFromPrefs(context)
            selectedCalendarIdsState = loadSelectedCalendarIds(context)
            hasCalendarPermissionState = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

            val appPrefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, MODE_PRIVATE)
            val systemIsDark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            initialDarkThemeLoadedState = appPrefs.getBoolean(AppThemeSetup.KEY_DARK_THEME_ENABLED, systemIsDark)

            if (hasCalendarPermissionState) {
                refreshDataFromCalendarProviderAndUpdateStatesInternal()
            }
        }

        setContent {
            var isDarkThemeEnabled by remember(initialDarkThemeLoadedState) { mutableStateOf(initialDarkThemeLoadedState) }

            LaunchedEffect(initialDarkThemeLoadedState) {
                isDarkThemeEnabled = initialDarkThemeLoadedState
            }

            val toggleTheme: (Boolean) -> Unit = { newThemeState ->
                isDarkThemeEnabled = newThemeState
                val appPrefs = getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, MODE_PRIVATE)
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
                    refreshDataFromCalendarProviderAndUpdateStatesInternal()
                },
                onCalendarDataUpdated = { newEvents, newAvailable, newSelectedIds ->
                    eventsByDateState = newEvents
                    availableCalendarsState = newAvailable
                    selectedCalendarIdsState = newSelectedIds
                    lifecycleScope.launch {
                        saveEventsToPrefs(this@MainActivity, newEvents)
                        saveSelectedCalendarIds(this@MainActivity, newSelectedIds)
                        notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity)
                    }
                },
                onPermissionUpdated = { newPermissionState ->
                    hasCalendarPermissionState = newPermissionState
                    if (newPermissionState) {
                        refreshDataFromCalendarProviderAndUpdateStatesInternal()
                    } else {
                        eventsByDateState = emptyMap()
                        availableCalendarsState = emptyList()
                        lifecycleScope.launch {
                            saveEventsToPrefs(this@MainActivity, emptyMap())
                            notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity)
                        }
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val context = this@MainActivity
            val hasPermissionNow = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
            val permissionStateChanged = hasCalendarPermissionState != hasPermissionNow
            hasCalendarPermissionState = hasPermissionNow

            if (hasPermissionNow) {
                refreshDataFromCalendarProviderAndUpdateStatesInternal()
            } else {
                if (permissionStateChanged || eventsByDateState.isNotEmpty() || availableCalendarsState.isNotEmpty()) {
                    eventsByDateState = emptyMap()
                    availableCalendarsState = emptyList()
                    saveEventsToPrefs(context, emptyMap())
                    notifyCalendarWidgetsDataChangedMainActivity(context)
                }
            }
        }
    }

    private fun refreshDataFromCalendarProviderAndUpdateStatesInternal() {
        lifecycleScope.launch {
            val context = this@MainActivity
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                if (hasCalendarPermissionState) { // Only update if state changed
                    hasCalendarPermissionState = false
                    eventsByDateState = emptyMap()
                    availableCalendarsState = emptyList()
                    saveEventsToPrefs(context, emptyMap())
                    notifyCalendarWidgetsDataChangedMainActivity(context)
                }
                return@launch
            }

            if (!hasCalendarPermissionState) {
                hasCalendarPermissionState = true
            }

            try {
                val currentSelectedIds = loadSelectedCalendarIds(context)
                val freshAvailableCalendars = loadAvailableCalendarsSuspend(context)

                var dataChanged = false

                if (availableCalendarsState != freshAvailableCalendars) {
                    availableCalendarsState = freshAvailableCalendars
                    dataChanged = true
                }

                val validSelectedIds = currentSelectedIds.filter { sid -> freshAvailableCalendars.any { cal -> cal.id == sid } }.toSet()
                if (validSelectedIds != selectedCalendarIdsState) {
                    selectedCalendarIdsState = validSelectedIds
                    saveSelectedCalendarIds(context, validSelectedIds)
                    dataChanged = true
                }

                val freshEventsMap = if (selectedCalendarIdsState.isNotEmpty() || freshAvailableCalendars.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, selectedCalendarIdsState, freshAvailableCalendars)
                } else {
                    emptyMap()
                }

                if (eventsByDateState != freshEventsMap) {
                    eventsByDateState = freshEventsMap
                    saveEventsToPrefs(context, freshEventsMap)
                    dataChanged = true
                }

                if (dataChanged) {
                    notifyCalendarWidgetsDataChangedMainActivity(context)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error refrescando datos del calendario", e)
                Toast.makeText(context, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()

                eventsByDateState = emptyMap()
                availableCalendarsState = emptyList()
                saveEventsToPrefs(context, emptyMap())
                notifyCalendarWidgetsDataChangedMainActivity(context)
            }
        }
    }
}

fun notifyCalendarWidgetsConfigurationChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIdsArray: IntArray? = appWidgetManager.getAppWidgetIds(componentName)

    @IdRes val remoteViewId: Int = R.id.widget_event_list

    if (appWidgetIdsArray != null && appWidgetIdsArray.isNotEmpty()) {
        for (widgetId: Int in appWidgetIdsArray) {
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, remoteViewId)
        }
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar widgets por CAMBIO DE CONFIGURACIÓN (IdRes explícito).")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets que notificar para cambio de configuración.")
    }
}

fun notifyCalendarWidgetsDataChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIdsArray: IntArray? = appWidgetManager.getAppWidgetIds(componentName)

    @IdRes val remoteViewId: Int = R.id.widget_event_list

    if (appWidgetIdsArray != null && appWidgetIdsArray.isNotEmpty()) {
        for (widgetId: Int in appWidgetIdsArray) {
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, remoteViewId)
        }
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar datos de EVENTOS en widgets (IdRes explícito).")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets que notificar para cambio de datos de eventos.")
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


