package com.example.calendario

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class PdfExportOptions(
    val titleText: String = "",
    val targetMonth: YearMonth = YearMonth.now(),
    val isInkSaver: Boolean = false,
    val includeNotes: Boolean = true,
    val includeHolidays: Boolean = true,
    val showEventTimes: Boolean = true
)

object PdfExportEngine {

    // Tamaño Din A4 Apaisado (Landscape): 842 x 595 puntos (pt)
    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595

    fun generateMonthlyPdf(
        context: Context,
        options: PdfExportOptions,
        eventsByDate: Map<LocalDate, List<Festivo>>,
        notesByDate: Map<LocalDate, DailyNote>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val locale = Locale.getDefault()
        val monthYearTitle = options.titleText.ifBlank {
            val monthName = options.targetMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }
            "$monthName ${options.targetMonth.year}"
        }

        // --- PINCELES (PAINTS) ---
        val titlePaint = Paint().apply {
            color = if (options.isInkSaver) Color.BLACK else 0xFF2E7D32.toInt()
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = if (options.isInkSaver) Color.LTGRAY else 0xFFE8F5E9.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val headerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val gridLinePaint = Paint().apply {
            color = if (options.isInkSaver) Color.GRAY else Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val dayNumberPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val sundayNumberPaint = Paint().apply {
            color = if (options.isInkSaver) Color.DKGRAY else 0xFFD32F2F.toInt()
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val eventTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
            isAntiAlias = true
        }

        val holidayBgPaint = Paint().apply {
            color = if (options.isInkSaver) 0xFFEEEEEE.toInt() else 0xFFFFEBEE.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // --- 1. ENCABEZADO ---
        val margin = 30f
        var currentY = margin + 20f

        canvas.drawText(monthYearTitle, margin, currentY, titlePaint)
        
        val appSignaturePaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("${AppConstants.APP_SIGNATURE} — PDF", PAGE_WIDTH - margin, currentY, appSignaturePaint)

        currentY += 15f

        // --- 2. CABECERA DE DÍAS DE LA SEMANA ---
        val gridWidth = PAGE_WIDTH - (margin * 2)
        val colWidth = gridWidth / 7f
        val headerHeight = 22f

        val daysOfWeek = listOf("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO")
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRect(headerRect, headerPaint)
        canvas.drawRect(headerRect, gridLinePaint)

        for (i in 0..6) {
            val cx = margin + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(daysOfWeek[i], cx, currentY + 15f, headerTextPaint)
        }

        currentY += headerHeight

        // --- 3. CUADRÍCULA MENSUAL ---
        val firstDayOfMonth = options.targetMonth.atDay(1)
        val daysInMonth = options.targetMonth.lengthOfMonth()
        
        // Ajustar primer día a Lunes = 1
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
        
        val totalWeeks = Math.ceil((dayOfWeekOffset + daysInMonth) / 7.0).toInt()
        val availableGridHeight = if (options.includeNotes) 360f else 460f
        val rowHeight = availableGridHeight / totalWeeks

        var dayCounter = 1
        for (week in 0 until totalWeeks) {
            val rowY = currentY + (week * rowHeight)
            for (col in 0..6) {
                val cellX = margin + (col * colWidth)
                val cellRect = RectF(cellX, rowY, cellX + colWidth, rowY + rowHeight)

                if (week == 0 && col < dayOfWeekOffset || dayCounter > daysInMonth) {
                    // Celda fuera del mes
                    val outOfMonthPaint = Paint().apply {
                        color = 0xFFFAFAFA.toInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(cellRect, outOfMonthPaint)
                    canvas.drawRect(cellRect, gridLinePaint)
                } else {
                    val date = options.targetMonth.atDay(dayCounter)
                    val dayEvents = eventsByDate[date] ?: emptyList()
                    val isSunday = col == 6
                    val hasHoliday = options.includeHolidays && dayEvents.any { it.isFromHolidaySource }

                    if (isSunday || hasHoliday) {
                        canvas.drawRect(cellRect, holidayBgPaint)
                    }
                    canvas.drawRect(cellRect, gridLinePaint)

                    // Número del día
                    val numPaint = if (isSunday || hasHoliday) sundayNumberPaint else dayNumberPaint
                    canvas.drawText(dayCounter.toString(), cellX + 5f, rowY + 14f, numPaint)

                    // Eventos en la celda (máximo 3 líneas)
                    var eventY = rowY + 26f
                    var printedCount = 0
                    for (event in dayEvents) {
                        if (printedCount >= 3) {
                            canvas.drawText("...", cellX + 5f, eventY, eventTextPaint)
                            break
                        }
                        val timeStr = if (options.showEventTimes && !event.isAllDay && event.startTime != null) {
                            "${event.startTime.format(AppFormats.TimeShort)} "
                        } else ""

                        val titleStr = (timeStr + event.title).take(18)
                        canvas.drawText("• $titleStr", cellX + 5f, eventY, eventTextPaint)
                        eventY += 10f
                        printedCount++
                    }

                    dayCounter++
                }
            }
        }

        currentY += availableGridHeight + 15f

        // --- 4. SECCIÓN NOTAS DIARIAS ---
        if (options.includeNotes) {
            val monthNotes = notesByDate.filter { (date, note) ->
                YearMonth.from(date) == options.targetMonth && note.content.isNotBlank() && !note.isDeleted
            }

            if (monthNotes.isNotEmpty()) {
                val notesTitlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("📌 NOTAS DEL MES:", margin, currentY, notesTitlePaint)
                currentY += 12f

                var noteX = margin
                var noteY = currentY + 10f
                var count = 0
                for ((date, note) in monthNotes) {
                    if (count >= 4) break
                    val noteSnippet = "${date.dayOfMonth}: ${note.content.take(40)}"
                    canvas.drawText("• $noteSnippet", noteX, noteY, eventTextPaint)
                    noteX += (gridWidth / 2f)
                    if (count % 2 == 1) {
                        noteX = margin
                        noteY += 12f
                    }
                    count++
                }
            }
        }

        pdfDocument.finishPage(page)

        // Guardar archivo PDF temporal en caché
        val outputFile = File(context.cacheDir, "calendario_export_${options.targetMonth}.pdf")
        if (outputFile.exists()) outputFile.delete()

        return try {
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outputFile
        } catch (_: Exception) {
            pdfDocument.close()
            null
        }
    }
}
