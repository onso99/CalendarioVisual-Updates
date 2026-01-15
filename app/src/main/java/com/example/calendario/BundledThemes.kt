package com.example.calendario

object BundledThemes {
    val themes = listOf(
        mapOf(
            "themeManifest" to mapOf(
                "version" to AppConstants.CURRENT_THEME_VERSION.toString(),
                "appName" to AppConstants.APP_SIGNATURE,
                "name" to "Océano",
                AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE to "gradient"
            ),
            "lightTheme" to mapOf(
                AppConstants.ColorKeys.LIGHT_CABECERA to "#FF0077C2",
                AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND to "#FFE0F7FA",
                AppConstants.ColorKeys.LIGHT_FONDO_SECCIONES to "#FFB2EBF2",
                AppConstants.ColorKeys.LIGHT_FONDO_DIALOGOS to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM to "#FF000000",
                AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY to "#FFD32F2F",
                AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR to "#FF004D40",
                AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR to "#80B2EBF2",
                AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY to "#FF0000FF",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT to "#FF000000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1 to "#FF008000",
                AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2 to "#FFFF00FF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF81D4FA",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT to "#FF0077C2",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FFFFFFFF",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND to "#FFE0F7FA",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER to "#FF0077C2",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL to "#FF000000",
                AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND to "#FF4FC3F7",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#4081D4FA",
                AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL to "#E6000000"
            ),
            "darkTheme" to mapOf(
                AppConstants.ColorKeys.DARK_CABECERA to "#FF01579B",
                AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND to "#FF001F29",
                AppConstants.ColorKeys.DARK_FONDO_SECCIONES to "#FF003341",
                AppConstants.ColorKeys.DARK_FONDO_DIALOGOS to "#FF001F29",
                AppConstants.ColorKeys.DARK_TEXT_SYSTEM to "#FFE0F7FA",
                AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY to "#FFFF8A80",
                AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR to "#FF80DEEA",
                AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR to "#804FC3F7",
                AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY to "#FF82B1FF",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT to "#FFE0F7FA",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_1 to "#FF69F0AE",
                AppConstants.ColorKeys.DARK_TEXT_EVENT_2 to "#FFFF80AB",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND to "#FF004D40",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT to "#FF01579B",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND to "#FF003341",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND to "#FF001F29",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER to "#FF4FC3F7",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL to "#FFE0F7FA",
                AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND to "#FF0077C2",
                AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND to "#4001579B",
                AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL to "#E6E0F7FA"
            )
        )
        // Añade más temas aquí si quieres
    )
}
