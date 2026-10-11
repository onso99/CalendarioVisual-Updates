package com.example.calendario

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun PdfExportScreen(
    onBackPress: () -> Unit,
    eventsByDate: Map<LocalDate, List<Festivo>> = emptyMap(),
    dailyNotes: Map<String, DailyNote> = emptyMap()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val parsedNotes = remember(dailyNotes) { dailyNotes.mapKeys { LocalDate.parse(it.key) } }

    var targetMonth by remember { mutableStateOf(YearMonth.now()) }
    var customTitleText by remember { mutableStateOf<String?>(null) }
    
    var isColorMode by remember { mutableStateOf(true) }
    var isMixedLayout by remember { mutableStateOf(false) }

    var isZoomed by remember { mutableStateOf(false) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(targetMonth, isMixedLayout) {
        isZoomed = false
        panOffset = Offset.Zero
        containerSize = Size.Zero
    }

    var includeEvents by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }
    var includeHolidays by remember { mutableStateOf(true) }
    var showEventTimes by remember { mutableStateOf(true) }
    var showLongPeriods by remember { mutableStateOf(true) }

    val defaultMonthName = remember(targetMonth, locale) {
        val monthStr = targetMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }
        "$monthStr ${targetMonth.year}"
    }

    val displayTitle = customTitleText ?: defaultMonthName

    var pdfBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(targetMonth, displayTitle, isColorMode, isMixedLayout, includeNotes, includeHolidays, showEventTimes, eventsByDate, parsedNotes) {
        isGeneratingPreview = true
        withContext(Dispatchers.IO) {
            try {
                val options = PdfExportOptions(
                    titleText = displayTitle,
                    targetMonth = targetMonth,
                    isInkSaver = !isColorMode,
                    isMixedLayout = isMixedLayout,
                    includeNotes = includeNotes,
                    includeHolidays = includeHolidays,
                    showEventTimes = showEventTimes
                )
                val file = PdfExportEngine.generateMonthlyPdf(context, options, eventsByDate, parsedNotes)
                if (file != null && file.exists()) {
                    val parcelFileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val renderScale = 2f
                        val bitmap = android.graphics.Bitmap.createBitmap((page.width * renderScale).toInt(), (page.height * renderScale).toInt(), android.graphics.Bitmap.Config.ARGB_8888)
                        val matrix = android.graphics.Matrix().apply { setScale(renderScale, renderScale) }
                        page.render(bitmap, null, matrix, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()
                        parcelFileDescriptor.close()
                        pdfBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfExportScreen", "Error rendering preview", e)
            } finally {
                isGeneratingPreview = false
            }
        }
    }

    AppScreen(
        title = stringResource(id = R.string.pdf_print_title),
        onBackClick = onBackPress,
        actions = {
            IconButton(
                onClick = {
                    scope.launch {
                        val options = PdfExportOptions(
                            titleText = displayTitle,
                            targetMonth = targetMonth,
                            isInkSaver = !isColorMode,
                            isMixedLayout = isMixedLayout,
                            includeNotes = includeNotes,
                            includeHolidays = includeHolidays,
                            showEventTimes = showEventTimes
                        )
                        val file = PdfExportEngine.generateMonthlyPdf(context, options, eventsByDate, parsedNotes)
                        if (file != null && file.exists()) {
                            sharePdf(context, file)
                        } else {
                            context.showToast("Error al generar PDF para compartir")
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share_outlined),
                    contentDescription = "Compartir",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    scope.launch {
                        val options = PdfExportOptions(
                            titleText = displayTitle,
                            targetMonth = targetMonth,
                            isInkSaver = !isColorMode,
                            isMixedLayout = isMixedLayout,
                            includeNotes = includeNotes,
                            includeHolidays = includeHolidays,
                            showEventTimes = showEventTimes
                        )
                        val file = PdfExportEngine.generateMonthlyPdf(context, options, eventsByDate, parsedNotes)
                        if (file != null && file.exists()) {
                            savePdfToDownloads(context, file, displayTitle.replace(" ", "_"))
                        } else {
                            context.showToast("Error al generar PDF")
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_save_outlined),
                    contentDescription = "Guardar",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    scope.launch {
                        val options = PdfExportOptions(
                            titleText = displayTitle,
                            targetMonth = targetMonth,
                            isInkSaver = !isColorMode,
                            isMixedLayout = isMixedLayout,
                            includeNotes = includeNotes,
                            includeHolidays = includeHolidays,
                            showEventTimes = showEventTimes
                        )
                        val file = PdfExportEngine.generateMonthlyPdf(context, options, eventsByDate, parsedNotes)
                        if (file != null && file.exists()) {
                            printPdf(context, file, displayTitle)
                        } else {
                            context.showToast("Error al generar PDF para imprimir")
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_print_outlined),
                    contentDescription = "Imprimir",
                    tint = Color.White
                )
            }
        },
        scrollable = !isZoomed
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AppLayout.ScreenHorizontalPadding, end = AppLayout.ScreenHorizontalPadding, bottom = 24.dp)
        ) {
            // FOLIO A4 DE PREVISUALIZACIÓN DIRECTO SOBRE LA PANTALLA (Altura fija invariable a 160.dp)
            val previewHeight = 160.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(if (isMixedLayout) (595f / 842f) else (842f / 595f))
                        .graphicsLayer(
                            scaleX = if (isZoomed) 3f else 1f,
                            scaleY = if (isZoomed) 3f else 1f,
                            translationX = if (isZoomed) panOffset.x else 0f,
                            translationY = if (isZoomed) panOffset.y else 0f
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .onGloballyPositioned { coordinates ->
                            containerSize = androidx.compose.ui.geometry.Size(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    isZoomed = !isZoomed
                                    if (isZoomed) {
                                        val centerX = containerSize.width / 2f
                                        val centerY = containerSize.height / 2f
                                        val scale = 3f
                                        panOffset = Offset(
                                            (centerX - tapOffset.x) * (scale - 1f),
                                            (centerY - tapOffset.y) * (scale - 1f)
                                        )
                                    } else {
                                        panOffset = Offset.Zero
                                    }
                                }
                            )
                        }
                        .pointerInput(isZoomed) {
                            if (isZoomed) {
                                detectTransformGestures { _, pan, _, _ ->
                                    val maxPanX = containerSize.width * 1.5f
                                    val maxPanY = containerSize.height * 1.5f
                                    val newX = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX)
                                    val newY = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                    panOffset = Offset(newX, newY)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGeneratingPreview && pdfBitmap == null) {
                        CircularProgressIndicator(
                            color = CalendarioTheme.colors.cabecera,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (pdfBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = pdfBitmap!!.asImageBitmap(),
                            contentDescription = "Previsualización del PDF",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = displayTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Text(
                text = "* Se muestra un máximo de 7 eventos/día.",
                fontSize = 11.sp,
                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // SECCIÓN 1: INICIAL (SIN TÍTULO DE SECCIÓN)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Título en primer lugar
                OutlinedTextField(
                    value = displayTitle,
                    onValueChange = { customTitleText = it },
                    label = { Text("Título del PDF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalendarioTheme.colors.cabecera,
                        focusedLabelColor = CalendarioTheme.colors.cabecera
                    )
                )

                // Elección del mes debajo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        targetMonth = targetMonth.minusMonths(1)
                        customTitleText = null // Actualizar título al nuevo mes
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = null, tint = CalendarioTheme.colors.textSystem)
                    }
                    Text(
                        text = defaultMonthName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalendarioTheme.colors.textSystem
                    )
                    IconButton(onClick = {
                        targetMonth = targetMonth.plusMonths(1)
                        customTitleText = null // Actualizar título al nuevo mes
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null, tint = CalendarioTheme.colors.textSystem)
                    }
                }
            }

            // SECCIÓN 2: ESTILO DE DOCUMENTO
            SectionTitle(text = "Estilo de documento")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                OptionRow(
                    text = "Mensual",
                    isSelected = !isMixedLayout,
                    onClick = { isMixedLayout = false }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Mixto (vertical)",
                    isSelected = isMixedLayout,
                    onClick = { isMixedLayout = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Color",
                    isSelected = isColorMode,
                    onClick = { isColorMode = !isColorMode },
                    trailingDotColor = if (isColorMode) CalendarioTheme.colors.cabecera else Color.Gray
                )
            }

            // SECCIÓN 3: CONTENIDO
            SectionTitle(text = "Contenido")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                OptionRow(
                    text = "Eventos",
                    isSelected = includeEvents,
                    onClick = { includeEvents = !includeEvents }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Notas",
                    isSelected = includeNotes,
                    onClick = { includeNotes = !includeNotes }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Hora de inicio",
                    isSelected = showEventTimes,
                    onClick = { showEventTimes = !showEventTimes }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                OptionRow(
                    text = "Periodos largos",
                    isSelected = showLongPeriods,
                    onClick = { showLongPeriods = !showLongPeriods }
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailingDotColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = CalendarioTheme.colors.textSystem
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (trailingDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(trailingDotColor)
                )
            }
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.cabecera,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun printPdf(context: Context, pdfFile: File, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    printManager?.print(jobName, object : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            val pInfo = PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback?.onLayoutFinished(pInfo, true)
        }

        override fun onWrite(pages: Array<out PageRange>?, destination: android.os.ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
            try {
                FileInputStream(pdfFile).use { input ->
                    FileOutputStream(destination?.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message)
            }
        }
    }, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape()).build())
}

private fun sharePdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
}

private fun savePdfToDownloads(context: Context, sourceFile: File, displayName: String) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            context.showToast("PDF guardado en Descargas")
        } else {
            context.showToast("Error al guardar PDF en Descargas")
        }
    } catch (e: Exception) {
        context.showToast("Error al guardar PDF: ${e.message}")
    }
}
