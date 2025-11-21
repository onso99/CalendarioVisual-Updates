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
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.launch
import java.time.LocalDate


object AppThemeSetup {
    const val APP_SETTINGS_PREFS_NAME = "app_settings_prefs_internal"
    const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled_internal"
    const val KEY_TODAY_HIGHLIGHT_COLOR = "today_highlight_color_app"
    const val KEY_ON_TODAY_HIGHLIGHT_COLOR = "on_today_highlight_color_app"

    object ColorKeys {
        // Light Theme
        const val LIGHT_PRIMARY = "light_primary"
        const val LIGHT_ON_PRIMARY = "light_on_primary"
        const val LIGHT_BACKGROUND = "light_background"
        const val LIGHT_SETTINGS_BACKGROUND = "light_settings_background"
        const val LIGHT_SURFACE = "light_surface"
        const val LIGHT_ON_BACKGROUND = "light_on_background"
        const val LIGHT_ON_SURFACE = "light_on_surface"
        const val LIGHT_ERROR = "light_error"
        const val LIGHT_ON_ERROR = "light_on_error"
        const val LIGHT_ON_SCREEN_TEXT_NORMAL = "light_on_screen_text_normal"
        const val LIGHT_ON_SCREEN_TEXT_SECONDARY = "light_on_screen_text_secondary"
        const val LIGHT_DROPDOWN_MENU_BACKGROUND = "light_dropdown_menu_background"
        const val LIGHT_NAVIGATION_BUTTON_BACKGROUND = "light_navigation_button_background"
        const val LIGHT_NAVIGATION_BUTTON_CONTENT = "light_navigation_button_content"
        const val LIGHT_EVENT_LIST_ITEM_HOLIDAY_TEXT = "light_event_list_item_holiday_text"
        const val LIGHT_EVENT_LIST_ITEM_BIRTHDAY_TEXT = "light_event_list_item_birthday_text"
        const val LIGHT_EVENT_LIST_ITEM_DEFAULT_TEXT = "light_event_list_item_default_text"
        const val LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND = "light_monthly_calendar_grid_background"
        const val LIGHT_MONTHLY_CALENDAR_GRID_BORDER = "light_monthly_calendar_grid_border"
        const val LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "light_monthly_calendar_day_cell_background"
        const val LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND = "light_monthly_calendar_empty_cell_background"
        const val LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER = "light_monthly_calendar_today_cell_border"
        const val LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND = "light_monthly_calendar_header_background"
        const val LIGHT_MONTHLY_CALENDAR_HEADER_TEXT = "light_monthly_calendar_header_text"
        const val LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL = "light_monthly_calendar_day_number_normal"
        const val LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_HOLIDAY = "light_monthly_calendar_day_number_holiday"
        const val LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_SUNDAY = "light_monthly_calendar_day_number_sunday"
        const val LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_GHOST = "light_monthly_calendar_day_number_ghost"
        const val LIGHT_MONTHLY_CALENDAR_EVENT_INDICATOR = "light_monthly_calendar_event_indicator"
        const val LIGHT_MINI_MONTH_HEADER_BACKGROUND = "light_mini_month_header_background"
        const val LIGHT_MINI_MONTH_HEADER_TEXT = "light_mini_month_header_text"
        const val LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL = "light_mini_month_day_number_normal"
        const val LIGHT_MINI_MONTH_DAY_NUMBER_HOLIDAY = "light_mini_month_day_number_holiday"
        const val LIGHT_MINI_MONTH_DAY_NUMBER_SUNDAY = "light_mini_month_day_number_sunday"
        const val LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "light_mini_month_today_highlight_background"
        const val LIGHT_DIALOG_EVENT_HOLIDAY_TEXT = "light_dialog_event_holiday_text"
        const val LIGHT_DIALOG_EVENT_BIRTHDAY_TEXT = "light_dialog_event_birthday_text"
        const val LIGHT_DIALOG_EVENT_DEFAULT_TEXT = "light_dialog_event_default_text"
        const val LIGHT_DIALOG_CALENDAR_COLOR_INDICATOR_BORDER = "light_dialog_calendar_color_indicator_border"
        const val LIGHT_FILTER_BUTTON_BACKGROUND = "light_filter_button_background"

        // Dark Theme
        const val DARK_PRIMARY = "dark_primary"
        const val DARK_ON_PRIMARY = "dark_on_primary"
        const val DARK_BACKGROUND = "dark_background"
        const val DARK_SETTINGS_BACKGROUND = "dark_settings_background"
        const val DARK_SURFACE = "dark_surface"
        const val DARK_ON_BACKGROUND = "dark_on_background"
        const val DARK_ON_SURFACE = "dark_on_surface"
        const val DARK_ERROR = "dark_error"
        const val DARK_ON_ERROR = "dark_on_error"
        const val DARK_ON_SCREEN_TEXT_NORMAL = "dark_on_screen_text_normal"
        const val DARK_ON_SCREEN_TEXT_SECONDARY = "dark_on_screen_text_secondary"
        const val DARK_DROPDOWN_MENU_BACKGROUND = "dark_dropdown_menu_background"
        const val DARK_NAVIGATION_BUTTON_BACKGROUND = "dark_navigation_button_background"
        const val DARK_NAVIGATION_BUTTON_CONTENT = "dark_navigation_button_content"
        const val DARK_EVENT_LIST_TITLE_COLOR = "dark_event_list_title_color"
        const val DARK_EVENT_LIST_ITEM_HOLIDAY_TEXT = "dark_event_list_item_holiday_text"
        const val DARK_EVENT_LIST_ITEM_BIRTHDAY_TEXT = "dark_event_list_item_birthday_text"
        const val DARK_EVENT_LIST_ITEM_DEFAULT_TEXT = "dark_event_list_item_default_text"
        const val DARK_MONTHLY_CALENDAR_GRID_BACKGROUND = "dark_monthly_calendar_grid_background"
        const val DARK_MONTHLY_CALENDAR_GRID_BORDER = "dark_monthly_calendar_grid_border"
        const val DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "dark_monthly_calendar_day_cell_background"
        const val DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND = "dark_monthly_calendar_empty_cell_background"
        const val DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER = "dark_monthly_calendar_today_cell_border"
        const val DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND = "dark_monthly_calendar_header_background"
        const val DARK_MONTHLY_CALENDAR_HEADER_TEXT = "dark_monthly_calendar_header_text"
        const val DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL = "dark_monthly_calendar_day_number_normal"
        const val DARK_MONTHLY_CALENDAR_DAY_NUMBER_HOLIDAY = "dark_monthly_calendar_day_number_holiday"
        const val DARK_MONTHLY_CALENDAR_DAY_NUMBER_SUNDAY = "dark_monthly_calendar_day_number_sunday"
        const val DARK_MONTHLY_CALENDAR_DAY_NUMBER_GHOST = "dark_monthly_calendar_day_number_ghost"
        const val DARK_MONTHLY_CALENDAR_EVENT_INDICATOR = "dark_monthly_calendar_event_indicator"
        const val DARK_MINI_MONTH_HEADER_BACKGROUND = "dark_mini_month_header_background"
        const val DARK_MINI_MONTH_HEADER_TEXT = "dark_mini_month_header_text"
        const val DARK_MINI_MONTH_DAY_NUMBER_NORMAL = "dark_mini_month_day_number_normal"
        const val DARK_MINI_MONTH_DAY_NUMBER_HOLIDAY = "dark_mini_month_day_number_holiday"
        const val DARK_MINI_MONTH_DAY_NUMBER_SUNDAY = "dark_mini_month_day_number_sunday"
        const val DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "dark_mini_month_today_highlight_background"
        const val DARK_DIALOG_EVENT_HOLIDAY_TEXT = "dark_dialog_event_holiday_text"
        const val DARK_DIALOG_EVENT_BIRTHDAY_TEXT = "dark_dialog_event_birthday_text"
        const val DARK_DIALOG_EVENT_DEFAULT_TEXT = "dark_dialog_event_default_text"
        const val DARK_DIALOG_CALENDAR_COLOR_INDICATOR_BORDER = "dark_dialog_calendar_color_indicator_border"
        const val DARK_FILTER_BUTTON_BACKGROUND = "dark_filter_button_background"
    }

    private val baseAppPrimaryColor = Color(0xFF2196F3)
    private val baseAppOnPrimaryColor = Color.White

    object LightColors {
        val primary = baseAppPrimaryColor
        val onPrimary = baseAppOnPrimaryColor
        val background = Color(0xFFFCFDFE)
        val settingsBackground = Color(0xFFEDF3FC)
        val surface = Color.White
        val onBackground = Color.Black
        val onSurface = Color.Black
        val error = Color.Red
        val onError = Color.White
        val onScreenTextNormal = onBackground
        val onScreenTextSecondary = Color.DarkGray
        val dropdownMenuBackground = Color.White
        val navigationButtonBackground = Color(0xFFffbb77)
        val navigationButtonContent = Color.Black
        val eventListItemHolidayText = error
        val eventListItemBirthdayText = Color(0xFF0000FF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = Color(0xFFDBE7F9)
        val monthlyCalendarGridBorder = Color(0xFFCCCCCC)
        val monthlyCalendarDayCellBackground = Color.White
        val monthlyCalendarEmptyCellBackground = Color(0xFFEDF3F5)
        val monthlyCalendarTodayCellBorder = primary
        val monthlyCalendarHeaderBackground = Color(0xFFADD1FA)
        val monthlyCalendarHeaderText = Color.Black
        val monthlyCalendarDayNumberNormal = Color.Black
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error // CORREGIDO
        val monthlyCalendarDayNumberGhost = Color.Gray.copy(alpha = 0.5f)
        val monthlyCalendarEventIndicator = primary
        val miniMonthHeaderBackground = Color(0xFFE0E0E0)
        val miniMonthHeaderText = Color.DarkGray
        val miniMonthDayNumberNormal = Color.Black.copy(alpha = 0.9f)
        val miniMonthDayNumberHoliday = error
        val miniMonthDayNumberSunday = error // CORREGIDO
        val miniMonthTodayHighlightBackground = primary.copy(alpha = 0.15f)
        val dialogEventHolidayText = error
        val dialogEventBirthdayText = eventListItemBirthdayText
        val dialogEventDefaultText = onScreenTextNormal
        val dialogCalendarColorIndicatorBorder = Color.DarkGray.copy(alpha = 0.5f)
        val filterButtonBackground = Color(0xFFC1D7F2)
    }

    object DarkColors {
        val primary = Color(0xFF2173ed)
        val onPrimary = Color.White
        val background = Color(0xFF121212)
        val settingsBackground = Color(0xFF1A1A1A) 
        val surface = Color(0xFF1E1E1E)
        val onBackground = Color(0xFFE0E0E0)
        val onSurface = Color(0xFFE0E0E0)
        val error = Color(0xFFFF5252)
        val onError = Color.Black
        val onScreenTextNormal = onBackground
        val onScreenTextSecondary = Color(0xFFA0A0A0)
        val dropdownMenuBackground = Color(0xFF2C2C2C)
        val navigationButtonBackground = Color(0xFFB87333)
        val navigationButtonContent = Color.White
        val eventListTitleColor = Color(0xFFD28C45)
        val eventListItemHolidayText = Color(0xFFE57373)
        val eventListItemBirthdayText = Color(0xFFAECBFF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = Color(0xFF333333)
        val monthlyCalendarGridBorder = Color(0xFF424242)
        val monthlyCalendarDayCellBackground = Color(0xFF6A6A6A)
        val monthlyCalendarEmptyCellBackground = Color(0xFF4F4F4F)
        val monthlyCalendarTodayCellBorder = eventListTitleColor
        val monthlyCalendarHeaderBackground = Color(0xFF0060BF)
        val monthlyCalendarHeaderText = Color(0xFFAAD7FF)
        val monthlyCalendarDayNumberNormal = onSurface
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error // CORREGIDO
        val monthlyCalendarDayNumberGhost = Color(0xFF7F7F7F)
        val monthlyCalendarEventIndicator = Color(0xFF64B5F6)
        val miniMonthHeaderBackground = Color(0xFF333333)
        val miniMonthHeaderText = Color(0xFFB0B0B0)
        val miniMonthDayNumberNormal = onSurface.copy(alpha = 0.9f)
        val miniMonthDayNumberHoliday = Color(0xFFFF8A80)
        val miniMonthDayNumberSunday = Color(0xFFFF8A80)
        val miniMonthTodayHighlightBackground = primary.copy(alpha = 0.20f)
        val dialogEventHolidayText = error
        val dialogEventBirthdayText = eventListItemBirthdayText
        val dialogEventDefaultText = onScreenTextNormal
        val dialogCalendarColorIndicatorBorder = Color.DarkGray.copy(alpha = 0.5f)
        val filterButtonBackground = Color(0xFF555555)
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
            var themeUpdateTrigger by remember { mutableIntStateOf(0) }
            val onThemeUpdated = { themeUpdateTrigger += 1 }

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

            CalendarioTheme(darkTheme = isDarkThemeEnabled, themeUpdateTrigger = themeUpdateTrigger) {
                CalendarioApp(
                    isDarkTheme = isDarkThemeEnabled,
                    onThemeToggle = toggleTheme,
                    onThemeUpdated = onThemeUpdated,
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

    val remoteViewId: Int = R.id.widget_event_list

    if (appWidgetIdsArray != null && appWidgetIdsArray.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIdsArray, remoteViewId)
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar widgets por CAMBIO DE CONFIGURACIÓN (IdRes explícito).")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets que notificar para cambio de configuración.")
    }
}

fun notifyCalendarWidgetsDataChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIdsArray: IntArray? = appWidgetManager.getAppWidgetIds(componentName)

    val remoteViewId: Int = R.id.widget_event_list

    if (appWidgetIdsArray != null && appWidgetIdsArray.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIdsArray, remoteViewId)
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar datos de EVENTOS en widgets (IdRes explícito).")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets que notificar para cambio de datos de eventos.")
    }
}
