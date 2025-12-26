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

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        cabecera = AppConstants.LightColors.cabecera,
        fondoSecciones = AppConstants.LightColors.fondoSecciones,
        fondoDialogos = AppConstants.LightColors.fondoDialogos,
        settingsBackground = AppConstants.LightColors.settingsBackground,
        background = AppConstants.LightColors.background,
        error = AppConstants.LightColors.error,
        textSystem = AppConstants.LightColors.textSystem,
        textSundayHoliday = AppConstants.LightColors.textSundayHoliday,
        textBirthday = AppConstants.LightColors.textBirthday,
        textEventDefault = AppConstants.LightColors.textEventDefault,
        textEvent1 = AppConstants.LightColors.textEvent1,
        textEvent2 = AppConstants.LightColors.textEvent2,
        dropdownMenuBackground = AppConstants.LightColors.dropdownMenuBackground,
        todayHighlightColor = AppConstants.LightColors.todayHighlightColor,
        eventListTitleColor = AppConstants.LightColors.eventListTitleColor,
        toggleButtonSelectedBackground = AppConstants.LightColors.toggleButtonselectedBackground,
        monthlyCalendarGridBackground = AppConstants.LightColors.monthlyCalendarGridBackground,
        monthlyCalendarGridEffect = AppConstants.LightColors.monthlyCalendarGridEffect,
        monthlyCalendarDayCellBackground = AppConstants.LightColors.monthlyCalendarDayCellBackground,
        monthlyCalendarEmptyCellBackground = AppConstants.LightColors.monthlyCalendarEmptyCellBackground,
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

    val onPrimaryColor = if (isColorDark(customColors.cabecera, customColors.background)) Color.White else Color.Black
    val onErrorColor = if (isColorDark(customColors.error, customColors.background)) Color.White else Color.Black

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            background = customColors.background,
            onBackground = customColors.textSystem,
            surface = customColors.fondoSecciones,
            onSurface = customColors.textSystem,
            error = customColors.error,
            onError = onErrorColor
        )
    } else {
        lightColorScheme(
            primary = customColors.cabecera,
            onPrimary = onPrimaryColor,
            background = customColors.background,
            onBackground = customColors.textSystem,
            surface = customColors.fondoSecciones,
            onSurface = customColors.textSystem,
            error = customColors.error,
            onError = onErrorColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isColorDark(colorScheme.primary, customColors.background)
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
    return if (darkTheme) {
        CustomColors(
            cabecera = Color(prefs.getInt(AppConstants.ColorKeys.DARK_CABECERA, AppConstants.DarkColors.cabecera.toArgb())),
            fondoSecciones = Color(prefs.getInt(AppConstants.ColorKeys.DARK_FONDO_SECCIONES, AppConstants.DarkColors.fondoSecciones.toArgb())),
            fondoDialogos = Color(prefs.getInt(AppConstants.ColorKeys.DARK_FONDO_DIALOGOS, AppConstants.DarkColors.fondoDialogos.toArgb())),
            settingsBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.DarkColors.settingsBackground.toArgb())),
            background = Color(prefs.getInt(AppConstants.ColorKeys.DARK_BACKGROUND, AppConstants.DarkColors.background.toArgb())),
            error = Color(prefs.getInt(AppConstants.ColorKeys.DARK_ERROR, AppConstants.DarkColors.error.toArgb())),
            textSystem = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TEXT_SYSTEM, AppConstants.DarkColors.textSystem.toArgb())),
            textSundayHoliday = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppConstants.DarkColors.textSundayHoliday.toArgb())),
            textBirthday = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY, AppConstants.DarkColors.textBirthday.toArgb())),
            textEventDefault = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT, AppConstants.DarkColors.textEventDefault.toArgb())),
            textEvent1 = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_1, AppConstants.DarkColors.textEvent1.toArgb())),
            textEvent2 = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TEXT_EVENT_2, AppConstants.DarkColors.textEvent2.toArgb())),
            dropdownMenuBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND, AppConstants.DarkColors.dropdownMenuBackground.toArgb())),
            todayHighlightColor = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.DarkColors.todayHighlightColor.toArgb())),
            eventListTitleColor = Color(prefs.getInt(AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR, AppConstants.DarkColors.eventListTitleColor.toArgb())),
            toggleButtonSelectedBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_TOGGLE_BUTTON_SELECTED_BACKGROUND, AppConstants.DarkColors.toggleButtonselectedBackground.toArgb())),
            monthlyCalendarGridBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.DarkColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.DarkColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.DarkColors.monthlyCalendarDayCellBackground.toArgb())),
            monthlyCalendarEmptyCellBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppConstants.DarkColors.monthlyCalendarEmptyCellBackground.toArgb())),
            monthlyCalendarTodayCellBorder = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.DarkColors.monthlyCalendarTodayCellBorder.toArgb())),
            monthlyCalendarHeaderBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.DarkColors.monthlyCalendarHeaderBackground.toArgb())),
            monthlyCalendarDayNumberNormal = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.DarkColors.monthlyCalendarDayNumberNormal.toArgb())),
            miniMonthTodayHighlightBackground = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.DarkColors.miniMonthTodayHighlightBackground.toArgb())),
            miniMonthDayNumberNormal = Color(prefs.getInt(AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.DarkColors.miniMonthDayNumberNormal.toArgb()))
        )
    } else {
        CustomColors(
            cabecera = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.LightColors.cabecera.toArgb())),
            fondoSecciones = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_FONDO_SECCIONES, AppConstants.LightColors.fondoSecciones.toArgb())),
            fondoDialogos = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_FONDO_DIALOGOS, AppConstants.LightColors.fondoDialogos.toArgb())),
            settingsBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground.toArgb())),
            background = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_BACKGROUND, AppConstants.LightColors.background.toArgb())),
            error = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_ERROR, AppConstants.LightColors.error.toArgb())),
            textSystem = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM, AppConstants.LightColors.textSystem.toArgb())),
            textSundayHoliday = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday.toArgb())),
            textBirthday = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday.toArgb())),
            textEventDefault = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT, AppConstants.LightColors.textEventDefault.toArgb())),
            textEvent1 = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.LightColors.textEvent1.toArgb())),
            textEvent2 = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.LightColors.textEvent2.toArgb())),
            dropdownMenuBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, AppConstants.LightColors.dropdownMenuBackground.toArgb())),
            todayHighlightColor = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor.toArgb())),
            eventListTitleColor = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR, AppConstants.LightColors.eventListTitleColor.toArgb())),
            toggleButtonSelectedBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_TOGGLE_BUTTON_SELECTED_BACKGROUND, AppConstants.LightColors.toggleButtonselectedBackground.toArgb())),
            monthlyCalendarGridBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground.toArgb())),
            monthlyCalendarGridEffect = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.LightColors.monthlyCalendarGridEffect.toArgb())),
            monthlyCalendarDayCellBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground.toArgb())),
            monthlyCalendarEmptyCellBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarEmptyCellBackground.toArgb())),
            monthlyCalendarTodayCellBorder = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.LightColors.monthlyCalendarTodayCellBorder.toArgb())),
            monthlyCalendarHeaderBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.LightColors.monthlyCalendarHeaderBackground.toArgb())),
            monthlyCalendarDayNumberNormal = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.LightColors.monthlyCalendarDayNumberNormal.toArgb())),
            miniMonthTodayHighlightBackground = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.LightColors.miniMonthTodayHighlightBackground.toArgb())),
            miniMonthDayNumberNormal = Color(prefs.getInt(AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.LightColors.miniMonthDayNumberNormal.toArgb()))
        )
    }
}

fun isColorDark(color: Color, background: Color): Boolean {
    val contrastingColor = Color(ColorUtils.compositeColors(color.toArgb(), background.toArgb()))
    return ColorUtils.calculateLuminance(contrastingColor.toArgb()) < 0.5
}