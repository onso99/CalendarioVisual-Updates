package com.example.calendario

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
    val includeEvents: Boolean = true,
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

        // --- FUENTES CONDENSADAS (Estrechas) ---
        val condensedTypeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        val condensedBoldTypeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)

        // --- PINCELES (PAINTS) ---
        val titlePaint = Paint().apply {
            color = if (options.isInkSaver) Color.BLACK else 0xFF2E7D32.toInt()
            textSize = 20f
            typeface = condensedBoldTypeface
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
            typeface = condensedBoldTypeface
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
            typeface = condensedBoldTypeface
            isAntiAlias = true
        }

        val sundayNumberPaint = Paint().apply {
            color = if (options.isInkSaver) Color.BLACK else 0xFFD32F2F.toInt()
            textSize = 11f
            typeface = condensedBoldTypeface
            isAntiAlias = true
        }

        val eventTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
            typeface = condensedTypeface
            isAntiAlias = true
        }

        val holidayBgPaint = Paint().apply {
            color = if (options.isInkSaver) 0xFFF5F5F5.toInt() else 0xFFFFEBEE.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // --- 1. ENCABEZADO ---
        // --- 1. ENCABEZADO ---
        val margin = 18f
        var currentY = margin + 12f

        canvas.drawText(monthYearTitle, margin, currentY, titlePaint)
        
        val appSignaturePaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("CalendarioVisual — PDF", pageWidth - margin, currentY, appSignaturePaint)

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
        
        // Celdas cuadradas en vertical (Mixto) y más anchas/altas en apaisado (Mensual)
        val availableGridHeight = if (options.isMixedLayout) 400f else if (options.includeNotes) 420f else 490f
        val rowHeight = availableGridHeight / totalWeeks

        val overflowPaint = Paint(eventTextPaint).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

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
                    val dayEvents = if (options.includeEvents) (eventsByDate[date] ?: emptyList()) else emptyList()
                    val isSunday = col == 6
                    val hasHoliday = options.includeHolidays && dayEvents.any { it.isFromHolidaySource }

                    if (isSunday || hasHoliday) {
                        canvas.drawRect(cellRect, holidayBgPaint)
                    }
                    canvas.drawRect(cellRect, gridLinePaint)

                    val numPaint = if (isSunday || hasHoliday) sundayNumberPaint else dayNumberPaint
                    canvas.drawText(dayCounter.toString(), cellX + 3f, rowY + 11f, numPaint)

                    var eventY = rowY + 20f
                    val maxEventsInCell = 7
                    var printedCount = 0

                    for (eventIndex in dayEvents.indices) {
                        val event = dayEvents[eventIndex]
                        if (printedCount >= maxEventsInCell && dayEvents.size > maxEventsInCell) {
                            val remaining = dayEvents.size - printedCount
                            canvas.drawText("+$remaining", cellX + (colWidth / 2f), eventY, overflowPaint)
                            break
                        }

                        val timeStr = if (options.showEventTimes && !event.isAllDay && event.startTime != null) {
                            "${event.startTime.format(AppFormats.TimeShort)} "
                        } else ""

                        val rawTitle = timeStr + event.title
                        val maxChars = if (options.isMixedLayout) 20 else 29
                        val titleStr = if (rawTitle.length > maxChars) rawTitle.take(maxChars - 3) + "..." else rawTitle

                        canvas.drawText("• $titleStr", cellX + 3f, eventY, eventTextPaint)
                        eventY += 8.5f
                        printedCount++
                    }

                    dayCounter++
                }
            }
        }

        currentY += availableGridHeight + 14f

        // --- 4. FORMATO MIXTO (LISTA VERTICAL Y NOTAS EN 3 COLUMNAS CONTINUAS EN A4 PORTRAIT) ---
        if (options.isMixedLayout) {
            val sectionTitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // Construir lista unificada de elementos (par de booleano esTitulo y texto)
            val displayItems = mutableListOf<Pair<Boolean, String>>()

            if (options.includeEvents) {
                val monthEvents = (1..daysInMonth).flatMap { day ->
                    val date = options.targetMonth.atDay(day)
                    (eventsByDate[date] ?: emptyList()).map { date to it }
                }
                if (monthEvents.isNotEmpty()) {
                    displayItems.add(true to "AGENDA DEL MES:")
                    for ((date, event) in monthEvents) {
                        val timeStr = if (options.showEventTimes && !event.isAllDay && event.startTime != null) {
                            "${event.startTime.format(AppFormats.TimeShort)} "
                        } else ""
                        val rawLine = "${date.format(AppFormats.DateDayMonth)}: $timeStr${event.title}"
                        val lineStr = if (rawLine.length > 46) rawLine.take(43) + "..." else rawLine
                        displayItems.add(false to "• $lineStr")
                    }
                }
            }

            val maxRowsPerCol = 26 // Ampliado a 26 filas por columna (hasta 78 elementos)

            if (options.includeNotes) {
                val monthNotes = notesByDate.filter { (date, note) ->
                    YearMonth.from(date) == options.targetMonth && note.content.isNotBlank() && !note.isDeleted
                }
                if (monthNotes.isNotEmpty()) {
                    // Si ya había eventos en la lista, insertamos un espacio vertical de separación
                    if (displayItems.isNotEmpty()) {
                        displayItems.add(false to "")
                    }
                    // Evitar que el título quede huérfano al final de una columna
                    if (displayItems.size % maxRowsPerCol == maxRowsPerCol - 1) {
                        displayItems.add(false to "") // Salto preventivo a la siguiente columna
                    }
                    displayItems.add(true to "NOTAS DEL MES:")
                    for ((date, note) in monthNotes) {
                        val rawLine = "${date.dayOfMonth}/${options.targetMonth.monthValue}: ${note.content}"
                        val lineStr = if (rawLine.length > 46) rawLine.take(43) + "..." else rawLine
                        displayItems.add(false to "• $lineStr")
                    }
                }
            }

            val colWidth3 = gridWidth / 3f
            val col1X = margin
            val col2X = margin + colWidth3
            val col3X = margin + (colWidth3 * 2f)

            val startListY = currentY + 4f

            for (i in displayItems.indices) {
                if (i >= 78) break // Hasta 78 elementos (3 columnas x 26 filas)
                val (isTitle, itemText) = displayItems[i]

                val colIndex = i / maxRowsPerCol // 0, 1 o 2
                val rowIndex = i % maxRowsPerCol // 0 a 25

                val currentX = when (colIndex) {
                    0 -> col1X
                    1 -> col2X
                    else -> col3X
                }
                val currentYPos = startListY + (rowIndex * 11f)
                val paintToUse = if (isTitle) sectionTitlePaint else eventTextPaint
                if (itemText.isNotBlank()) {
                    canvas.drawText(itemText, currentX, currentYPos, paintToUse)
                }
            }
        } else {
            // --- 5. SECCIÓN NOTAS DIARIAS EN VISTA MENSUAL (3 Columnas en Apaisado) ---
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
                    canvas.drawText("NOTAS DEL MES:", margin, currentY, notesTitlePaint)
                    currentY += 12f

                    val colWidth3 = gridWidth / 3f
                    val col1X = margin
                    val col2X = margin + colWidth3
                    val col3X = margin + (colWidth3 * 2f)

                    val startNotesY = currentY + 4f
                    val notesList = monthNotes.toList()
                    val maxRowsPerCol = 5

                    for (i in notesList.indices) {
                        if (i >= 15) break // Hasta 15 notas (3 columnas x 5 filas)
                        val (date, note) = notesList[i]
                        val noteSnippet = "${date.dayOfMonth}: ${note.content.take(30)}"

                        val colIndex = i / maxRowsPerCol // 0, 1 o 2
                        val rowIndex = i % maxRowsPerCol // 0 a 4

                        val currentX = when (colIndex) {
                            0 -> col1X
                            1 -> col2X
                            else -> col3X
                        }
                        val currentYPos = startNotesY + (rowIndex * 11f)
                        canvas.drawText("• $noteSnippet", currentX, currentYPos, eventTextPaint)
                    }
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
