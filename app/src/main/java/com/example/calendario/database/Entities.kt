package com.example.calendario.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val adn: String, // Identidad única: Fecha_Título_Hora
    val googleId: Long, // ID original de Android Calendar
    val title: String,
    val description: String?,
    val date: String, // LocalDate ISO
    val startTime: String?, 
    val endTime: String?,
    val isAllDay: Boolean,
    val calendarId: Long,
    val isFromHolidaySource: Boolean,
    val rrule: String?,
    val age: Int?,
    val isBirthday: Boolean,
    val originalBirthDate: String?, // Añadido: LocalDate ISO para cálculo de edad
    val isLongPeriod: Boolean,
    val lane: Int?,
    val totalDays: Int,
    val currentDay: Int,
    val customColor: Int?,
    val fullStartMillis: Long?,
    val fullEndMillis: Long?,
    val repeatCount: Int?,
    val repeatIndex: Int?, // Añadido v3.1.64
    val hasIncident: Boolean, // Añadido v3.2.06
    val lastModified: Long,
    val isDeleted: Boolean,
    val isGhost: Boolean
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val dateStr: String, // Fecha ISO
    val content: String,
    val lastModified: Long,
    val isDeleted: Boolean
)
