@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendario.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class CalendarioUiState(
    val eventsByDate: Map<LocalDate, List<Festivo>> = emptyMap(),
    val availableCalendars: List<CalendarInfo> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val hasCalendarPermission: Boolean = false,
    val favoriteCalendarId: Long? = null,
    val importedEvent: Festivo? = null,
    val isSyncing: Boolean = false,
    val isRestoring: Boolean = false,
    val dailyNotes: Map<String, DailyNote> = emptyMap(),
    val cleaningCandidates: List<SearchItem> = emptyList()
)

class CalendarioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.calendarDao()

    private val _uiState = MutableStateFlow(CalendarioUiState())
    val uiState: StateFlow<CalendarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Asegurar trasvase JSON -> Room si es necesario
            MigrationManager.checkAndMigrate(application, database)
            
            // OBSERVACIÓN REACTIVA: La UI se actualiza sola cuando cambia la DB
            combine(dao.getAllEvents(), dao.getAllNotes()) { entities, noteEntities ->
                val events = entities.map { it.toFestivo() }
                val notes = noteEntities.associate { it.dateStr to it.toDailyNote() }
                events to notes
            }.collect { (events, notes) ->
                _uiState.update { it.copy(
                    eventsByDate = events.groupBy { it.date },
                    dailyNotes = notes
                ) }
                updateCleaningCandidates()
                // Notificar a los widgets
                CalendarAppWidgetProvider.triggerWidgetUpdate(application)
                WidgetStateManager.updateWidgetState(application, events)
            }
        }
        loadAllData()
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = isGranted) }
        if (isGranted) {
            loadAllData()
        }
    }

    fun refreshData(onComplete: () -> Unit = {}) {
        loadAllData(onComplete)
    }

    private fun loadAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                _uiState.update { it.copy(hasCalendarPermission = false) }
                onComplete()
                return@launch
            }

            try {
                // 1. CARGA DE CONFIGURACIÓN
                val availableCalendars = withContext(Dispatchers.IO) { loadAvailableCalendarsSuspend(context) }
                val selectedIds = withContext(Dispatchers.IO) { loadSelectedCalendarIds(context) }
                var favoriteId = withContext(Dispatchers.IO) { getFavoriteCalendarId(context) }
                
                BackupScheduler.ensureBackupScheduled(context)

                // 2. AUTO-REPARACIÓN DE FAVORITO
                if (favoriteId == null && availableCalendars.isNotEmpty()) {
                    favoriteId = availableCalendars.find { it.isPrimary }?.id ?: availableCalendars.find { it.canModify }?.id
                    favoriteId?.let { setFavoriteCalendar(it) }
                }

                // 3. SINCRONIZACIÓN CON GOOGLE
                val finalSelectedIds = if (selectedIds.isNotEmpty()) {
                    selectedIds.filter { sid -> availableCalendars.any { cal -> cal.id == sid } }.toSet()
                } else {
                    val defaultIds = availableCalendars.filter { it.canModify }.map { it.id }.toSet()
                    saveSelectedCalendarIds(context, defaultIds)
                    defaultIds
                }

                val systemEventsMap = if (finalSelectedIds.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, finalSelectedIds)
                } else {
                    emptyMap()
                }

                // 4. ACTUALIZACIÓN DE BASE DE DATOS
                withContext(Dispatchers.IO) {
                    val currentEntities = dao.getAllEvents().first()
                    val cachedHistory = currentEntities.map { it.toFestivo() }
                    val merged = mergeHistoryWithSystem(cachedHistory, systemEventsMap.values.flatten(), availableCalendars)
                    // Usamos refreshEvents para purgar duplicados antiguos y ADN obsoletos
                    dao.refreshEvents(merged.map { it.toEntity() })
                }

                _uiState.update { it.copy(
                    availableCalendars = availableCalendars,
                    selectedCalendarIds = finalSelectedIds,
                    hasCalendarPermission = true,
                    favoriteCalendarId = favoriteId
                ) }

            } catch (e: Exception) {
                Log.e("CalendarioViewModel", "Error en carga", e)
            } finally {
                onComplete()
            }
        }
    }

    fun refreshAvailableCalendars() {
        viewModelScope.launch {
            val context = getApplication<Application>()
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
    }

    fun updateCalendarData(newEvents: Map<LocalDate, List<Festivo>>, newAvailable: List<CalendarInfo>, newSelectedIds: Set<Long>) {
        viewModelScope.launch {
            val currentEntities = dao.getAllEvents().first()
            val cachedHistory = currentEntities.map { it.toFestivo() }
            val mergedEvents = mergeHistoryWithSystem(cachedHistory, newEvents.values.flatten(), newAvailable)

            withContext(Dispatchers.IO) {
                dao.refreshEvents(mergedEvents.map { it.toEntity() })
            }
            saveSelectedCalendarIds(getApplication(), newSelectedIds)
            _uiState.update { it.copy(availableCalendars = newAvailable, selectedCalendarIds = newSelectedIds) }
        }
    }

    fun setFavoriteCalendar(calendarId: Long?) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                if (calendarId == null) remove(AppConstants.KEY_FAVORITE_CALENDAR_ID)
                else putLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, calendarId)
            }
            _uiState.update { it.copy(favoriteCalendarId = calendarId) }
        }
    }

    private fun getFavoriteCalendarId(context: Context): Long? {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            val favoriteId = prefs.getLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, -1L)
            if (favoriteId != -1L) favoriteId else null
        } catch (_: Exception) { null }
    }

    fun setImportedEvent(event: Festivo?) { _uiState.update { it.copy(importedEvent = event) } }
    fun consumeImportedEvent() { _uiState.update { it.copy(importedEvent = null) } }

    fun syncHistoryToDrive(context: Context, onComplete: (SyncResult) -> Unit) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = withContext(Dispatchers.IO) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                        ?: return@withContext SyncResult(0, 0, false, 0L)
                    GoogleDriveHelper(context, account).syncHistoryWithDrive()
                } catch (e: Exception) { SyncResult(0, 0, false, 0L) }
            }
            if (result.success) {
                val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit { 
                    putLong(AppConstants.KEY_LAST_BACKUP_TIME, System.currentTimeMillis()) 
                    putInt(AppConstants.KEY_LAST_BACKUP_COUNT, result.totalEvents)
                    putLong(AppConstants.KEY_LAST_BACKUP_SIZE, result.sizeBytes)
                }
            }
            _uiState.update { it.copy(isSyncing = false) }
            onComplete(result)
        }
    }

    fun restoreHistoryFromDrive(context: Context, restorePrefs: Boolean, restoreHolidays: Boolean, restoreNotes: Boolean, restoreEvents: Boolean, onComplete: (Boolean) -> Unit) {
        if (_uiState.value.isRestoring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            val success = withContext(Dispatchers.IO) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                        ?: return@withContext false
                    GoogleDriveHelper(context, account).downloadAndRestoreSelective(restorePrefs, restoreHolidays, restoreNotes, restoreEvents)
                } catch (e: Exception) { false }
            }
            if (success) {
                withContext(Dispatchers.IO) {
                    val freshHistory = loadHistoryFromDisk(context)
                    val freshNotes = loadNotesFromDisk(context)
                    dao.insertEvents(freshHistory.map { it.toEntity() })
                    dao.insertNotes(freshNotes.map { it.toEntity() })
                }
            }
            _uiState.update { it.copy(isRestoring = false) }
            onComplete(success)
        }
    }

    fun restoreFromLocal(context: Context, uri: Uri, restorePrefs: Boolean, restoreHolidays: Boolean, restoreNotes: Boolean, restoreEvents: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            val success = withContext(Dispatchers.IO) {
                try {
                    BackupManager.importFullBackup(context, uri, restorePrefs, restoreHolidays, restoreNotes, restoreEvents)
                    true
                } catch (e: Exception) { false }
            }
            if (success) {
                withContext(Dispatchers.IO) {
                    val freshHistory = loadHistoryFromDisk(context)
                    val freshNotes = loadNotesFromDisk(context)
                    dao.insertEvents(freshHistory.map { it.toEntity() })
                    dao.insertNotes(freshNotes.map { it.toEntity() })
                }
            }
            _uiState.update { it.copy(isRestoring = false) }
            onComplete(success)
        }
    }

    fun saveDailyNote(date: LocalDate, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = DailyNote(date.toString(), content, System.currentTimeMillis(), false)
            dao.insertNote(note.toEntity())
        }
    }

    fun deleteDailyNote(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertNote(NoteEntity(date.toString(), "", System.currentTimeMillis(), true))
        }
    }

    fun removeOrphanEvent(event: Festivo) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markEventAsDeleted(event.id, System.currentTimeMillis())
        }
    }

    private fun updateCleaningCandidates() {
        val ghosts = _uiState.value.eventsByDate.values.flatten().filter { it.isGhost }.map { SearchItem.Event(it) }
        val emptyNotes = _uiState.value.dailyNotes.values.filter { it.content.isBlank() }.map { SearchItem.Note(it) }
        val candidates = (ghosts + emptyNotes).sortedBy { it.date }
        _uiState.update { it.copy(cleaningCandidates = candidates) }
    }

    fun deleteCleaningCandidate(item: SearchItem) {
        viewModelScope.launch(Dispatchers.IO) {
            when (item) {
                is SearchItem.Event -> dao.markEventAsDeleted(item.festivo.id, System.currentTimeMillis())
                is SearchItem.Note -> dao.insertNote(item.dailyNote.copy(content = "", isDeleted = true).toEntity())
            }
        }
    }

    fun deleteAllCleaningCandidates() {
        viewModelScope.launch(Dispatchers.IO) {
            val candidates = _uiState.value.cleaningCandidates
            candidates.forEach { item ->
                when (item) {
                    is SearchItem.Event -> dao.markEventAsDeleted(item.festivo.id, System.currentTimeMillis())
                    is SearchItem.Note -> dao.insertNote(item.dailyNote.copy(content = "", isDeleted = true).toEntity())
                }
            }
        }
    }

    private suspend fun mergeHistoryWithSystem(cachedHistory: List<Festivo>, systemEvents: List<Festivo>, availableCalendars: List<CalendarInfo>): List<Festivo> = withContext(Dispatchers.Default) {
        val today = LocalDate.now()
        val systemKeys = systemEvents.asSequence().map { it.adn }.toSet()
        val fuzzySystemMap = systemEvents.associateBy { "${it.date}_${it.title.unaccent().trim().lowercase()}" }
        val systemIdMap = systemEvents.associateBy( { "${it.id}_${it.date}" }, { it.adn } )

        (systemEvents + cachedHistory).asSequence()
            .distinctBy { event ->
                val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
                when {
                    event.id > 0 && systemIdMap.containsKey("${event.id}_${event.date}") -> systemIdMap["${event.id}_${event.date}"]
                    systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey) -> fuzzyKey
                    else -> event.adn
                }
            }
            .filter { event ->
                if (event.isDeleted) return@filter false
                if (!systemKeys.contains(event.adn) && !fuzzySystemMap.containsKey("${event.date}_${event.title.unaccent().trim().lowercase()}")) {
                    if (event.id < 0) return@filter false
                }
                true
            }
            .filter { it.date.isAfter(today.minusYears(20)) && it.date.isBefore(today.plusYears(6)) }
            .map { event ->
                if (event.id > 0) {
                    val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
                    val isPresent = systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey)
                    if (isPresent) event.copy(isGhost = false)
                    else {
                        val isWithinYear = !event.date.isBefore(today) && event.date.isBefore(today.plusYears(1))
                        val calendarExists = availableCalendars.any { it.id == event.calendarId }
                        event.copy(isGhost = isWithinYear && !calendarExists && !event.isFromHolidaySource && !event.isBirthday)
                    }
                } else event
            }
            .toList()
    }
}
