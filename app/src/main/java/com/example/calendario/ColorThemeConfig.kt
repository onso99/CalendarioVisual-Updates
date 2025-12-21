package com.example.calendario

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

data class ColorThemeItem(
    @field:StringRes val labelRes: Int,
    val lightThemeKey: String,
    val darkThemeKey: String,
    val defaultLight: Color,
    val defaultDark: Color,
    val category: String, // Keep category as String for grouping
    val isSeparator: Boolean = false
)

object ColorThemeConfig {
    val colorThemeItems = listOf(
        // --- CATEGORÍA: GENERAL ---
        ColorThemeItem(R.string.header_background, AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.ColorKeys.DARK_CABECERA, AppConstants.LightColors.cabecera, AppConstants.DarkColors.cabecera, "General"),
        ColorThemeItem(R.string.screen_background, AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground, AppConstants.DarkColors.settingsBackground, "General"),
        ColorThemeItem(R.string.section_background, AppConstants.ColorKeys.LIGHT_FONDO_SECCIONES, AppConstants.ColorKeys.DARK_FONDO_SECCIONES, AppConstants.LightColors.fondoSecciones, AppConstants.DarkColors.fondoSecciones, "General"),
        ColorThemeItem(R.string.dialog_background, AppConstants.ColorKeys.LIGHT_FONDO_DIALOGOS, AppConstants.ColorKeys.DARK_FONDO_DIALOGOS, AppConstants.LightColors.fondoDialogos, AppConstants.DarkColors.fondoDialogos, "General"),
        ColorThemeItem(R.string.dropdown_menu_background, AppConstants.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, AppConstants.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND, AppConstants.LightColors.dropdownMenuBackground, AppConstants.DarkColors.dropdownMenuBackground, "General"),
        ColorThemeItem(R.string.warning, AppConstants.ColorKeys.LIGHT_ERROR, AppConstants.ColorKeys.DARK_ERROR, AppConstants.LightColors.error, AppConstants.DarkColors.error, "General"),
        ColorThemeItem(R.string.system_text, AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM, AppConstants.ColorKeys.DARK_TEXT_SYSTEM, AppConstants.LightColors.textSystem, AppConstants.DarkColors.textSystem, "General"),
        ColorThemeItem(R.string.sundays_and_holidays, AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday, AppConstants.DarkColors.textSundayHoliday, "General"),
        
        // --- CATEGORÍA: LISTA DE EVENTOS ---
        ColorThemeItem(R.string.title_text, AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR, AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR, AppConstants.LightColors.eventListTitleColor, AppConstants.DarkColors.eventListTitleColor, "Lista de Eventos"),
        ColorThemeItem(R.string.pending_button_background, AppConstants.ColorKeys.LIGHT_TOGGLE_BUTTON_SELECTED_BACKGROUND, AppConstants.ColorKeys.DARK_TOGGLE_BUTTON_SELECTED_BACKGROUND, AppConstants.LightColors.toggleButtonselectedBackground, AppConstants.DarkColors.toggleButtonselectedBackground, "Lista de Eventos"),
        ColorThemeItem(R.string.event_list_background, AppConstants.ColorKeys.LIGHT_BACKGROUND, AppConstants.ColorKeys.DARK_BACKGROUND, AppConstants.LightColors.background, AppConstants.DarkColors.background, "Lista de Eventos"),
        ColorThemeItem(R.string.today_highlight_list, AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor, AppConstants.DarkColors.todayHighlightColor, "Lista de Eventos"),
        ColorThemeItem(0, "", "", Color.Transparent, Color.Transparent, "Lista de Eventos", isSeparator = true),
        ColorThemeItem(R.string.events, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT, AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT, AppConstants.LightColors.textEventDefault, AppConstants.DarkColors.textEventDefault, "Lista de Eventos"),
        ColorThemeItem(R.string.birthdays, AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday, AppConstants.DarkColors.textBirthday, "Lista de Eventos"),
        ColorThemeItem(R.string.event_1, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.ColorKeys.DARK_TEXT_EVENT_1, AppConstants.LightColors.textEvent1, AppConstants.DarkColors.textEvent1, "Lista de Eventos"),
        ColorThemeItem(R.string.event_2, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.ColorKeys.DARK_TEXT_EVENT_2, AppConstants.LightColors.textEvent2, AppConstants.DarkColors.textEvent2, "Lista de Eventos"),

        // --- CATEGORÍA: CALENDARIO MENSUAL ---
        ColorThemeItem(R.string.calendar_background, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground, AppConstants.DarkColors.monthlyCalendarGridBackground, "Calendario Mensual"),
        ColorThemeItem(R.string.current_month_cell, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground, AppConstants.DarkColors.monthlyCalendarDayCellBackground, "Calendario Mensual"),
        ColorThemeItem(R.string.other_month_cell, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarEmptyCellBackground, AppConstants.DarkColors.monthlyCalendarEmptyCellBackground, "Calendario Mensual"),
        ColorThemeItem(R.string.today_border, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.LightColors.monthlyCalendarTodayCellBorder, AppConstants.DarkColors.monthlyCalendarTodayCellBorder, "Calendario Mensual"),
        ColorThemeItem(R.string.normal_day_number, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.LightColors.monthlyCalendarDayNumberNormal, AppConstants.DarkColors.monthlyCalendarDayNumberNormal, "Calendario Mensual"),
        ColorThemeItem(R.string.week_day_header, AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.LightColors.monthlyCalendarHeaderBackground, AppConstants.DarkColors.monthlyCalendarHeaderBackground, "Calendario Mensual"),

        // --- CATEGORÍA: CALENDARIO ANUAL (MINI) ---
        ColorThemeItem(R.string.mini_today_highlight, AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.LightColors.miniMonthTodayHighlightBackground, AppConstants.DarkColors.miniMonthTodayHighlightBackground, "Calendario Anual"),
        ColorThemeItem(R.string.mini_normal_day, AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.LightColors.miniMonthDayNumberNormal, AppConstants.DarkColors.miniMonthDayNumberNormal, "Calendario Anual")
    )
}
