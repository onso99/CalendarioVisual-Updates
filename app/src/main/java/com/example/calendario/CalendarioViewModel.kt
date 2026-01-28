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
    val favoriteCalendarId: Long? = null
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
                saveEventsToPrefs(context, emptyMap())
                saveSelectedCalendarIds(context, emptySet())
                setFavoriteCalendar(null) // Clear favorite
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
            val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                _uiState.value = CalendarioUiState(hasCalendarPermission = false)
                return@launch
            }

            try {
                val initialEvents = loadEventsFromPrefs(context)
                var selectedIds = loadSelectedCalendarIds(context)
                var favoriteId = getFavoriteCalendarId(context)
                val availableCalendars = loadAvailableCalendarsSuspend(context)

                val favoriteExists = availableCalendars.any { it.id == favoriteId }
                if ((favoriteId == null || !favoriteExists) && availableCalendars.any { it.canModify }) {
                    findBestCalendarCandidate(availableCalendars)?.id?.let {
                        favoriteId = it
                        setFavoriteCalendar(it)
                    }
                }

                if (favoriteId != null && !selectedIds.contains(favoriteId)) {
                    selectedIds = selectedIds.toMutableSet().apply { add(favoriteId) }
                    saveSelectedCalendarIds(context, selectedIds)
                }

                val validSelectedIds = selectedIds.filter { sid -> availableCalendars.any { cal -> cal.id == sid } }.toSet()

                val finalEvents = if (validSelectedIds.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, validSelectedIds)
                } else {
                    initialEvents
                }

                _uiState.value = CalendarioUiState(
                    eventsByDate = finalEvents,
                    availableCalendars = availableCalendars,
                    selectedCalendarIds = validSelectedIds,
                    hasCalendarPermission = true,
                    favoriteCalendarId = favoriteId
                )

                if (validSelectedIds != selectedIds) {
                    saveSelectedCalendarIds(context, validSelectedIds)
                }
                if (finalEvents != initialEvents) {
                    saveEventsToPrefs(context, finalEvents)
                }

                CalendarAppWidgetProvider.triggerWidgetUpdate(context)

            } catch (e: Exception) {
                Log.e("CalendarioViewModel", "Error loading all data", e)
                Toast.makeText(context, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun refreshAvailableCalendars() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            withContext(Dispatchers.IO) { // Ensure suspend function is called from a coroutine
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
            _uiState.update {
                it.copy(
                    eventsByDate = newEvents,
                    availableCalendars = newAvailable,
                    selectedCalendarIds = newSelectedIds
                )
            }
            saveEventsToPrefs(context, newEvents)
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
        val favoriteId = prefs.getLong(AppConstants.KEY_FAVORITE_CALENDAR_ID, -1L)
        return if (favoriteId != -1L) favoriteId else null
    }
}
