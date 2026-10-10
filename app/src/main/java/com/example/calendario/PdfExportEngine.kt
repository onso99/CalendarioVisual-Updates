package com.example.calendario

import android.content.Context
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
    val isMixedLayout: Boolean = false,
    val includeNotes: Boolean = true,
    val includeHolidays: Boolean = true,
    val showEventTimes: Boolean = true
)

object PdfExportEngine {

    fun generateMonthlyPdf(
        context: Context,
        options: PdfExportOptions,
        eventsByDate: Map<LocalDate, List<Festivo>>,
        notesByDate: Map<LocalDate, DailyNote>
    ): File? {
        // Din A4 Vertical (Portrait) para Mixto: 595 x 842 pt
        // Din A4 Apaisado (Landscape) para Mensual: 842 x 595 pt
        val pageWidth = if (options.isMixedLayout) 595 else 842
        val pageHeight = if (options.isMixedLayout) 842 else 595

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
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
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = if (options.isInkSaver) 0xFFE0E0E0.toInt() else 0xFFE8F5E9.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val headerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val gridLinePaint = Paint().apply {
            color = if (options.isInkSaver) Color.DKGRAY else Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val dayNumberPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val sundayNumberPaint = Paint().apply {
            color = if (options.isInkSaver) Color.BLACK else 0xFFD32F2F.toInt()
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val eventTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
            isAntiAlias = true
        }

        val holidayBgPaint = Paint().apply {
            color = if (options.isInkSaver) 0xFFF5F5F5.toInt() else 0xFFFFEBEE.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // --- 1. ENCABEZADO ---
        val margin = 24f
        var currentY = margin + 12f

        canvas.drawText(monthYearTitle, margin, currentY, titlePaint)
        
        val appSignaturePaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("${AppConstants.APP_SIGNATURE} — PDF", pageWidth - margin, currentY, appSignaturePaint)

        currentY += 10f

        // --- 2. CABECERA DE DÍAS DE LA SEMANA ---
        val gridWidth = pageWidth - (margin * 2)
        val colWidth = gridWidth / 7f
        val headerHeight = 18f

        val daysOfWeek = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
        val headerRect = RectF(margin, currentY, pageWidth - margin, currentY + headerHeight)
        canvas.drawRect(headerRect, headerPaint)
        canvas.drawRect(headerRect, gridLinePaint)

        for (i in 0..6) {
            val cx = margin + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(daysOfWeek[i], cx, currentY + 12f, headerTextPaint)
        }

        currentY += headerHeight

        // --- 3. CUADRÍCULA MENSUAL ---
        val firstDayOfMonth = options.targetMonth.atDay(1)
        val daysInMonth = options.targetMonth.lengthOfMonth()
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
        
        val totalWeeks = Math.ceil((dayOfWeekOffset + daysInMonth) / 7.0).toInt()
        
        // Si es mixto, la rejilla ocupa la mitad superior (aprox 260pt); si es mensual, ocupa la mayor parte
        val availableGridHeight = if (options.isMixedLayout) 260f else if (options.includeNotes) 360f else 460f
        val rowHeight = availableGridHeight / totalWeeks

        var dayCounter = 1
        for (week in 0 until totalWeeks) {
            val rowY = currentY + (week * rowHeight)
            for (col in 0..6) {
                val cellX = margin + (col * colWidth)
                val cellRect = RectF(cellX, rowY, cellX + colWidth, rowY + rowHeight)

                if ((week == 0 && col < dayOfWeekOffset) || dayCounter > daysInMonth) {
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

                    val numPaint = if (isSunday || hasHoliday) sundayNumberPaint else dayNumberPaint
                    canvas.drawText(dayCounter.toString(), cellX + 3f, rowY + 11f, numPaint)

                    var eventY = rowY + 20f
                    var printedCount = 0
                    for (event in dayEvents) {
                        if (printedCount >= (if (options.isMixedLayout) 2 else 3)) {
                            canvas.drawText("...", cellX + 3f, eventY, eventTextPaint)
                            break
                        }
                        val timeStr = if (options.showEventTimes && !event.isAllDay && event.startTime != null) {
                            "${event.startTime.format(AppFormats.TimeShort)} "
                        } else ""

                        val titleStr = (timeStr + event.title).take(12)
                        canvas.drawText("• $titleStr", cellX + 3f, eventY, eventTextPaint)
                        eventY += 8f
                        printedCount++
                    }

                    dayCounter++
                }
            }
        }

        currentY += availableGridHeight + 10f

        // --- 4. FORMATO MIXTO (LISTA VERTICAL DETALLADA) ---
        if (options.isMixedLayout) {
            val mixedTitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("📋 AGENDA DETALLADA:", margin, currentY, mixedTitlePaint)
            currentY += 12f

            val monthEvents = (1..daysInMonth).flatMap { day ->
                val date = options.targetMonth.atDay(day)
                (eventsByDate[date] ?: emptyList()).map { date to it }
            }

            var listY = currentY + 10f
            var listCount = 0
            for ((date, event) in monthEvents) {
                if (listCount >= 14) break
                val timeStr = if (options.showEventTimes && !event.isAllDay && event.startTime != null) {
                    "${event.startTime.format(AppFormats.TimeShort)} "
                } else ""
                val lineStr = "${date.format(AppFormats.DateDayMonth)}: $timeStr${event.title}"
                canvas.drawText("• $lineStr", margin, listY, eventTextPaint)
                listY += 12f
                listCount++
            }
            currentY += 40f
        }

        // --- 5. SECCIÓN NOTAS DIARIAS ---
        if (options.includeNotes) {
            val monthNotes = notesByDate.filter { (date, note) ->
                YearMonth.from(date) == options.targetMonth && note.content.isNotBlank() && !note.isDeleted
            }

            if (monthNotes.isNotEmpty()) {
                val notesTitlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("📌 NOTAS DEL MES:", margin, currentY, notesTitlePaint)
                currentY += 12f

                var noteY = currentY + 10f
                var count = 0
                for ((date, note) in monthNotes) {
                    if (count >= 5) break
                    val noteSnippet = "${date.dayOfMonth}: ${note.content.take(50)}"
                    canvas.drawText("• $noteSnippet", margin, noteY, eventTextPaint)
                    noteY += 12f
                    count++
                }
            }
        }

        pdfDocument.finishPage(page)

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
