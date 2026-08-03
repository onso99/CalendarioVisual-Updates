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
    val dailyNotes: Map<String, DailyNote> = emptyMap(), // dateStr -> DailyNote
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

                // --- CARGA DE NOTAS DIARIAS ---
                val notes = withContext(Dispatchers.IO) { loadNotesFromDisk(context) }
                val notesMap = notes.asSequence().filter { !it.isDeleted }.associateBy { it.dateStr }

                // --- PASO 0: CARGA ULTRA-INSTANTÁNEA (JSON + Migración) ---
                var cachedHistory = withContext(Dispatchers.IO) { 
                    runCatching { loadHistoryFromDisk(context) }.getOrDefault(emptyList())
                }
                
                // MIGRACIÓN: Si el histórico está vacío, rescatamos de Prefs antiguos
                if (cachedHistory.isEmpty()) {
                    val oldPrefsEvents = loadEventsFromPrefs(context)
                    if (oldPrefsEvents.isNotEmpty()) {
                        cachedHistory = oldPrefsEvents.values.flatten()
                    }
                }
                
                // BLINDAJE DE ARRANQUE: Limpiamos la caché antes de mostrarla por primera vez
                if (cachedHistory.isNotEmpty()) {
                    cachedHistory = withContext(Dispatchers.Default) {
                        // Aplicamos la misma lógica de fusión pero sin eventos de sistema nuevos aún
                        mergeHistoryWithSystem(cachedHistory, emptyList())
                    }
                    _uiState.update { state -> 
                        state.copy(
                            eventsByDate = cachedHistory.groupBy { it.date },
                            hasCalendarPermission = true,
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

                // PROCESAMIENTO PESADO EN HILO DE CÓMPUTO (No bloquea la UI)
                val finalEventsList = withContext(Dispatchers.Default) {
                    mergeHistoryWithSystem(cachedHistory, systemEvents)
                }
                
                // GUARDADO SANEADO: Aseguramos que lo que va al disco y a Drive estÃ© deduplicado por ADN
                val cleanListToSave = withContext(Dispatchers.Default) {
                    finalEventsList.distinctBy { 
                        "${it.date}_${it.title.trim().lowercase().unaccent()}_${it.startTime}"
                    }
                }

                withContext(Dispatchers.IO) {
                    saveHistoryToDisk(context, cleanListToSave)
                }

                _uiState.value = CalendarioUiState(
                    eventsByDate = cleanListToSave.groupBy { it.date },
                    availableCalendars = availableCalendars,
                    selectedCalendarIds = validSelectedIds,
                    hasCalendarPermission = true,
                    favoriteCalendarId = favoriteId,
                    dailyNotes = notesMap,
                )

                if (validSelectedIds != selectedIds) {
                    saveSelectedCalendarIds(context, validSelectedIds)
                }
                
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                WidgetStateManager.updateWidgetState(context, cleanListToSave)

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("CalendarioViewModel", "Error loading all data", e)
                    Toast.makeText(context, R.string.error_updating_data, Toast.LENGTH_SHORT).show()
                }
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
            WidgetStateManager.updateWidgetState(context, mergedEvents)
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
                        ?: return@withContext SyncResult(0, 0, success = false)

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
                    SyncResult(0, 0, success = false)
                }
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

    fun restoreHistoryFromDrive(
        context: Context, 
        restorePrefs: Boolean,
        restoreHolidays: Boolean,
        restoreNotes: Boolean,
        restoreEvents: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        if (_uiState.value.isRestoring) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            val success = withContext(Dispatchers.IO) {
                try {
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                        ?: return@withContext false

                    GoogleDriveHelper(context, account).downloadAndRestoreSelective(
                        restorePrefs, restoreHolidays, restoreNotes, restoreEvents
                    )
                } catch (e: Exception) {
                    Log.e("ViewModel", "Restore error", e)
                    false
                }
            }

            if (success) {
                refreshData() 
            }

            _uiState.update { it.copy(isRestoring = false) }
            onComplete(success)
        }
    }

    fun restoreFromLocal(
        context: Context,
        uri: Uri,
        restorePrefs: Boolean,
        restoreHolidays: Boolean,
        restoreNotes: Boolean,
        restoreEvents: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            
            val success = withContext(Dispatchers.IO) {
                BackupManager.importFullBackup(
                    context, uri, restorePrefs, restoreHolidays, restoreNotes, restoreEvents
                )
            }
            
            if (success) {
                refreshData()
            }
            
            _uiState.update { it.copy(isRestoring = false) }
            onComplete(success)
        }
    }

    fun saveDailyNote(date: LocalDate, content: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val dateStr = date.toString()
            val newNote = DailyNote(dateStr = dateStr, content = content)
            
            val currentNotes = _uiState.value.dailyNotes.toMutableMap()
            currentNotes[dateStr] = newNote
            
            _uiState.update { it.copy(dailyNotes = currentNotes) }
            
            withContext(Dispatchers.IO) {
                val allNotes = loadNotesFromDisk(context).toMutableList()
                allNotes.removeAll { it.dateStr == dateStr }
                allNotes.add(newNote)
                saveNotesToDisk(context, allNotes)
            }
        }
    }

    fun deleteDailyNote(date: LocalDate) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val dateStr = date.toString()
            
            val currentNotes = _uiState.value.dailyNotes.toMutableMap()
            currentNotes.remove(dateStr)
            
            _uiState.update { it.copy(dailyNotes = currentNotes) }
            
            withContext(Dispatchers.IO) {
                val allNotes = loadNotesFromDisk(context).toMutableList()
                allNotes.removeAll { it.dateStr == dateStr }
                allNotes.add(DailyNote(dateStr = dateStr, content = "", isDeleted = true))
                saveNotesToDisk(context, allNotes)
            }
        }
    }

    private suspend fun mergeHistoryWithSystem(cachedHistory: List<Festivo>, systemEvents: List<Festivo>): List<Festivo> = withContext(Dispatchers.Default) {
        val context = getApplication<Application>()
        val deletedIds = getDeletedEventIds(context)
        val today = LocalDate.now()
        
        // Cargar ajustes para filtrado de laborables
        val adjustments = loadHolidayAdjustments(context)
        val workingDayIds = adjustments.asSequence().filter { it.type == HolidayAdjustmentType.WORKING_DAY }.mapNotNull { it.originalEventId }.toSet()

        // 1. GENERACIÓN DE CLAVES DE SISTEMA (Para comparación rápida)
        val systemKeys = systemEvents.asSequence()
            .map { "${it.date}_${it.title.trim().lowercase().unaccent()}_${it.startTime}" }
            .toSet()

        // 2. PROCESAMIENTO UNIFICADO CON SECUENCIAS
        // Combinamos historial y sistema. El sistema (fresco) va primero para mandar en la deduplicación.
        return@withContext (systemEvents + cachedHistory).asSequence()
            // Deduplicación agresiva por contenido
            .distinctBy { "${it.date}_${it.title.trim().lowercase().unaccent()}_${it.startTime}" }
            .filter { event ->
                val eventKey = "${event.date}_${event.title.trim().lowercase().unaccent()}_${event.startTime}"
                
                // A) Filtro de Seguridad: No recuperar si está marcado como borrado o laborable
                if ((event.id in deletedIds) || workingDayIds.contains(event.id)) return@filter false

                // B) Lógica de Resurrección Inteligente:
                // Si el evento NO está en el sistema pero SI en el historial...
                if (!systemKeys.contains(eventKey)) {
                    // Si es un festivo manual (ID < 0), solo lo mantenemos si es FRESCO (systemEvents lo trae)
                    if (event.id < 0) return@filter false
                    
                    // Si es un evento de Google (ID >= 0) y es FUTURO o muy reciente, 
                    // confiamos en que si Google no lo trae es porque se ha BORRADO.
                    if (event.date.isAfter(today.minusDays(7))) return@filter false
                }
                true
            }
            // 3. RECORTAR VENTANA (JSON Ligero pero inclusivo: 20 años atrás, 6 adelante)
            .filter { it.date.isAfter(today.minusYears(20)) && it.date.isBefore(today.plusYears(6)) }
            .toList()
    }
}
