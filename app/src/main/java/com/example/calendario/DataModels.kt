package com.example.calendario

import org.json.JSONObject
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
    val repeatIndex: Int? = null, // Índice ordinal para eventos repetidos (v3.1.64)
    val hasIncident: Boolean = false, // Marca para eventos con datos extra/complejos (v3.2.06)
    
    // --- Campos de Optimización Punto 1 ---
    val cleanTitle: String = title.unaccent().trim().lowercase(),
    val fuzzyAdn: String = "${date}_${cleanTitle}",

    // --- Campos para Sincronización Segura ---
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isGhost: Boolean = false
) {
    /**
     * Serializa el objeto a JSON (Fase 4 v3.1.34)
     * @param forExport Si es true, omite datos técnicos como IDs reales y reglas de repetición
     *                  para tratarlo como un evento simple e independiente.
     */
    fun toJson(forExport: Boolean = false): JSONObject = JSONObject().apply {
        put("id", if (forExport) 0L else id)
        put("title", title)
        put("description", description)
        put("dateStr", date.toString())
        put("startTimeStr", startTime?.toString())
        put("endTimeStr", endTime?.toString())
        put("isAllDay", isAllDay)
        put("calendarId", if (forExport) 0L else calendarId)
        put("isFromHolidaySource", if (forExport) false else isFromHolidaySource)
        put("rrule", if (forExport) null else rrule)
        put("age", age)
        put("isBirthday", if (forExport) false else isBirthday)
        put("isLongPeriod", isLongPeriod)
        put("lane", if (forExport) null else lane)
        put("totalDays", totalDays)
        put("currentDay", currentDay)
        put("customColor", customColor)
        put("lastModified", lastModified)
        put("isDeleted", isDeleted)
        put("isGhost", if (forExport) false else isGhost)
        put("hasIncident", hasIncident)
        put("repeatIndex", repeatIndex)
        put("adn", if (forExport) "" else adn)
    }

    companion object {
        fun generateAdn(date: LocalDate, title: String, startTime: LocalTime?): String {
            // Usamos una versión local para evitar depender de la inicialización si se llama desde fuera
            val cleanTitle = title.unaccent().trim().lowercase()
            val timeStr = startTime?.let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) } ?: "null"
            return "${date}_${cleanTitle}_$timeStr"
        }

        /**
         * Crea un objeto Festivo desde un JSON con seguridad ante nulos (Fase 4 v3.1.34)
         */
        fun fromJson(obj: JSONObject): Festivo {
            val dateStr = obj.getString("dateStr")
            val startTimeStr = if (obj.isNull("startTimeStr")) null else obj.optString("startTimeStr")
            val endTimeStr = if (obj.isNull("endTimeStr")) null else obj.optString("endTimeStr")

            return Festivo(
                id = obj.optLong("id", 0L),
                title = obj.optString("title", ""),
                description = if (obj.isNull("description")) null else obj.optString("description"),
                date = LocalDate.parse(dateStr),
                startTime = startTimeStr?.let { 
                    try { LocalTime.parse(it) } catch(_:Exception) { 
                        // Fallback para formatos parciales (HH:mm)
                        try { LocalTime.parse(it.take(5)) } catch(_:Exception) { null }
                    }
                },
                endTime = endTimeStr?.let { 
                    try { LocalTime.parse(it) } catch(_:Exception) { 
                        try { LocalTime.parse(it.take(5)) } catch(_:Exception) { null }
                    }
                },
                isAllDay = obj.optBoolean("isAllDay", startTimeStr == null),
                calendarId = obj.optLong("calendarId", 0L),
                isFromHolidaySource = obj.optBoolean("isFromHolidaySource", false),
                rrule = if (obj.isNull("rrule")) null else obj.optString("rrule"),
                age = if (obj.has("age") && !obj.isNull("age")) obj.getInt("age") else null,
                isBirthday = obj.optBoolean("isBirthday", false),
                isLongPeriod = obj.optBoolean("isLongPeriod", false),
                lane = if (obj.has("lane") && !obj.isNull("lane")) obj.getInt("lane") else null,
                totalDays = obj.optInt("totalDays", 1),
                currentDay = obj.optInt("currentDay", 1),
                customColor = if (obj.has("customColor") && !obj.isNull("customColor")) obj.getInt("customColor") else null,
                fullStartMillis = if (obj.has("fullStartMillis")) obj.getLong("fullStartMillis") else null,
                fullEndMillis = if (obj.has("fullEndMillis")) obj.getLong("fullEndMillis") else null,
                repeatCount = if (obj.has("repeatCount")) obj.getInt("repeatCount") else null,
                hasIncident = obj.optBoolean("hasIncident", false),
                repeatIndex = if (obj.has("repeatIndex") && !obj.isNull("repeatIndex")) obj.getInt("repeatIndex") else null,
                lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                isDeleted = obj.optBoolean("isDeleted", false),
                isGhost = obj.optBoolean("isGhost", false),
                adn = obj.optString("adn", "")
            )
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
) {
    /**
     * Serializa el ajuste a JSON (Fase 4 v3.1.34)
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put("fecha", date.toString())
        put("titulo", title)
        put("tipo", type.name)
        originalEventId?.let { put("originalEventId", it) }
    }

    companion object {
        fun fromJson(obj: JSONObject): HolidayAdjustment {
            return HolidayAdjustment(
                date = LocalDate.parse(obj.getString("fecha")),
                title = obj.getString("titulo"),
                type = HolidayAdjustmentType.valueOf(obj.getString("tipo")),
                originalEventId = if (obj.has("originalEventId") && !obj.isNull("originalEventId")) obj.getLong("originalEventId") else null
            )
        }
    }
}

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
    val isDeleted: Boolean = false,
    val hasIncident: Boolean = false
) {
    // Propiedad para facilitar la ordenación y búsqueda
    val date: LocalDate get() = try { LocalDate.parse(dateStr) } catch(_: Exception) { LocalDate.now() }

    /**
     * Serializa la nota a JSON (Fase 4 v3.1.34)
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put("dateStr", dateStr)
        put("content", content)
        put("lastModified", lastModified)
        put("isDeleted", isDeleted)
    }

    companion object {
        fun fromJson(obj: JSONObject): DailyNote {
            return DailyNote(
                dateStr = obj.getString("dateStr"),
                content = obj.getString("content"),
                lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                isDeleted = obj.optBoolean("isDeleted", false)
            )
        }
    }
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
