@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class CalendarioUiState(
    val eventsByDate: Map<LocalDate, List<Festivo>> = emptyMap(),
    val availableCalendars: List<CalendarInfo> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val hasCalendarPermission: Boolean = false,
    val favoriteCalendarId: Long? = null,
    val importedEvent: Festivo? = null,
    val isSyncing: Boolean = false,
    val isRestoring: Boolean = false,
)

class CalendarioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarioUiState())
    val uiState: StateFlow<CalendarioUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = isGranted) }
        if (isGranted) {
            loadAllData()
        } else {
            viewModelScope.launch {
                val context = getApplication<Application>()
                _uiState.value = CalendarioUiState() // Clear all data
                saveHistoryToDisk(context, emptyList())
                saveSelectedCalendarIds(context, emptySet())
                setFavoriteCalendar(null)
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            }
        }
    }

    fun refreshData() {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val hasRead = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
            val hasWrite = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
            val hasPermission = hasRead && hasWrite

            if (!hasPermission) {
                _uiState.value = CalendarioUiState(hasCalendarPermission = false)
                return@launch
            }

            try {
                // Asegurar que el respaldo automático esté programado en el sistema
                BackupScheduler.ensureBackupScheduled(context)

                // --- PASO 0: CARGA ULTRA-INSTANTÁNEA (JSON + Migración) ---
                var cachedHistory = withContext(Dispatchers.IO) { loadHistoryFromDisk(context) }
                
                // MIGRACIÓN: Si el histórico está vacío, rescatamos de Prefs antiguos
                if (cachedHistory.isEmpty()) {
                    val oldPrefsEvents = loadEventsFromPrefs(context)
                    if (oldPrefsEvents.isNotEmpty()) {
                        cachedHistory = oldPrefsEvents.values.flatten()
                        viewModelScope.launch(Dispatchers.IO) { saveHistoryToDisk(context, cachedHistory) }
                    }
                }
                
                if (cachedHistory.isNotEmpty()) {
                    _uiState.update { state -> 
                        state.copy(
                            eventsByDate = cachedHistory.groupBy { it.date },
                            hasCalendarPermission = true
                        ) 
                    }
                }

                // --- PASO 1: TAREAS DE SISTEMA (Calendarios disponibles) ---
                var selectedIds = loadSelectedCalendarIds(context)
                var favoriteId = getFavoriteCalendarId(context)
                val availableCalendars = loadAvailableCalendarsSuspend(context)

                val favoriteExists = availableCalendars.any { it.id == favoriteId }
                if (((favoriteId == null) || !favoriteExists) && availableCalendars.any { it.canModify }) {
                    findBestCalendarCandidate(availableCalendars)?.id?.let {
                        favoriteId = it
                        setFavoriteCalendar(it)
                    }
                }

                if ((favoriteId != null) && !selectedIds.contains(favoriteId)) {
                    selectedIds = selectedIds.toMutableSet().apply { add(favoriteId) }
                    saveSelectedCalendarIds(context, selectedIds)
                }

                val validSelectedIds = selectedIds.asSequence()
                    .filter { sid -> availableCalendars.any { cal -> cal.id == sid } }
                    .toSet()

                // --- PASO 2: SINCRONIZACIÓN CON EL SISTEMA ---
                val systemEventsMap = if (validSelectedIds.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, validSelectedIds)
                } else {
                    emptyMap()
                }
                val systemEvents = systemEventsMap.values.flatten()

                // Aplicar Ventana de Coherencia (-1 año a +5 años)
                val finalEventsList = mergeHistoryWithSystem(cachedHistory, systemEvents)
                
                viewModelScope.launch(Dispatchers.IO) {
                    saveHistoryToDisk(context, finalEventsList)
                }

                _uiState.value = CalendarioUiState(
                    eventsByDate = finalEventsList.groupBy { it.date },
                    availableCalendars = availableCalendars,
                    selectedCalendarIds = validSelectedIds,
                    hasCalendarPermission = true,
                    favoriteCalendarId = favoriteId
                )

                if (validSelectedIds != selectedIds) {
                    saveSelectedCalendarIds(context, validSelectedIds)
                }
                
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)

            } catch (e: Exception) {
                Log.e("CalendarioViewModel", "Error loading all data", e)
                Toast.makeText(context, R.string.error_updating_data, Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun refreshAvailableCalendars() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            withContext(Dispatchers.IO) { 
                val freshAvailableCalendars = loadAvailableCalendarsSuspend(context)
                if (_uiState.value.availableCalendars != freshAvailableCalendars) {
                    _uiState.update { it.copy(availableCalendars = freshAvailableCalendars) }
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarioViewModel", "Error refreshing available calendars", e)
        }
    }
    
    fun updateCalendarData(newEvents: Map<LocalDate, List<Festivo>>, newAvailable: List<CalendarInfo>, newSelectedIds: Set<Long>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            
            // Cargar histórico actual para no perder el pasado remoto
            val cachedHistory = withContext(Dispatchers.IO) { loadHistoryFromDisk(context) }
            val mergedEvents = mergeHistoryWithSystem(cachedHistory, newEvents.values.flatten())

            _uiState.update { state ->
                state.copy(
                    eventsByDate = mergedEvents.groupBy { it.date },
                    availableCalendars = newAvailable,
                    selectedCalendarIds = newSelectedIds,
                )
            }
            // Guardar en histórico JSON respetando coherencia
            withContext(Dispatchers.IO) {
                saveHistoryToDisk(context, mergedEvents)
            }
            saveSelectedCalendarIds(context, newSelectedIds)
            CalendarAppWidgetProvider.triggerWidgetUpdate(context)
        }
    }

    fun setFavoriteCalendar(calendarId: Long?) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                if (calendarId == null) {
                    remove(AppConstants.KEY_FAVORITE_CALENDAR_ID)
                } else {
                    putLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, calendarId)
                }
            }
            _uiState.update { it.copy(favoriteCalendarId = calendarId) }
        }
    }

    private fun getFavoriteCalendarId(context: Context): Long? {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            val favoriteId = prefs.getLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, -1L)
            if (favoriteId != -1L) favoriteId else null
        } catch (_: ClassCastException) {
            when (val value = prefs.all[AppConstants.KEY_FAVORITE_CALENDAR_ID]) {
                is Number -> value.toLong().takeIf { it != -1L }
                is String -> value.toLongOrNull()?.takeIf { it != -1L }
                else -> null
            }
        }
    }

    fun setImportedEvent(event: Festivo?) {
        _uiState.update { it.copy(importedEvent = event) }
    }

    fun consumeImportedEvent() {
        _uiState.update { it.copy(importedEvent = null) }
    }

    fun syncHistoryToDrive(context: Context, onComplete: (SyncResult) -> Unit) {
        if (_uiState.value.isSyncing) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                        ?: return@withContext SyncResult(0, 0, false)

                    // 1. Asegurar datos frescos respetando coherencia
                    val selectedIds = loadSelectedCalendarIds(context)
                    if (selectedIds.isNotEmpty()) {
                        val freshEvents = readFestivosFromCalendarsSync(context, selectedIds)
                        val cachedHistory = loadHistoryFromDisk(context)
                        val merged = mergeHistoryWithSystem(cachedHistory, freshEvents.values.flatten())
                        saveHistoryToDisk(context, merged)
                    }

                    // 2. Sincronización Incremental (Shield)
                    GoogleDriveHelper(context, account).syncHistoryWithDrive()
                } catch (e: Exception) {
                    Log.e("ViewModel", "Sync error", e)
                    SyncResult(0, 0, false)
                }
            }

            if (result.success) {
                val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit { 
                    putLong(AppConstants.KEY_LAST_BACKUP_TIME, System.currentTimeMillis()) 
                    putInt(AppConstants.KEY_LAST_BACKUP_COUNT, result.totalEvents)
                }
            }

            _uiState.update { it.copy(isSyncing = false) }
            onComplete(result)
        }
    }

    fun restoreHistoryFromDrive(context: Context, onComplete: (Boolean) -> Unit) {
        if (_uiState.value.isRestoring) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            val success = withContext(Dispatchers.IO) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                        ?: return@withContext false

                    // 1. Descargar y sobrescribir el JSON local
                    GoogleDriveHelper(context, account).downloadHistoryFile()
                } catch (e: Exception) {
                    Log.e("ViewModel", "Restore error", e)
                    false
                }
            }

            if (success) {
                refreshData() // Recargar la UI con los nuevos datos
            }

            _uiState.update { it.copy(isRestoring = false) }
            onComplete(success)
        }
    }

    private fun mergeHistoryWithSystem(cachedHistory: List<Festivo>, systemEvents: List<Festivo>): List<Festivo> {
        val today = LocalDate.now()
        val windowStart = today.minusYears(1)
        val windowEnd = today.plusYears(5)
        val deletedIds = getDeletedEventIds(getApplication())

        // 1. Conservar eventos fuera de la ventana
        val historyOutsideWindow = cachedHistory.filter { 
            it.date.isBefore(windowStart) || it.date.isAfter(windowEnd) 
        }

        // 2. Unir con eventos del sistema (limpiando borrados)
        return (systemEvents + historyOutsideWindow)
            .filter { it.id !in deletedIds }
            .distinctBy { "${it.id}_${it.date}_${it.title}" }
    }
}
