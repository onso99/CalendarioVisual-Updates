package com.example.calendario.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.calendario.AppConstants

data class CustomColors(
    val cabecera: Color,
    val settingsBackground: Color,
    val textSystem: Color,
    val textSundayHoliday: Color,
    val textBirthday: Color,
    val textEventDefault: Color,
    val textEvent1: Color,
    val textEvent2: Color,
    val todayHighlightColor: Color,
    val eventListTitleColor: Color,
    val monthlyCalendarGridBackground: Color,
    val monthlyCalendarGridEffect: Color,
    val monthlyCalendarDayCellBackground: Color,
    val monthlyCalendarTodayCellBorder: Color,
    val monthlyCalendarHeaderBackground: Color,
    val monthlyCalendarDayNumberNormal: Color,
    val miniMonthTodayHighlightBackground: Color,
    val miniMonthDayNumberNormal: Color
) {
    // Cálculo automático del fondo de secciones (15% de variación respecto al fondo)
    val fondoSecciones: Color
        get() {
            val r = (settingsBackground.red * 255).toInt()
            val g = (settingsBackground.green * 255).toInt()
            val b = (settingsBackground.blue * 255).toInt()
            val alpha = (settingsBackground.alpha * 255).toInt()

            // Usamos la lógica de contraste del proyecto para decidir dirección
            val factor = if (isColorDark(settingsBackground, Color.Black)) 38 else -38

            val newR = (r + factor).coerceIn(0, 255)
            val newG = (g + factor).coerceIn(0, 255)
            val newB = (b + factor).coerceIn(0, 255)
            
            return Color(newR, newG, newB, alpha)
        }

    // Cálculo automático del fondo de diálogos (10% más claro que el fondo de pantalla)
    val fondoDialogos: Color
        get() {
            val r = (settingsBackground.red * 255).toInt()
            val g = (settingsBackground.green * 255).toInt()
            val b = (settingsBackground.blue * 255).toInt()
            val alpha = (settingsBackground.alpha * 255).toInt()
            
            val newR = (r + 25).coerceAtMost(255)
            val newG = (g + 25).coerceAtMost(255)
            val newB = (b + 25).coerceAtMost(255)
            
            return Color(newR, newG, newB, alpha)
        }
}

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        cabecera = AppConstants.LightColors.cabecera,
        settingsBackground = AppConstants.LightColors.settingsBackground,
        textSystem = AppConstants.LightColors.textSystem,
        textSundayHoliday = AppConstants.LightColors.textSundayHoliday,
        textBirthday = AppConstants.LightColors.textBirthday,
        textEventDefault = AppConstants.LightColors.textEventDefault,
        textEvent1 = AppConstants.LightColors.textEvent1,
        textEvent2 = AppConstants.LightColors.textEvent2,
        todayHighlightColor = AppConstants.LightColors.todayHighlightColor,
        eventListTitleColor = AppConstants.LightColors.eventListTitleColor,
        monthlyCalendarGridBackground = AppConstants.LightColors.monthlyCalendarGridBackground,
        monthlyCalendarGridEffect = AppConstants.LightColors.monthlyCalendarGridEffect,
        monthlyCalendarDayCellBackground = AppConstants.LightColors.monthlyCalendarDayCellBackground,
        monthlyCalendarTodayCellBorder = AppConstants.LightColors.monthlyCalendarTodayCellBorder,
        monthlyCalendarHeaderBackground = AppConstants.LightColors.monthlyCalendarHeaderBackground,
        monthlyCalendarDayNumberNormal = AppConstants.LightColors.monthlyCalendarDayNumberNormal,
        miniMonthTodayHighlightBackground = AppConstants.LightColors.miniMonthTodayHighlightBackground,
        miniMonthDayNumberNormal = AppConstants.LightColors.miniMonthDayNumberNormal
    )
}

@Composable
fun CalendarioTheme(
    darkTheme: Boolean,
    themeUpdateTrigger: Int, // Agrega este parámetro
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val customColors = remember(darkTheme, themeUpdateTrigger) {
        getThemeColors(context, darkTheme)
    }

    val onPrimaryColor = if (isColorDark(customColors.cabecera, customColors.settingsBackground)) Color.White else Color.Black

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            background = customColors.settingsBackground,
            onBackground = customColors.textSystem,
            surface = customColors.fondoSecciones,
            onSurface = customColors.textSystem
        )
    } else {
        lightColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            background = customColors.settingsBackground,
            onBackground = customColors.textSystem,
            surface = customColors.fondoSecciones,
            onSurface = customColors.textSystem
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isColorDark(colorScheme.primary, customColors.settingsBackground)
        }
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object CalendarioTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}

fun getThemeColors(context: Context, darkTheme: Boolean): CustomColors {
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    fun getSafeInt(key: String, default: Int): Int {
        return try {
            prefs.getInt(key, default)
        } catch (_: ClassCastException) {
            (prefs.all[key] as? Number)?.toInt() ?: default
        }
    }

    return if (darkTheme) {
        CustomColors(
            cabecera = Color(getSafeInt(AppConstants.ColorKeys.DARK_CABECERA, AppConstants.DarkColors.cabecera.toArgb())),
            settingsBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.DarkColors.settingsBackground.toArgb())),
            textSystem = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_SYSTEM, AppConstants.DarkColors.textSystem.toArgb())),
            textSundayHoliday = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppConstants.DarkColors.textSundayHoliday.toArgb())),
            textBirthday = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY, AppConstants.DarkColors.textBirthday.toArgb())),
            textEventDefault = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT, AppConstants.DarkColors.textEventDefault.toArgb())),
            textEvent1 = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_1, AppConstants.DarkColors.textEvent1.toArgb())),
            textEvent2 = Color(getSafeInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_2, AppConstants.DarkColors.textEvent2.toArgb())),
            todayHighlightColor = Color(getSafeInt(AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.DarkColors.todayHighlightColor.toArgb())),
            eventListTitleColor = Color(getSafeInt(AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR, AppConstants.DarkColors.eventListTitleColor.toArgb())),
            monthlyCalendarGridBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.DarkColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.DarkColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.DarkColors.monthlyCalendarDayCellBackground.toArgb())),
            monthlyCalendarTodayCellBorder = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.DarkColors.monthlyCalendarTodayCellBorder.toArgb())),
            monthlyCalendarHeaderBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.DarkColors.monthlyCalendarHeaderBackground.toArgb())),
            monthlyCalendarDayNumberNormal = Color(getSafeInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.DarkColors.monthlyCalendarDayNumberNormal.toArgb())),
            miniMonthTodayHighlightBackground = Color(getSafeInt(AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.DarkColors.miniMonthTodayHighlightBackground.toArgb())),
            miniMonthDayNumberNormal = Color(getSafeInt(AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.DarkColors.miniMonthDayNumberNormal.toArgb()))
        )
    } else {
        CustomColors(
            cabecera = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.LightColors.cabecera.toArgb())),
            settingsBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground.toArgb())),
            textSystem = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM, AppConstants.LightColors.textSystem.toArgb())),
            textSundayHoliday = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday.toArgb())),
            textBirthday = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday.toArgb())),
            textEventDefault = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT, AppConstants.LightColors.textEventDefault.toArgb())),
            textEvent1 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.LightColors.textEvent1.toArgb())),
            textEvent2 = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.LightColors.textEvent2.toArgb())),
            todayHighlightColor = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor.toArgb())),
            eventListTitleColor = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR, AppConstants.LightColors.eventListTitleColor.toArgb())),
            monthlyCalendarGridBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.LightColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground.toArgb())),
            monthlyCalendarTodayCellBorder = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.LightColors.monthlyCalendarTodayCellBorder.toArgb())),
            monthlyCalendarHeaderBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.LightColors.monthlyCalendarHeaderBackground.toArgb())),
            monthlyCalendarDayNumberNormal = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.LightColors.monthlyCalendarDayNumberNormal.toArgb())),
            miniMonthTodayHighlightBackground = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.LightColors.miniMonthTodayHighlightBackground.toArgb())),
            miniMonthDayNumberNormal = Color(getSafeInt(AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.LightColors.miniMonthDayNumberNormal.toArgb()))
        )
    }
}

fun isColorDark(color: Color, background: Color): Boolean {
    val contrastingColor = Color(ColorUtils.compositeColors(color.toArgb(), background.toArgb()))
    return ColorUtils.calculateLuminance(contrastingColor.toArgb()) < 0.5
}
