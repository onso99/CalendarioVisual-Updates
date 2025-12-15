package com.example.calendario

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

class MainActivity : ComponentActivity() {

    private var eventsByDateState by mutableStateOf<Map<LocalDate, List<Festivo>>>(emptyMap())
    private var availableCalendarsState by mutableStateOf<List<CalendarInfo>>(emptyList())
    private var selectedCalendarIdsState by mutableStateOf<Set<Long>>(emptySet())
    private var hasCalendarPermissionState by mutableStateOf(false)

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

            if (hasCalendarPermissionState) {
                refreshDataFromCalendarProviderAndUpdateStatesInternal()
            }
        }

        setContent {
            val themeManager = rememberThemeManager()
            val themeSetting by themeManager.themeSetting.collectAsState()
            var themeUpdateTrigger by remember { mutableIntStateOf(0) }
            val onThemeUpdated = { themeUpdateTrigger += 1 }

            val useDarkTheme = when (themeSetting) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            CalendarioTheme(darkTheme = useDarkTheme, themeUpdateTrigger = themeUpdateTrigger) {
                CalendarioApp(
                    themeManager = themeManager,
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
