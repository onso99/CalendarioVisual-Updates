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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color // ¡¡¡IMPORT AÑADIDO!!!
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate


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
        val monthlyCalendarGridBackground = Color(0xFFE4EDFA) // MODIFICADO
        val monthlyCalendarGridBorder = Color(0xFFCCCCCC)
        val monthlyCalendarDayCellBackground = Color.White
        val monthlyCalendarEmptyCellBackground = Color(0xFFF0F0F0)
        val monthlyCalendarDayCellBorder = Color(0xFFCCCCCC)
        val monthlyCalendarTodayCellBorder = primary
        val monthlyCalendarHeaderBackground = Color(0xFFADD1FA)
        val monthlyCalendarHeaderText = Color.Black
        val monthlyCalendarDayNumberNormal = Color.Black
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error // CORREGIDO
        val monthlyCalendarDayNumberGhost = Color.Gray.copy(alpha = 0.5f)
        val monthlyCalendarEventIndicator = primary
        val miniMonthBackground = Color(0xFFF0F0F0)
        val miniMonthBorder = Color(0xFFDCDCDC)
        val miniMonthHeaderBackground = Color(0xFFE0E0E0)
        val miniMonthHeaderText = Color.DarkGray
        val miniMonthDayNumberNormal = Color.Black.copy(alpha = 0.9f)
        val miniMonthDayNumberHoliday = error
        val miniMonthDayNumberSunday = error // CORREGIDO
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
        val error = Color(0xFFFF5252)
        val onError = Color.Black
        val screenBackground = background
        val onScreenTextNormal = onBackground
        val onScreenTextSecondary = Color(0xFFA0A0A0)
        val dropdownMenuBackground = Color(0xFF2C2C2C)
        val navigationButtonBackground = Color(0xFFB87333)
        val navigationButtonContent = Color.White
        val eventListTitleColor = Color(0xFFD28C45)
        val upperSectionBackground = Color(0xFF252525)
        val eventListItemHolidayText = Color(0xFFE57373)
        val eventListItemBirthdayText = Color(0xFFAECBFF)
        val eventListItemDefaultText = onScreenTextNormal
        val monthlyCalendarGridBackground = upperSectionBackground
        val monthlyCalendarGridBorder = Color(0xFF424242)
        val monthlyCalendarDayCellBackground = Color(0xFF555555)
        val monthlyCalendarEmptyCellBackground = Color(0xFF353535)
        val monthlyCalendarDayCellBorder = Color(0xFF424242)
        val monthlyCalendarTodayCellBorder = eventListTitleColor
        val monthlyCalendarHeaderBackground = Color(0xFF004284)
        val monthlyCalendarHeaderText = Color(0xFFAAD7FF)
        val monthlyCalendarDayNumberNormal = onSurface
        val monthlyCalendarDayNumberHoliday = error
        val monthlyCalendarDayNumberSunday = error // CORREGIDO
        val monthlyCalendarDayNumberGhost = Color.Gray.copy(alpha = 0.4f)
        val monthlyCalendarEventIndicator = Color(0xFF64B5F6)
        val miniMonthBackground = Color(0xFF2A2A2A)
        val miniMonthBorder = Color(0xFF404040)
        val miniMonthHeaderBackground = Color(0xFF333333)
        val miniMonthHeaderText = Color(0xFFB0B0B0)
        val miniMonthDayNumberNormal = onSurface.copy(alpha = 0.9f)
        val miniMonthDayNumberHoliday = Color(0xFFFF8A80)
        val miniMonthDayNumberSunday = Color(0xFFFF8A80)
        val miniMonthTodayHighlightText = Color.White // MODIFICADO
        val miniMonthTodayHighlightBackground = primary.copy(alpha = 0.20f)
        val dialogEventHolidayText = error
        val dialogEventBirthdayText = eventListItemBirthdayText
        val dialogEventDefaultText = onScreenTextNormal
        val dialogCalendarColorIndicatorBorder = Color.DarkGray.copy(alpha = 0.5f)
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

    val remoteViewId: Int = R.id.widget_event_list

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

    val remoteViewId: Int = R.id.widget_event_list

    if (appWidgetIdsArray != null && appWidgetIdsArray.isNotEmpty()) {
        for (widgetId: Int in appWidgetIdsArray) {
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, remoteViewId)
        }
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar datos de EVENTOS en widgets (IdRes explícito).")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets que notificar para cambio de datos de eventos.")
    }
}
