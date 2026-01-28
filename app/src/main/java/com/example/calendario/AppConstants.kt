package com.example.calendario

import androidx.compose.ui.graphics.Color

object AppConstants {
    // SharedPreferences
    const val APP_SETTINGS_PREFS_NAME = "app_settings_prefs"

    // Preference Keys
    const val KEY_START_OF_WEEK = "start_of_week"
    const val KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW = "show_week_number_in_year_view"
    const val KEY_THEME_SETTING = "theme_setting_key"
    const val KEY_LIGHT_THEME_NAME = "light_theme_name_key"
    const val KEY_DARK_THEME_NAME = "dark_theme_name_key"
    const val KEY_EVENT_1_KEYWORD = "event_1_keyword"
    const val KEY_EVENT_2_KEYWORD = "event_2_keyword"
    const val KEY_MONTHLY_CALENDAR_EFFECT_TYPE = "monthly_calendar_effect_type"
    const val KEY_FAVORITE_CALENDAR_ID = "favorite_calendar_id"
    const val CURRENT_THEME_VERSION = 6
    const val APP_SIGNATURE = "Calendario"

    object ColorKeys {
        // Light Theme
        const val LIGHT_CABECERA = "light_cabecera"
        const val LIGHT_FONDO_SECCIONES = "light_fondo_secciones"
        const val LIGHT_FONDO_DIALOGOS = "light_fondo_dialogos"
        const val LIGHT_SETTINGS_BACKGROUND = "light_settings_background"
        const val LIGHT_TEXT_SYSTEM = "light_text_system"
        const val LIGHT_TEXT_SUNDAY_HOLIDAY = "light_text_sunday_holiday"
        const val LIGHT_TEXT_BIRTHDAY = "light_text_birthday"
        const val LIGHT_TEXT_EVENT_DEFAULT = "light_text_event_default"
        const val LIGHT_TEXT_EVENT_1 = "light_text_event_1"
        const val LIGHT_TEXT_EVENT_2 = "light_text_event_2"
        const val LIGHT_TODAY_HIGHLIGHT_COLOR = "light_today_highlight_color"
        const val LIGHT_EVENT_LIST_TITLE_COLOR = "light_event_list_title_color"
        const val LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND = "light_monthly_calendar_grid_background"
        const val LIGHT_MONTHLY_CALENDAR_GRID_EFFECT = "light_monthly_calendar_grid_effect"
        const val LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "light_monthly_calendar_day_cell_background"
        const val LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND = "light_monthly_calendar_empty_cell_background"
        const val LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER = "light_monthly_calendar_today_cell_border"
        const val LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND = "light_monthly_calendar_header_background"
        const val LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL = "light_monthly_calendar_day_number_normal"
        const val LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL = "light_mini_month_day_number_normal"
        const val LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "light_mini_month_today_highlight_background"

        // Dark Theme
        const val DARK_CABECERA = "dark_cabecera"
        const val DARK_FONDO_SECCIONES = "dark_fondo_secciones"
        const val DARK_FONDO_DIALOGOS = "dark_fondo_dialogos"
        const val DARK_SETTINGS_BACKGROUND = "dark_settings_background"
        const val DARK_TEXT_SYSTEM = "dark_text_system"
        const val DARK_TEXT_SUNDAY_HOLIDAY = "dark_text_sunday_holiday"
        const val DARK_TEXT_BIRTHDAY = "dark_text_birthday"
        const val DARK_TEXT_EVENT_DEFAULT = "dark_text_event_default"
        const val DARK_TEXT_EVENT_1 = "dark_text_event_1"
        const val DARK_TEXT_EVENT_2 = "dark_text_event_2"
        const val DARK_EVENT_LIST_TITLE_COLOR = "dark_event_list_title_color"
        const val DARK_TODAY_HIGHLIGHT_COLOR = "dark_today_highlight_color"
        const val DARK_MONTHLY_CALENDAR_GRID_BACKGROUND = "dark_monthly_calendar_grid_background"
        const val DARK_MONTHLY_CALENDAR_GRID_EFFECT = "dark_monthly_calendar_grid_effect"
        const val DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "dark_monthly_calendar_day_cell_background"
        const val DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND = "dark_monthly_calendar_empty_cell_background"
        const val DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER = "dark_monthly_calendar_today_cell_border"
        const val DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND = "dark_monthly_calendar_header_background"
        const val DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL = "dark_monthly_calendar_day_number_normal"
        const val DARK_MINI_MONTH_DAY_NUMBER_NORMAL = "dark_mini_month_day_number_normal"
        const val DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "dark_mini_month_today_highlight_background"
    }

    object LightColors {
        val cabecera = Color(0xFF2196F3)
        val settingsBackground = Color(0xFFF6F6FF)
        val fondoSecciones = Color(0xFFD6ECFD)
        val fondoDialogos = Color(0xFFFFFFFF)
        val textSystem = Color(0xFF000000)
        val textSundayHoliday = Color(0xFFFF0000)
        val textBirthday = Color(0xFF0000FF)
        val textEventDefault = Color.Black
        val textEvent1 = Color(0xFF008000) // Verde
        val textEvent2 = Color(0xFFFF00FF) // Magenta
        val eventListTitleColor = Color(0xFF0A4C87)
        val todayHighlightColor = Color(0x91FFEA82)
        val monthlyCalendarGridBackground = Color(0xFF1F93F2)
        val monthlyCalendarGridEffect = Color(0x808FC7BA)
        val monthlyCalendarDayCellBackground = Color(0xFFFFFFFF)
        val monthlyCalendarEmptyCellBackground = Color(0xFFC8D9E1)
        val monthlyCalendarTodayCellBorder = Color(0xFF2196F3)
        val monthlyCalendarDayNumberNormal = Color(0xFF000000)
        val monthlyCalendarHeaderBackground = Color(0xFFADD1FA)
        val miniMonthTodayHighlightBackground = Color(0x262196F3)
        val miniMonthDayNumberNormal = Color(0xE6000000)
    }

    object DarkColors {
        val cabecera = Color(0xFF1051B4)
        val settingsBackground = Color(0xFF2C2C2C)
        val fondoSecciones = Color(0xFF274566)
        val fondoDialogos = Color(0xFF2C2C2C)
        val textSystem = Color(0xFFE0E0E0)
        val textSundayHoliday = Color(0xFFE57373)
        val textBirthday = Color(0xFFAECBFF)
        val textEventDefault = Color(0xFFE0E0E0)
        val textEvent1 = Color(0xFF66BB6A) // Verde claro
        val textEvent2 = Color(0xFFF06292) // Rosa
        val eventListTitleColor = Color(0xFFCF9A21)
        val todayHighlightColor = Color(0x8EACACAC)
        val monthlyCalendarGridBackground = Color(0x800000AA)
        val monthlyCalendarGridEffect = Color(0x8000304D)
        val monthlyCalendarDayCellBackground = Color(0xFF6A6A6A)
        val monthlyCalendarEmptyCellBackground = Color(0xFF4F4F4F)
        val monthlyCalendarTodayCellBorder = Color(0xFFD28C45)
        val monthlyCalendarDayNumberNormal = Color(0xFFE0E0E0)
        val monthlyCalendarHeaderBackground = Color(0xFF0060BF)
        val miniMonthTodayHighlightBackground = Color(0x332173ED)
        val miniMonthDayNumberNormal = Color(0xE6E0E0E0)
    }
}