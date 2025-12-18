package com.example.calendario

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalendarioUiState(
    val eventsByDate: Map<LocalDate, List<Festivo>> = emptyMap(),
    val availableCalendars: List<CalendarInfo> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val hasCalendarPermission: Boolean = false
)

class CalendarioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarioUiState())
    val uiState: StateFlow<CalendarioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val initialEvents = loadEventsFromPrefs(context)
            val initialSelectedIds = loadSelectedCalendarIds(context)
            val initialPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

            _uiState.value = CalendarioUiState(
                eventsByDate = initialEvents,
                selectedCalendarIds = initialSelectedIds,
                hasCalendarPermission = initialPermission
            )

            if (initialPermission) {
                refreshData()
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = isGranted) }
        if (isGranted) {
            refreshData()
        } else {
            viewModelScope.launch {
                val context = getApplication<Application>()
                _uiState.value = CalendarioUiState() // Clear all data
                saveEventsToPrefs(context, emptyMap())
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                if (_uiState.value.hasCalendarPermission) { // Only update if state changed
                    _uiState.value = CalendarioUiState() // Reset state
                    saveEventsToPrefs(context, emptyMap())
                    CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                }
                return@launch
            }

            if (!_uiState.value.hasCalendarPermission) {
                 _uiState.update { it.copy(hasCalendarPermission = true) }
            }

            try {
                val currentSelectedIds = _uiState.value.selectedCalendarIds
                val freshAvailableCalendars = loadAvailableCalendarsSuspend(context)

                var dataChanged = false

                if (_uiState.value.availableCalendars != freshAvailableCalendars) {
                    _uiState.update { it.copy(availableCalendars = freshAvailableCalendars) }
                    dataChanged = true
                }

                val validSelectedIds = currentSelectedIds.filter { sid -> freshAvailableCalendars.any { cal -> cal.id == sid } }.toSet()
                if (validSelectedIds != _uiState.value.selectedCalendarIds) {
                    _uiState.update { it.copy(selectedCalendarIds = validSelectedIds) }
                    saveSelectedCalendarIds(context, validSelectedIds)
                    dataChanged = true
                }

                val freshEventsMap = if (_uiState.value.selectedCalendarIds.isNotEmpty() || freshAvailableCalendars.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(context, _uiState.value.selectedCalendarIds, freshAvailableCalendars)
                } else {
                    emptyMap()
                }

                if (_uiState.value.eventsByDate != freshEventsMap) {
                    _uiState.update { it.copy(eventsByDate = freshEventsMap) }
                    saveEventsToPrefs(context, freshEventsMap)
                    dataChanged = true
                }

                if (dataChanged) {
                    CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                }

            } catch (e: Exception) {
                Log.e("CalendarioViewModel", "Error refreshing calendar data", e)
                Toast.makeText(context, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()

                 _uiState.value = CalendarioUiState(hasCalendarPermission = _uiState.value.hasCalendarPermission) // Keep permission state
                saveEventsToPrefs(context, emptyMap())
                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            }
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
}
