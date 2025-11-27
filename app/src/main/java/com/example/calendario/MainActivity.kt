package com.example.calendario

import android.annotation.SuppressLint
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
    const val KEY_LIGHT_THEME_NAME = "light_theme_name"
    const val KEY_DARK_THEME_NAME = "dark_theme_name"
    const val CURRENT_THEME_VERSION = 2
    const val APP_SIGNATURE = "Calendario"

    object ColorKeys {
        // Light Theme
        const val LIGHT_CABECERA = "light_cabecera"
        const val LIGHT_FONDO_SECCIONES = "light_fondo_secciones"
        const val LIGHT_FONDO_DIALOGOS = "light_fondo_dialogos"
        const val LIGHT_BACKGROUND = "light_background"
        const val LIGHT_SETTINGS_BACKGROUND = "light_settings_background"
        const val LIGHT_ON_BACKGROUND = "light_on_background"
        const val LIGHT_ERROR = "light_error"
        const val LIGHT_ON_ERROR = "light_on_error"
        const val LIGHT_TEXT_SYSTEM = "light_text_system"
        const val LIGHT_TEXT_SUNDAY_HOLIDAY = "light_text_sunday_holiday"
        const val LIGHT_TEXT_BIRTHDAY = "light_text_birthday"
        const val LIGHT_DROPDOWN_MENU_BACKGROUND = "light_dropdown_menu_background"
        const val LIGHT_TODAY_HIGHLIGHT_COLOR = "light_today_highlight_color"
        const val LIGHT_EVENT_LIST_TITLE_COLOR = "light_event_list_title_color"
        const val LIGHT_TOGGLE_BUTTON_SELECTED_BACKGROUND = "light_toggle_button_selected_background"
        const val LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND = "light_monthly_calendar_grid_background"
        const val LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "light_monthly_calendar_day_cell_background"
        const val LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND = "light_monthly_calendar_empty_cell_background"
        const val LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER = "light_monthly_calendar_today_cell_border"
        const val LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND = "light_monthly_calendar_header_background"
        const val LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL = "light_monthly_calendar_day_number_normal"
        const val LIGHT_MONTHLY_CALENDAR_EVENT_INDICATOR = "light_monthly_calendar_event_indicator"
        const val LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL = "light_mini_month_day_number_normal"
        const val LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "light_mini_month_today_highlight_background"

        // Dark Theme
        const val DARK_CABECERA = "dark_cabecera"
        const val DARK_FONDO_SECCIONES = "dark_fondo_secciones"
        const val DARK_FONDO_DIALOGOS = "dark_fondo_dialogos"
        const val DARK_BACKGROUND = "dark_background"
        const val DARK_SETTINGS_BACKGROUND = "dark_settings_background"
        const val DARK_ON_BACKGROUND = "dark_on_background"
        const val DARK_ERROR = "dark_error"
        const val DARK_ON_ERROR = "dark_on_error"
        const val DARK_TEXT_SYSTEM = "dark_text_system"
        const val DARK_TEXT_SUNDAY_HOLIDAY = "dark_text_sunday_holiday"
        const val DARK_TEXT_BIRTHDAY = "dark_text_birthday"
        const val DARK_DROPDOWN_MENU_BACKGROUND = "dark_dropdown_menu_background"
        const val DARK_EVENT_LIST_TITLE_COLOR = "dark_event_list_title_color"
        const val DARK_TODAY_HIGHLIGHT_COLOR = "dark_today_highlight_color"
        const val DARK_TOGGLE_BUTTON_SELECTED_BACKGROUND = "dark_toggle_button_selected_background"
        const val DARK_MONTHLY_CALENDAR_GRID_BACKGROUND = "dark_monthly_calendar_grid_background"
        const val DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "dark_monthly_calendar_day_cell_background"
        const val DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND = "dark_monthly_calendar_empty_cell_background"
        const val DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER = "dark_monthly_calendar_today_cell_border"
        const val DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND = "dark_monthly_calendar_header_background"
        const val DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL = "dark_monthly_calendar_day_number_normal"
        const val DARK_MONTHLY_CALENDAR_EVENT_INDICATOR = "dark_monthly_calendar_event_indicator"
        const val DARK_MINI_MONTH_DAY_NUMBER_NORMAL = "dark_mini_month_day_number_normal"
        const val DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "dark_mini_month_today_highlight_background"
    }

    object LightColors {
        val cabecera = Color(0xFF2196F3)
        val settingsBackground = Color(0xFFF6F6FF)
        val fondoSecciones = Color(0xFFD6ECFD)
        val fondoDialogos = Color(0xFFFFFFFF)
        val dropdownMenuBackground = Color(0xFFFFFFFF)
        val error = Color(0xFFFF0000)
        val textSystem = Color(0xFF000000)
        val textSundayHoliday = Color(0xFFFF0000)
        val textBirthday = Color(0xFF0000FF)
        val eventListTitleColor = Color(0xFF0A4C87)
        val toggleButtonselectedBackground = Color(0x332196F3)
        val background = Color(0xFFF4F7FD)
        val todayHighlightColor = Color(0x91FFEA82)
        val monthlyCalendarGridBackground = Color(0xFFCADCF6)
        val monthlyCalendarDayCellBackground = Color(0xFFFFFFFF)
        val monthlyCalendarEmptyCellBackground = Color(0xFFEDF3F5)
        val monthlyCalendarTodayCellBorder = Color(0xFF2196F3)
        val monthlyCalendarDayNumberNormal = Color(0xFF000000)
        val monthlyCalendarHeaderBackground = Color(0xFFADD1FA)
        val monthlyCalendarEventIndicator = Color(0xFF2196F3)
        val miniMonthTodayHighlightBackground = Color(0x262196F3)
        val miniMonthDayNumberNormal = Color(0xE6000000)
        val onBackground = Color.Black
        val onError = Color.White
    }

    object DarkColors {
        val cabecera = Color(0xFF2173ED)
        val settingsBackground = Color(0xFF2C2C2C)
        val fondoSecciones = Color(0xFF274566)
        val fondoDialogos = Color(0xFF2C2C2C)
        val dropdownMenuBackground = Color(0xFF2C2C2C)
        val error = Color(0xFFFF5252)
        val textSystem = Color(0xFFE0E0E0)
        val textSundayHoliday = Color(0xFFE57373)
        val textBirthday = Color(0xFFAECBFF)
        val eventListTitleColor = Color(0xFFCF9A21)
        val toggleButtonselectedBackground = Color(0x84595959)
        val background = Color(0xFF121212)
        val todayHighlightColor = Color(0x8EACACAC)
        val monthlyCalendarGridBackground = Color(0xFF333333)
        val monthlyCalendarDayCellBackground = Color(0xFF6A6A6A)
        val monthlyCalendarEmptyCellBackground = Color(0xFF4F4F4F)
        val monthlyCalendarTodayCellBorder = Color(0xFFD28C45)
        val monthlyCalendarDayNumberNormal = Color(0xFFE0E0E0)
        val monthlyCalendarHeaderBackground = Color(0xFF0060BF)
        val monthlyCalendarEventIndicator = Color(0xFF64B5F6)
        val miniMonthTodayHighlightBackground = Color(0x332173ED)
        val miniMonthDayNumberNormal = Color(0xE6E0E0E0)
        val onBackground = Color(0xFFE0E0E0)
        val onError = Color.Black
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
                            CalendarAppWidgetProvider.triggerWidgetUpdate(this@MainActivity)
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
                                CalendarAppWidgetProvider.triggerWidgetUpdate(this@MainActivity)
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
                    CalendarAppWidgetProvider.triggerWidgetUpdate(context)
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
                    CalendarAppWidgetProvider.triggerWidgetUpdate(context)
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
                    CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error refrescando datos del calendario", e)
                Toast.makeText(context, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()

                eventsByDateState = emptyMap()
                availableCalendarsState = emptyList()
                saveEventsToPrefs(context, emptyMap())
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            }
        }
    }
}
