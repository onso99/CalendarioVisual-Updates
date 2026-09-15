package com.example.calendario

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

/**
 * Gestor Centralizado de Ajustes y Preferencias (Fase 4 - Optimización v3.1.34)
 * Encapsula todo el acceso a SharedPreferences para evitar inconsistencias.
 */
object SettingsManager {

    fun getPrefs(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    // --- 1. AJUSTES GENERALES (App Preferences) ---
    private fun appPrefs(context: Context) = getPrefs(context, AppConstants.APP_SETTINGS_PREFS_NAME)

    fun getEvent1Keyword(context: Context): String =
        appPrefs(context).getString(AppConstants.KEY_EVENT_1_KEYWORD, "") ?: ""

    fun saveEvent1Keyword(context: Context, keyword: String) =
        appPrefs(context).edit { putString(AppConstants.KEY_EVENT_1_KEYWORD, keyword.trim()) }

    fun getEvent2Keyword(context: Context): String =
        appPrefs(context).getString(AppConstants.KEY_EVENT_2_KEYWORD, "") ?: ""

    fun saveEvent2Keyword(context: Context, keyword: String) =
        appPrefs(context).edit { putString(AppConstants.KEY_EVENT_2_KEYWORD, keyword.trim()) }

    fun isEvent1PulseEnabled(context: Context): Boolean =
        appPrefs(context).getBoolean(AppConstants.KEY_EVENT_1_PULSE, false)

    fun isEvent2PulseEnabled(context: Context): Boolean =
        appPrefs(context).getBoolean(AppConstants.KEY_EVENT_2_PULSE, false)

    fun getThemeSetting(context: Context): ThemeSetting {
        val name = appPrefs(context).getString(AppConstants.KEY_THEME_SETTING, ThemeSetting.SYSTEM.name)
        return try { ThemeSetting.valueOf(name!!) } catch (_: Exception) { ThemeSetting.SYSTEM }
    }

    fun getStartOfWeek(context: Context): StartOfWeekOption {
        val key = appPrefs(context).getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key)
        return StartOfWeekOption.fromKey(key!!)
    }

    fun isWeekNumberVisible(context: Context): Boolean =
        getSafeBoolean(appPrefs(context), AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false)

    fun getMonthlyEffectType(context: Context): String =
        appPrefs(context).getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient") ?: "gradient"

    fun getFavoriteCalendarId(context: Context): Long? {
        val id = appPrefs(context).getLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, -1L)
        return if (id == -1L) null else id
    }

    fun getAllAppPrefs(context: Context): Map<String, *> = appPrefs(context).all
    fun getAllWidgetPrefs(context: Context): Map<String, *> = widgetPrefs(context).all
    fun getAllHolidayPrefs(context: Context): Map<String, *> = holidayPrefs(context).all
    fun getAllCalendarPrefs(context: Context): Map<String, *> = calendarPrefs(context).all
    fun getAllAlarmPrefs(context: Context): Map<String, *> = alarmPrefs(context).all

    fun saveFavoriteCalendarId(context: Context, id: Long) =
        appPrefs(context).edit { putLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, id) }

    fun removeFavoriteCalendarId(context: Context) =
        appPrefs(context).edit { remove(AppConstants.KEY_FAVORITE_CALENDAR_ID) }

    fun isLoggingEnabled(context: Context): Boolean =
        appPrefs(context).getBoolean(AppConstants.KEY_LOGGING_ENABLED, false)

    fun getGoogleAccountEmail(context: Context): String? =
        appPrefs(context).getString("google_account_email", null)

    fun saveGoogleAccountEmail(context: Context, email: String?) =
        appPrefs(context).edit { putString("google_account_email", email) }

    fun saveStartOfWeek(context: Context, option: StartOfWeekOption) =
        appPrefs(context).edit { putString(AppConstants.KEY_START_OF_WEEK, option.key) }

    fun saveWeekNumberVisibility(context: Context, visible: Boolean) =
        appPrefs(context).edit { putBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, visible) }

    fun saveThemeSetting(context: Context, setting: ThemeSetting) =
        appPrefs(context).edit { putString(AppConstants.KEY_THEME_SETTING, setting.name) }

    fun saveEvent1Pulse(context: Context, enabled: Boolean) =
        appPrefs(context).edit { putBoolean(AppConstants.KEY_EVENT_1_PULSE, enabled) }

    fun saveEvent2Pulse(context: Context, enabled: Boolean) =
        appPrefs(context).edit { putBoolean(AppConstants.KEY_EVENT_2_PULSE, enabled) }

    fun saveDefaultAlarmOffset(context: Context, offset: Int) =
        appPrefs(context).edit { putInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, offset) }

    fun saveDefaultSnoozeInterval(context: Context, interval: Int) =
        appPrefs(context).edit { putInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, interval) }

    fun saveLoggingEnabled(context: Context, enabled: Boolean) =
        appPrefs(context).edit { putBoolean(AppConstants.KEY_LOGGING_ENABLED, enabled) }

    fun getSafeInt(prefs: SharedPreferences, key: String, default: Int): Int {
        val all = prefs.all
        return try {
            if (all[key] is Int) prefs.getInt(key, default)
            else (all[key] as? Number)?.toInt() ?: all[key]?.toString()?.toIntOrNull() ?: default
        } catch (_: Exception) { default }
    }

    fun getSafeLong(prefs: SharedPreferences, key: String, default: Long): Long {
        val all = prefs.all
        return try {
            if (all[key] is Long) prefs.getLong(key, default)
            else (all[key] as? Number)?.toLong() ?: all[key]?.toString()?.toLongOrNull() ?: default
        } catch (_: Exception) { default }
    }

    fun getSafeBoolean(prefs: SharedPreferences, key: String, default: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, default)
        } catch (_: ClassCastException) {
            prefs.all[key]?.toString()?.toBoolean() ?: default
        }
    }

    fun isJsonToRoomMigrated(context: Context): Boolean =
        appPrefs(context).getBoolean("json_to_room_migrated", false)

    fun setJsonToRoomMigrated(context: Context) =
        appPrefs(context).edit { putBoolean("json_to_room_migrated", true) }

    fun getLightThemeName(context: Context): String? =
        appPrefs(context).getString(AppConstants.KEY_LIGHT_THEME_NAME, null)

    fun saveLightThemeName(context: Context, name: String?) =
        appPrefs(context).edit { putString(AppConstants.KEY_LIGHT_THEME_NAME, name) }

    fun saveDarkThemeName(context: Context, name: String?) =
        appPrefs(context).edit { putString(AppConstants.KEY_DARK_THEME_NAME, name) }

    // --- 2. COPIA DE SEGURIDAD (Drive) ---
    fun isAutoBackupEnabled(context: Context): Boolean =
        getSafeBoolean(appPrefs(context), AppConstants.KEY_AUTO_BACKUP_DRIVE, false)

    fun getBackupFrequency(context: Context): String =
        appPrefs(context).getString(AppConstants.KEY_BACKUP_FREQUENCY, "manual") ?: "manual"

    fun getLastBackupTime(context: Context): Long =
        getSafeLong(appPrefs(context), AppConstants.KEY_LAST_BACKUP_TIME, 0L)

    fun getLastBackupSize(context: Context): Long =
        getSafeLong(appPrefs(context), AppConstants.KEY_LAST_BACKUP_SIZE, 0L)

    fun saveLastBackupMetadata(context: Context, count: Int, sizeBytes: Long) {
        appPrefs(context).edit {
            putLong(AppConstants.KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
            putInt(AppConstants.KEY_LAST_BACKUP_COUNT, count)
            putLong(AppConstants.KEY_LAST_BACKUP_SIZE, sizeBytes)
        }
    }

    fun saveBackupFrequency(context: Context, frequency: String) {
        val isAuto = frequency != "manual"
        appPrefs(context).edit {
            putBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, isAuto)
            putString(AppConstants.KEY_BACKUP_FREQUENCY, frequency)
        }
    }

    // --- 3. ALARMAS ---
    private fun alarmPrefs(context: Context) = getPrefs(context, AppConstants.ALARM_PREFS_NAME)

    fun getDefaultAlarmOffset(context: Context): Int =
        getSafeInt(appPrefs(context), AppConstants.KEY_DEFAULT_ALARM_OFFSET, 30)

    fun getDefaultSnoozeInterval(context: Context): Int =
        getSafeInt(appPrefs(context), AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, 10)

    fun getEventAlarmOffset(context: Context, eventId: Long): Int? {
        val prefs = alarmPrefs(context)
        return if (prefs.contains(eventId.toString())) prefs.getInt(eventId.toString(), 20) else null
    }

    fun saveEventAlarmOffset(context: Context, eventId: Long, offset: Int?) {
        alarmPrefs(context).edit {
            if (offset == null) remove(eventId.toString()) else putInt(eventId.toString(), offset)
        }
    }

    // --- 4. CALENDARIOS Y BORRADOS ---
    private fun calendarPrefs(context: Context) = getPrefs(context, "calendar_prefs")
    private fun deletedEventsPrefs(context: Context) = getPrefs(context, "deleted_events_prefs")

    fun getSelectedCalendarIds(context: Context): Set<Long> {
        val rawSet = try {
            calendarPrefs(context).getStringSet("selected_ids", emptySet())
        } catch (_: ClassCastException) {
            val all = calendarPrefs(context).all["selected_ids"]
            if (all is String) {
                all.removeSurrounding("[", "]").split(",").map { it.trim() }.toSet()
            } else emptySet()
        } ?: emptySet()
        return rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun saveSelectedCalendarIds(context: Context, ids: Set<Long>) {
        calendarPrefs(context).edit {
            putStringSet("selected_ids", ids.map { it.toString() }.toSet())
        }
    }

    fun getDeletedEventIds(context: Context): Set<Long> =
        deletedEventsPrefs(context).all.keys.mapNotNull { it.toLongOrNull() }.toSet()

    fun markEventAsDeleted(context: Context, eventId: Long) =
        deletedEventsPrefs(context).edit { putBoolean(eventId.toString(), true) }

    fun clearDeletedEventIds(context: Context) =
        deletedEventsPrefs(context).edit { clear() }

    // --- 5. COLORES DE PERIODOS ---
    private fun periodColorPrefs(context: Context) = getPrefs(context, AppConstants.PERIOD_COLOR_PREFS_NAME)

    fun getPeriodColor(context: Context, eventId: Long): Int? {
        val prefs = periodColorPrefs(context)
        return if (prefs.contains(eventId.toString())) prefs.getInt(eventId.toString(), 0) else null
    }

    fun savePeriodColor(context: Context, eventId: Long, colorInt: Int?) {
        periodColorPrefs(context).edit {
            if (colorInt == null) remove(eventId.toString()) else putInt(eventId.toString(), colorInt)
        }
    }

    // --- 6. FESTIVOS (Ajustes manuales) ---
    private fun holidayPrefs(context: Context) = getPrefs(context, AppConstants.HOLIDAY_ADJUSTMENTS_PREFS_NAME)

    fun getHolidayAdjustments(context: Context): List<HolidayAdjustment> {
        val json = holidayPrefs(context).getString("adjustments", null) ?: return emptyList()
        val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
        val dtoList: List<HolidayAdjustmentDto> = try { Gson().fromJson(json, type) } catch (_: Exception) { emptyList() }
        return dtoList.map { HolidayAdjustment(LocalDate.parse(it.dateStr), it.title, HolidayAdjustmentType.valueOf(it.type), it.originalEventId) }
    }

    fun saveHolidayAdjustments(context: Context, adjustments: List<HolidayAdjustment>) {
        val json = Gson().toJson(adjustments.map { HolidayAdjustmentDto(it.date.toString(), it.title, it.type.name, it.originalEventId) })
        holidayPrefs(context).edit { putString("adjustments", json) }
    }

    fun getNextRefreshTime(context: Context): String =
        getPrefs(context, "widget_log_prefs").getString("next_refresh_time", "NOT_SCHEDULED") ?: "NOT_SCHEDULED"

    fun saveNextRefreshTime(context: Context, timeStr: String) =
        getPrefs(context, "widget_log_prefs").edit { putString("next_refresh_time", timeStr) }

    // --- 7. WIDGET ---
    private fun widgetPrefs(context: Context) = getPrefs(context, WidgetConstants.GLOBAL_WIDGET_PREFS_NAME)

    fun getWidgetEventCount(context: Context): Int =
        widgetPrefs(context).getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)

    fun getWidgetTextBoost(context: Context): Float =
        widgetPrefs(context).getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f)

    fun saveWidgetEventCount(context: Context, count: Int) =
        widgetPrefs(context).edit { putInt(WidgetConstants.KEY_EVENT_COUNT, count) }

    fun saveWidgetTextBoost(context: Context, boost: Float) =
        widgetPrefs(context).edit { putFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, boost) }

    fun isWidgetFontBold(context: Context): Boolean =
        widgetPrefs(context).getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, false)

    fun saveWidgetFontBold(context: Context, bold: Boolean) =
        widgetPrefs(context).edit { putBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, bold) }

    fun getWidgetFontFamily(context: Context): String =
        widgetPrefs(context).getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.FONT_FAMILY_SYSTEM) ?: WidgetConstants.FONT_FAMILY_SYSTEM

    fun saveWidgetFontFamily(context: Context, family: String) =
        widgetPrefs(context).edit { putString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, family) }

    fun getWidgetEventColor(context: Context): Color =
        Color(widgetPrefs(context).getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB))

    fun saveWidgetEventColor(context: Context, color: Color) =
        widgetPrefs(context).edit { putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, color.toArgb()) }

    fun getWidgetTodayEventColor(context: Context): Color =
        Color(widgetPrefs(context).getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB))

    fun saveWidgetTodayEventColor(context: Context, color: Color) =
        widgetPrefs(context).edit { putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, color.toArgb()) }

    fun getWidgetBackgroundColor(context: Context): Color =
        Color(widgetPrefs(context).getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB))

    fun saveWidgetBackgroundColor(context: Context, color: Color) =
        widgetPrefs(context).edit { putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, color.toArgb()) }

    fun getWidgetSelectedCalendarIds(context: Context): Set<Long> {
        return widgetPrefs(context).getStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    fun saveWidgetSelectedCalendarIds(context: Context, ids: Set<Long>) {
        widgetPrefs(context).edit {
            putStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, ids.map { it.toString() }.toSet())
        }
    }
}
