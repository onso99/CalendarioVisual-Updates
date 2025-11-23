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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.example.calendario.AppThemeSetup

// 1. DATA CLASS PARA COLORES PERSONALIZADOS
data class CustomColors(
    val settingsBackground: Color,
    val textSystem: Color,
    val textSundayHoliday: Color,
    val textBirthday: Color,
    val dropdownMenuBackground: Color,
    val monthlyCalendarGridBackground: Color,
    val monthlyCalendarDayCellBackground: Color,
    val monthlyCalendarEmptyCellBackground: Color,
    val monthlyCalendarTodayCellBorder: Color,
    val monthlyCalendarHeaderBackground: Color,
    val monthlyCalendarHeaderText: Color,
    val monthlyCalendarDayNumberNormal: Color,
    val monthlyCalendarEventIndicator: Color,
    val miniMonthHeaderBackground: Color,
    val miniMonthDayNumberNormal: Color,
    val miniMonthTodayHighlightBackground: Color,
    val eventListTitleColor: Color, // Específico del modo oscuro
    val todayHighlightColor: Color,
    val onTodayHighlightColor: Color
)

// 2. COMPOSITION LOCAL
val LocalCustomColors = staticCompositionLocalOf {
    createLightCustomColors(null)
}

// 3. ENVOLtorio DEL TEMA
@Composable
fun CalendarioTheme(
    darkTheme: Boolean,
    themeUpdateTrigger: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val (colorScheme, customColors) = remember(darkTheme, themeUpdateTrigger) {
        val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val cs = if (darkTheme) createDarkColorScheme(prefs) else createLightColorScheme(prefs)
        val cc = if (darkTheme) createDarkCustomColors(prefs) else createLightCustomColors(prefs)
        cs to cc
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

fun isColorDark(color: Color): Boolean {
    return ColorUtils.calculateLuminance(color.toArgb()) < 0.5
}

// --- Light Color Scheme --- //
private fun createLightColorScheme(prefs: SharedPreferences?): ColorScheme {
    val cabeceraColor = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_CABECERA, AppThemeSetup.LightColors.cabecera)
    val onCabeceraColor = if (isColorDark(cabeceraColor)) Color.White else Color.Black
    val fondoPantallasDialogosColor = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_FONDO_PANTALLAS_DIALOGOS, AppThemeSetup.LightColors.fondoPantallasDialogos)
    val onFondoPantallasDialogosColor = if (isColorDark(fondoPantallasDialogosColor)) Color.White else Color.Black

    return lightColorScheme(
        primary = cabeceraColor,
        onPrimary = onCabeceraColor,
        surfaceVariant = fondoPantallasDialogosColor,
        onSurfaceVariant = onFondoPantallasDialogosColor,
        background = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_BACKGROUND, AppThemeSetup.LightColors.background),
        onBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_BACKGROUND, AppThemeSetup.LightColors.onBackground),
        error = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ERROR, AppThemeSetup.LightColors.error),
        onError = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_ON_ERROR, AppThemeSetup.LightColors.onError)
    )
}

private fun createLightCustomColors(prefs: SharedPreferences?): CustomColors {
    val todayHighlight = getColor(prefs, AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, Color(0xFFE9E9E9))
    val onTodayHighlight = if (isColorDark(todayHighlight)) Color.White else Color.Black

    return CustomColors(
        settingsBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppThemeSetup.LightColors.settingsBackground),
        todayHighlightColor = todayHighlight,
        onTodayHighlightColor = onTodayHighlight,
        textSystem = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_TEXT_SYSTEM, AppThemeSetup.LightColors.textSystem),
        textSundayHoliday = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppThemeSetup.LightColors.textSundayHoliday),
        textBirthday = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppThemeSetup.LightColors.textBirthday),
        dropdownMenuBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, AppThemeSetup.LightColors.dropdownMenuBackground),
        monthlyCalendarGridBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarGridBackground),
        monthlyCalendarDayCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarDayCellBackground),
        monthlyCalendarEmptyCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarEmptyCellBackground),
        monthlyCalendarTodayCellBorder = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppThemeSetup.LightColors.monthlyCalendarTodayCellBorder),
        monthlyCalendarHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarHeaderBackground),
        monthlyCalendarHeaderText = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_TEXT, AppThemeSetup.LightColors.monthlyCalendarHeaderText),
        monthlyCalendarDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppThemeSetup.LightColors.monthlyCalendarDayNumberNormal),
        monthlyCalendarEventIndicator = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EVENT_INDICATOR, AppThemeSetup.LightColors.monthlyCalendarEventIndicator),
        miniMonthHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_HEADER_BACKGROUND, AppThemeSetup.LightColors.miniMonthHeaderBackground),
        miniMonthDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppThemeSetup.LightColors.miniMonthDayNumberNormal),
        miniMonthTodayHighlightBackground = getColor(prefs, AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppThemeSetup.LightColors.miniMonthTodayHighlightBackground),
        eventListTitleColor = Color.Transparent // No se usa en modo claro
    )
}

// --- Dark Color Scheme --- //
private fun createDarkColorScheme(prefs: SharedPreferences?): ColorScheme {
    val cabeceraColor = getColor(prefs, AppThemeSetup.ColorKeys.DARK_CABECERA, AppThemeSetup.DarkColors.cabecera)
    val onCabeceraColor = if (isColorDark(cabeceraColor)) Color.White else Color.Black
    val fondoPantallasDialogosColor = getColor(prefs, AppThemeSetup.ColorKeys.DARK_FONDO_PANTALLAS_DIALOGOS, AppThemeSetup.DarkColors.fondoPantallasDialogos)
    val onFondoPantallasDialogosColor = if (isColorDark(fondoPantallasDialogosColor)) Color.White else Color.Black

    return darkColorScheme(
        primary = cabeceraColor,
        onPrimary = onCabeceraColor,
        surfaceVariant = fondoPantallasDialogosColor,
        onSurfaceVariant = onFondoPantallasDialogosColor,
        background = getColor(prefs, AppThemeSetup.ColorKeys.DARK_BACKGROUND, AppThemeSetup.DarkColors.background),
        onBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_BACKGROUND, AppThemeSetup.DarkColors.onBackground),
        error = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ERROR, AppThemeSetup.DarkColors.error),
        onError = getColor(prefs, AppThemeSetup.ColorKeys.DARK_ON_ERROR, AppThemeSetup.DarkColors.onError)
    )
}

private fun createDarkCustomColors(prefs: SharedPreferences?): CustomColors {
    val baseTodayHighlight = getColor(prefs, AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, Color(0xFFE9E9E9))
    val todayHighlight = baseTodayHighlight.copy(alpha = 0.5f)
    val onTodayHighlight = if (isColorDark(todayHighlight)) Color.White else Color.Black

    return CustomColors(
        settingsBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_SETTINGS_BACKGROUND, AppThemeSetup.DarkColors.settingsBackground),
        todayHighlightColor = todayHighlight,
        onTodayHighlightColor = onTodayHighlight,
        textSystem = getColor(prefs, AppThemeSetup.ColorKeys.DARK_TEXT_SYSTEM, AppThemeSetup.DarkColors.textSystem),
        textSundayHoliday = getColor(prefs, AppThemeSetup.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppThemeSetup.DarkColors.textSundayHoliday),
        textBirthday = getColor(prefs, AppThemeSetup.ColorKeys.DARK_TEXT_BIRTHDAY, AppThemeSetup.DarkColors.textBirthday),
        dropdownMenuBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND, AppThemeSetup.DarkColors.dropdownMenuBackground),
        monthlyCalendarGridBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarGridBackground),
        monthlyCalendarDayCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground),
        monthlyCalendarEmptyCellBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarEmptyCellBackground),
        monthlyCalendarTodayCellBorder = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppThemeSetup.DarkColors.monthlyCalendarTodayCellBorder),
        monthlyCalendarHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppThemeSetup.DarkColors.monthlyCalendarHeaderBackground),
        monthlyCalendarHeaderText = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_TEXT, AppThemeSetup.DarkColors.monthlyCalendarHeaderText),
        monthlyCalendarDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppThemeSetup.DarkColors.monthlyCalendarDayNumberNormal),
        monthlyCalendarEventIndicator = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EVENT_INDICATOR, AppThemeSetup.DarkColors.monthlyCalendarEventIndicator),
        miniMonthHeaderBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_HEADER_BACKGROUND, AppThemeSetup.DarkColors.miniMonthHeaderBackground),
        miniMonthDayNumberNormal = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppThemeSetup.DarkColors.miniMonthDayNumberNormal),
        miniMonthTodayHighlightBackground = getColor(prefs, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppThemeSetup.DarkColors.miniMonthTodayHighlightBackground),
        eventListTitleColor = getColor(prefs, AppThemeSetup.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR, AppThemeSetup.DarkColors.eventListTitleColor)
    )
}
