package com.example.calendario

import java.time.LocalDate
import java.time.LocalTime

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int? = null,
    val isPrimary: Boolean,
    val canModify: Boolean
)

data class Festivo(
    val id: Long,
    val title: String,
    val description: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val calendarId: Long,
    var isFromHolidaySource: Boolean = false,
    val rrule: String? = null
)

data class FestivoDto(
    val desc: String,
    val id: Long, // En FestivoDto, este 'id' es el calendarId
    val startTimeStr: String?,
    val endTimeStr: String?,
    val isAllDay: Boolean,
    val rrule: String? = null
    // No persistimos 'date' aquí porque es la clave del Map en SharedPreferences
    // No persistimos 'title' explícitamente si 'desc' es suficiente para el DTO
    // No persistimos 'isFromHolidaySource' directamente aquí, se determina al cargar/procesar
)

