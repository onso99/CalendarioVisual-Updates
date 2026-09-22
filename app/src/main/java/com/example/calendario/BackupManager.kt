package com.example.calendario

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import com.example.calendario.database.*
import org.json.JSONArray
import org.json.JSONObject

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

        // 2. Preferencias (Centralizado v3.1.34)
        root.put(KEY_APP_PREFS, JSONObject(SettingsManager.getAllAppPrefs(context)))
        root.put(KEY_WIDGET_PREFS, JSONObject(SettingsManager.getAllWidgetPrefs(context)))
        root.put(KEY_ALARM_PREFS, JSONObject(SettingsManager.getAllAlarmPrefs(context)))
        root.put(KEY_HOLIDAY_PREFS, JSONObject(SettingsManager.getAllHolidayPrefs(context)))

        val calPrefsMap = SettingsManager.getAllCalendarPrefs(context).mapValues { entry ->
            val value = entry.value
            if (value is Set<*>) JSONArray(value) else value
        }
        root.put(KEY_CALENDAR_PREFS, JSONObject(calPrefsMap))

        // 3. Notas Diarias (Centralizado v3.1.34)
        val database = AppDatabase.getDatabase(context)
        val dao = database.calendarDao()
        val notesArray = JSONArray()
        dao.getAllNotesSync().forEach { entity ->
            notesArray.put(entity.toDailyNote().toJson())
        }
        root.put(KEY_DAILY_NOTES, notesArray)

        // 4. Historial de Eventos (Centralizado v3.1.34)
        val historyArray = JSONArray()
        dao.getAllEventsSync().forEach { entity ->
            historyArray.put(entity.toFestivo().toJson())
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
                val isSelectedInWidget = SettingsManager.getWidgetSelectedCalendarIds(context).contains(cal.id)
                put("selApp", isSelectedInApp)
                put("selWid", isSelectedInWidget)
                put("isFav", cal.id == favoriteId)
            })
        }
        root.put("calendar_mapping", calendarMapping)

        // 6. Lista de IDs Borrados
        val deletedArray = JSONArray()
        SettingsManager.getDeletedEventIds(context).forEach { id -> deletedArray.put(id) }
        root.put(KEY_DELETED_EVENTS, deletedArray)

        return root
    }

    suspend fun importFullBackupFromJson(
        context: Context,
        json: JSONObject,
        restorePrefs: Boolean,
        restoreHolidays: Boolean,
        restoreNotes: Boolean,
        restoreEvents: Boolean,
        source: BackupSource,
        logEntry: Boolean = true,
        isSync: Boolean = false // NUEVO: Evita limpieza destructiva de borrados (v3.3.06)
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
                restorePrefs(SettingsManager.getPrefs(context, AppConstants.APP_SETTINGS_PREFS_NAME), json.optJSONObject(KEY_APP_PREFS))
                restorePrefs(SettingsManager.getPrefs(context, WidgetConstants.GLOBAL_WIDGET_PREFS_NAME), json.optJSONObject(KEY_WIDGET_PREFS))
                restorePrefs(SettingsManager.getPrefs(context, AppConstants.ALARM_PREFS_NAME), json.optJSONObject(KEY_ALARM_PREFS))
                
                val calendarJson = json.optJSONObject(KEY_CALENDAR_PREFS)
                calendarJson?.let {
                    SettingsManager.getPrefs(context, "calendar_prefs").edit {
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
                        SettingsManager.getPrefs(context, "calendar_prefs").edit {
                            putStringSet("selected_ids", newSelectedApp.map { it.toString() }.toSet())
                        }
                    }
                    if (newSelectedWidget.isNotEmpty()) {
                        SettingsManager.getPrefs(context, WidgetConstants.GLOBAL_WIDGET_PREFS_NAME).edit {
                            putStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, newSelectedWidget)
                        }
                    }
                    if (newFavoriteId != null) {
                        SettingsManager.saveFavoriteCalendarId(context, newFavoriteId)
                    }
                }
            }

            // 2. FESTIVOS MANUALES
            if (restoreHolidays) {
                restorePrefs(SettingsManager.getPrefs(context, AppConstants.HOLIDAY_PREFS_NAME), json.optJSONObject(KEY_HOLIDAY_PREFS))
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
                            remoteNotes.add(DailyNote.fromJson(obj).toEntity())
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
                            remoteEvents.add(Festivo.fromJson(obj))
                        } catch (_: Exception) {}
                    }
                    eventsCount = remoteEvents.size
                    
                    val localEvents = dao.getAllEventsSync().map { it.toFestivo() }
                    val availableCalendars = loadAvailableCalendarsSync(context)
                    
                    // REGLA DE ORO (v3.3.06): Durante la sincronización NUNCA borramos la lista local de eventos eliminados.
                    // Solo la limpiamos si es una restauración total solicitada por el usuario.
                    if (!isSync) {
                        SettingsManager.clearDeletedEventIds(context)
                    }
                    
                    json.optJSONArray(KEY_DELETED_EVENTS)?.let { array ->
                        for (i in 0 until array.length()) SettingsManager.markEventAsDeleted(context, array.optLong(i))
                    }
                    
                    val selectedIds = SettingsManager.getSelectedCalendarIds(context)
                    val systemEventsMap = if (selectedIds.isNotEmpty()) readFestivosFromCalendarsSync(context, selectedIds) else emptyMap()
                    val systemEvents = systemEventsMap.values.flatten()
                    
                    val merged = mergeHistoryWithSystemData(context, localEvents + remoteEvents, systemEvents, availableCalendars)
                    dao.smartRefreshEvents(merged.map { it.toEntity() })
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
        val prefs = SettingsManager.getPrefs(context, AppConstants.APP_SETTINGS_PREFS_NAME)
        prefs.edit { colorMap.forEach { (key, hex) -> try { putInt(key, hex.toColorInt()) } catch (_: Exception) { } } }
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
