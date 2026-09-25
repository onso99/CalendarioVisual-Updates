@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendario.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

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
    val cleaningCandidates: List<SearchItem> = emptyList(),
    val workingDayDates: Set<LocalDate> = emptySet(),
    // --- Estado para Importación de Agenda (.cvo) ---
    val agendaImportEvents: List<Festivo> = emptyList(),
    val agendaImportNotes: List<DailyNote> = emptyList(),
    val showAgendaImportPreview: Boolean = false,
    
    // --- Estado para Importación de Festivos (.cvo) v3.1.34 ---
    val holidayImportItems: List<HolidayAdjustment> = emptyList(),
    val showHolidayImportPreview: Boolean = false,

    // --- NUEVOS ESTADOS BÚSQUEDA UNIFICADA (v3.2.14) ---
    val mainSearchQuery: String = "",
    val mainSearchStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val mainSearchEndDate: LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
    val mainSearchFilters: Set<String> = setOf("EVENT", "NOTE"), // Restaurado a seleccionados por defecto (v3.2.16.2)
    val mainSearchTimeShortcut: String? = "MONTH", // Mes por defecto (v3.2.14.1)
    val mainSearchResults: Map<LocalDate, List<SearchItem>> = emptyMap()
)

class CalendarioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.calendarDao()

    private val _uiState = MutableStateFlow(CalendarioUiState())
    val uiState: StateFlow<CalendarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Carga inicial de ajustes de festivos
            refreshAdjustments()

            // OBSERVACIÓN REACTIVA: La UI se actualiza sola cuando cambia la DB
            combine(dao.getAllEvents(), dao.getAllNotes()) { entities, noteEntities ->
                val events = entities.map { it.toFestivo() }
                val notes = noteEntities.associateBy { it.dateStr }.mapValues { it.value.toDailyNote() }
                events to notes
            }.collect { (events, notes) ->
                val sortedEventsByDate = events.groupBy { event -> event.date }
                    .mapValues { (_, dayEvents) ->
                        dayEvents.sortedWith(
                            compareBy(
                                { !it.isAllDay && it.startTime != null },
                                { it.startTime ?: LocalTime.MIN },
                                { it.title.unaccent().lowercase() }
                            )
                        )
                    }
                _uiState.update { state -> state.copy(
                    eventsByDate = sortedEventsByDate,
                    dailyNotes = notes
                ) }
                updateCleaningCandidates()
                updateMainSearchResults()   // Sincronizar resultados de búsqueda principal (v3.2.14)
                
                // Notificar a los widgets
                CalendarAppWidgetProvider.triggerWidgetUpdate(application)
                WidgetStateManager.refreshWithCurrentEvents(application)

                // SINCRONIZACIÓN DE ALARMAS EN TIEMPO REAL:
                // Pasamos la lista 'events' que ya tenemos para ahorrar una lectura de DB.
                withContext(Dispatchers.IO) {
                    AlarmUtils.rescheduleAllAlarms(application, events)
                }
            }
        }
        loadAllData()
    }

    // --- ACCIONES BÚSQUEDA UNIFICADA (v3.2.14) ---
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(mainSearchQuery = query) }
        updateMainSearchResults()
    }

    fun updateSearchStartDate(date: LocalDate) {
        _uiState.update { state ->
            val newEndDate = if (state.mainSearchEndDate.isBefore(date)) date.plusYears(1) else state.mainSearchEndDate
            state.copy(mainSearchStartDate = date, mainSearchEndDate = newEndDate, mainSearchTimeShortcut = null)
        }
        updateMainSearchResults()
    }

    fun updateSearchEndDate(date: LocalDate) {
        _uiState.update { it.copy(mainSearchEndDate = date, mainSearchTimeShortcut = null) }
        updateMainSearchResults()
    }

    fun toggleSearchFilter(filter: String) {
        _uiState.update { state ->
            val current = state.mainSearchFilters
            val newFilters = if (current.contains(filter)) {
                current - filter 
            } else {
                current + filter
            }
            state.copy(mainSearchFilters = newFilters)
        }
        updateMainSearchResults()
    }

    fun applySearchTimeShortcut(shortcut: String) {
        _uiState.update { state ->
            // LÓGICA DE TOGGLE: Si ya está activo, lo quitamos
            if (state.mainSearchTimeShortcut == shortcut) {
                return@update state.copy(mainSearchTimeShortcut = null)
            }

            // Mantenemos las fechas actuales si el nuevo modo es ALWAYS (v3.2.14.5)
            if (shortcut == "ALWAYS") {
                return@update state.copy(mainSearchTimeShortcut = "ALWAYS")
            }

            val (start, end) = when (shortcut) {
                "MONTH" -> {
                    val month = java.time.YearMonth.now()
                    month.atDay(1) to month.atEndOfMonth()
                }
                "YEAR" -> {
                    val year = java.time.Year.now()
                    year.atDay(1) to year.atDay(year.length())
                }
                else -> state.mainSearchStartDate to state.mainSearchEndDate
            }
            state.copy(mainSearchStartDate = start, mainSearchEndDate = end, mainSearchTimeShortcut = shortcut)
        }
        updateMainSearchResults()
    }

    private fun updateMainSearchResults() {
        val state = _uiState.value
        
        // REGLA: No mostrar nada hasta que el usuario escriba
        if (state.mainSearchQuery.isBlank()) {
            _uiState.update { it.copy(mainSearchResults = emptyMap()) }
            return
        }

        val normalizedQuery = state.mainSearchQuery.unaccent().lowercase()
        
        // LÓGICA SIEMPRE (v3.2.14.5): Si está activo, ignoramos el rango visual
        val isAlways = state.mainSearchTimeShortcut == "ALWAYS"
        val effectiveStart = if (isAlways) LocalDate.of(1924, 1, 1) else state.mainSearchStartDate
        val effectiveEnd = if (isAlways) LocalDate.of(2124, 12, 31) else state.mainSearchEndDate

        // FILTRADO ESTRICTO (v3.2.16.2): Si no hay filtros, no mostramos nada (pero por defecto vienen ambos)
        val filteredEvents = if (state.mainSearchFilters.contains("EVENT")) {
            state.eventsByDate.values.flatten()
                .filter { event ->
                    val inRange = !event.date.isBefore(effectiveStart) && !event.date.isAfter(effectiveEnd)
                    if (!inRange) return@filter false
                    normalizedQuery.isBlank() || event.title.unaccent().lowercase().contains(normalizedQuery)
                }
                .map { SearchItem.Event(it) }
        } else emptyList()

        val filteredNotes = if (state.mainSearchFilters.contains("NOTE")) {
            state.dailyNotes.values
                .filter { note ->
                    val inRange = !note.date.isBefore(effectiveStart) && !note.date.isAfter(effectiveEnd)
                    if (!inRange) return@filter false
                    normalizedQuery.isBlank() || note.content.unaccent().lowercase().contains(normalizedQuery)
                }
                .map { SearchItem.Note(it) }
        } else emptyList()

        val sortedResults = (filteredEvents + filteredNotes)
            .groupBy { it.date }
            .mapValues { (_, items) ->
                // Orden interno del día: Hora ➔ Título (v3.2.14.6)
                items.sortedWith(compareBy({ (it as? SearchItem.Event)?.festivo?.startTime }, { (it as? SearchItem.Event)?.festivo?.title }))
            }
            .toSortedMap(compareByDescending { it })
        _uiState.update { it.copy(mainSearchResults = sortedResults) }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = isGranted) }
        if (isGranted) {
            loadAllData()
        }
    }

    private var lastLoadedAnchorYear: Int? = null

    fun refreshData(anchorDate: LocalDate = LocalDate.now(), onComplete: () -> Unit = {}) {
        refreshAdjustments()
        loadAllData(anchorDate, onComplete)
    }

    fun checkAndRefreshForAnchorDate(date: LocalDate) {
        val loadedYear = lastLoadedAnchorYear ?: return
        if (abs(date.year - loadedYear) >= 2) {
            refreshData(anchorDate = date)
        }
    }

    fun refreshAdjustments() {
        val context = getApplication<Application>()
        val adjustments = SettingsManager.getHolidayAdjustments(context)
        val workingDates = adjustments.asSequence()
            .filter { (it.type == HolidayAdjustmentType.WORKING_DAY) && (it.originalEventId == null) }
            .map { it.date }
            .toSet()
        _uiState.update { it.copy(workingDayDates = workingDates) }
    }

    private fun loadAllData(anchorDate: LocalDate = LocalDate.now(), onComplete: () -> Unit = {}) {
        lastLoadedAnchorYear = anchorDate.year
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
                val selectedIds = withContext(Dispatchers.IO) { SettingsManager.getSelectedCalendarIds(context) }
                var favoriteId = withContext(Dispatchers.IO) { getFavoriteCalendarId(context) }
                
                BackupScheduler.ensureBackupScheduled(context)

                // 2. AUTO-REPARACIÓN DE FAVORITO (Búsqueda inteligente v3.1.05)
                if (favoriteId == null && availableCalendars.isNotEmpty()) {
                    val bestCandidate = findBestCalendarCandidate(availableCalendars)
                    favoriteId = bestCandidate?.id
                    favoriteId?.let { setFavoriteCalendar(it) }
                }

                // 3. SINCRONIZACIÓN Y CURACIÓN DE SELECCIÓN
                // Si la selección restaurada no es válida en este móvil, intentamos auto-reparar
                val validSelectedIds = selectedIds.asSequence()
                    .filter { sid -> availableCalendars.any { cal -> cal.id == sid } }
                    .toSet()
                
                val finalSelectedIds = if (validSelectedIds.isEmpty() && selectedIds.isNotEmpty()) {
                    // SI ESTAMOS AQUÍ, ES QUE LOS IDs HAN CAMBIADO (POST-RESTAURACIÓN)
                    // Intentamos recuperar seleccionando calendarios primarios o modificables por defecto
                    availableCalendars.filter { it.canModify }.map { it.id }.toSet()
                        .ifEmpty { availableCalendars.asSequence().take(1).map { it.id }.toSet() }
                } else {
                    validSelectedIds.ifEmpty { 
                        // REGLA FASE-1 (v3.1.05): En primera instalación, solo el principal/favorito seleccionado
                        favoriteId?.let { setOf(it) } ?: availableCalendars.filter { it.canModify }.map { it.id }.toSet() 
                    }
                }
                
                // Si la selección ha cambiado tras la curación, la guardamos
                if (finalSelectedIds != selectedIds) {
                    SettingsManager.saveSelectedCalendarIds(context, finalSelectedIds)
                }

                val systemEventsMap = if (finalSelectedIds.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, finalSelectedIds, anchorDate)
                } else {
                    emptyMap()
                }

                // 4. ACTUALIZACIÓN DE BASE DE DATOS
                withContext(Dispatchers.IO) {
                    val currentEntities = dao.getAllEventsSync()
                    val cachedHistory = currentEntities.map { it.toFestivo() }
                    val merged = mergeHistoryWithSystemData(context, cachedHistory, systemEventsMap.values.flatten(), availableCalendars)
                    // Optimización: Usamos smartRefreshEvents para no borrar todo innecesariamente
                    dao.smartRefreshEvents(merged.map { it.toEntity() })
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
            val context = getApplication<Application>()
            val currentEntities = dao.getAllEventsSync()
            val cachedHistory = currentEntities.map { it.toFestivo() }
            val mergedEvents = mergeHistoryWithSystemData(context, cachedHistory, newEvents.values.flatten(), newAvailable)

            withContext(Dispatchers.IO) {
                // Optimización: Actualización quirúrgica
                dao.smartRefreshEvents(mergedEvents.map { it.toEntity() })
            }
            SettingsManager.saveSelectedCalendarIds(getApplication(), newSelectedIds)
            _uiState.update { it.copy(availableCalendars = newAvailable, selectedCalendarIds = newSelectedIds) }
        }
    }

    fun setFavoriteCalendar(calendarId: Long?) {
        viewModelScope.launch {
            if (calendarId == null) {
                SettingsManager.removeFavoriteCalendarId(getApplication())
            } else {
                SettingsManager.saveFavoriteCalendarId(getApplication(), calendarId)
            }
            _uiState.update { it.copy(favoriteCalendarId = calendarId) }
        }
    }

    private fun getFavoriteCalendarId(context: Context): Long? {
        return SettingsManager.getFavoriteCalendarId(context)
    }

    fun setImportedEvent(event: Festivo?) { _uiState.update { it.copy(importedEvent = event) } }
    fun consumeImportedEvent() { _uiState.update { it.copy(importedEvent = null) } }

    fun cancelAgendaImport() {
        _uiState.update { it.copy(
            showAgendaImportPreview = false,
            agendaImportEvents = emptyList(),
            agendaImportNotes = emptyList()
        ) }
    }

    fun cancelHolidayImport() {
        _uiState.update { it.copy(
            showHolidayImportPreview = false,
            holidayImportItems = emptyList()
        ) }
    }

    /**
     * Fuerza la visualización del asistente de agenda tras una confirmación manual (v3.1.34)
     */
    fun showAgendaImportPreview() {
        _uiState.update { it.copy(showAgendaImportPreview = true) }
    }

    fun syncHistoryToDrive(context: Context, onComplete: (SyncResult) -> Unit) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = withContext(Dispatchers.IO) {
                try {
                    // 1. Asegurar datos frescos respetando coherencia
                    val freshSelectedIds = SettingsManager.getSelectedCalendarIds(context)
                    val freshFavoriteId = getFavoriteCalendarId(context)
                    
                    val freshEvents = readFestivosFromCalendarsSync(context, freshSelectedIds)
                    val currentEntities = dao.getAllEventsSync()
                    val cachedHistory = currentEntities.map { it.toFestivo() }
                    val available = loadAvailableCalendarsSync(context)
                    val merged = mergeHistoryWithSystemData(context, cachedHistory, freshEvents.values.flatten(), available)
                    // Optimización: Sincronización ligera
                    dao.smartRefreshEvents(merged.map { it.toEntity() })

                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                        ?: return@withContext SyncResult(0, 0, false, 0L)
                        
                    // 2. Crear y subir Backup con metadatos de identidad de calendarios
                    val fullJson = BackupManager.createFullBackupJson(context, freshSelectedIds, freshFavoriteId)
                    GoogleDriveHelper(context, account).syncHistoryWithDrive()
                    SyncResult(merged.size, 0, true, fullJson.toString().toByteArray().size.toLong())
                } catch (_: Exception) { SyncResult(0, 0, false, 0L) }
            }
            if (result.success) {
                SettingsManager.saveLastBackupMetadata(context, result.totalEvents, result.sizeBytes)
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
                } catch (_: Exception) { false }
            }
            if (success) {
                // Al terminar con éxito, refreshData cargará los nuevos calendarios y Room 
                // ya tendrá los eventos inyectados por el BackupManager.
                refreshData()
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

    fun updateCleaningCandidates() {
        val allEvents = _uiState.value.eventsByDate.values.flatten()
        val ghosts = allEvents.filter { it.isGhost }.map { SearchItem.Event(it) }
        val emptyNotes = _uiState.value.dailyNotes.values.filter { it.content.isBlank() }.map { SearchItem.Note(it) }
        
        // Detectar duplicados de festivos manuales (ID -1) en el mismo día
        val manualHolidayDuplicates = allEvents.asSequence()
            .filter { it.id == -1L && it.isFromHolidaySource }
            .groupBy { it.date }
            .filter { it.value.size > 1 }
            .flatMap { (_, list) -> 
                // Sugerimos borrar todos excepto el que tenga el nombre más largo (probablemente el corregido)
                list.sortedByDescending { it.title.length }.drop(1) 
            }
            .map { SearchItem.Event(it) }
            .toList()

        val candidates = (ghosts + emptyNotes + manualHolidayDuplicates).sortedBy { it.date }
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

    /**
     * Motor de procesamiento para archivos .cvo externos (v3.1.34)
     * Soporta tanto CVO_AGENDA (con previsualización) como CVO_HOLIDAYS (directo)
     */
    fun processExternalCvo(
        uri: Uri, 
        autoShowAgendaPreview: Boolean = true, // Nuevo parámetro (v3.1.34)
        onResult: (Boolean, String?, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var isAgenda = false
            val success = withContext(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return@withContext false
                    val json = org.json.JSONObject(content)
                    val tipo = json.optString("tipo")
                    
                    when (tipo) {
                        "CVO_AGENDA" -> {
                            isAgenda = true
                            // MODO AGENDA: Cargar datos (Centralizado v3.1.34)
                            val eventsArray = json.optJSONArray("eventos")
                            val notesArray = json.optJSONArray("notas")
                            
                            val importedEvents = mutableListOf<Festivo>()
                            if (eventsArray != null) {
                                for (i in 0 until eventsArray.length()) {
                                    try {
                                        importedEvents.add(Festivo.fromJson(eventsArray.getJSONObject(i)))
                                    } catch (_: Exception) {}
                                }
                            }

                            val importedNotes = mutableListOf<DailyNote>()
                            if (notesArray != null) {
                                for (i in 0 until notesArray.length()) {
                                    try {
                                        importedNotes.add(DailyNote.fromJson(notesArray.getJSONObject(i)))
                                    } catch (_: Exception) {}
                                }
                            }

                            _uiState.update { it.copy(
                                agendaImportEvents = importedEvents,
                                agendaImportNotes = importedNotes,
                                showAgendaImportPreview = autoShowAgendaPreview // Activamos solo si se solicita (v3.1.34)
                            ) }
                            true
                        }
                        "CVO_HOLIDAYS" -> {
                            // MODO FESTIVOS: Preparar previsualización (Centralizado v3.1.34)
                            val dataArray = json.getJSONArray("ajustes")
                            val imported = mutableListOf<HolidayAdjustment>()
                            for (i in 0 until dataArray.length()) {
                                try {
                                    imported.add(HolidayAdjustment.fromJson(dataArray.getJSONObject(i)))
                                } catch (_: Exception) {}
                            }

                            _uiState.update { it.copy(
                                holidayImportItems = imported,
                                showHolidayImportPreview = true
                            ) }
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e("CalendarioVM", "Error procesando CVO externo: ${e.message}")
                    false
                }
            }
            // Importante: No refrescamos datos aquí, esperamos a la confirmación del diálogo (v3.1.34)
            onResult(success, if (success) null else "Error al procesar archivo", isAgenda)
        }
    }

    /**
     * Aplica la importación de festivos seleccionados (v3.1.34)
     */
    fun applyHolidayImport(selectedItems: List<HolidayAdjustment>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            withContext(Dispatchers.IO) {
                val current = SettingsManager.getHolidayAdjustments(context).toMutableList()
                selectedItems.forEach { imp ->
                    current.removeAll { adj -> adj.date == imp.date }
                    current.add(imp)
                }
                SettingsManager.saveHolidayAdjustments(context, current)
            }
            
            cancelHolidayImport()
            refreshAdjustments()
            refreshData()
            context.showToast(R.string.import_success)
        }
    }

    fun applyAgendaImport(targetCalendarId: Long) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val eventsToImport = _uiState.value.agendaImportEvents
            val notesToImport = _uiState.value.agendaImportNotes
            
            try {
                val anchorDate = eventsToImport.minOfOrNull { it.date } ?: LocalDate.now()
                withContext(Dispatchers.IO) {
                    // 1. Importar Eventos (Evitando duplicados exactos en el mismo calendario) v3.1.34
                    val currentEvents = readFestivosFromCalendarsSync(context, setOf(targetCalendarId), anchorDate).values.flatten()
                    val currentAdns = currentEvents.map { it.adn }.toSet()

                    eventsToImport.forEach { event ->
                        // 1. REGLA DE ORO FASE 4 (v3.1.34): Todo entra como evento SIMPLE.
                        // Calculamos el ADN localmente basado solo en datos básicos.
                        val targetAdn = Festivo.generateAdn(event.date, event.title, event.startTime)
                        
                        if (!currentAdns.contains(targetAdn)) {
                            // Reconstruimos fechas reales (sin repetición)
                            val startDT = if (event.startTime != null) java.time.LocalDateTime.of(event.date, event.startTime) else event.date.atStartOfDay()
                            val endDT = when {
                                event.endTime != null -> java.time.LocalDateTime.of(event.date, event.endTime)
                                event.isLongPeriod -> event.date.plusDays(1).atStartOfDay()
                                else -> startDT.plusMinutes(30)
                            }

                            createEvent(
                                context = context,
                                title = event.title,
                                calendarId = targetCalendarId,
                                startDate = startDT,
                                endDate = endDT,
                                isAllDay = event.isAllDay,
                                repetitionRule = RepetitionRule.NONE, // Forzado a SIMPLE
                                repeatUntil = null,
                                repeatCount = null,
                                isLongPeriod = event.isLongPeriod,
                                showToast = false
                            )
                        }
                    }

                    // 2. Importar Notas (Lógica de Fusión Inteligente por Contención v3.1.34)
                    notesToImport.forEach { importedNote ->
                        val existingNote = dao.getNoteByDate(importedNote.dateStr)
                        
                        if (existingNote == null || existingNote.content.isBlank()) {
                            dao.insertNote(NoteEntity(importedNote.dateStr, importedNote.content, System.currentTimeMillis(), false))
                        } else {
                            val localContent = existingNote.content.trim()
                            val impContent = importedNote.content.trim()

                            when {
                                localContent.contains(impContent) -> {
                                    // Ya incluido
                                }
                                impContent.contains(localContent) -> {
                                    // La importada es más completa. Sustituir.
                                    dao.insertNote(NoteEntity(importedNote.dateStr, importedNote.content, System.currentTimeMillis(), false))
                                }
                                else -> {
                                    // Son diferentes. Añadir al final.
                                    val combined = "${existingNote.content}\n---\n${importedNote.content}"
                                    dao.insertNote(NoteEntity(importedNote.dateStr, combined, System.currentTimeMillis(), false))
                                }
                            }
                        }
                    }
                }
                
                cancelAgendaImport()
                refreshData(anchorDate = anchorDate)
                context.showToast(R.string.import_success)
            } catch (e: Exception) {
                Log.e("CalendarioVM", "Error en applyAgendaImport", e)
                context.showToast("Error crítico durante la importación")
            }
        }
    }

}
