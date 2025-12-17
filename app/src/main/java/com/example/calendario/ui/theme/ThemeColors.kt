package com.example.calendario.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.example.calendario.AppConstants
import java.lang.IllegalArgumentException

data class CustomColors(
    val cabecera: Color,
    val fondoSecciones: Color,
    val fondoDialogos: Color,
    val settingsBackground: Color,
    val background: Color,
    val onBackground: Color,
    val error: Color,
    val textSystem: Color,
    val textSundayHoliday: Color,
    val textBirthday: Color,
    val textEventDefault: Color,
    val textEvent1: Color,
    val textEvent2: Color,
    val dropdownMenuBackground: Color,
    val todayHighlightColor: Color,
    val eventListTitleColor: Color,
    val toggleButtonSelectedBackground: Color,
    val monthlyCalendarGridBackground: Color,
    val monthlyCalendarDayCellBackground: Color,
    val monthlyCalendarEmptyCellBackground: Color,
    val monthlyCalendarTodayCellBorder: Color,
    val monthlyCalendarHeaderBackground: Color,
    val monthlyCalendarDayNumberNormal: Color,
    val miniMonthTodayHighlightBackground: Color,
    val miniMonthDayNumberNormal: Color
)

fun getThemeColors(context: Context, darkTheme: Boolean): CustomColors {
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    return CustomColors(
        cabecera = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_CABECERA else AppConstants.ColorKeys.LIGHT_CABECERA, if (darkTheme) AppConstants.DarkColors.cabecera else AppConstants.LightColors.cabecera),
        fondoSecciones = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_FONDO_SECCIONES else AppConstants.ColorKeys.LIGHT_FONDO_SECCIONES, if (darkTheme) AppConstants.DarkColors.fondoSecciones else AppConstants.LightColors.fondoSecciones),
        fondoDialogos = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_FONDO_DIALOGOS else AppConstants.ColorKeys.LIGHT_FONDO_DIALOGOS, if (darkTheme) AppConstants.DarkColors.fondoDialogos else AppConstants.LightColors.fondoDialogos),
        settingsBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND else AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, if (darkTheme) AppConstants.DarkColors.settingsBackground else AppConstants.LightColors.settingsBackground),
        background = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_BACKGROUND else AppConstants.ColorKeys.LIGHT_BACKGROUND, if (darkTheme) AppConstants.DarkColors.background else AppConstants.LightColors.background),
        onBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_ON_BACKGROUND else AppConstants.ColorKeys.LIGHT_ON_BACKGROUND, if (darkTheme) AppConstants.DarkColors.onBackground else AppConstants.LightColors.onBackground),
        error = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_ERROR else AppConstants.ColorKeys.LIGHT_ERROR, if (darkTheme) AppConstants.DarkColors.error else AppConstants.LightColors.error),
        textSystem = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TEXT_SYSTEM else AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM, if (darkTheme) AppConstants.DarkColors.textSystem else AppConstants.LightColors.textSystem),
        textSundayHoliday = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY else AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, if (darkTheme) AppConstants.DarkColors.textSundayHoliday else AppConstants.LightColors.textSundayHoliday),
        textBirthday = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY else AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, if (darkTheme) AppConstants.DarkColors.textBirthday else AppConstants.LightColors.textBirthday),
        textEventDefault = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT else AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT, if (darkTheme) AppConstants.DarkColors.textEventDefault else AppConstants.LightColors.textEventDefault),
        textEvent1 = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TEXT_EVENT_1 else AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, if (darkTheme) AppConstants.DarkColors.textEvent1 else AppConstants.LightColors.textEvent1),
        textEvent2 = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TEXT_EVENT_2 else AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, if (darkTheme) AppConstants.DarkColors.textEvent2 else AppConstants.LightColors.textEvent2),
        dropdownMenuBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND else AppConstants.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, if (darkTheme) AppConstants.DarkColors.dropdownMenuBackground else AppConstants.LightColors.dropdownMenuBackground),
        todayHighlightColor = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR else AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, if (darkTheme) AppConstants.DarkColors.todayHighlightColor else AppConstants.LightColors.todayHighlightColor),
        eventListTitleColor = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR else AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR, if (darkTheme) AppConstants.DarkColors.eventListTitleColor else AppConstants.LightColors.eventListTitleColor),
        toggleButtonSelectedBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_TOGGLE_BUTTON_SELECTED_BACKGROUND else AppConstants.ColorKeys.LIGHT_TOGGLE_BUTTON_SELECTED_BACKGROUND, if (darkTheme) AppConstants.DarkColors.toggleButtonselectedBackground else AppConstants.LightColors.toggleButtonselectedBackground),
        monthlyCalendarGridBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND else AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, if (darkTheme) AppConstants.DarkColors.monthlyCalendarGridBackground else AppConstants.LightColors.monthlyCalendarGridBackground),
        monthlyCalendarDayCellBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND else AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, if (darkTheme) AppConstants.DarkColors.monthlyCalendarDayCellBackground else AppConstants.LightColors.monthlyCalendarDayCellBackground),
        monthlyCalendarEmptyCellBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND else AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, if (darkTheme) AppConstants.DarkColors.monthlyCalendarEmptyCellBackground else AppConstants.LightColors.monthlyCalendarEmptyCellBackground),
        monthlyCalendarTodayCellBorder = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER else AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, if (darkTheme) AppConstants.DarkColors.monthlyCalendarTodayCellBorder else AppConstants.LightColors.monthlyCalendarTodayCellBorder),
        monthlyCalendarHeaderBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND else AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, if (darkTheme) AppConstants.DarkColors.monthlyCalendarHeaderBackground else AppConstants.LightColors.monthlyCalendarHeaderBackground),
        monthlyCalendarDayNumberNormal = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL else AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, if (darkTheme) AppConstants.DarkColors.monthlyCalendarDayNumberNormal else AppConstants.LightColors.monthlyCalendarDayNumberNormal),
        miniMonthTodayHighlightBackground = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND else AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, if (darkTheme) AppConstants.DarkColors.miniMonthTodayHighlightBackground else AppConstants.LightColors.miniMonthTodayHighlightBackground),
        miniMonthDayNumberNormal = getThemeColor(prefs, if (darkTheme) AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL else AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, if (darkTheme) AppConstants.DarkColors.miniMonthDayNumberNormal else AppConstants.LightColors.miniMonthDayNumberNormal)
    )
}

private fun getThemeColor(prefs: SharedPreferences, key: String, defaultColor: Color): Color {
    if (!prefs.contains(key)) return defaultColor
    return when (val value = prefs.all[key]) {
        is Int -> Color(value)
        is String -> {
            try {
                Color(value.toColorInt())
            } catch (_: IllegalArgumentException) {
                defaultColor
            }
        }
        else -> defaultColor
    }
}

fun blendWithBackground(colorToBlend: Color, backgroundColor: Color, ratio: Float = 0.5f): Color {
    val blended = ColorUtils.compositeColors(colorToBlend.toArgb(), backgroundColor.toArgb())
    return Color(blended)
}

fun isColorDark(color: Color): Boolean {
    return ColorUtils.calculateLuminance(color.toArgb()) < 0.5
}
