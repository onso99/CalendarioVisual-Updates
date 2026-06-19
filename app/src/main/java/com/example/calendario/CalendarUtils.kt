package com.example.calendario

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

fun createEvent(
    context: Context,
    title: String,
    calendarId: Long?,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    isAllDay: Boolean,
    repetitionRule: RepetitionRule,
    repeatUntil: LocalDate? = null,
    customColor: Int? = null
): Long? {
    if (calendarId == null) {
        Toast.makeText(context, R.string.no_calendar_selected_error, Toast.LENGTH_LONG).show()
        return null
    }
    if (title.isBlank()) {
        Toast.makeText(context, R.string.title_empty_error, Toast.LENGTH_SHORT).show()
        return null
    }

    return try {
        val operations = ArrayList<ContentProviderOperation>()
        val values = createEventValues(startDate, endDate, isAllDay, title, calendarId, repetitionRule, repeatUntil, customColor)
        
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
            val eventId = ContentUris.parseId(results[0].uri!!)
            if (customColor != null) {
                savePeriodColor(context, eventId, customColor)
            }
            Toast.makeText(context, R.string.event_saved_successfully, Toast.LENGTH_SHORT).show()
            eventId
        } else {
            Toast.makeText(context, R.string.error_saving_event, Toast.LENGTH_LONG).show()
            null
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.permission_denied_calendar, Toast.LENGTH_LONG).show()
        null
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.unexpected_error_create, e.message), Toast.LENGTH_LONG).show()
        null
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
    repetitionRule: RepetitionRule,
    repeatUntil: LocalDate? = null,
    customColor: Int? = null
): Long? {
     if (calendarId == null) {
        Toast.makeText(context, R.string.no_calendar_selected_error, Toast.LENGTH_LONG).show()
        return null
    }
    if (title.isBlank()) {
        Toast.makeText(context, R.string.title_empty_error, Toast.LENGTH_SHORT).show()
        return null
    }

    return try {
        val operations = ArrayList<ContentProviderOperation>()
        val values = createEventValues(startDate, endDate, isAllDay, title, calendarId, repetitionRule, repeatUntil, customColor)
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
        
        if (customColor != null) {
            savePeriodColor(context, eventId, customColor)
        }

        // Forzar actualización del widget para asegurar sincronización en dispositivos como Xiaomi
        CalendarAppWidgetProvider.triggerWidgetUpdate(context)
        
        Toast.makeText(context, R.string.event_updated_successfully, Toast.LENGTH_SHORT).show()
        eventId

    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.permission_denied_calendar, Toast.LENGTH_LONG).show()
        null
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.unexpected_error_update, e.message), Toast.LENGTH_LONG).show()
        null
    }
}

fun updateSingleEventInSeries(
    context: Context,
    originalEvent: Festivo,
    title: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    isAllDay: Boolean
): Long? {
    return try {
        val originalInstanceStartTime = if (originalEvent.isAllDay) {
            originalEvent.date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } else {
            originalEvent.date.atTime(originalEvent.startTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val values = createEventValues(startDate, endDate, isAllDay, title, originalEvent.calendarId, RepetitionRule.NONE).apply {
            put(CalendarContract.Events.ORIGINAL_ID, originalEvent.id)
            put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, originalInstanceStartTime)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        
        if (uri != null) {
            // Forzar actualización del widget para asegurar sincronización en dispositivos como Xiaomi
            CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            
            Toast.makeText(context, R.string.event_updated_successfully, Toast.LENGTH_SHORT).show()
            ContentUris.parseId(uri)
        } else {
            Toast.makeText(context, R.string.error_saving_event, Toast.LENGTH_LONG).show()
            null
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.permission_denied_calendar, Toast.LENGTH_LONG).show()
        null
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.unexpected_error_update, e.message), Toast.LENGTH_LONG).show()
        null
    }
}

fun deleteEvent(context: Context, eventId: Long, eventTitle: String, eventDate: LocalDate) {
    try {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(deleteUri, null, null)

        if (rows > 0) {
            // Forzar actualización del widget tras eliminar un evento
            CalendarAppWidgetProvider.triggerWidgetUpdate(context)
            
            // Cancelamos la alarma asociada si existe
            AlarmUtils.cancelAlarm(context, eventId)
            AlarmUtils.saveAlarmSetting(context, eventId, null)
            savePeriodColor(context, eventId, null)

            val dateStr = eventDate.format(DateTimeFormatter.ofPattern("d/M/yy"))
            val displayTitle = if (eventTitle.length > 60) eventTitle.take(57) + "..." else eventTitle
            val message = context.getString(R.string.event_deleted_message, dateStr, displayTitle)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.error_deleting_event, Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.permission_denied_calendar, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.unexpected_error_delete, e.message), Toast.LENGTH_LONG).show()
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
            // Forzar actualización del widget tras cancelar una instancia
            CalendarAppWidgetProvider.triggerWidgetUpdate(context)

            // Cancelamos la alarma asociada si existe
            AlarmUtils.cancelAlarm(context, eventToCancel.id)
            AlarmUtils.saveAlarmSetting(context, eventToCancel.id, null)

            val dateStr = eventToCancel.date.format(DateTimeFormatter.ofPattern("d/M/yy"))
            val displayTitle = if (eventToCancel.title.length > 60) eventToCancel.title.take(57) + "..." else eventToCancel.title
            val message = context.getString(R.string.event_deleted_message, dateStr, displayTitle)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.error_canceling_event_instance, Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.permission_denied_calendar, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.unexpected_error_cancel, e.message), Toast.LENGTH_LONG).show()
    }
}

private fun createEventValues(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    isAllDay: Boolean,
    title: String,
    calendarId: Long,
    repetitionRule: RepetitionRule,
    repeatUntil: LocalDate? = null,
    customColor: Int? = null
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
        if (customColor != null) {
            put(CalendarContract.Events.EVENT_COLOR, customColor)
        }

        if (repetitionRule == RepetitionRule.NONE) {
            val endMillis = if (isAllDay) {
                // IMPORTANTE: Para eventos Todo el día, DTEND debe ser el día siguiente a las 00:00
                endDate.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli()
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
            
            val finalRrule = if (repeatUntil != null) {
                val untilStr = repeatUntil.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'235959'Z'"))
                "${repetitionRule.rrule};UNTIL=$untilStr"
            } else {
                repetitionRule.rrule
            }
            put(CalendarContract.Events.RRULE, finalRrule)
            putNull(CalendarContract.Events.DTEND)
        }
    }
}

fun findBestCalendarCandidate(calendars: List<CalendarInfo>): CalendarInfo? {
    if (calendars.isEmpty()) return null

    return calendars
        .filter { it.canModify } // Only consider editable calendars
        .maxByOrNull { calendar ->
            var score = 0
            if (calendar.accountName.contains("com.google", ignoreCase = true)) score += 10
            if (calendar.isPrimary) score += 5
            score
        }
}