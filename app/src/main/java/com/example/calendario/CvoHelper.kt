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
     * Genera el contenido JSON de un paquete de agenda (v3.1.34)
     */
    fun generateAgendaJson(events: Collection<Festivo>, notes: Collection<DailyNote>): String {
        val root = JSONObject()
        root.put("tipo", "CVO_AGENDA")
        root.put("version", 1)
        root.put("fecha_creacion", System.currentTimeMillis())

        val eventsArray = JSONArray()
        events.forEach { event ->
            eventsArray.put(JSONObject().apply {
                put("titulo", event.title)
                put("fecha", event.date.toString())
                put("es_todo_el_dia", event.isAllDay)
                put("es_periodo_largo", event.isLongPeriod)
                put("rrule", event.rrule)
            })
        }
        root.put("eventos", eventsArray)

        val notesArray = JSONArray()
        notes.forEach { note ->
            notesArray.put(JSONObject().apply {
                put("fecha", note.dateStr)
                put("contenido", note.content)
            })
        }
        root.put("notas", notesArray)
        
        return root.toString(4)
    }

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
            val jsonContent = generateAgendaJson(events, notes)
            val fileName = "AgendaVisual.cvo"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(jsonContent.toByteArray()) }

            shareFile(context, file, "Exportar Agenda", "Te comparto una selección de mi Agenda Visual (${events.size} eventos, ${notes.size} notas).")

        } catch (e: Exception) {
            Log.e("CvoHelper", "Error generando paquete CVO: ${e.message}")
            Toast.makeText(context, "Error al generar el archivo de agenda", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Empaqueta festivos locales seleccionados en un archivo .cvo y abre el selector de compartir.
     */
    fun shareHolidaysPackage(
        context: Context,
        adjustments: List<HolidayAdjustment>
    ) {
        if (adjustments.isEmpty()) return

        try {
            val currentYear = java.time.LocalDate.now().year
            val exportable = adjustments.filter { it.date.year >= currentYear }
            
            val root = JSONObject()
            root.put("tipo", "CVO_HOLIDAYS")
            root.put("version", 1)
            root.put("fecha_creacion", System.currentTimeMillis())
            
            val dataArray = JSONArray()
            exportable.forEach { adj ->
                dataArray.put(JSONObject().apply {
                    put("fecha", adj.date.toString())
                    put("titulo", adj.title)
                    put("tipo", adj.type.name)
                    adj.originalEventId?.let { put("originalEventId", it) }
                })
            }
            root.put("ajustes", dataArray)

            val fileName = "FestivosLocales.cvo"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(root.toString(4).toByteArray()) }

            shareFile(context, file, "Exportar Festivos", "Te comparto mis festivos locales (${exportable.size} ajustes).")

        } catch (e: Exception) {
            Log.e("CvoHelper", "Error generando paquete CVO: ${e.message}")
            Toast.makeText(context, "Error al generar el archivo de festivos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(context: Context, file: File, chooserTitle: String, text: String) {
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            // Usamos application/json para que Drive respete la extensión del archivo (v3.1.34)
            type = "application/json" 
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(chooserTitle, contentUri)
        }

        val chooser = Intent.createChooser(shareIntent, chooserTitle)
        context.startActivity(chooser)
    }
}
