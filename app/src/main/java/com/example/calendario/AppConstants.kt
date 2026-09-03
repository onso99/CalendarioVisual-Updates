package com.example.calendario

import androidx.compose.ui.graphics.Color

object AppConstants {
    // SharedPreferences
    const val APP_SETTINGS_PREFS_NAME = "app_preferences" // Sincronizado con BackupManager
    const val HOLIDAY_PREFS_NAME = "holiday_adjustments" // Nombre unificado para Backup y Gestor
    const val ALARM_PREFS_NAME = "alarm_preferences"
    const val PERIOD_COLOR_PREFS_NAME = "period_colors"
    const val HOLIDAY_ADJUSTMENTS_PREFS_NAME = "holiday_adjustments" // Antes "holiday_adjustments_prefs"

    // Preference Keys
    const val KEY_START_OF_WEEK = "start_of_week"
    const val KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW = "show_week_number_in_year_view"
    const val KEY_THEME_SETTING = "theme_setting_key"
    const val KEY_LIGHT_THEME_NAME = "light_theme_name_key"
    const val KEY_DARK_THEME_NAME = "dark_theme_name_key"
    const val KEY_EVENT_1_KEYWORD = "event_1_keyword"
    const val KEY_EVENT_2_KEYWORD = "event_2_keyword"
    const val KEY_EVENT_1_PULSE = "event_1_pulse"
    const val KEY_EVENT_2_PULSE = "event_2_pulse"
    const val KEY_AUTO_BACKUP_DRIVE = "auto_backup_drive_key"
    const val KEY_BACKUP_FREQUENCY = "backup_frequency_key"
    const val KEY_LAST_BACKUP_TIME = "last_backup_timestamp_key"
    const val KEY_LAST_BACKUP_COUNT = "last_backup_count_key"
    const val KEY_LAST_BACKUP_SIZE = "last_backup_size_key"
    const val KEY_MONTHLY_CALENDAR_EFFECT_TYPE = "monthly_calendar_effect_type"
    const val KEY_FAVORITE_CALENDAR_ID = "favorite_calendar_id"
    const val KEY_LOGGING_ENABLED = "logging_enabled_key"
    const val KEY_DEFAULT_ALARM_OFFSET = "default_alarm_offset"
    const val KEY_DEFAULT_SNOOZE_INTERVAL = "default_snooze_interval"
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
        const val LIGHT_NOTE_ICON_COLOR = "light_note_icon_color"
        const val LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND = "light_monthly_calendar_grid_background"
        const val LIGHT_MONTHLY_CALENDAR_GRID_EFFECT = "light_monthly_calendar_grid_effect"
        const val LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "light_monthly_calendar_day_cell_background"

        // Dark Theme
        const val DARK_CABECERA = "dark_cabecera"
        const val DARK_SETTINGS_BACKGROUND = "dark_settings_background"
        const val DARK_TEXT_SUNDAY_HOLIDAY = "dark_text_sunday_holiday"
        const val DARK_TEXT_BIRTHDAY = "dark_text_birthday"
        const val DARK_TEXT_EVENT_1 = "dark_text_event_1"
        const val DARK_TEXT_EVENT_2 = "dark_text_event_2"
        const val DARK_TODAY_HIGHLIGHT_COLOR = "dark_today_highlight_color"
        const val DARK_NOTE_ICON_COLOR = "dark_note_icon_color"
        const val DARK_MONTHLY_CALENDAR_GRID_BACKGROUND = "dark_monthly_calendar_grid_background"
        const val DARK_MONTHLY_CALENDAR_GRID_EFFECT = "dark_monthly_calendar_grid_effect"
        const val DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND = "dark_monthly_calendar_day_cell_background"
    }

    object LightColors {
        val cabecera = Color(0xFF0077C2)
        val settingsBackground = Color(0xFFF0F9FE)
        val textSundayHoliday = Color(0xFFD32F2F)
        val textBirthday = Color(0xFF0000FF)
        val textEvent1 = Color(0xFF008000)
        val textEvent2 = Color(0xFF980062)
        val todayHighlightColor = Color(0x260077C2) // 15% opacidad
        val noteIconColor = Color(0xFFFFB300)
        val monthlyCalendarGridBackground = Color(0xFF0077C2)
        val monthlyCalendarGridEffect = Color(0xFF38A391)
        val monthlyCalendarDayCellBackground = Color(0xFFFFFFFF)
    }

    object DarkColors {
        val cabecera = Color(0xFF0288D1)
        val settingsBackground = Color(0xFF001F29)
        val textSundayHoliday = Color(0xFFFF8A80)
        val textBirthday = Color(0xFF82B1FF)
        val textEvent1 = Color(0xFFB9F6CA)
        val textEvent2 = Color(0xFFFF80AB)
        val todayHighlightColor = Color(0x4D4FC3F7) // 30% opacidad
        val noteIconColor = Color(0xFFFFCC80)
        val monthlyCalendarGridBackground = Color(0xFF006064)
        val monthlyCalendarGridEffect = Color(0xFF01579B)
        val monthlyCalendarDayCellBackground = Color(0xFF003341)
    }
}
