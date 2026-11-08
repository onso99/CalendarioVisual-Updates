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
        // --- CATEGORÍA: FONDOS ---
        ColorThemeItem(R.string.header_background, AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.ColorKeys.DARK_CABECERA, AppConstants.LightColors.cabecera, AppConstants.DarkColors.cabecera, "Fondos"),
        ColorThemeItem(R.string.screen_background, AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground, AppConstants.DarkColors.settingsBackground, "Fondos"),
        ColorThemeItem(R.string.today_highlight_list, AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor, AppConstants.DarkColors.todayHighlightColor, "Fondos"),
        ColorThemeItem(R.string.calendar_background, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground, AppConstants.DarkColors.monthlyCalendarGridBackground, "Fondos"),
        ColorThemeItem(R.string.effect, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT, AppConstants.LightColors.monthlyCalendarGridEffect, AppConstants.DarkColors.monthlyCalendarGridEffect, "Fondos"),

        // --- CATEGORÍA: CELDAS ---
        ColorThemeItem(R.string.current_month_cell, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground, AppConstants.DarkColors.monthlyCalendarDayCellBackground, "Celdas"),
        ColorThemeItem(R.string.today_border_color, AppConstants.ColorKeys.LIGHT_TODAY_BORDER_COLOR, AppConstants.ColorKeys.DARK_TODAY_BORDER_COLOR, AppConstants.LightColors.todayBorderColor, AppConstants.DarkColors.todayBorderColor, "Celdas", isIndependent = true),
        ColorThemeItem(R.string.note_icon_color, AppConstants.ColorKeys.LIGHT_NOTE_ICON_COLOR, AppConstants.ColorKeys.LIGHT_NOTE_ICON_COLOR, AppConstants.LightColors.noteIconColor, AppConstants.LightColors.noteIconColor, "Celdas", isIndependent = true)
    )
}
