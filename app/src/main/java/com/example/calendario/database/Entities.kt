package com.example.calendario.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val dbId: Long = 0,
    val googleId: Long, // ID original de Android Calendar
    val title: String,
    val description: String?,
    val date: String, // LocalDate en formato ISO (yyyy-MM-dd)
    val startTime: String?, // LocalTime en formato HH:mm
    val endTime: String?,
    val isAllDay: Boolean,
    val calendarId: Long,
    val isFromHolidaySource: Boolean,
    val rrule: String?,
    val age: Int?,
    val isBirthday: Boolean,
    val isLongPeriod: Boolean,
    val lane: Int?,
    val totalDays: Int,
    val currentDay: Int,
    val customColor: Int?,
    val fullStartMillis: Long?,
    val fullEndMillis: Long?,
    val repeatCount: Int?,
    val adn: String,
    val lastModified: Long,
    val isDeleted: Boolean,
    val isGhost: Boolean
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val dateStr: String, // Fecha ISO (yyyy-MM-dd) como clave primaria
    val content: String,
    val lastModified: Long,
    val isDeleted: Boolean
)
