package com.example.calendario

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
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
            // DTEND en eventos de todo el día es exclusivo (el día siguiente)
            sb.append("DTEND;VALUE=DATE:${event.date.plusDays(1).format(dateFormatter)}\n")
        } else {
            val startTime = event.startTime ?: java.time.LocalTime.MIDNIGHT
            val endTime = event.endTime ?: startTime.plusHours(1)
            
            val startDateTime = event.date.atTime(startTime)
            val endDateTime = event.date.atTime(endTime)
            
            sb.append("DTSTART:${startDateTime.format(dateTimeFormatter)}\n")
            sb.append("DTEND:${endDateTime.format(dateTimeFormatter)}\n")
        }

        // Regla de repetición (si existe)
        if (!event.rrule.isNullOrBlank()) {
            // El campo rrule suele venir ya en formato "FREQ=..."
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
}
