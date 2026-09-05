package com.example.calendario

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CvoHelper {

    /**
     * Empaqueta eventos y notas seleccionados en un archivo .cvo y abre el selector de compartir.
     */
    fun shareAgendaPackage(
        context: Context,
        events: Collection<Festivo>,
        notes: Collection<DailyNote>
    ) {
        if (events.isEmpty() && notes.isEmpty()) return

        try {
            val root = JSONObject()
            root.put("tipo", "CVO_AGENDA")
            root.put("version", 1)
            root.put("fecha_creacion", System.currentTimeMillis())

            // 1. Empaquetar Eventos
            val eventsArray = JSONArray()
            events.forEach { event ->
                eventsArray.put(JSONObject().apply {
                    put("titulo", event.title)
                    put("fecha", event.date.toString())
                    put("es_todo_el_dia", event.isAllDay)
                    put("es_periodo_largo", event.isLongPeriod)
                    put("rrule", event.rrule)
                    // No incluimos colores ni alarmas por privacidad/soberanía del receptor
                })
            }
            root.put("eventos", eventsArray)

            // 2. Empaquetar Notas
            val notesArray = JSONArray()
            notes.forEach { note ->
                notesArray.put(JSONObject().apply {
                    put("fecha", note.dateStr)
                    put("contenido", note.content)
                })
            }
            root.put("notas", notesArray)

            // 3. Generar archivo temporal
            val fileName = "AgendaVisual_${System.currentTimeMillis()}.cvo"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(root.toString(4).toByteArray()) }

            // 4. Compartir
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Agenda Visual Compartida")
                putExtra(Intent.EXTRA_TEXT, "Te comparto una selección de mi Agenda Visual (${events.size} eventos, ${notes.size} notas).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Exportar Agenda"))

        } catch (e: Exception) {
            Log.e("CvoHelper", "Error generando paquete CVO: ${e.message}")
            Toast.makeText(context, "Error al generar el archivo de agenda", Toast.LENGTH_SHORT).show()
        }
    }
}
