package com.example.calendario

import java.time.LocalDate
import java.time.LocalTime

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int? = null // El color original del calendario SÍ se usa para el diálogo de selección de calendarios
)

// --- FESTIVO MODIFICADO (SIN CAMPO 'color' DE EVENTO) ---
data class Festivo(
    val id: Long,                 // ID único del evento del CalendarProvider
    val title: String,            // Título del evento
    val description: String,
    val date: LocalDate,          // Fecha del evento (LocalDate)
    val startTime: LocalTime?,    // Hora de inicio si no es todo el día
    val isAllDay: Boolean,
    // No añadimos 'color: Int' aquí si no se va a usar para este evento
    val calendarId: Long,         // ID del calendario del que proviene
    var isFromHolidaySource: Boolean = false
)
// --- FIN FESTIVO MODIFICADO ---

data class FestivoDto(
    val desc: String,
    val id: Long,
    val startTimeStr: String? = null,
    val isAllDay: Boolean = true
)

