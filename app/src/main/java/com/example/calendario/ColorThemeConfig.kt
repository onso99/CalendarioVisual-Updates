package com.example.calendario

import androidx.compose.ui.graphics.Color

data class ColorThemeItem(
    val label: String,
    val lightThemeKey: String,
    val darkThemeKey: String,
    val defaultLight: Color,
    val defaultDark: Color,
    val category: String,
    val isSeparator: Boolean = false // Nuevo campo para identificar separadores
)

object ColorThemeConfig {
    val colorThemeItems = listOf(
        // --- CATEGORÍA: GENERAL ---
        ColorThemeItem("Cabecera", AppConstants.ColorKeys.LIGHT_CABECERA, AppConstants.ColorKeys.DARK_CABECERA, AppConstants.LightColors.cabecera, AppConstants.DarkColors.cabecera, "General"),
        ColorThemeItem("Fondo Pantallas", AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND, AppConstants.LightColors.settingsBackground, AppConstants.DarkColors.settingsBackground, "General"),
        ColorThemeItem("Fondo Secciones", AppConstants.ColorKeys.LIGHT_FONDO_SECCIONES, AppConstants.ColorKeys.DARK_FONDO_SECCIONES, AppConstants.LightColors.fondoSecciones, AppConstants.DarkColors.fondoSecciones, "General"),
        ColorThemeItem("Fondo Diálogos", AppConstants.ColorKeys.LIGHT_FONDO_DIALOGOS, AppConstants.ColorKeys.DARK_FONDO_DIALOGOS, AppConstants.LightColors.fondoDialogos, AppConstants.DarkColors.fondoDialogos, "General"),
        ColorThemeItem("Fondo Menú Desplegable", AppConstants.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, AppConstants.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND, AppConstants.LightColors.dropdownMenuBackground, AppConstants.DarkColors.dropdownMenuBackground, "General"),
        ColorThemeItem("Advertencia", AppConstants.ColorKeys.LIGHT_ERROR, AppConstants.ColorKeys.DARK_ERROR, AppConstants.LightColors.error, AppConstants.DarkColors.error, "General"),
        ColorThemeItem("Texto de sistema", AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM, AppConstants.ColorKeys.DARK_TEXT_SYSTEM, AppConstants.LightColors.textSystem, AppConstants.DarkColors.textSystem, "General"),
        ColorThemeItem("Domingos y Festivos", AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday, AppConstants.DarkColors.textSundayHoliday, "General"),
        
        // --- CATEGORÍA: LISTA DE EVENTOS ---
        ColorThemeItem("Texto de Título", AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR, AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR, AppConstants.LightColors.eventListTitleColor, AppConstants.DarkColors.eventListTitleColor, "Lista de Eventos"),
        ColorThemeItem("Fondo botón Pendientes", AppConstants.ColorKeys.LIGHT_TOGGLE_BUTTON_SELECTED_BACKGROUND, AppConstants.ColorKeys.DARK_TOGGLE_BUTTON_SELECTED_BACKGROUND, AppConstants.LightColors.toggleButtonselectedBackground, AppConstants.DarkColors.toggleButtonselectedBackground, "Lista de Eventos"),
        ColorThemeItem("Fondo lista de eventos", AppConstants.ColorKeys.LIGHT_BACKGROUND, AppConstants.ColorKeys.DARK_BACKGROUND, AppConstants.LightColors.background, AppConstants.DarkColors.background, "Lista de Eventos"),
        ColorThemeItem("Resaltado Día Actual (Lista)", AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR, AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR, AppConstants.LightColors.todayHighlightColor, AppConstants.DarkColors.todayHighlightColor, "Lista de Eventos"),
        ColorThemeItem("SEPARATOR", "", "", Color.Transparent, Color.Transparent, "Lista de Eventos", isSeparator = true),
        ColorThemeItem("Eventos", AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT, AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT, AppConstants.LightColors.textEventDefault, AppConstants.DarkColors.textEventDefault, "Lista de Eventos"),
        ColorThemeItem("Cumpleaños", AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday, AppConstants.DarkColors.textBirthday, "Lista de Eventos"),
        ColorThemeItem("Evento-1", AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.ColorKeys.DARK_TEXT_EVENT_1, AppConstants.LightColors.textEvent1, AppConstants.DarkColors.textEvent1, "Lista de Eventos"),
        ColorThemeItem("Evento-2", AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.ColorKeys.DARK_TEXT_EVENT_2, AppConstants.LightColors.textEvent2, AppConstants.DarkColors.textEvent2, "Lista de Eventos"),

        // --- CATEGORÍA: CALENDARIO MENSUAL ---
        ColorThemeItem("Fondo de calendario", AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppConstants.LightColors.monthlyCalendarGridBackground, AppConstants.DarkColors.monthlyCalendarGridBackground, "Calendario Mensual"),
        ColorThemeItem("Celda Mes Actual", AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarDayCellBackground, AppConstants.DarkColors.monthlyCalendarDayCellBackground, "Calendario Mensual"),
        ColorThemeItem("Celda Otros Meses", AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppConstants.LightColors.monthlyCalendarEmptyCellBackground, AppConstants.DarkColors.monthlyCalendarEmptyCellBackground, "Calendario Mensual"),
        ColorThemeItem("Borde Día Actual", AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppConstants.LightColors.monthlyCalendarTodayCellBorder, AppConstants.DarkColors.monthlyCalendarTodayCellBorder, "Calendario Mensual"),
        ColorThemeItem("Número Día Normal", AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppConstants.LightColors.monthlyCalendarDayNumberNormal, AppConstants.DarkColors.monthlyCalendarDayNumberNormal, "Calendario Mensual"),
        ColorThemeItem("Cabecera Días Semana", AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppConstants.LightColors.monthlyCalendarHeaderBackground, AppConstants.DarkColors.monthlyCalendarHeaderBackground, "Calendario Mensual"),

        // --- CATEGORÍA: CALENDARIO ANUAL (MINI) ---
        ColorThemeItem("Mini: Resaltado Día Actual", AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppConstants.LightColors.miniMonthTodayHighlightBackground, AppConstants.DarkColors.miniMonthTodayHighlightBackground, "Calendario Anual"),
        ColorThemeItem("Mini: Día Normal", AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppConstants.LightColors.miniMonthDayNumberNormal, AppConstants.DarkColors.miniMonthDayNumberNormal, "Calendario Anual")
    )
}
