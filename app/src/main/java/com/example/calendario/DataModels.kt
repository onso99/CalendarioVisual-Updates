package com.example.calendario

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

data class Festivo(
    val id: Long, // eventId para eventos del calendario
    val title: String,
    val description: String?,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val calendarId: Long,
    val isFromHolidaySource: Boolean,
    val rrule: String?,
    val age: Int? = null,
    val isBirthday: Boolean = false,
    val originalBirthDate: LocalDate? = null
)

data class FestivoDto(
    val title: String?,
    val description: String?,
    val id: Long,
    val startTimeStr: String?,
    val endTimeStr: String?,
    val isAllDay: Boolean,
    val rrule: String?,
    val age: Int?,
    val isBirthday: Boolean? = false
)


data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int?,
    val isPrimary: Boolean,
    val canModify: Boolean
)

data class CustomColor(
    val color: Color,
    val name: String,
    val source: String
)

