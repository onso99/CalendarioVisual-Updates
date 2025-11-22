package com.example.calendario

import androidx.compose.runtime.Composable
import java.time.LocalDate

@Composable
fun CalendarioApp(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onThemeUpdated: () -> Unit,
    initialEventsByDate: Map<LocalDate, List<Festivo>>,
    initialAvailableCalendars: List<CalendarInfo>,
    initialSelectedCalendarIds: Set<Long>,
    initialHasPermission: Boolean,
    onRefreshRequest: () -> Unit,
    onCalendarDataUpdated: (Map<LocalDate, List<Festivo>>, List<CalendarInfo>, Set<Long>) -> Unit,
    onPermissionUpdated: (Boolean) -> Unit
) {
    CalendarioScreen(
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onThemeUpdated = onThemeUpdated,
        eventsByDateExternal = initialEventsByDate,
        availableCalendarsExternal = initialAvailableCalendars,
        selectedCalendarIdsExternal = initialSelectedCalendarIds,
        hasCalendarPermissionExternal = initialHasPermission,
        onRefreshRequest = onRefreshRequest,
        onCalendarDataUpdated = onCalendarDataUpdated,
        onPermissionUpdated = onPermissionUpdated
    )
}
