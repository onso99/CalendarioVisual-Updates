package com.example.calendario

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.widget.Toast
import java.util.Calendar
import java.util.TimeZone

fun saveEvent(context: Context, title: String, isAllDay: Boolean, calendarId: Long, eventDate: java.time.LocalDate) {
    try {
        val cal = Calendar.getInstance().apply {
            set(eventDate.year, eventDate.monthValue - 1, eventDate.dayOfMonth)
        }

        val startMillis: Long = cal.timeInMillis
        val endMillis: Long = if (isAllDay) startMillis else startMillis + 60 * 60 * 1000 // 1 hora

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
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
