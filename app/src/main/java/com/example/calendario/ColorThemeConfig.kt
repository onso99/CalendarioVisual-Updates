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
        // --- CATEGORÍA: ESTÁNDAR --- //
        ColorThemeItem(
            label = "Primario",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_PRIMARY,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_PRIMARY,
            defaultLight = AppThemeSetup.LightColors.primary,
            defaultDark = AppThemeSetup.DarkColors.primary,
            category = "Estándar"
        ),
        ColorThemeItem(
            label = "Texto en Primario",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_ON_PRIMARY,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_ON_PRIMARY,
            defaultLight = AppThemeSetup.LightColors.onPrimary,
            defaultDark = AppThemeSetup.DarkColors.onPrimary,
            category = "Estándar"
        ),
        ColorThemeItem(
            label = "Fondo Principal",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.background,
            defaultDark = AppThemeSetup.DarkColors.background,
            category = "Estándar"
        ),
        ColorThemeItem(
            label = "Fondo Superficie",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_SURFACE,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_SURFACE,
            defaultLight = AppThemeSetup.LightColors.surface,
            defaultDark = AppThemeSetup.DarkColors.surface,
            category = "Estándar"
        ),

        // --- CATEGORÍA: TEXTOS --- //
        ColorThemeItem(
            label = "Texto Normal",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_ON_SCREEN_TEXT_NORMAL,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_ON_SCREEN_TEXT_NORMAL,
            defaultLight = AppThemeSetup.LightColors.onScreenTextNormal,
            defaultDark = AppThemeSetup.DarkColors.onScreenTextNormal,
            category = "Textos"
        ),
        ColorThemeItem(
            label = "Texto Secundario",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_ON_SCREEN_TEXT_SECONDARY,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_ON_SCREEN_TEXT_SECONDARY,
            defaultLight = AppThemeSetup.LightColors.onScreenTextSecondary,
            defaultDark = AppThemeSetup.DarkColors.onScreenTextSecondary,
            category = "Textos"
        ),
        ColorThemeItem(
            label = "Texto Cumpleaños/Aniv.",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_EVENT_LIST_ITEM_BIRTHDAY_TEXT,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_EVENT_LIST_ITEM_BIRTHDAY_TEXT,
            defaultLight = AppThemeSetup.LightColors.eventListItemBirthdayText,
            defaultDark = AppThemeSetup.DarkColors.eventListItemBirthdayText,
            category = "Textos"
        ),
        ColorThemeItem(
            label = "Texto Festivo",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_EVENT_LIST_ITEM_HOLIDAY_TEXT,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_EVENT_LIST_ITEM_HOLIDAY_TEXT,
            defaultLight = AppThemeSetup.LightColors.eventListItemHolidayText,
            defaultDark = AppThemeSetup.DarkColors.eventListItemHolidayText,
            category = "Textos"
        ),

        // --- CATEGORÍA: CALENDARIO MENSUAL --- //
        ColorThemeItem(
            label = "Fondo Grid Calendario",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.monthlyCalendarGridBackground,
            defaultDark = AppThemeSetup.DarkColors.monthlyCalendarGridBackground,
            category = "Calendario Mensual"
        ),
        ColorThemeItem(
            label = "Celda Mes Actual",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.monthlyCalendarDayCellBackground,
            defaultDark = AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground,
            category = "Calendario Mensual"
        ),
        ColorThemeItem(
            label = "Celda Otros Meses",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.monthlyCalendarEmptyCellBackground,
            defaultDark = AppThemeSetup.DarkColors.monthlyCalendarEmptyCellBackground,
            category = "Calendario Mensual"
        ),
        ColorThemeItem(
            label = "Días Otros Meses",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_GHOST,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_GHOST,
            defaultLight = AppThemeSetup.LightColors.monthlyCalendarDayNumberGhost,
            defaultDark = AppThemeSetup.DarkColors.monthlyCalendarDayNumberGhost,
            category = "Calendario Mensual"
        ),
        ColorThemeItem(
            label = "Cabecera Días Semana",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.monthlyCalendarHeaderBackground,
            defaultDark = AppThemeSetup.DarkColors.monthlyCalendarHeaderBackground,
            category = "Calendario Mensual"
        ),
         ColorThemeItem(
            label = "Texto Cabecera Días",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_TEXT,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_TEXT,
            defaultLight = AppThemeSetup.LightColors.monthlyCalendarHeaderText,
            defaultDark = AppThemeSetup.DarkColors.monthlyCalendarHeaderText,
            category = "Calendario Mensual"
        ),

        // --- CATEGORÍA: COMPONENTES --- //
        ColorThemeItem(
            label = "Fondo Menú Desplegable",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_DROPDOWN_MENU_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_DROPDOWN_MENU_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.dropdownMenuBackground,
            defaultDark = AppThemeSetup.DarkColors.dropdownMenuBackground,
            category = "Componentes"
        ),
        ColorThemeItem(
            label = "Fondo Botón 'Pendientes'",
            lightThemeKey = AppThemeSetup.ColorKeys.LIGHT_FILTER_BUTTON_BACKGROUND,
            darkThemeKey = AppThemeSetup.ColorKeys.DARK_FILTER_BUTTON_BACKGROUND,
            defaultLight = AppThemeSetup.LightColors.filterButtonBackground,
            defaultDark = AppThemeSetup.DarkColors.filterButtonBackground,
            category = "Componentes"
        )
    )
}
