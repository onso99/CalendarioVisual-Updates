package com.example.calendario

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import java.time.LocalDate

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