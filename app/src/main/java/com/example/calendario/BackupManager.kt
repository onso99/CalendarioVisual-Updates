package com.example.calendario

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import com.example.calendario.database.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

object BackupManager {

    private const val KEY_APP_PREFS = "app_preferences"
    private const val KEY_WIDGET_PREFS = "widget_preferences"
    private const val KEY_HOLIDAY_PREFS = "holiday_preferences"
    private const val KEY_CALENDAR_PREFS = "calendar_preferences"
    private const val KEY_ALARM_PREFS = "alarm_preferences"
    private const val KEY_DAILY_NOTES = "daily_notes"
    private const val KEY_CALENDAR_HISTORY = "calendar_history"
    private const val KEY_DELETED_EVENTS = "deleted_events"
    private const val KEY_BACKUP_METADATA = "backup_metadata"

    fun createFullBackupJson(context: Context, selectedAppIds: Set<Long>, favoriteId: Long?): JSONObject {
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

        // 3. Notas Diarias (Desde Room)
        val database = AppDatabase.getDatabase(context)
        val dao = database.calendarDao()
        val notesArray = JSONArray()
        dao.getAllNotesSync().forEach { entity ->
            notesArray.put(JSONObject().apply {
                put("dateStr", entity.dateStr)
                put("content", entity.content)
                put("lastModified", entity.lastModified)
                put("isDeleted", entity.isDeleted)
            })
        }
        root.put(KEY_DAILY_NOTES, notesArray)

        // 4. Historial de Eventos (Desde Room)
        val historyArray = JSONArray()
        dao.getAllEventsSync().forEach { entity ->
            historyArray.put(JSONObject().apply {
                put("id", entity.googleId)
                put("title", entity.title)
                put("description", entity.description)
                put("dateStr", entity.date)
                put("startTimeStr", entity.startTime)
                put("endTimeStr", entity.endTime)
                put("isAllDay", entity.isAllDay)
                put("calendarId", entity.calendarId)
                put("rrule", entity.rrule)
                put("age", entity.age)
                put("isBirthday", entity.isBirthday)
                put("isFromHolidaySource", entity.isFromHolidaySource)
                put("isLongPeriod", entity.isLongPeriod)
                put("lane", entity.lane)
                put("totalDays", entity.totalDays)
                put("currentDay", entity.currentDay)
                put("customColor", entity.customColor)
                put("lastModified", entity.lastModified)
                put("isDeleted", entity.isDeleted)
                put("isGhost", entity.isGhost)
            })
        }
        root.put(KEY_CALENDAR_HISTORY, historyArray)

        // 5. Mapeo de Identidad de Calendarios (Crucial para restauración inteligente)
        val calendarMapping = JSONArray()
        loadAvailableCalendarsSync(context).forEach { cal ->
            calendarMapping.put(JSONObject().apply {
                put("id", cal.id)
                put("name", cal.displayName)
                put("account", cal.accountName)
                put("isPrimary", cal.isPrimary)
                val isSelectedInApp = selectedAppIds.contains(cal.id)
                val isSelectedInWidget = widgetPrefs.getStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, emptySet())?.contains(cal.id.toString()) ?: false
                put("selApp", isSelectedInApp)
                put("selWid", isSelectedInWidget)
                put("isFav", cal.id == favoriteId)
            })
        }
        root.put("calendar_mapping", calendarMapping)

        // 6. Lista de IDs Borrados
        val deletedArray = JSONArray()
        getDeletedEventIds(context).forEach { id -> deletedArray.put(id) }
        root.put(KEY_DELETED_EVENTS, deletedArray)

        return root
    }

    fun exportFullBackup(context: Context, uri: Uri, selectedAppIds: Set<Long>, favoriteId: Long?) {
        try {
            val json = createFullBackupJson(context, selectedAppIds, favoriteId)
            val jsonStr = json.toString(4)
            val bytes = jsonStr.toByteArray()
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            
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

    suspend fun importFullBackupFromJson(
        context: Context,
        json: JSONObject,
        restorePrefs: Boolean,
        restoreHolidays: Boolean,
        restoreNotes: Boolean,
        restoreEvents: Boolean,
        source: BackupSource,
        logEntry: Boolean = true
    ): Boolean {
        var eventsCount = 0
        var notesCount = 0
        
        return try {
            val metadata = json.optJSONObject(KEY_BACKUP_METADATA)
            if (metadata == null || (metadata.optString("appName") != AppConstants.APP_SIGNATURE)) {
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
                            if (value != null && value != JSONObject.NULL) putPreference(this, key, value)
                        }
                    }
                }
                
                val appJson = json.optJSONObject(KEY_APP_PREFS)
                val hasIndividualColors = appJson?.keys()?.asSequence()?.any { it.startsWith("light_") || it.startsWith("dark_") } ?: false
                if (!hasIndividualColors) {
                    appJson?.optString(AppConstants.KEY_LIGHT_THEME_NAME)?.let { applyBundledThemeColors(context, it, isDark = false) }
                    appJson?.optString(AppConstants.KEY_DARK_THEME_NAME)?.let { applyBundledThemeColors(context, it, isDark = true) }
                }

                // --- RESTAURACIÓN INTELIGENTE DE VÍNCULOS (Mapeo por Nombre/Cuenta) ---
                val mapping = json.optJSONArray("calendar_mapping")
                if (mapping != null) {
                    val available = loadAvailableCalendarsSync(context)
                    val newSelectedApp = mutableSetOf<Long>()
                    val newSelectedWidget = mutableSetOf<String>()
                    var newFavoriteId: Long? = null

                    for (i in 0 until mapping.length()) {
                        val mapObj = mapping.getJSONObject(i)
                        val name = mapObj.getString("name")
                        val account = mapObj.getString("account")
                        
                        // Buscamos si ese calendario existe en este móvil
                        val currentCal = available.find { it.displayName == name && it.accountName == account }
                        if (currentCal != null) {
                            if (mapObj.optBoolean("selApp")) newSelectedApp.add(currentCal.id)
                            if (mapObj.optBoolean("selWid")) newSelectedWidget.add(currentCal.id.toString())
                            if (mapObj.optBoolean("isFav")) newFavoriteId = currentCal.id
                        }
                    }

                    // Aplicamos los IDs reparados
                    if (newSelectedApp.isNotEmpty()) {
                        context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE).edit {
                            putStringSet("selected_ids", newSelectedApp.map { it.toString() }.toSet())
                        }
                    }
                    if (newSelectedWidget.isNotEmpty()) {
                        context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit {
                            putStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, newSelectedWidget)
                        }
                    }
                    if (newFavoriteId != null) {
                        context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE).edit {
                            putLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, newFavoriteId)
                        }
                    }
                }
            }

            // 2. FESTIVOS MANUALES
            if (restoreHolidays) {
                restorePrefs(context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE), json.optJSONObject(KEY_HOLIDAY_PREFS))
            }

            val database = AppDatabase.getDatabase(context)
            val dao = database.calendarDao()

            // 3. NOTAS DIARIAS (Fusión Room)
            if (restoreNotes) {
                val notesJson = json.optJSONArray(KEY_DAILY_NOTES)
                if (notesJson != null) {
                    val remoteNotes = mutableListOf<NoteEntity>()
                    for (i in 0 until notesJson.length()) {
                        try {
                            val obj = notesJson.getJSONObject(i)
                            remoteNotes.add(NoteEntity(
                                dateStr = obj.getString("dateStr"),
                                content = obj.getString("content"),
                                lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                                isDeleted = obj.optBoolean("isDeleted", false)
                            ))
                        } catch (_: Exception) {}
                    }
                    notesCount = remoteNotes.size
                    dao.insertNotes(remoteNotes)
                }
            }

            // 4. HISTORIAL DE EVENTOS (Fusión Room)
            if (restoreEvents) {
                val historyJson = json.optJSONArray(KEY_CALENDAR_HISTORY)
                if (historyJson != null) {
                    val remoteEvents = mutableListOf<Festivo>()
                    for (i in 0 until historyJson.length()) {
                        try {
                            val obj = historyJson.getJSONObject(i)
                            remoteEvents.add(Festivo(
                                title = obj.optString("title", ""),
                                description = if (!obj.isNull("description")) obj.getString("description") else null,
                                id = if (obj.has("id")) obj.getLong("id") else 0L,
                                date = LocalDate.parse(obj.getString("dateStr")),
                                startTime = if (!obj.isNull("startTimeStr")) java.time.LocalTime.parse(obj.getString("startTimeStr")) else null,
                                endTime = if (!obj.isNull("endTimeStr")) java.time.LocalTime.parse(obj.getString("endTimeStr")) else null,
                                isAllDay = obj.optBoolean("isAllDay", true),
                                calendarId = if (obj.has("calendarId")) obj.getLong("calendarId") else 0L,
                                isFromHolidaySource = obj.optBoolean("isFromHolidaySource", false),
                                rrule = if (!obj.isNull("rrule")) obj.getString("rrule") else null,
                                age = if (obj.has("age") && !obj.isNull("age")) obj.getInt("age") else null,
                                isBirthday = obj.optBoolean("isBirthday", false),
                                isLongPeriod = obj.optBoolean("isLongPeriod", false),
                                lane = if (obj.has("lane") && !obj.isNull("lane")) obj.getInt("lane") else null,
                                totalDays = obj.optInt("totalDays", 1),
                                currentDay = obj.optInt("currentDay", 1),
                                customColor = if (obj.has("customColor") && !obj.isNull("customColor")) obj.getInt("customColor") else null,
                                fullStartMillis = if (obj.has("fullStartMillis")) obj.getLong("fullStartMillis") else null,
                                fullEndMillis = if (obj.has("fullEndMillis")) obj.getLong("fullEndMillis") else null,
                                repeatCount = if (obj.has("repeatCount")) obj.getInt("repeatCount") else null,
                                lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                                isDeleted = obj.optBoolean("isDeleted", false),
                                isGhost = obj.optBoolean("isGhost", false),
                                adn = obj.optString("adn", "")
                            ))
                        } catch (_: Exception) {}
                    }
                    eventsCount = remoteEvents.size
                    
                    val localEvents = dao.getAllEventsSync().map { it.toFestivo() }
                    val availableCalendars = loadAvailableCalendarsSync(context)
                    
                    clearDeletedEventIds(context)
                    json.optJSONArray(KEY_DELETED_EVENTS)?.let { array ->
                        for (i in 0 until array.length()) markEventAsDeleted(context, array.optLong(i))
                    }
                    
                    val merged = mergeHistoryWithSystemData(context, localEvents, remoteEvents, availableCalendars)
                    dao.refreshEvents(merged.map { it.toEntity() })
                }
            }
            
            AlarmUtils.rescheduleAllAlarms(context)
            if (logEntry) {
                BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                    timestamp = System.currentTimeMillis(), source = source, action = BackupAction.RESTORE, 
                    isSuccess = true, eventsCount = eventsCount, notesCount = notesCount, includePrefs = restorePrefs
                ))
            }
            true
        } catch (e: Exception) {
            if (logEntry) {
                BackupHistoryManager.addEntry(context, BackupHistoryEntry(
                    timestamp = System.currentTimeMillis(), source = source, action = BackupAction.RESTORE, 
                    isSuccess = false, technicalError = e.message
                ))
            }
            false
        }
    }

    suspend fun importFullBackup(context: Context, uri: Uri, restorePrefs: Boolean, restoreHolidays: Boolean, restoreNotes: Boolean, restoreEvents: Boolean): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { BufferedReader(InputStreamReader(it)).readText() } ?: return false
            importFullBackupFromJson(context, JSONObject(content), restorePrefs, restoreHolidays, restoreNotes, restoreEvents, BackupSource.LOCAL)
        } catch (_: Exception) { false }
    }

    private fun restorePrefs(prefs: SharedPreferences, json: JSONObject?) {
        json?.let {
            prefs.edit {
                clear()
                val keys = it.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = it.opt(key)
                    if (value != null && value != JSONObject.NULL) putPreference(this, key, value)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyBundledThemeColors(context: Context, themeIdOrName: String, isDark: Boolean) {
        val cleanId = themeIdOrName.removeSuffix("***")
        val themeMap = BundledThemes.themes.find { theme ->
            val manifest = theme["themeManifest"] as Map<*, *>
            (manifest["id"] as? String) == cleanId || (manifest["name"] as? String) == cleanId
        } ?: return
        val colorMap = (if (isDark) themeMap["darkTheme"] else themeMap["lightTheme"]) as? Map<String, String> ?: return
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { colorMap.forEach { (key, hex) -> try { putInt(key, hex.toColorInt()) } catch (_: Exception) { } } }
    }

    fun getEventsFromLegacyJson(context: Context): List<Festivo> {
        return try {
            val file = context.getFileStreamPath("calendar_history_v2.json")
            if (!file.exists()) return emptyList()
            val json = context.openFileInput("calendar_history_v2.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val result = mutableListOf<Festivo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(Festivo(
                    title = obj.optString("title", ""), id = obj.optLong("id", 0L),
                    date = LocalDate.parse(obj.getString("dateStr")), 
                    isAllDay = obj.optBoolean("isAllDay", true), calendarId = obj.optLong("calendarId", 0L),
                    adn = obj.optString("adn", ""), isFromHolidaySource = obj.optBoolean("isFromHolidaySource", false),
                    description = null, startTime = null, endTime = null, rrule = null
                ))
            }
            result
        } catch (_: Exception) { emptyList() }
    }

    fun getNotesFromLegacyJson(context: Context): List<DailyNote> {
        return try {
            val file = context.getFileStreamPath("notes_history.json")
            if (!file.exists()) return emptyList()
            val json = context.openFileInput("notes_history.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val result = mutableListOf<DailyNote>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(DailyNote(obj.getString("dateStr"), obj.getString("content"), obj.optLong("lastModified", 0L), obj.optBoolean("isDeleted", false)))
            }
            result
        } catch (_: Exception) { emptyList() }
    }

    private fun putPreference(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (key) {
            AppConstants.KEY_FAVORITE_CALENDAR_ID -> editor.putLong(key, (value as? Number)?.toLong() ?: value.toString().toLongOrNull() ?: 0L)
            WidgetConstants.KEY_WIDGET_TEXT_BOOST -> editor.putFloat(key, (value as? Number)?.toFloat() ?: value.toString().toFloatOrNull() ?: 0f)
            else -> {
                if (key.startsWith("light_") || key.startsWith("dark_") || key.contains("color")) {
                    val intVal = (value as? Number)?.toInt() ?: value.toString().toLongOrNull()?.toInt()
                    if (intVal != null) { editor.putInt(key, intVal); return }
                }
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) { val item = value.opt(i); if (item != null) set.add(item.toString()) }
                        editor.putStringSet(key, set)
                    }
                    else -> editor.putString(key, value.toString())
                }
            }
        }
    }
}
