package com.example.calendario

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

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
    val isBirthday: Boolean = false,
    val originalBirthDate: LocalDate? = null,
    val age: Int? = if (isBirthday && originalBirthDate != null) {
        java.time.Period.between(originalBirthDate, date).years
    } else null,
    val alarmTimeMillis: Long? = null,
    val isLongPeriod: Boolean = false,
    val lane: Int? = null,
    val totalDays: Int = 1,
    val currentDay: Int = 1,
    val customColor: Int? = null,
    val fullStartMillis: Long? = null,
    val fullEndMillis: Long? = null,
    val repeatCount: Int? = null,
    val adn: String = "", // Huella digital única para deduplicación y refresco
    // --- Campos para Sincronización Segura ---
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isGhost: Boolean = false
) {
    companion object {
        fun generateAdn(date: LocalDate, title: String, startTime: LocalTime?): String {
            val cleanTitle = title.unaccent().trim().lowercase()
            // Normalizamos la hora a HH:mm (sin segundos ni milisegundos) especificando Locale.US
            val timeStr = startTime?.let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) } ?: "null"
            return "${date}_${cleanTitle}_$timeStr"
        }
    }
}

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
    val success: Boolean,
    val sizeBytes: Long = 0L
)


data class HolidayAdjustmentDto(
    val dateStr: String,
    val title: String,
    val type: String,
    val originalEventId: Long? = null
)

// --- Modelos para Notas Diarias ---

data class DailyNote(
    val dateStr: String, // Formato "yyyy-MM-dd"
    val content: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    // Propiedad para facilitar la ordenación y búsqueda
    val date: LocalDate get() = try { LocalDate.parse(dateStr) } catch(_: Exception) { LocalDate.now() }
}


// --- Modelo para Resultados de Búsqueda Mixtos ---

sealed class SearchItem {
    abstract val date: LocalDate
    abstract val adn: String

    data class Event(val festivo: Festivo) : SearchItem() {
        override val date: LocalDate = festivo.date
        override val adn: String = festivo.adn
    }

    data class Note(val dailyNote: DailyNote) : SearchItem() {
        override val date: LocalDate = dailyNote.date
        override val adn: String = "note_${dailyNote.dateStr}"
    }
}
