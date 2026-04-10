package com.example.calendario

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object IcsHelper {

    /**
     * Genera un archivo .ics para el evento dado y abre el selector de compartir.
     */
    fun shareEvent(context: Context, event: Festivo) {
        try {
            val icsContent = generateIcsContent(event)
            val file = File(context.cacheDir, "evento.ics")
            
            FileOutputStream(file).use { 
                it.write(icsContent.toByteArray()) 
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Texto descriptivo para acompañar al archivo
            val dateText = event.date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.getDefault()))
            val shareMessage = "📅 Evento: ${event.title}\n🗓️ $dateText"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareMessage)
                putExtra(Intent.EXTRA_SUBJECT, event.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_event)))

        } catch (e: Exception) {
            Log.e("IcsHelper", "Error compartiendo evento: ${e.message}")
        }
    }

    /**
     * Lee un archivo .ics de una URI y lo convierte en un objeto Festivo provisional.
     */
    fun parseIcs(context: Context, uri: Uri): Festivo? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var title = ""
            var description: String? = null
            var startDate: LocalDate? = null
            var startTime: LocalTime? = null
            var endTime: LocalTime? = null
            var isAllDay = false
            var rrule: String? = null

            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

            reader.forEachLine { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size < 2) return@forEachLine
                
                val key = parts[0]
                val value = unescapeIcs(parts[1])

                when {
                    key.startsWith("SUMMARY") -> title = value
                    key.startsWith("DESCRIPTION") -> description = value
                    key.startsWith("RRULE") -> rrule = value
                    key.startsWith("DTSTART") -> {
                        if (key.contains("VALUE=DATE")) {
                            isAllDay = true
                            startDate = LocalDate.parse(value, dateFormatter)
                        } else {
                            val dt = LocalDateTime.parse(value.take(15), dateTimeFormatter)
                            startDate = dt.toLocalDate()
                            startTime = dt.toLocalTime()
                        }
                    }
                    key.startsWith("DTEND") -> {
                        if (!key.contains("VALUE=DATE")) {
                            val dt = LocalDateTime.parse(value.take(15), dateTimeFormatter)
                            endTime = dt.toLocalTime()
                        }
                    }
                }
            }
            
            return if (startDate != null && title.isNotBlank()) {
                Festivo(
                    id = 0L, // ID 0 para indicar que es un evento nuevo
                    title = title,
                    description = description,
                    date = startDate!!,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    calendarId = 0L,
                    isFromHolidaySource = false,
                    rrule = rrule
                )
            } else null

        } catch (e: Exception) {
            Log.e("IcsHelper", "Error parseando ICS: ${e.message}")
            return null
        }
    }

    private fun generateIcsContent(event: Festivo): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\n")
        sb.append("VERSION:2.0\n")
        sb.append("PRODID:-//Calendario//ES\n")
        sb.append("CALSCALE:GREGORIAN\n")
        sb.append("BEGIN:VEVENT\n")
        
        // Título y Descripción
        sb.append("SUMMARY:${escapeIcs(event.title)}\n")
        if (!event.description.isNullOrBlank()) {
            sb.append("DESCRIPTION:${escapeIcs(event.description)}\n")
        }

        // Fechas y Horas
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

        if (event.isAllDay) {
            sb.append("DTSTART;VALUE=DATE:${event.date.format(dateFormatter)}\n")
            sb.append("DTEND;VALUE=DATE:${event.date.plusDays(1).format(dateFormatter)}\n")
        } else {
            val startTime = event.startTime ?: java.time.LocalTime.MIDNIGHT
            val endTime = event.endTime ?: startTime.plusHours(1)
            
            val startDateTime = event.date.atTime(startTime)
            val endDateTime = event.date.atTime(endTime)
            
            sb.append("DTSTART:${startDateTime.format(dateTimeFormatter)}\n")
            sb.append("DTEND:${endDateTime.format(dateTimeFormatter)}\n")
        }

        if (!event.rrule.isNullOrBlank()) {
            sb.append("RRULE:${event.rrule}\n")
        }

        sb.append("END:VEVENT\n")
        sb.append("END:VCALENDAR")
        
        return sb.toString()
    }

    private fun escapeIcs(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    private fun unescapeIcs(text: String): String {
        return text.replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
