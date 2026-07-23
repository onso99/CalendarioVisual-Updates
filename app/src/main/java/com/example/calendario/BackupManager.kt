package com.example.calendario

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
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
    private const val KEY_ALARM_PREFS = "alarm_preferences"
    private const val KEY_DAILY_NOTES = "daily_notes"
    private const val KEY_CALENDAR_HISTORY = "calendar_history"
    private const val KEY_BACKUP_METADATA = "backup_metadata"

    /**
     * Genera el paquete completo de datos de la App en formato JSON.
     * Útil para exportación local y sincronización con Drive.
     */
    fun createFullBackupJson(context: Context): JSONObject {
        val appPrefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val widgetPrefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        val holidayPrefs = context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE)
        val calendarPrefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val alarmPrefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)

        val root = JSONObject()

        // 1. Metadata
        val metadata = JSONObject().apply {
            put("version", AppConstants.CURRENT_THEME_VERSION)
            put("appName", AppConstants.APP_SIGNATURE)
            put("date", System.currentTimeMillis())
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                put("appVersion", pInfo.versionName)
            } catch (_: Exception) {}
        }
        root.put(KEY_BACKUP_METADATA, metadata)

        // 2. Preferencias
        root.put(KEY_APP_PREFS, JSONObject(appPrefs.all))
        root.put(KEY_WIDGET_PREFS, JSONObject(widgetPrefs.all))
        root.put(KEY_ALARM_PREFS, JSONObject(alarmPrefs.all))
        root.put(KEY_HOLIDAY_PREFS, JSONObject(holidayPrefs.all))

        val calPrefsMap = calendarPrefs.all.mapValues { entry ->
            val value = entry.value
            if (value is Set<*>) JSONArray(value) else value
        }
        root.put(KEY_CALENDAR_PREFS, JSONObject(calPrefsMap))

        // 3. Notas Diarias
        val notesArray = JSONArray()
        loadNotesFromDisk(context).forEach { note ->
            notesArray.put(JSONObject().apply {
                put("dateStr", note.dateStr)
                put("content", note.content)
                put("lastModified", note.lastModified)
                put("isDeleted", note.isDeleted)
            })
        }
        root.put(KEY_DAILY_NOTES, notesArray)

        // 4. Historial de Eventos
        val historyArray = JSONArray()
        loadHistoryFromDisk(context).forEach { event ->
            val dto = event.toDto()
            historyArray.put(JSONObject().apply {
                put("id", dto.id)
                put("title", dto.title)
                put("description", dto.description)
                put("dateStr", dto.dateStr)
                put("startTimeStr", dto.startTimeStr)
                put("endTimeStr", dto.endTimeStr)
                put("isAllDay", dto.isAllDay)
                put("calendarId", dto.calendarId)
                put("rrule", dto.rrule)
                put("age", dto.age)
                put("isBirthday", dto.isBirthday)
                put("isFromHolidaySource", dto.isFromHolidaySource)
                put("isLongPeriod", dto.isLongPeriod)
                put("lane", dto.lane)
                put("totalDays", dto.totalDays)
                put("currentDay", dto.currentDay)
                put("customColor", dto.customColor)
                put("lastModified", dto.lastModified)
                put("isDeleted", dto.isDeleted)
            })
        }
        root.put(KEY_CALENDAR_HISTORY, historyArray)

        return root
    }

    /**
     * Exporta el JSON completo a un archivo local (Uri)
     */
    fun exportFullBackup(context: Context, uri: Uri) {
        try {
            val json = createFullBackupJson(context)
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(json.toString(4).toByteArray()) 
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Export error", e)
            throw e
        }
    }

    /**
     * Motor de Importación Maestro (Drive y Local pasan por aquí)
     */
    fun importFullBackupFromJson(
        context: Context,
        json: JSONObject,
        restorePrefs: Boolean,
        restoreHolidays: Boolean,
        restoreNotes: Boolean,
        restoreEvents: Boolean
    ): Boolean {
        return try {
            val metadata = json.optJSONObject(KEY_BACKUP_METADATA)
            if (metadata == null || metadata.optString("appName") != AppConstants.APP_SIGNATURE) {
                Log.e("BackupManager", "Firma de App no válida o metadata ausente")
                return false
            }

            // 1. PREFERENCIAS
            if (restorePrefs) {
                restorePrefs(context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE), json.optJSONObject(KEY_APP_PREFS))
                restorePrefs(context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE), json.optJSONObject(KEY_WIDGET_PREFS))
                restorePrefs(context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE), json.optJSONObject(KEY_ALARM_PREFS))
                
                val calendarJson = json.optJSONObject(KEY_CALENDAR_PREFS)
                calendarJson?.let {
                    context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE).edit {
                        clear()
                        val keys = it.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = it.get(key)
                            if (value is JSONArray) {
                                val set = mutableSetOf<String>()
                                for (i in 0 until value.length()) set.add(value.getString(i))
                                putStringSet(key, set)
                            } else if (value != null && value != JSONObject.NULL) {
                                putPreference(this, key, value)
                            }
                        }
                    }
                }
                
                val appJson = json.optJSONObject(KEY_APP_PREFS)
                val hasIndividualColors = appJson?.keys()?.asSequence()?.any { it.startsWith("light_") || it.startsWith("dark_") } ?: false
                if (!hasIndividualColors) {
                    appJson?.optString(AppConstants.KEY_LIGHT_THEME_NAME)?.let { applyBundledThemeColors(context, it, false) }
                    appJson?.optString(AppConstants.KEY_DARK_THEME_NAME)?.let { applyBundledThemeColors(context, it, true) }
                }
            }

            // 2. FESTIVOS MANUALES
            if (restoreHolidays) {
                restorePrefs(context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE), json.optJSONObject(KEY_HOLIDAY_PREFS))
            }

            // 3. NOTAS DIARIAS (Fusión Incremental)
            if (restoreNotes) {
                val notesJson = json.optJSONArray(KEY_DAILY_NOTES)
                if (notesJson != null) {
                    val localNotes = loadNotesFromDisk(context)
                    val remoteNotes = mutableListOf<DailyNote>()
                    for (i in 0 until notesJson.length()) {
                        try {
                            val obj = notesJson.getJSONObject(i)
                            remoteNotes.add(DailyNote(
                                obj.getString("dateStr"),
                                obj.getString("content"),
                                obj.optLong("lastModified", System.currentTimeMillis()),
                                obj.optBoolean("isDeleted", false)
                            ))
                        } catch (_: Exception) {}
                    }
                    saveNotesToDisk(context, mergeNotesLists(localNotes, remoteNotes))
                }
            }

            // 4. HISTORIAL DE EVENTOS (Fusión Incremental)
            if (restoreEvents) {
                val historyJson = json.optJSONArray(KEY_CALENDAR_HISTORY)
                if (historyJson != null) {
                    val localEvents = loadHistoryFromDisk(context)
                    val remoteEvents = mutableListOf<Festivo>()
                    for (i in 0 until historyJson.length()) {
                        try {
                            val obj = historyJson.getJSONObject(i)
                            val dto = FestivoDto(
                                title = obj.optString("title", ""),
                                description = if (obj.isNull("description")) null else obj.optString("description", null),
                                id = if (obj.has("id")) obj.getLong("id") else null,
                                dateStr = obj.optString("dateStr", null),
                                startTimeStr = if (obj.isNull("startTimeStr")) null else obj.optString("startTimeStr", null),
                                endTimeStr = if (obj.isNull("endTimeStr")) null else obj.optString("endTimeStr", null),
                                isAllDay = obj.optBoolean("isAllDay", true),
                                calendarId = if (obj.has("calendarId")) obj.getLong("calendarId") else 0L,
                                rrule = if (obj.isNull("rrule")) null else obj.optString("rrule", null),
                                age = if (obj.has("age") && !obj.isNull("age")) obj.getInt("age") else null,
                                isBirthday = obj.optBoolean("isBirthday", false),
                                isFromHolidaySource = obj.optBoolean("isFromHolidaySource", false),
                                isLongPeriod = obj.optBoolean("isLongPeriod", false),
                                lane = if (obj.has("lane") && !obj.isNull("lane")) obj.getInt("lane") else null,
                                totalDays = obj.optInt("totalDays", 1),
                                currentDay = obj.optInt("currentDay", 1),
                                customColor = if (obj.has("customColor") && !obj.isNull("customColor")) obj.getInt("customColor") else null,
                                lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                                isDeleted = obj.optBoolean("isDeleted", false)
                            )
                            dto.toFestivo()?.let { remoteEvents.add(it) }
                        } catch (_: Exception) {}
                    }
                    val (merged, _) = mergeHistoryLists(context, localEvents, remoteEvents)
                    saveHistoryToDisk(context, merged)
                }
            }
            
            AlarmUtils.rescheduleAllAlarms(context)
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Error en proceso de importación JSON", e)
            false
        }
    }

    /**
     * Importación desde archivo local Uri
     */
    fun importFullBackup(context: Context, uri: Uri, restorePrefs: Boolean, restoreHolidays: Boolean, restoreNotes: Boolean, restoreEvents: Boolean): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { 
                BufferedReader(InputStreamReader(it)).readText() 
            } ?: return false
            importFullBackupFromJson(context, JSONObject(content), restorePrefs, restoreHolidays, restoreNotes, restoreEvents)
        } catch (e: Exception) {
            Log.e("BackupManager", "Local file import error", e)
            false
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
    private fun applyBundledThemeColors(context: Context, themeIdOrName: String, isDark: Boolean) {
        val cleanId = themeIdOrName.removeSuffix("***")
        val themeMap = BundledThemes.themes.find { theme ->
            val manifest = theme["themeManifest"] as Map<*, *>
            val id = manifest["id"] as? String
            val legacyName = manifest["name"] as? String
            id == cleanId || legacyName == cleanId
        } ?: return

        val colorMap = (if (isDark) themeMap["darkTheme"] else themeMap["lightTheme"]) as? Map<String, String> ?: return
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            colorMap.forEach { (key, hex) ->
                try { putInt(key, hex.toColorInt()) } catch (_: Exception) { }
            }
        }
    }

    private fun putPreference(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (key) {
            AppConstants.KEY_FAVORITE_CALENDAR_ID -> editor.putLong(key, (value as? Number)?.toLong() ?: value.toString().toLongOrNull() ?: 0L)
            WidgetConstants.KEY_WIDGET_TEXT_BOOST -> editor.putFloat(key, (value as? Number)?.toFloat() ?: value.toString().toFloatOrNull() ?: 0f)
            else -> {
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Double -> if (value == value.toInt().toDouble()) editor.putInt(key, value.toInt()) else editor.putFloat(key, value.toFloat())
                    else -> editor.putString(key, value.toString())
                }
            }
        }
    }
}
