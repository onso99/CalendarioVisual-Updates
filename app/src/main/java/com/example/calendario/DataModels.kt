package com.example.calendario

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
    val originalBirthDate: LocalDate? = null,
    val alarmTimeMillis: Long? = null,
    val isLongPeriod: Boolean = false,
    val lane: Int? = null,
    val totalDays: Int = 1,
    val currentDay: Int = 1,
    val customColor: Int? = null,
    val fullStartMillis: Long? = null,
    val fullEndMillis: Long? = null,
    val repeatCount: Int? = null,
    // --- Campos para Sincronización Segura ---
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val ownerAccount: String,
    val isPrimary: Boolean,
    val color: Int,
    val canModify: Boolean,
    val accessLevel: Int,
    val isDeleted: Boolean
)

data class FestivoDto(
    val title: String?,
    val description: String?,
    val id: Long,
    val calendarId: Long? = 0L,
    val startTimeStr: String?,
    val endTimeStr: String?,
    val isAllDay: Boolean,
    val rrule: String?,
    val age: Int?,
    val isBirthday: Boolean? = false,
    val isFromHolidaySource: Boolean? = false,
    val alarmTimeMillis: Long? = null,
    val isLongPeriod: Boolean? = false,
    val lane: Int? = null,
    val totalDays: Int? = 1,
    val currentDay: Int? = 1,
    val customColor: Int? = null,
    val fullStartMillis: Long? = null,
    val fullEndMillis: Long? = null,
    val repeatCount: Int? = null,
    // --- Campos para Sincronización Segura ---
    val lastModified: Long? = null,
    val isDeleted: Boolean? = false
)

data class HolidayAdjustment(
    val date: LocalDate,
    val title: String,
    val type: HolidayAdjustmentType,
    val originalEventId: Long? = null
)

enum class HolidayAdjustmentType {
    HOLIDAY,
    WORKING_DAY
}

data class SyncResult(
    val totalEvents: Int,
    val deletedCount: Int,
    val success: Boolean
)

data class HolidayAdjustmentDto(
    val dateStr: String,
    val title: String,
    val type: String,
    val originalEventId: Long? = null
)
