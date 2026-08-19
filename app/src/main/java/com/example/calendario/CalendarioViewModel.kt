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
import kotlinx.coroutines.async
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
    val cleaningCandidates: List<SearchItem> = emptyList() // Candidatos para limpieza de datos
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

    fun refreshData(onComplete: () -> Unit = {}) {
        loadAllData(onComplete)
    }

    private fun loadAllData(onComplete: () -> Unit = {}) {
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

                // --- 1. CARGA PARALELA INICIAL (IO) ---
                val notesTask = async(Dispatchers.IO) { loadNotesFromDisk(context) }
                val historyTask = async(Dispatchers.IO) { 
                    runCatching { loadHistoryFromDisk(context) }.getOrDefault(emptyList())
                }
                
                val notes = notesTask.await()
                val notesMap = notes.asSequence().filter { !it.isDeleted }.associateBy { it.dateStr }
                var cachedHistory = historyTask.await()
                
                // MIGRACIÓN: Si el histórico está vacío, rescatamos de Prefs antiguos
                if (cachedHistory.isEmpty()) {
                    cachedHistory = withContext(Dispatchers.IO) { loadEventsFromPrefs(context).values.flatten() }
                }
                
                // --- 2. ARRANQUE "FLASH" (Paso 0) ---
                // Mostramos lo que tenemos en disco inmediatamente sin filtros pesados
                if (cachedHistory.isNotEmpty()) {
                    val fastGrouped = withContext(Dispatchers.Default) { 
                        cachedHistory.groupBy { it.date } 
                    }
                    _uiState.update { state -> 
                        state.copy(
                            eventsByDate = fastGrouped,
                            dailyNotes = notesMap,
                            hasCalendarPermission = true,
                        ) 
                    }
                }

                // --- 3. TAREAS DE SISTEMA (Calendarios disponibles) ---
                val selectedIdsTask = async(Dispatchers.IO) { loadSelectedCalendarIds(context) }
                val favoriteIdTask = async(Dispatchers.IO) { getFavoriteCalendarId(context) }
                val availableCalendars = loadAvailableCalendarsSuspend(context)

                var selectedIds = selectedIdsTask.await()
                var favoriteId = favoriteIdTask.await()

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

                // CURACIÓN DE SELECCIÓN POST-RESTAURACIÓN: 
                // Si los IDs han cambiado, intentamos recuperar la selección por nombre de calendario
                var finalSelectedIds = validSelectedIds
                if (selectedIds.isNotEmpty() && validSelectedIds.isEmpty()) {
                    val recoveredIds = availableCalendars.filter { it.canModify }.map { it.id }.toSet()
                    finalSelectedIds = recoveredIds.ifEmpty { 
                        availableCalendars.asSequence().take(1).map { it.id }.toSet() 
                    }
                    saveSelectedCalendarIds(context, finalSelectedIds)
                }

                // --- PASO 2: SINCRONIZACIÓN CON EL SISTEMA ---
                val systemEventsMap = if (finalSelectedIds.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, finalSelectedIds)
                } else {
                    emptyMap()
                }
                val systemEvents = systemEventsMap.values.flatten()

                // PROCESAMIENTO PESADO EN HILO DE CÓMPUTO (No bloquea la UI)
                val finalEventsList = withContext(Dispatchers.Default) {
                    mergeHistoryWithSystem(cachedHistory, systemEvents, availableCalendars)
                }
                
                // GUARDADO EN SEGUNDO PLANO: No bloqueamos la UI final por el disco
                launch(Dispatchers.IO) {
                    saveHistoryToDisk(context, finalEventsList)
                }

                _uiState.update { state -> 
                    state.copy(
                        eventsByDate = finalEventsList.groupBy { it.date },
                        availableCalendars = availableCalendars,
                        selectedCalendarIds = finalSelectedIds,
                        hasCalendarPermission = true,
                        favoriteCalendarId = favoriteId,
                        dailyNotes = notesMap,
                    )
                }
                
                // Actualizar candidatos de limpieza
                updateCleaningCandidates()

                if (validSelectedIds != selectedIds) {
                    saveSelectedCalendarIds(context, validSelectedIds)
                }
                
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                WidgetStateManager.updateWidgetState(context, finalEventsList)
                onComplete()

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("CalendarioViewModel", "Error loading all data", e)
                    Toast.makeText(context, R.string.error_updating_data, Toast.LENGTH_SHORT).show()
                }
                onComplete()
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
            val available = loadAvailableCalendarsSuspend(context)
            val mergedEvents = mergeHistoryWithSystem(cachedHistory, newEvents.values.flatten(), available)

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
                        val available = loadAvailableCalendarsSuspend(context)
                        val merged = mergeHistoryWithSystem(cachedHistory, freshEvents.values.flatten(), available)
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

    fun removeOrphanEvent(event: Festivo) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            withContext(Dispatchers.IO) {
                // MARCADO PERSISTENTE: No lo borramos, lo marcamos como borrado para que no resucite al sincronizar
                val currentHistory = loadHistoryFromDisk(context).toMutableList()
                val index = currentHistory.indexOfFirst { it.adn == event.adn }
                if (index != -1) {
                    currentHistory[index] = currentHistory[index].copy(isDeleted = true, lastModified = System.currentTimeMillis())
                    saveHistoryToDisk(context, currentHistory)
                }
            }
            refreshData()
        }
    }

    private fun updateCleaningCandidates() {
        val ghosts = _uiState.value.eventsByDate.values.flatten()
            .filter { it.isGhost }
            .map { SearchItem.Event(it) }
        
        val emptyNotes = _uiState.value.dailyNotes.values
            .filter { it.content.isBlank() }
            .map { SearchItem.Note(it) }
        
        val candidates = (ghosts + emptyNotes).sortedBy { it.date }
        _uiState.update { it.copy(cleaningCandidates = candidates) }
    }

    fun deleteCleaningCandidate(item: SearchItem) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            withContext(Dispatchers.IO) {
                when (item) {
                    is SearchItem.Event -> {
                        val currentHistory = loadHistoryFromDisk(context).toMutableList()
                        val index = currentHistory.indexOfFirst { it.adn == item.festivo.adn }
                        if (index != -1) {
                            currentHistory[index] = currentHistory[index].copy(isDeleted = true, lastModified = System.currentTimeMillis())
                            saveHistoryToDisk(context, currentHistory)
                        }
                    }
                    is SearchItem.Note -> {
                        val allNotes = loadNotesFromDisk(context).toMutableList()
                        allNotes.removeAll { it.dateStr == item.dailyNote.dateStr }
                        saveNotesToDisk(context, allNotes)
                    }
                }
            }
            refreshData()
        }
    }

    fun deleteAllCleaningCandidates() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val candidates = _uiState.value.cleaningCandidates
            if (candidates.isEmpty()) return@launch

            withContext(Dispatchers.IO) {
                // Borrado persistente de eventos fantasma
                val eventAdns = candidates.filterIsInstance<SearchItem.Event>().map { it.festivo.adn }.toSet()
                if (eventAdns.isNotEmpty()) {
                    val currentHistory = loadHistoryFromDisk(context).toMutableList()
                    var changed = false
                    currentHistory.forEachIndexed { i, ev ->
                        if (ev.adn in eventAdns) {
                            currentHistory[i] = ev.copy(isDeleted = true, lastModified = System.currentTimeMillis())
                            changed = true
                        }
                    }
                    if (changed) saveHistoryToDisk(context, currentHistory)
                }

                // Borrado de notas vacías
                val noteDates = candidates.filterIsInstance<SearchItem.Note>().map { it.dailyNote.dateStr }.toSet()
                if (noteDates.isNotEmpty()) {
                    val currentNotes = loadNotesFromDisk(context).toMutableList()
                    currentNotes.removeAll { it.dateStr in noteDates }
                    saveNotesToDisk(context, currentNotes)
                }
            }
            refreshData()
        }
    }

    private suspend fun mergeHistoryWithSystem(
        cachedHistory: List<Festivo>, 
        systemEvents: List<Festivo>,
        availableCalendars: List<CalendarInfo>
    ): List<Festivo> = withContext(Dispatchers.Default) {
        val context = getApplication<Application>()
        val deletedIds = getDeletedEventIds(context)
        val today = LocalDate.now()
        
        // Cargar ajustes para filtrado de laborables
        val adjustments = loadHolidayAdjustments(context)
        val workingDayIds = adjustments.asSequence().filter { it.type == HolidayAdjustmentType.WORKING_DAY }.mapNotNull { it.originalEventId }.toSet()

        // 1. GENERACIÓN DE MAPAS DE SISTEMA PARA CURACIÓN
        val systemKeys = systemEvents.asSequence().map { it.adn }.toSet()
        
        // Mapa para curación por similitud (Fecha + Título -> Evento)
        val fuzzySystemMap = systemEvents.associateBy { "${it.date}_${it.title.unaccent().trim().lowercase()}" }

        // MAPA DE ANCLAJE POR ID: Crucial para evitar duplicados al cambiar TÍTULO
        // (ID + Fecha -> ADN actual en el sistema)
        val systemIdMap = systemEvents.associateBy( { "${it.id}_${it.date}" }, { it.adn } )

        // 2. PROCESAMIENTO UNIFICADO CON SECUENCIAS
        // Combinamos historial y sistema. El sistema (fresco) va primero para mandar en la deduplicación.
        return@withContext (systemEvents + cachedHistory).asSequence()
            // Deduplicación agresiva:
            .distinctBy { event ->
                val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
                
                when {
                    // A) Si el ID coincide en el mismo día, usamos el ADN del sistema como clave única
                    // Esto fusiona "Viejo Título" con "Nuevo Título" instantáneamente.
                    event.id > 0 && systemIdMap.containsKey("${event.id}_${event.date}") -> {
                        systemIdMap["${event.id}_${event.date}"]
                    }
                    // B) Si el Título y Fecha coinciden (aunque cambie la hora), usamos FuzzyKey
                    systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey) -> {
                        fuzzyKey
                    }
                    // C) Si no hay coincidencias, mantenemos su propia identidad
                    else -> event.adn
                }
            }
            .filter { event ->
                // A) Filtro de Seguridad: No recuperar si está marcado como borrado (físico o lógico) o laborable
                if (event.isDeleted || (event.id in deletedIds) || workingDayIds.contains(event.id)) return@filter false

                // B) Lógica de Resurrección Inteligente:
                // Si el evento NO está en el sistema (por ADN ni por Similitud)...
                val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
                val isPresentInSystem = systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey)

                if (!isPresentInSystem) {
                    // Si es un festivo manual (ID < 0), solo lo mantenemos si es FRESCO (systemEvents lo trae)
                    // Si ya no viene en systemEvents es porque el usuario lo borró del gestor.
                    if (event.id < 0) return@filter false
                    
                    // Si es un evento de Google (ID >= 0) lo mantenemos siempre para decidir 
                    // si es Fantasma o Historial Legítimo en el siguiente paso.
                }
                true
            }
            // 3. RECORTAR VENTANA (JSON Ligero pero inclusivo: 20 años atrás, 6 adelante)
            .filter { it.date.isAfter(today.minusYears(20)) && it.date.isBefore(today.plusYears(6)) }
            // 4. CURACIÓN DE IDENTIDAD Y MARCADO DE FANTASMAS
            .map { event ->
                if (event.id > 0) {
                    val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
                    val isPresentInSystem = systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey)
                    
                    // Si el evento está en Google (por ADN o Similitud), forzamos que NO sea fantasma
                    if (isPresentInSystem) return@map event.copy(isGhost = false)

                    // CRITERIO DE FANTASMA RESTRINGIDO:
                    // 1. Es un evento PRÓXIMO (desde hoy hasta dentro de 1 año)
                    //    (Evitamos marcar como fantasmas eventos muy lejanos que Google aún no sincroniza)
                    // 2. El calendario ya no existe en el móvil actual (cambio de dispositivo/ID)
                    // 3. NO es un festivo oficial de Google ni un cumpleaños de sistema
                    
                    val isWithinOneYear = !event.date.isBefore(today) && event.date.isBefore(today.plusYears(1))
                    val calendarExists = availableCalendars.any { it.id == event.calendarId }
                    
                    val isGhostCandidate = isWithinOneYear && !calendarExists && !event.isFromHolidaySource && !event.isBirthday
                    
                    event.copy(isGhost = isGhostCandidate)
                } else {
                    event
                }
            }
            .toList()
    }
}
