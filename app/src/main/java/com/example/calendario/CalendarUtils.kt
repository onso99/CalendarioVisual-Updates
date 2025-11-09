package com.example.calendario

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

fun createEvent(
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
        val values = createEventValues(startDate, endDate, isAllDay, title, calendarId, repetitionRule)
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

        if (uri != null) {
            Toast.makeText(context, "Evento guardado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al guardar el evento", Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, "Error: Permiso denegado para escribir en el calendario.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error inesperado al crear el evento: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun updateEvent(
    context: Context,
    eventId: Long,
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
        val values = createEventValues(startDate, endDate, isAllDay, title, calendarId, repetitionRule)
        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.update(updateUri, values, null, null)

        if (rows > 0) {
            Toast.makeText(context, "Evento actualizado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al actualizar el evento", Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, "Error: Permiso denegado para escribir en el calendario.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error inesperado al actualizar el evento: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun deleteEvent(context: Context, eventId: Long) {
    try {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(deleteUri, null, null)

        if (rows > 0) {
            Toast.makeText(context, "Evento eliminado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al eliminar el evento", Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, "Error: Permiso denegado para escribir en el calendario.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error inesperado al eliminar el evento: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun createEventValues(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    isAllDay: Boolean,
    title: String,
    calendarId: Long,
    repetitionRule: RepetitionRule
): ContentValues {
    val startMillis: Long
    val endMillis: Long
    val timezone = if (isAllDay) TimeZone.getTimeZone("UTC").id else TimeZone.getDefault().id

    if (isAllDay) {
        startMillis = startDate.toLocalDate().atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli()
        endMillis = endDate.toLocalDate().atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli()
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

    return ContentValues().apply {
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.DTEND, endMillis)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
        put(CalendarContract.Events.EVENT_TIMEZONE, timezone)
        rrule?.let { put(CalendarContract.Events.RRULE, it) } ?: remove(CalendarContract.Events.RRULE)
    }
}
