package com.example.calendario

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

data class ColorThemeItem(
    @field:StringRes val labelRes: Int,
    val lightThemeKey: String,
    val darkThemeKey: String,
    val defaultLight: Color,
    val defaultDark: Color,
    val category: String,
    val isSeparator: Boolean = false,
    val isIndependent: Boolean = false
)

object ColorThemeConfig {
    val colorThemeItems = listOf(
        // --- CATEGORÍA: TEMA (Afectan a la estética y añaden ***) ---
        ColorThemeItem(R.string.header_background, AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.ColorKeys.DARK_CABECERA, AppConstants.LightColors.cabecera, AppConstants.DarkColors.cabecera, "Tema"),
        ColorThemeItem(R.string.screen_background, AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground, AppConstants.DarkColors.settingsBackground, "Tema"),
        ColorThemeItem(R.string.calendar_background, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground, AppConstants.DarkColors.monthlyCalendarGridBackground, "Tema"),
        ColorThemeItem(R.string.today_highlight_list, AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor, AppConstants.DarkColors.todayHighlightColor, "Tema"),
        ColorThemeItem(R.string.current_month_cell, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground, AppConstants.DarkColors.monthlyCalendarDayCellBackground, "Tema"),
        ColorThemeItem(R.string.effect, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.LightColors.monthlyCalendarGridEffect, AppConstants.DarkColors.monthlyCalendarGridEffect, "Tema"),

        // --- CATEGORÍA: PERMANENTES (Independientes, con Reset, NO añaden ***, Invariantes a modo oscuro) ---
        ColorThemeItem(R.string.note_icon_color, AppConstants.ColorKeys.LIGHT_NOTE_ICON_COLOR, AppConstants.ColorKeys.LIGHT_NOTE_ICON_COLOR, AppConstants.LightColors.noteIconColor, AppConstants.LightColors.noteIconColor, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.sundays_and_holidays, AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday, AppConstants.LightColors.textSundayHoliday, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.birthdays, AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday, AppConstants.LightColors.textBirthday, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.event_1, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.LightColors.textEvent1, AppConstants.LightColors.textEvent1, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.event_2, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.LightColors.textEvent2, AppConstants.LightColors.textEvent2, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.event_3, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_3, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_3, AppConstants.LightColors.textEvent3, AppConstants.LightColors.textEvent3, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.event_4, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_4, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_4, AppConstants.LightColors.textEvent4, AppConstants.LightColors.textEvent4, "Permanentes", isIndependent = true),
        ColorThemeItem(R.string.event_5, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_5, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_5, AppConstants.LightColors.textEvent5, AppConstants.LightColors.textEvent5, "Permanentes", isIndependent = true)
    )
}
