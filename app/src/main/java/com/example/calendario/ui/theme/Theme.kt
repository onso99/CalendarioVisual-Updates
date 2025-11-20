package com.example.calendario.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.calendario.AppThemeSetup

// 1. DATA CLASS PARA COLORES PERSONALIZADOS
data class CustomColors(
    val onScreenTextNormal: Color,
    val onScreenTextSecondary: Color,
    val dropdownMenuBackground: Color,
    val navigationButtonBackground: Color,
    val navigationButtonContent: Color,
    val eventListItemHolidayText: Color,
    val eventListItemBirthdayText: Color,
    val eventListItemDefaultText: Color,
    val monthlyCalendarGridBackground: Color,
    val monthlyCalendarGridBorder: Color,
    val monthlyCalendarDayCellBackground: Color,
    val monthlyCalendarEmptyCellBackground: Color,
    val monthlyCalendarTodayCellBorder: Color,
    val monthlyCalendarHeaderBackground: Color,
    val monthlyCalendarHeaderText: Color,
    val monthlyCalendarDayNumberNormal: Color,
    val monthlyCalendarDayNumberHoliday: Color,
    val monthlyCalendarDayNumberSunday: Color,
    val monthlyCalendarDayNumberGhost: Color,
    val monthlyCalendarEventIndicator: Color,
    val miniMonthHeaderBackground: Color,
    val miniMonthHeaderText: Color,
    val miniMonthDayNumberNormal: Color,
    val miniMonthDayNumberHoliday: Color,
    val miniMonthDayNumberSunday: Color,
    val miniMonthTodayHighlightBackground: Color,
    val dialogEventHolidayText: Color,
    val dialogEventBirthdayText: Color,
    val dialogEventDefaultText: Color,
    val dialogCalendarColorIndicatorBorder: Color,
    val eventListTitleColor: Color, // Específico del modo oscuro
    val filterButtonBackground: Color, // Específico del modo oscuro
)

// 2. COMPOSITION LOCAL
val LocalCustomColors = staticCompositionLocalOf {
    createLightCustomColors(null)
}

// 3. ENVOLtorio DEL TEMA
@Composable
fun CalendarioTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }

    val colorScheme = if (darkTheme) {
        createDarkColorScheme(prefs)
    } else {
        createLightColorScheme(prefs)
    }

    val customColors = if (darkTheme) {
        createDarkCustomColors(prefs)
    } else {
        createLightCustomColors(prefs)
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// Objeto para facilitar el acceso a los colores
object CalendarioTheme {
    val colors: CustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current
}

private fun getColor(prefs: SharedPreferences?, key: String, defaultColor: Color): Color {
    if (prefs == null) return defaultColor
    val colorInt = prefs.getInt(key, defaultColor.toArgb())
    return Color(colorInt)
}

// --- Light Color Scheme --- //
private fun createLightColorScheme(prefs: SharedPreferences?): ColorScheme {
    return lightColorScheme(
        primary = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_PRIMARY, AppThemeSetup.LightColors.primary),
        onPrimary = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_PRIMARY, AppThemeSetup.LightColors.onPrimary),
        background = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_BACKGROUND, AppThemeSetup.LightColors.background),
        surface = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_SURFACE, AppThemeSetup.LightColors.surface),
        onBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_BACKGROUND, AppThemeSetup.LightColors.onBackground),
        onSurface = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_SURFACE, AppThemeSetup.LightColors.onSurface),
        error = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ERROR, AppThemeSetup.LightColors.error),
        onError = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_ERROR, AppThemeSetup.LightColors.onError)
    )
}

private fun createLightCustomColors(prefs: SharedPreferences?): CustomColors {
    return CustomColors(
        onScreenTextNormal = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_SCREEN_TEXT_NORMAL, AppThemeSetup.LightColors.onScreenTextNormal),
        onScreenTextSecondary = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_SCREEN_TEXT_SECONDARY, AppThemeSetup.LightColors.onScreenTextSecondary),
        dropdownMenuBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, AppThemeSetup.LightColors.dropdownMenuBackground),
        navigationButtonBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_NAVIGATION_BUTTON_BACKGROUND, AppThemeSetup.LightColors.navigationButtonBackground),
        navigationButtonContent = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_NAVIGATION_BUTTON_CONTENT, AppThemeSetup.LightColors.navigationButtonContent),
        eventListItemHolidayText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_EVENT_LIST_ITEM_HOLIDAY_TEXT, AppThemeSetup.LightColors.eventListItemHolidayText),
        eventListItemBirthdayText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_EVENT_LIST_ITEM_BIRTHDAY_TEXT, AppThemeSetup.LightColors.eventListItemBirthdayText),
        eventListItemDefaultText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_EVENT_LIST_ITEM_DEFAULT_TEXT, AppThemeSetup.LightColors.eventListItemDefaultText),
        monthlyCalendarGridBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarGridBackground),
        monthlyCalendarGridBorder = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BORDER, AppThemeSetup.LightColors.monthlyCalendarGridBorder),
        monthlyCalendarDayCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarDayCellBackground),
        monthlyCalendarEmptyCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarEmptyCellBackground),
        monthlyCalendarTodayCellBorder = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppThemeSetup.LightColors.monthlyCalendarTodayCellBorder),
        monthlyCalendarHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarHeaderBackground),
        monthlyCalendarHeaderText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_TEXT, AppThemeSetup.LightColors.monthlyCalendarHeaderText),
        monthlyCalendarDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppThemeSetup.LightColors.monthlyCalendarDayNumberNormal),
        monthlyCalendarDayNumberHoliday = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_HOLIDAY, AppThemeSetup.LightColors.monthlyCalendarDayNumberHoliday),
        monthlyCalendarDayNumberSunday = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_SUNDAY, AppThemeSetup.LightColors.monthlyCalendarDayNumberSunday),
        monthlyCalendarDayNumberGhost = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_GHOST, AppThemeSetup.LightColors.monthlyCalendarDayNumberGhost),
        monthlyCalendarEventIndicator = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EVENT_INDICATOR, AppThemeSetup.LightColors.monthlyCalendarEventIndicator),
        miniMonthHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_HEADER_BACKGROUND, AppThemeSetup.LightColors.miniMonthHeaderBackground),
        miniMonthHeaderText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_HEADER_TEXT, AppThemeSetup.LightColors.miniMonthHeaderText),
        miniMonthDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppThemeSetup.LightColors.miniMonthDayNumberNormal),
        miniMonthDayNumberHoliday = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_HOLIDAY, AppThemeSetup.LightColors.miniMonthDayNumberHoliday),
        miniMonthDayNumberSunday = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_SUNDAY, AppThemeSetup.LightColors.miniMonthDayNumberSunday),
        miniMonthTodayHighlightBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppThemeSetup.LightColors.miniMonthTodayHighlightBackground),
        dialogEventHolidayText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_DIALOG_EVENT_HOLIDAY_TEXT, AppThemeSetup.LightColors.dialogEventHolidayText),
        dialogEventBirthdayText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_DIALOG_EVENT_BIRTHDAY_TEXT, AppThemeSetup.LightColors.dialogEventBirthdayText),
        dialogEventDefaultText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_DIALOG_EVENT_DEFAULT_TEXT, AppThemeSetup.LightColors.dialogEventDefaultText),
        dialogCalendarColorIndicatorBorder = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_DIALOG_CALENDAR_COLOR_INDICATOR_BORDER, AppThemeSetup.LightColors.dialogCalendarColorIndicatorBorder),
        eventListTitleColor = Color.Transparent, // No se usa en modo claro
        filterButtonBackground = Color(0xFFC1D7F2), // Usamos el color directamente aquí
    )
}

// --- Dark Color Scheme --- //
private fun createDarkColorScheme(prefs: SharedPreferences?): ColorScheme {
    return darkColorScheme(
        primary = getColor(prefs, AppThemeSetup.ColorKeys.DARK_PRIMARY, AppThemeSetup.DarkColors.primary),
        onPrimary = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_PRIMARY, AppThemeSetup.DarkColors.onPrimary),
        background = getColor(prefs, AppThemeSetup.ColorKeys.DARK_BACKGROUND, AppThemeSetup.DarkColors.background),
        surface = getColor(prefs, AppThemeSetup.ColorKeys.DARK_SURFACE, AppThemeSetup.DarkColors.surface),
        onBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_BACKGROUND, AppThemeSetup.DarkColors.onBackground),
        onSurface = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_SURFACE, AppThemeSetup.DarkColors.onSurface),
        error = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ERROR, AppThemeSetup.DarkColors.error),
        onError = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_ERROR, AppThemeSetup.DarkColors.onError)
    )
}

private fun createDarkCustomColors(prefs: SharedPreferences?): CustomColors {
    return CustomColors(
        onScreenTextNormal = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_SCREEN_TEXT_NORMAL, AppThemeSetup.DarkColors.onScreenTextNormal),
        onScreenTextSecondary = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_SCREEN_TEXT_SECONDARY, AppThemeSetup.DarkColors.onScreenTextSecondary),
        dropdownMenuBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND, AppThemeSetup.DarkColors.dropdownMenuBackground),
        navigationButtonBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_NAVIGATION_BUTTON_BACKGROUND, AppThemeSetup.DarkColors.navigationButtonBackground),
        navigationButtonContent = getColor(prefs, AppThemeSetup.ColorKeys.DARK_NAVIGATION_BUTTON_CONTENT, AppThemeSetup.DarkColors.navigationButtonContent),
        eventListItemHolidayText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_EVENT_LIST_ITEM_HOLIDAY_TEXT, AppThemeSetup.DarkColors.eventListItemHolidayText),
        eventListItemBirthdayText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_EVENT_LIST_ITEM_BIRTHDAY_TEXT, AppThemeSetup.DarkColors.eventListItemBirthdayText),
        eventListItemDefaultText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_EVENT_LIST_ITEM_DEFAULT_TEXT, AppThemeSetup.DarkColors.eventListItemDefaultText),
        monthlyCalendarGridBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarGridBackground),
        monthlyCalendarGridBorder = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BORDER, AppThemeSetup.DarkColors.monthlyCalendarGridBorder),
        monthlyCalendarDayCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground),
        monthlyCalendarEmptyCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarEmptyCellBackground),
        monthlyCalendarTodayCellBorder = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppThemeSetup.DarkColors.monthlyCalendarTodayCellBorder),
        monthlyCalendarHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarHeaderBackground),
        monthlyCalendarHeaderText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_TEXT, AppThemeSetup.DarkColors.monthlyCalendarHeaderText),
        monthlyCalendarDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppThemeSetup.DarkColors.monthlyCalendarDayNumberNormal),
        monthlyCalendarDayNumberHoliday = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_HOLIDAY, AppThemeSetup.DarkColors.monthlyCalendarDayNumberHoliday),
        monthlyCalendarDayNumberSunday = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_SUNDAY, AppThemeSetup.DarkColors.monthlyCalendarDayNumberSunday),
        monthlyCalendarDayNumberGhost = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_GHOST, AppThemeSetup.DarkColors.monthlyCalendarDayNumberGhost),
        monthlyCalendarEventIndicator = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EVENT_INDICATOR, AppThemeSetup.DarkColors.monthlyCalendarEventIndicator),
        miniMonthHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_HEADER_BACKGROUND, AppThemeSetup.DarkColors.miniMonthHeaderBackground),
        miniMonthHeaderText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_HEADER_TEXT, AppThemeSetup.DarkColors.miniMonthHeaderText),
        miniMonthDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppThemeSetup.DarkColors.miniMonthDayNumberNormal),
        miniMonthDayNumberHoliday = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_HOLIDAY, AppThemeSetup.DarkColors.miniMonthDayNumberHoliday),
        miniMonthDayNumberSunday = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_SUNDAY, AppThemeSetup.DarkColors.miniMonthDayNumberSunday),
        miniMonthTodayHighlightBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppThemeSetup.DarkColors.miniMonthTodayHighlightBackground),
        dialogEventHolidayText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_DIALOG_EVENT_HOLIDAY_TEXT, AppThemeSetup.DarkColors.dialogEventHolidayText),
        dialogEventBirthdayText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_DIALOG_EVENT_BIRTHDAY_TEXT, AppThemeSetup.DarkColors.dialogEventBirthdayText),
        dialogEventDefaultText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_DIALOG_EVENT_DEFAULT_TEXT, AppThemeSetup.DarkColors.dialogEventDefaultText),
        dialogCalendarColorIndicatorBorder = getColor(prefs, AppThemeSetup.ColorKeys.DARK_DIALOG_CALENDAR_COLOR_INDICATOR_BORDER, AppThemeSetup.DarkColors.dialogCalendarColorIndicatorBorder),
        eventListTitleColor = getColor(prefs, AppThemeSetup.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR, AppThemeSetup.DarkColors.eventListTitleColor),
        filterButtonBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_FILTER_BUTTON_BACKGROUND, AppThemeSetup.DarkColors.filterButtonBackground)
    )
}
