package com.example.calendario

import android.content.Context
import android.net.Uri
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupManager {

    private const val KEY_APP_PREFS = "app_preferences"
    private const val KEY_WIDGET_PREFS = "widget_preferences"
    private const val KEY_BACKUP_METADATA = "backup_metadata"

    fun exportFullBackup(context: Context, uri: Uri) {
        try {
            val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

            val fullBackupJson = JSONObject()

            // 1. Metadata
            val metadata = JSONObject()
            metadata.put("version", AppConstants.CURRENT_THEME_VERSION)
            metadata.put("appName", AppConstants.APP_SIGNATURE)
            metadata.put("date", System.currentTimeMillis())
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            metadata.put("appVersion", pInfo.versionName)
            fullBackupJson.put(KEY_BACKUP_METADATA, metadata)

            // 2. App Preferences (All except colors)
            val appPrefsMap = appPrefs.all.filterKeys { !isColorKey(it) }
            fullBackupJson.put(KEY_APP_PREFS, JSONObject(appPrefsMap))

            // 3. Widget Preferences (All)
            val widgetPrefsMap = widgetPrefs.all
            fullBackupJson.put(KEY_WIDGET_PREFS, JSONObject(widgetPrefsMap))

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

            val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

            // Importar App Prefs
            val appJson = json.optJSONObject(KEY_APP_PREFS)
            var restoredLightThemeName: String? = null
            var restoredDarkThemeName: String? = null
            
            appJson?.let {
                val editor = appPrefs.edit()
                editor.clear()
                val keys = it.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (isColorKey(key)) continue // Ignorar colores individuales, se restaurarán vía tema
                    val value = it.get(key)
                    if (value != null && value != JSONObject.NULL) {
                        putPreference(editor, key, value)
                        if (key == AppConstants.KEY_LIGHT_THEME_NAME) {
                            restoredLightThemeName = value.toString()
                        }
                        if (key == AppConstants.KEY_DARK_THEME_NAME) {
                            restoredDarkThemeName = value.toString()
                        }
                    }
                }
                editor.apply()
            }

            // Importar Widget Prefs
            val widgetJson = json.optJSONObject(KEY_WIDGET_PREFS)
            widgetJson?.let {
                val editor = widgetPrefs.edit()
                editor.clear()
                val keys = it.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (isColorKey(key)) continue // Ignorar colores en la importación
                    val value = it.get(key)
                    if (value != null && value != JSONObject.NULL) {
                        putPreference(editor, key, value)
                    }
                }
                editor.apply()
            }

            // Aplicar colores de los temas restaurados si existen
            restoredLightThemeName?.let { themeName ->
                applyBundledThemeColors(context, themeName, false)
            }
            restoredDarkThemeName?.let { themeName ->
                applyBundledThemeColors(context, themeName, true)
            }

            onComplete()

            Toast.makeText(context, R.string.backup_imported_successfully, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_importing_backup, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun applyBundledThemeColors(context: Context, themeName: String, isDark: Boolean) {
        val themeMap = BundledThemes.themes.find { 
            (it["themeManifest"] as? Map<*, *>)?.get("name") == themeName 
        } ?: return

        val colorMap = (if (isDark) themeMap["darkTheme"] else themeMap["lightTheme"]) as? Map<String, String> ?: return
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        colorMap.forEach { (key, hex) ->
            try {
                editor.putInt(key, android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                // Ignorar colores inválidos
            }
        }
        editor.apply()
    }

    private fun isColorKey(key: String): Boolean {
        return (key.startsWith("light_") || key.startsWith("dark_")) &&
                key != AppConstants.KEY_LIGHT_THEME_NAME &&
                key != AppConstants.KEY_DARK_THEME_NAME
    }

    private fun putPreference(editor: android.content.SharedPreferences.Editor, key: String, value: Any) {
        // Mapeo explícito de tipos para evitar ClassCastException al restaurar desde JSON
        when (key) {
            // Long
            AppConstants.KEY_FAVORITE_CALENDAR_ID -> {
                val longValue = when (value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    else -> 0L
                }
                editor.putLong(key, longValue)
            }
            // Float
            WidgetConstants.KEY_WIDGET_TEXT_BOOST -> {
                val floatValue = when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloatOrNull() ?: 1.0f
                    else -> 1.0f
                }
                editor.putFloat(key, floatValue)
            }
            // Int (Colores, contadores, etc.)
            WidgetConstants.KEY_EVENT_COUNT,
            WidgetConstants.KEY_WIDGET_EVENT_COLOR,
            WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR,
            WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR,
            AppConstants.ColorKeys.LIGHT_CABECERA,
            AppConstants.ColorKeys.LIGHT_FONDO_SECCIONES,
            AppConstants.ColorKeys.LIGHT_FONDO_DIALOGOS,
            AppConstants.ColorKeys.LIGHT_SETTINGS_BACKGROUND,
            AppConstants.ColorKeys.LIGHT_TEXT_SYSTEM,
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
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_TODAY_CELL_BORDER,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_HEADER_BACKGROUND,
            AppConstants.ColorKeys.LIGHT_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL,
            AppConstants.ColorKeys.LIGHT_MINI_MONTH_DAY_NUMBER_NORMAL,
            AppConstants.ColorKeys.LIGHT_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND,
            AppConstants.ColorKeys.DARK_CABECERA,
            AppConstants.ColorKeys.DARK_FONDO_SECCIONES,
            AppConstants.ColorKeys.DARK_FONDO_DIALOGOS,
            AppConstants.ColorKeys.DARK_SETTINGS_BACKGROUND,
            AppConstants.ColorKeys.DARK_TEXT_SYSTEM,
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
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_EMPTY_CELL_BACKGROUND,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_TODAY_CELL_BORDER,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_HEADER_BACKGROUND,
            AppConstants.ColorKeys.DARK_MONTHLY_CALENDAR_DAY_NUMBER_NORMAL,
            AppConstants.ColorKeys.DARK_MINI_MONTH_DAY_NUMBER_NORMAL,
            AppConstants.ColorKeys.DARK_MINI_MONTH_TODAY_HIGHLIGHT_BACKGROUND -> {
                val intValue = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
                editor.putInt(key, intValue)
            }
            // Fallback para tipos genéricos si no es una clave crítica conocida
            else -> {
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Double -> {
                        // Intentar deducir si es Int o Float
                        if (value == value.toInt().toDouble()) {
                            editor.putInt(key, value.toInt())
                        } else {
                            editor.putFloat(key, value.toFloat())
                        }
                    }
                    else -> editor.putString(key, value.toString())
                }
            }
        }
    }
}
