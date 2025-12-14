package com.example.calendario

import android.content.ContentProviderOperation
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
        val operations = ArrayList<ContentProviderOperation>()
        val values = createEventValues(startDate, endDate, isAllDay, title, calendarId, repetitionRule)
        
        val eventInsertOperation = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(values)
        operations.add(eventInsertOperation.build())

        if (isAllDay) {
            operations.add(ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                .withValueBackReference(CalendarContract.Reminders.EVENT_ID, 0)
                .withValue(CalendarContract.Reminders.MINUTES, 15 * 60) // 9 AM the day before (15 hours)
                .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_DEFAULT)
                .build())
        }

        val results = context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)

        if (results.isNotEmpty() && results[0].uri != null) {
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
        val operations = ArrayList<ContentProviderOperation>()
        val values = createEventValues(startDate, endDate, isAllDay, title, calendarId, repetitionRule)
        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        operations.add(ContentProviderOperation.newUpdate(updateUri).withValues(values).build())

        // First, delete any existing reminders for the event
        val reminderSelection = "${CalendarContract.Reminders.EVENT_ID} = ?"
        val reminderArgs = arrayOf(eventId.toString())
        operations.add(ContentProviderOperation.newDelete(CalendarContract.Reminders.CONTENT_URI).withSelection(reminderSelection, reminderArgs).build())

        // If the event is now an all-day event, add our smart reminder
        if (isAllDay) {
            operations.add(ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                .withValue(CalendarContract.Reminders.EVENT_ID, eventId)
                .withValue(CalendarContract.Reminders.MINUTES, 15 * 60) // 9 AM the day before (15 hours)
                .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_DEFAULT)
                .build())
        }
        
        context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
        Toast.makeText(context, "Evento actualizado", Toast.LENGTH_SHORT).show()

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

fun cancelEventInstance(context: Context, eventToCancel: Festivo) {
    try {
        val instanceStartDateTime = if (eventToCancel.isAllDay) {
            eventToCancel.date.atStartOfDay()
        } else {
            LocalDateTime.of(eventToCancel.date, eventToCancel.startTime ?: LocalDateTime.now().toLocalTime())
        }

        val timezone = if (eventToCancel.isAllDay) "UTC" else TimeZone.getDefault().id
        val startMillis = if (eventToCancel.isAllDay) {
            instanceStartDateTime.toLocalDate().atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli()
        } else {
            instanceStartDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, eventToCancel.calendarId)
            put(CalendarContract.Events.ORIGINAL_ID, eventToCancel.id)
            put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, startMillis)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, startMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, timezone)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

        if (uri != null) {
            Toast.makeText(context, "Instancia de evento cancelada", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al cancelar la instancia del evento", Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, "Error: Permiso denegado para modificar el calendario.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error inesperado al cancelar el evento: ${e.message}", Toast.LENGTH_LONG).show()
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
    val timezone = if (isAllDay) TimeZone.getTimeZone("UTC").id else TimeZone.getDefault().id
    val startMillis = if (isAllDay) {
        startDate.toLocalDate().atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli()
    } else {
        startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    return ContentValues().apply {
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
        put(CalendarContract.Events.EVENT_TIMEZONE, timezone)

        if (repetitionRule == RepetitionRule.NONE) {
            val endMillis = if (isAllDay) {
                endDate.toLocalDate().atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli()
            } else {
                endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            put(CalendarContract.Events.DTEND, endMillis)
            putNull(CalendarContract.Events.RRULE)
            putNull(CalendarContract.Events.DURATION)
        } else {
            if (isAllDay) {
                val durationInDays = java.time.Duration.between(startDate.toLocalDate().atStartOfDay(), endDate.toLocalDate().atStartOfDay()).toDays()
                val finalDurationDays = if (durationInDays < 1) 1L else durationInDays
                put(CalendarContract.Events.DURATION, "P${finalDurationDays}D")
            } else {
                val durationInSeconds = java.time.Duration.between(startDate, endDate).seconds
                put(CalendarContract.Events.DURATION, "PT${durationInSeconds}S")
            }
            put(CalendarContract.Events.RRULE, repetitionRule.rrule)
            putNull(CalendarContract.Events.DTEND)
        }
    }
}
