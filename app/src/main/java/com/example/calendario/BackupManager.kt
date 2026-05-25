package com.example.calendario

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupManager {

    private const val KEY_APP_PREFS = "app_preferences"
    private const val KEY_WIDGET_PREFS = "widget_preferences"
    private const val KEY_HOLIDAY_PREFS = "holiday_preferences"
    private const val KEY_CALENDAR_PREFS = "calendar_preferences"
    private const val KEY_BACKUP_METADATA = "backup_metadata"

    fun exportFullBackup(context: Context, uri: Uri) {
        try {
            val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val holidayPrefs = context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE)
            val calendarPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)

            val fullBackupJson = JSONObject()

            // 1. Metadata
            val metadata = JSONObject()
            metadata.put("version", AppConstants.CURRENT_THEME_VERSION)
            metadata.put("appName", AppConstants.APP_SIGNATURE)
            metadata.put("date", System.currentTimeMillis())
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            metadata.put("appVersion", pInfo.versionName)
            fullBackupJson.put(KEY_BACKUP_METADATA, metadata)

            // 2. App Preferences
            fullBackupJson.put(KEY_APP_PREFS, JSONObject(appPrefs.all))

            // 3. Widget Preferences
            fullBackupJson.put(KEY_WIDGET_PREFS, JSONObject(widgetPrefs.all))
            
            // 4. Holiday Adjustments (Gestor de Festivos)
            fullBackupJson.put(KEY_HOLIDAY_PREFS, JSONObject(holidayPrefs.all))
            
            // 5. Selected Calendars (Manejo especial para Set<String>)
            val calPrefsMap = calendarPrefs.all.mapValues { entry ->
                val value = entry.value
                if (value is Set<*>) JSONArray(value) else value
            }
            fullBackupJson.put(KEY_CALENDAR_PREFS, JSONObject(calPrefsMap))

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(fullBackupJson.toString(4).toByteArray())
            }
            Toast.makeText(context, R.string.backup_exported_successfully, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_exporting_backup, e.message), Toast.LENGTH_LONG).show()
        }
    }

    fun importFullBackup(context: Context, uri: Uri, onComplete: () -> Unit) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: throw Exception("Cannot read file")

            val json = JSONObject(content)

            // Validar firma
            val metadata = json.optJSONObject(KEY_BACKUP_METADATA)
            if (metadata == null || metadata.optString("appName") != AppConstants.APP_SIGNATURE) {
                throw Exception(context.getString(R.string.invalid_backup_file))
            }

            // --- RESTAURACIÓN ---
            
            // 1. App Prefs
            val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            restorePrefs(appPrefs, json.optJSONObject(KEY_APP_PREFS))

            // 2. Widget Prefs
            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            restorePrefs(widgetPrefs, json.optJSONObject(KEY_WIDGET_PREFS))
            
            // 3. Holiday Prefs
            val holidayPrefs = context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE)
            restorePrefs(holidayPrefs, json.optJSONObject(KEY_HOLIDAY_PREFS))
            
            // 4. Calendar Prefs (Tratamiento especial para los ID de calendarios que son un Set)
            val calendarPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
            val calendarJson = json.optJSONObject(KEY_CALENDAR_PREFS)
            calendarJson?.let {
                calendarPrefs.edit {
                    clear()
                    val keys = it.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = it.get(key)
                        if (value is JSONArray) {
                            val set = mutableSetOf<String>()
                            for (i in 0 until value.length()) {
                                set.add(value.getString(i))
                            }
                            putStringSet(key, set)
                        } else if (value != null && value != JSONObject.NULL) {
                            putPreference(this, key, value)
                        }
                    }
                }
            }

            // Re-aplicar lógica de colores de temas si no hay colores individuales
            val appJson = json.optJSONObject(KEY_APP_PREFS)
            val hasIndividualColors = appJson?.keys()?.asSequence()?.any { it.startsWith("light_") || it.startsWith("dark_") } ?: false
            if (!hasIndividualColors) {
                appJson?.optString(AppConstants.KEY_LIGHT_THEME_NAME)?.let { applyBundledThemeColors(context, it, false) }
                appJson?.optString(AppConstants.KEY_DARK_THEME_NAME)?.let { applyBundledThemeColors(context, it, true) }
            }

            onComplete()
            Toast.makeText(context, R.string.backup_imported_successfully, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_importing_backup, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun restorePrefs(prefs: SharedPreferences, json: JSONObject?) {
        json?.let {
            prefs.edit {
                clear()
                val keys = it.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = it.get(key)
                    if (value != null && value != JSONObject.NULL) {
                        putPreference(this, key, value)
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyBundledThemeColors(context: Context, themeName: String, isDark: Boolean) {
        val themeMap = (BundledThemes.themes as List<Map<String, Any>>).find { 
            (it["themeManifest"] as? Map<String, Any>)?.get("name") == themeName 
        } ?: return

        val colorMap = (if (isDark) themeMap["darkTheme"] else themeMap["lightTheme"]) as? Map<String, String> ?: return
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            colorMap.forEach { (key, hex) ->
                try {
                    putInt(key, hex.toColorInt())
                } catch (_: Exception) { }
            }
        }
    }

    private fun putPreference(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (key) {
            AppConstants.KEY_FAVORITE_CALENDAR_ID -> {
                val longValue = when (value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    else -> 0L
                }
                editor.putLong(key, longValue)
            }
            WidgetConstants.KEY_WIDGET_TEXT_BOOST -> {
                val floatValue = when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloatOrNull() ?: 0f
                    else -> 0f
                }
                editor.putFloat(key, floatValue)
            }
            // Int (Colores, contadores, etc.)
            WidgetConstants.KEY_EVENT_COUNT,
            WidgetConstants.KEY_WIDGET_EVENT_COLOR,
            WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR,
            WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR,
            AppConstants.ColorKeys.LIGHT_CABECERA,
            AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND,
            AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY,
            AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY,
            AppConstants.ColorKeys.LIGHT_TEXT_EVENT_DEFAULT,
            AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1,
            AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2,
            AppConstants.ColorKeys.LIGHT_TODAY_HIGHLIGHT_COLOR,
            AppConstants.ColorKeys.LIGHT_EVENT_LIST_TITLE_COLOR,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_BACKGROUND,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_GRID_EFFECT,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER,
            AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND,
            AppConstants.ColorKeys.DARK_CABECERA,
            AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND,
            AppConstants.ColorKeys.DARK_TEXT_SUNDAY_HOLIDAY,
            AppConstants.ColorKeys.DARK_TEXT_BIRTHDAY,
            AppConstants.ColorKeys.DARK_TEXT_EVENT_DEFAULT,
            AppConstants.ColorKeys.DARK_TEXT_EVENT_1,
            AppConstants.ColorKeys.DARK_TEXT_EVENT_2,
            AppConstants.ColorKeys.DARK_EVENT_LIST_TITLE_COLOR,
            AppConstants.ColorKeys.DARK_TODAY_HIGHLIGHT_COLOR,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_BACKGROUND,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_GRID_EFFECT,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_CELL_BACKGROUND,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER,
            AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND -> {
                val intValue = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
                editor.putInt(key, intValue)
            }
            else -> {
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Double -> {
                        if (value == value.toInt().toDouble()) editor.putInt(key, value.toInt())
                        else editor.putFloat(key, value.toFloat())
                    }
                    else -> editor.putString(key, value.toString())
                }
            }
        }
    }
}
