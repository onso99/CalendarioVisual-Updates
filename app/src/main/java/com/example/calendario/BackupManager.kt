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

    fun exportFullBackup(context: Context, uri: Uri) {
        try {
            val json = createFullBackupJson(context)
            val jsonStr = json.toString(4)
            val bytes = jsonStr.toByteArray()
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(bytes) 
            }
            
            // Registrar en historial
            BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                timestamp = System.currentTimeMillis(),
                source = BackupSource.LOCAL,
                action = BackupAction.SAVE,
                isSuccess = true,
                eventsCount = json.optJSONArray(KEY_CALENDAR_HISTORY)?.length() ?: 0,
                notesCount = json.optJSONArray(KEY_DAILY_NOTES)?.length() ?: 0,
                includePrefs = true,
                sizeBytes = bytes.size.toLong()
            ))
        } catch (e: Exception) {
            Log.e("BackupManager", "Export error", e)
            BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                timestamp = System.currentTimeMillis(),
                source = BackupSource.LOCAL,
                action = BackupAction.SAVE,
                isSuccess = false,
                technicalError = e.message
            ))
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
        restoreEvents: Boolean,
        source: BackupSource,
        logEntry: Boolean = true // Nuevo parámetro para silenciar logs durante sync
    ): Boolean {
        var eventsCount = 0
        var notesCount = 0
        
        return try {
            val metadata = json.optJSONObject(KEY_BACKUP_METADATA)
            if (metadata == null || (metadata.optString("appName") != AppConstants.APP_SIGNATURE)) {
                Log.e("BackupManager", "Firma de App no válida o metadata ausente")
                if (logEntry) {
                    BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                        timestamp = System.currentTimeMillis(),
                        source = source,
                        action = BackupAction.RESTORE,
                        isSuccess = false,
                        errorMessageRes = R.string.invalid_json_file
                    ))
                }
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
                            val value = it.opt(key)
                            if (value != null && value != JSONObject.NULL) {
                                putPreference(this, key, value)
                            }
                        }
                    }
                }
                
                val appJson = json.optJSONObject(KEY_APP_PREFS)
                val hasIndividualColors = appJson?.keys()?.asSequence()?.any { it.startsWith("light_") || it.startsWith("dark_") } ?: false
                if (!hasIndividualColors) {
                    appJson?.optString(AppConstants.KEY_LIGHT_THEME_NAME)?.let { applyBundledThemeColors(context, it, isDark = false) }
                    appJson?.optString(AppConstants.KEY_DARK_THEME_NAME)?.let { applyBundledThemeColors(context, it, isDark = true) }
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
                    notesCount = remoteNotes.size
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
                                description = if (!obj.isNull("description")) obj.getString("description") else null,
                                id = if (obj.has("id")) obj.getLong("id") else null,
                                dateStr = if (!obj.isNull("dateStr")) obj.getString("dateStr") else null,
                                startTimeStr = if (!obj.isNull("startTimeStr")) obj.getString("startTimeStr") else null,
                                endTimeStr = if (!obj.isNull("endTimeStr")) obj.getString("endTimeStr") else null,
                                isAllDay = obj.optBoolean("isAllDay", true),
                                calendarId = if (obj.has("calendarId")) obj.getLong("calendarId") else 0L,
                                rrule = if (!obj.isNull("rrule")) obj.getString("rrule") else null,
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
                    eventsCount = remoteEvents.size
                    val (merged, _) = mergeHistoryLists(context, localEvents, remoteEvents)
                    saveHistoryToDisk(context, merged)
                }
            }
            
            AlarmUtils.rescheduleAllAlarms(context)
            
            if (logEntry) {
                BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    source = source,
                    action = BackupAction.RESTORE,
                    isSuccess = true,
                    eventsCount = eventsCount,
                    notesCount = notesCount,
                    includePrefs = restorePrefs
                ))
            }
            
            true
        } catch (e: Exception) {
            Log.e("BackupManager", "Error en proceso de importación JSON", e)
            if (logEntry) {
                BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    source = source,
                    action = BackupAction.RESTORE,
                    isSuccess = false,
                    technicalError = e.message
                ))
            }
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
            importFullBackupFromJson(context, JSONObject(content), restorePrefs, restoreHolidays, restoreNotes, restoreEvents, BackupSource.LOCAL)
        } catch (e: Exception) {
            Log.e("BackupManager", "Local file import error", e)
            BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                timestamp = System.currentTimeMillis(),
                source = BackupSource.LOCAL,
                action = BackupAction.RESTORE,
                isSuccess = false,
                technicalError = e.message
            ))
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
                    val value = it.opt(key)
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
            AppConstants.KEY_LAST_BACKUP_TIME, AppConstants.KEY_LAST_BACKUP_SIZE -> editor.putLong(key, (value as? Number)?.toLong() ?: value.toString().toLongOrNull() ?: 0L)
            AppConstants.KEY_DEFAULT_ALARM_OFFSET, AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, WidgetConstants.KEY_EVENT_COUNT -> editor.putInt(key, (value as? Number)?.toInt() ?: value.toString().toIntOrNull() ?: 0)
            AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, WidgetConstants.KEY_WIDGET_FONT_BOLD -> editor.putBoolean(key, (value as? Boolean) ?: value.toString().toBoolean())
            else -> {
                // Para colores y otros que suelen ser Int
                if (key.startsWith("light_") || key.startsWith("dark_") || key.contains("color")) {
                    val intVal = (value as? Number)?.toInt() ?: value.toString().toLongOrNull()?.toInt()
                    if (intVal != null) {
                        editor.putInt(key, intVal)
                        return
                    }
                }

                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Double -> if (value == value.toInt().toDouble()) editor.putInt(key, value.toInt()) else editor.putFloat(key, value.toFloat())
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            val item = value.opt(i)
                            if (item != null) set.add(item.toString())
                        }
                        editor.putStringSet(key, set)
                    }
                    else -> editor.putString(key, value.toString())
                }
            }
        }
    }
}
