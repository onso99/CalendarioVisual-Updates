package com.example.calendario

import androidx.compose.ui.graphics.Color

object AppConstants {
    // SharedPreferences
    const val APP_SETTINGS_PREFS_NAME = "app_settings_prefs"
    const val HOLIDAY_PREFS_NAME = "holiday_adjustments"

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
    const val KEY_HOLIDAY_ADJUSTMENTS = "adjustments"
    const val KEY_LOGGING_ENABLED = "logging_enabled_key"
    const val CURRENT_THEME_VERSION = 7
    const val APP_SIGNATURE = "Calendario"

    object ColorKeys {
        // Light Theme
        const val LIGHT_CABECERA = "light_cabecera"
        const val LIGHT_SETTINGS_BACKGROUND = "light_settings_background"
        const val LIGHT_TEXT_SUNDAY_HOLIDAY = "light_text_sunday_holiday"
        const val LIGHT_TEXT_BIRTHDAY = "light_text_birthday"
        const val LIGHT_TEXT_EVENT_1 = "light_text_event_1"
        const val LIGHT_TEXT_EVENT_2 = "light_text_event_2"
        const val LIGHT_TODAY_HIGHLIGHT_COLOR = "light_today_highlight_color"
        const val LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND = "light_monthly_calendar_grid_background"
        const val LIGHT_MONTHLY_CALENDAR_GRID_EFFECT = "light_monthly_calendar_grid_effect"
        const val LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "light_monthly_calendar_day_cell_background"
        const val LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "light_mini_month_today_highlight_background"

        // Dark Theme
        const val DARK_CABECERA = "dark_cabecera"
        const val DARK_SETTINGS_BACKGROUND = "dark_settings_background"
        const val DARK_TEXT_SUNDAY_HOLIDAY = "dark_text_sunday_holiday"
        const val DARK_TEXT_BIRTHDAY = "dark_text_birthday"
        const val DARK_TEXT_EVENT_1 = "dark_text_event_1"
        const val DARK_TEXT_EVENT_2 = "dark_text_event_2"
        const val DARK_TODAY_HIGHLIGHT_COLOR = "dark_today_highlight_color"
        const val DARK_MONTHLY_CALENDAR_GRID_BACKGROUND = "dark_monthly_calendar_grid_background"
        const val DARK_MONTHLY_CALENDAR_GRID_EFFECT = "dark_monthly_calendar_grid_effect"
        const val DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "dark_monthly_calendar_day_cell_background"
        const val DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND = "dark_mini_month_today_highlight_background"
    }

    object LightColors {
        val cabecera = Color(0xFF0077C2)
        val settingsBackground = Color(0xFFF0F9FE)
        val textSundayHoliday = Color(0xFFD32F2F)
        val textBirthday = Color(0xFF0000FF)
        val textEvent1 = Color(0xFF008000)
        val textEvent2 = Color(0xFF6700FF)
        val todayHighlightColor = Color(0x30D6CF15)
        val monthlyCalendarGridBackground = Color(0xFF0077C2)
        val monthlyCalendarGridEffect = Color(0xFF38A391)
        val monthlyCalendarDayCellBackground = Color(0xFFFFFFFF)
        val miniMonthTodayHighlightBackground = Color(0x4081D4FA)
    }

    object DarkColors {
        val cabecera = Color(0xFF01579B)
        val settingsBackground = Color(0xFF001F29)
        val textSundayHoliday = Color(0xFFFF8A80)
        val textBirthday = Color(0xFF82B1FF)
        val textEvent1 = Color(0xFF69F0AE)
        val textEvent2 = Color(0xFFFF80AB)
        val todayHighlightColor = Color(0x804FC3F7)
        val monthlyCalendarGridBackground = Color(0xFF004D40)
        val monthlyCalendarGridEffect = Color(0xFF01579B)
        val monthlyCalendarDayCellBackground = Color(0xFF003341)
        val miniMonthTodayHighlightBackground = Color(0x4001579B)
    }
}
