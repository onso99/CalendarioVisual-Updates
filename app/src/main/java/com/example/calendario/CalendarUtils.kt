package com.example.calendario

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

fun saveEvent(
    context: Context,
    title: String,
    calendarId: Long?,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    isAllDay: Boolean,
    repetitionRule: RepetitionRule
) {
    if (calendarId == null) {
        Toast.makeText(context, "Error: No se ha seleccionado un calendario.", Toast.LENGTH_LONG).show()
        return
    }
    if (title.isBlank()) {
        Toast.makeText(context, "El título no puede estar vacío.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val startMillis: Long
        val endMillis: Long

        if (isAllDay) {
            // For all-day events, time should be midnight in UTC
            startMillis = startDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            // The end date for an all-day event is exclusive, so add one day.
            endMillis = endDate.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            startMillis = startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            endMillis = endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val rrule = when (repetitionRule) {
            RepetitionRule.DAILY -> "FREQ=DAILY"
            RepetitionRule.WEEKLY -> "FREQ=WEEKLY"
            RepetitionRule.MONTHLY -> "FREQ=MONTHLY"
            RepetitionRule.YEARLY -> "FREQ=YEARLY"
            else -> null
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            if (isAllDay) {
                // For all-day events, DURATION is often preferred over DTEND
                put(CalendarContract.Events.DURATION, "P${java.time.Duration.between(startDate.toLocalDate().atStartOfDay(), endDate.toLocalDate().atStartOfDay()).toDays() + 1}D")
            } else {
                put(CalendarContract.Events.DTEND, endMillis)
            }
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            rrule?.let { put(CalendarContract.Events.RRULE, it) }
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

        if (uri != null) {
            Toast.makeText(context, "Evento guardado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al guardar el evento", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Error: Permiso denegado para escribir en el calendario.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error inesperado al guardar el evento: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
