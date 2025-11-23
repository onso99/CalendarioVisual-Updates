package com.example.calendario

import androidx.compose.ui.graphics.Color

data class ColorThemeItem(
    val label: String,
    val lightThemeKey: String,
    val darkThemeKey: String,
    val defaultLight: Color,
    val defaultDark: Color,
    val category: String
)

object ColorThemeConfig {
    val colorThemeItems = listOf(
        // --- CATEGORÍA: GENERAL --- //
        ColorThemeItem("Cabecera", AppThemeSetup.ColorKeys.LIGHT_CABECERA, AppThemeSetup.ColorKeys.DARK_CABECERA, AppThemeSetup.LightColors.cabecera, AppThemeSetup.DarkColors.cabecera, "General"),
        ColorThemeItem("Fondo Pantallas", AppThemeSetup.ColorKeys.LIGHT_SETTINGS_BACKGROUND, AppThemeSetup.ColorKeys.DARK_SETTINGS_BACKGROUND, AppThemeSetup.LightColors.settingsBackground, AppThemeSetup.DarkColors.settingsBackground, "General"),
        ColorThemeItem("Fondo Cuadros/Diálogos", AppThemeSetup.ColorKeys.LIGHT_FONDO_PANTALLAS_DIALOGOS, AppThemeSetup.ColorKeys.DARK_FONDO_PANTALLAS_DIALOGOS, AppThemeSetup.LightColors.fondoPantallasDialogos, AppThemeSetup.DarkColors.fondoPantallasDialogos, "General"),
        ColorThemeItem("Fondo Menú Desplegable", AppThemeSetup.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND, AppThemeSetup.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND, AppThemeSetup.LightColors.dropdownMenuBackground, AppThemeSetup.DarkColors.dropdownMenuBackground, "General"),
        ColorThemeItem("Advertencia", AppThemeSetup.ColorKeys.LIGHT_ERROR, AppThemeSetup.ColorKeys.DARK_ERROR, AppThemeSetup.LightColors.error, AppThemeSetup.DarkColors.error, "General"),

        // --- CATEGORÍA: TEXTOS --- //
        ColorThemeItem("Texto de sistema", AppThemeSetup.ColorKeys.LIGHT_TEXT_SYSTEM, AppThemeSetup.ColorKeys.DARK_TEXT_SYSTEM, AppThemeSetup.LightColors.textSystem, AppThemeSetup.DarkColors.textSystem, "Textos"),
        ColorThemeItem("Domingos y Festivos", AppThemeSetup.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppThemeSetup.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY, AppThemeSetup.LightColors.textSundayHoliday, AppThemeSetup.DarkColors.textSundayHoliday, "Textos"),
        ColorThemeItem("Cumpleaños", AppThemeSetup.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppThemeSetup.ColorKeys.DARK_TEXT_BIRTHDAY, AppThemeSetup.LightColors.textBirthday, AppThemeSetup.DarkColors.textBirthday, "Textos"),

        // --- CATEGORÍA: LISTA DE EVENTOS --- //
        ColorThemeItem("Fondo lista de eventos", AppThemeSetup.ColorKeys.LIGHT_BACKGROUND, AppThemeSetup.ColorKeys.DARK_BACKGROUND, AppThemeSetup.LightColors.background, AppThemeSetup.DarkColors.background, "Lista de Eventos"),
        ColorThemeItem("Resaltado Día Actual (Lista)", AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, AppThemeSetup.LightColors.background, AppThemeSetup.DarkColors.background, "Lista de Eventos"),

        // --- CATEGORÍA: CALENDARIO MENSUAL --- //
        ColorThemeItem("Fondo de calendario", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarGridBackground, AppThemeSetup.DarkColors.monthlyCalendarGridBackground, "Calendario Mensual"),
        ColorThemeItem("Celda Mes Actual", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarDayCellBackground, AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground, "Calendario Mensual"),
        ColorThemeItem("Celda Otros Meses", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarEmptyCellBackground, AppThemeSetup.DarkColors.monthlyCalendarEmptyCellBackground, "Calendario Mensual"),
        ColorThemeItem("Borde Día Actual", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER, AppThemeSetup.LightColors.monthlyCalendarTodayCellBorder, AppThemeSetup.DarkColors.monthlyCalendarTodayCellBorder, "Calendario Mensual"),
        ColorThemeItem("Número Día Normal", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL, AppThemeSetup.LightColors.monthlyCalendarDayNumberNormal, AppThemeSetup.DarkColors.monthlyCalendarDayNumberNormal, "Calendario Mensual"),
        ColorThemeItem("Cabecera Días Semana", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND, AppThemeSetup.LightColors.monthlyCalendarHeaderBackground, AppThemeSetup.DarkColors.monthlyCalendarHeaderBackground, "Calendario Mensual"),
        ColorThemeItem("Texto Cabecera Días", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_TEXT, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_TEXT, AppThemeSetup.LightColors.monthlyCalendarHeaderText, AppThemeSetup.DarkColors.monthlyCalendarHeaderText, "Calendario Mensual"),
        ColorThemeItem("Indicador Evento", AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EVENT_INDICATOR, AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EVENT_INDICATOR, AppThemeSetup.LightColors.monthlyCalendarEventIndicator, AppThemeSetup.DarkColors.monthlyCalendarEventIndicator, "Calendario Mensual"),

        // --- CATEGORÍA: CALENDARIO ANUAL (MINI) --- //
        ColorThemeItem("Mini-cabecera", AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_HEADER_BACKGROUND, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_HEADER_BACKGROUND, AppThemeSetup.LightColors.miniMonthHeaderBackground, AppThemeSetup.DarkColors.miniMonthHeaderBackground, "Calendario Anual"),
        ColorThemeItem("Mini: Resaltado Día Actual", AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND, AppThemeSetup.LightColors.miniMonthTodayHighlightBackground, AppThemeSetup.DarkColors.miniMonthTodayHighlightBackground, "Calendario Anual"),
        ColorThemeItem("Mini: Día Normal", AppThemeSetup.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL, AppThemeSetup.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL, AppThemeSetup.LightColors.miniMonthDayNumberNormal, AppThemeSetup.DarkColors.miniMonthDayNumberNormal, "Calendario Anual")
    )
}
