@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendario.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    
    // --- ESTADOS FASE 3: Filtros de Agenda Compartida PERSISTENTES ---
    val agendaSearchQuery: String = "",
    val agendaStartDate: LocalDate = LocalDate.now(),
    val agendaEndDate: LocalDate = LocalDate.now().plusMonths(1),
    val agendaActiveFilters: Set<String> = emptySet(),
    val agendaSearchResults: Map<LocalDate, List<SearchItem>> = emptyMap()
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
            
            // Carga inicial de ajustes de festivos
            refreshAdjustments()

            // OBSERVACIÓN REACTIVA: La UI se actualiza sola cuando cambia la DB
            combine(dao.getAllEvents(), dao.getAllNotes()) { entities, noteEntities ->
                val events = entities.map { it.toFestivo() }
                val notes = noteEntities.associateBy { it.dateStr }.mapValues { it.value.toDailyNote() }
                events to notes
            }.collect { (events, notes) ->
                _uiState.update { state -> state.copy(
                    eventsByDate = events.groupBy { event -> event.date },
                    dailyNotes = notes
                ) }
                updateCleaningCandidates()
                updateAgendaSearchResults() // Sincronizar resultados de agenda
                
                // Notificar a los widgets
                CalendarAppWidgetProvider.triggerWidgetUpdate(application)
                WidgetStateManager.updateWidgetState(application, events)

                // SINCRONIZACIÓN DE ALARMAS EN TIEMPO REAL:
                // Pasamos la lista 'events' que ya tenemos para ahorrar una lectura de DB.
                withContext(Dispatchers.IO) {
                    AlarmUtils.rescheduleAllAlarms(application, events)
                }
            }
        }
        loadAllData()
    }

    // --- ACCIONES AGENDA COMPARTIDA (FASE 3) ---
    fun updateAgendaSearchQuery(query: String) {
        _uiState.update { it.copy(agendaSearchQuery = query) }
        updateAgendaSearchResults()
    }

    fun updateAgendaStartDate(date: LocalDate) {
        _uiState.update { state ->
            // REGLA FASE 3 (v3.1.34): Si el inicio supera al fin, movemos el fin 1 mes adelante (como al inicio)
            val newEndDate = if (state.agendaEndDate.isBefore(date)) date.plusMonths(1) else state.agendaEndDate
            state.copy(agendaStartDate = date, agendaEndDate = newEndDate)
        }
        updateAgendaSearchResults()
    }

    fun updateAgendaEndDate(date: LocalDate) {
        _uiState.update { it.copy(agendaEndDate = date) }
        updateAgendaSearchResults()
    }

    fun toggleAgendaFilter(filter: String) {
        _uiState.update { state ->
            val newFilters = if (state.agendaActiveFilters.contains(filter)) state.agendaActiveFilters - filter else state.agendaActiveFilters + filter
            state.copy(agendaActiveFilters = newFilters)
        }
        updateAgendaSearchResults()
    }

    private fun updateAgendaSearchResults() {
        val state = _uiState.value
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        
        val normalizedQuery = state.agendaSearchQuery.unaccent().lowercase()
        val event1Kw = prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "")?.unaccent()?.lowercase() ?: ""
        val event2Kw = prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "")?.unaccent()?.lowercase() ?: ""

        val filteredEvents = state.eventsByDate.values.flatten()
            .filter { event ->
                val inRange = !event.date.isBefore(state.agendaStartDate) && !event.date.isAfter(state.agendaEndDate)
                if (!inRange) return@filter false
                val matchesQuery = normalizedQuery.isBlank() || event.title.unaccent().lowercase().contains(normalizedQuery)
                if (!matchesQuery) return@filter false
                if (state.agendaActiveFilters.isNotEmpty()) {
                    val normalizedTitle = event.title.unaccent().lowercase()
                    val isE1 = event1Kw.isNotBlank() && normalizedTitle.contains(event1Kw)
                    val isE2 = event2Kw.isNotBlank() && normalizedTitle.contains(event2Kw)
                    return@filter (state.agendaActiveFilters.contains("EVENT1") && isE1) ||
                                 (state.agendaActiveFilters.contains("EVENT2") && isE2) ||
                                 (state.agendaActiveFilters.contains("BIRTHDAY") && event.isBirthday)
                }
                true
            }
            .map { SearchItem.Event(it) }

        val filteredNotes = state.dailyNotes.values
            .filter { note ->
                val inRange = !note.date.isBefore(state.agendaStartDate) && !note.date.isAfter(state.agendaEndDate)
                if (!inRange) return@filter false
                val matchesQuery = normalizedQuery.isBlank() || note.content.unaccent().lowercase().contains(normalizedQuery)
                if (!matchesQuery) return@filter false
                if (state.agendaActiveFilters.isNotEmpty() && !state.agendaActiveFilters.contains("NOTE")) return@filter false
                true
            }
            .map { SearchItem.Note(it) }

        val sortedResults = (filteredEvents + filteredNotes).groupBy { it.date }.toSortedMap(compareByDescending { it })
        _uiState.update { it.copy(agendaSearchResults = sortedResults) }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = isGranted) }
        if (isGranted) {
            loadAllData()
        }
    }

    fun refreshData(onComplete: () -> Unit = {}) {
        refreshAdjustments()
        loadAllData(onComplete)
    }

    fun refreshAdjustments() {
        val context = getApplication<Application>()
        val adjustments = loadHolidayAdjustments(context)
        val workingDates = adjustments.asSequence()
            .filter { (it.type == HolidayAdjustmentType.WORKING_DAY) && (it.originalEventId == null) }
            .map { it.date }
            .toSet()
        _uiState.update { it.copy(workingDayDates = workingDates) }
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
                    saveSelectedCalendarIds(context, finalSelectedIds)
                }

                val systemEventsMap = if (finalSelectedIds.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, finalSelectedIds)
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
                    val freshSelectedIds = loadSelectedCalendarIds(context)
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

    private fun updateCleaningCandidates() {
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
                    
                    if (tipo == "CVO_AGENDA") {
                        isAgenda = true
                        // MODO AGENDA: Cargar datos
                        val eventsArray = json.optJSONArray("eventos")
                        val notesArray = json.optJSONArray("notas")
                        
                        val importedEvents = mutableListOf<Festivo>()
                        if (eventsArray != null) {
                            for (i in 0 until eventsArray.length()) {
                                val obj = eventsArray.getJSONObject(i)
                                importedEvents.add(Festivo(
                                    id = 0, 
                                    title = obj.getString("titulo"),
                                    description = null,
                                    date = LocalDate.parse(obj.getString("fecha")),
                                    startTime = null, 
                                    endTime = null,
                                    isAllDay = obj.optBoolean("es_todo_el_dia", true),
                                    calendarId = 0,
                                    isFromHolidaySource = false,
                                    rrule = obj.optString("rrule").takeIf { it.isNotEmpty() && it != "null" },
                                    isLongPeriod = obj.optBoolean("es_periodo_largo", false)
                                ))
                            }
                        }

                        val importedNotes = mutableListOf<DailyNote>()
                        if (notesArray != null) {
                            for (i in 0 until notesArray.length()) {
                                val obj = notesArray.getJSONObject(i)
                                importedNotes.add(DailyNote(
                                    dateStr = obj.getString("fecha"),
                                    content = obj.getString("contenido")
                                ))
                            }
                        }

                        _uiState.update { it.copy(
                            agendaImportEvents = importedEvents,
                            agendaImportNotes = importedNotes,
                            showAgendaImportPreview = autoShowAgendaPreview // Activamos solo si se solicita (v3.1.34)
                        ) }
                        return@withContext true
                    } else if (tipo == "CVO_HOLIDAYS") {
                        // MODO FESTIVOS: Preparar previsualización (v3.1.34)
                        val dataArray = json.getJSONArray("ajustes")
                        val imported = mutableListOf<HolidayAdjustment>()
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            val originalId = if (obj.has("originalEventId") && !obj.isNull("originalEventId")) obj.getLong("originalEventId") else null
                            imported.add(
                                HolidayAdjustment(
                                    date = LocalDate.parse(obj.getString("fecha")),
                                    title = obj.getString("titulo"),
                                    type = HolidayAdjustmentType.valueOf(obj.getString("tipo")),
                                    originalEventId = originalId,
                                )
                            )
                        }

                        _uiState.update { it.copy(
                            holidayImportItems = imported,
                            showHolidayImportPreview = true
                        ) }
                        return@withContext true
                    } else false
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
                val current = loadHolidayAdjustments(context).toMutableList()
                selectedItems.forEach { imp ->
                    current.removeAll { adj -> adj.date == imp.date }
                    current.add(imp)
                }
                saveHolidayAdjustments(context, current)
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
            
            withContext(Dispatchers.IO) {
                // 1. Importar Eventos (Evitando duplicados exactos en el mismo calendario) v3.1.34
                val currentEvents = readFestivosFromCalendarsSync(context, setOf(targetCalendarId)).values.flatten()
                val currentAdns = currentEvents.map { it.adn }.toSet()

                eventsToImport.forEach { event ->
                    val targetAdn = Festivo.generateAdn(event.date, event.title, null)
                    
                    if (!currentAdns.contains(targetAdn)) {
                        createEvent(
                            context = context,
                            title = event.title,
                            calendarId = targetCalendarId,
                            startDate = event.date.atStartOfDay(),
                            endDate = if (event.isLongPeriod) event.date.plusDays(1).atStartOfDay() else event.date.atStartOfDay(),
                            isAllDay = event.isAllDay,
                            repetitionRule = RepetitionRule.entries.find { it.rrule == event.rrule } ?: RepetitionRule.NONE,
                            isLongPeriod = event.isLongPeriod
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
                                // Caso B: Ya está incluido. No hacer nada.
                                Log.d("CalendarioVM", "Nota ignorada por duplicado en ${importedNote.dateStr}")
                            }
                            impContent.contains(localContent) -> {
                                // Caso C: La importada es más completa. Sustituir.
                                dao.insertNote(NoteEntity(importedNote.dateStr, importedNote.content, System.currentTimeMillis(), false))
                            }
                            else -> {
                                // Caso D: Son diferentes. Añadir al final.
                                val combined = "${existingNote.content}\n---\n${importedNote.content}"
                                dao.insertNote(NoteEntity(importedNote.dateStr, combined, System.currentTimeMillis(), false))
                            }
                        }
                    }
                }
            }
            
            cancelAgendaImport()
            refreshData()
            context.showToast(R.string.import_success)
        }
    }

}
