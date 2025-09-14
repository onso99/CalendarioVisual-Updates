package com.example.calendario

import java.time.LocalDate
import java.time.LocalTime

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int? = null
)

data class Festivo(
    val date: LocalDate,
    val description: String,
    val calendarId: Long,
    var isFromHolidaySource: Boolean = false,
    val startTime: LocalTime? = null,
    val isAllDay: Boolean = true
)

data class FestivoDto(
    val desc: String,
    val id: Long,
    val startTimeStr: String? = null,
    val isAllDay: Boolean = true
)
