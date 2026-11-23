package com.example.calendario

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.math.roundToInt

@Composable
fun WidgetStyleScreen(
    onBackPress: () -> Unit,
    context: android.content.Context,
    refreshUI: () -> Unit
) {
    var eventColor by remember { mutableStateOf(SettingsManager.getWidgetEventColor(context)) }
    var todayEventColor by remember { mutableStateOf(SettingsManager.getWidgetTodayEventColor(context)) }
    var backgroundColor by remember { mutableStateOf(SettingsManager.getWidgetBackgroundColor(context)) }
    var fontBoost by remember { mutableFloatStateOf(SettingsManager.getWidgetTextBoost(context)) }
    var fontFamilyKey by remember { mutableStateOf(SettingsManager.getWidgetFontFamily(context)) }
    var fontBold by remember { mutableStateOf(SettingsManager.isWidgetFontBold(context)) }

    var showTodayEventColorDialog by remember { mutableStateOf(false) }
    var showEventColorDialog by remember { mutableStateOf(false) }
    var showBgColorDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    val previewFontFamily = when(fontFamilyKey) {
        WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif
        WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace
        WidgetConstants.FONT_FAMILY_CONDENSED -> {
            val style = if (fontBold) Typeface.BOLD else Typeface.NORMAL
            FontFamily(Typeface.create("sans-serif-condensed", style))
        }
        WidgetConstants.FONT_FAMILY_SANS_SERIF -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    val calculatedFontSize = (14f + fontBoost).sp

    AppScreen(
        title = stringResource(id = R.string.widget_style_screen_title),
        onBackClick = onBackPress
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- 1. RECTÁNGULO DE SIMULACIÓN DE WIDGET (Compacto, sin puntos, espaciado reducido) ---
            val checkerColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.08f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        val squareSize = 10.dp.toPx()
                        val rows = (size.height / squareSize).toInt() + 1
                        val cols = (size.width / squareSize).toInt() + 1
                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                if ((r + c) % 2 == 0) {
                                    drawRect(
                                        color = checkerColor,
                                        topLeft = Offset(c * squareSize, r * squareSize),
                                        size = Size(squareSize, squareSize)
                                    )
                                }
                            }
                        }
                    }
                    .background(backgroundColor)
                    .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Evento de Hoy simulado (Sin punto, formato Lun 23/11 Reunión importante)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lun 23/11 Reunión importante",
                            color = todayEventColor,
                            fontSize = calculatedFontSize,
                            fontFamily = previewFontFamily,
                            fontWeight = if (fontBold) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Evento normal simulado (Sin punto, formato Mar 24/11 Gimnasio y natación)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mar 24/11 Gimnasio y natación",
                            color = eventColor,
                            fontSize = calculatedFontSize,
                            fontFamily = previewFontFamily,
                            fontWeight = if (fontBold) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // --- 2. OPCIONES DE ESTILO ---
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                // Colores: Evento Hoy (Sin flecha >)
                ColorOptionRow(
                    label = stringResource(id = R.string.widget_color_today_event),
                    color = todayEventColor,
                    onClick = { showTodayEventColorDialog = true }
                )
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                // Colores: Evento (Sin flecha >)
                ColorOptionRow(
                    label = stringResource(id = R.string.widget_color_event),
                    color = eventColor,
                    onClick = { showEventColorDialog = true }
                )
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                // Colores: Fondo (Sin flecha >)
                ColorOptionRow(
                    label = stringResource(id = R.string.widget_color_background),
                    color = backgroundColor,
                    onClick = { showBgColorDialog = true }
                )
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                // Fuente (Sin flecha >)
                val fontDisplayName = when(fontFamilyKey) {
                    WidgetConstants.FONT_FAMILY_SERIF -> stringResource(id = R.string.font_serif)
                    WidgetConstants.FONT_FAMILY_MONOSPACE -> stringResource(id = R.string.font_monospace)
                    WidgetConstants.FONT_FAMILY_CONDENSED -> stringResource(id = R.string.font_condensed)
                    WidgetConstants.FONT_FAMILY_SANS_SERIF -> stringResource(id = R.string.font_sans_serif)
                    else -> stringResource(id = R.string.font_system)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showFontDialog = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.font), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Text(
                        text = fontDisplayName,
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 14.sp,
                        fontFamily = previewFontFamily,
                        fontWeight = if (fontBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                // Ajuste de texto (Slider)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(id = R.string.widget_text_adjustment), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = fontBoost,
                            onValueChange = {
                                fontBoost = it
                                SettingsManager.saveWidgetTextBoost(context, it)
                                refreshUI()
                            },
                            valueRange = -4f..4f,
                            steps = 7,
                            modifier = Modifier.weight(1f),
                            colors = appSliderColors()
                        )
                        Text(
                            text = (if (fontBoost.roundToInt() > 0) "+" else "") + fontBoost.roundToInt().toString(),
                            modifier = Modifier.width(40.dp).padding(start = 8.dp),
                            color = CalendarioTheme.colors.textSystem,
                            textAlign = TextAlign.End,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showTodayEventColorDialog) {
        AdvancedColorPickerDialog(
            initialColor = todayEventColor,
            onDismissRequest = { showTodayEventColorDialog = false },
            onColorConfirm = { color ->
                todayEventColor = color
                SettingsManager.saveWidgetTodayEventColor(context, color)
                refreshUI()
                showTodayEventColorDialog = false
            }
        )
    }

    if (showEventColorDialog) {
        AdvancedColorPickerDialog(
            initialColor = eventColor,
            onDismissRequest = { showEventColorDialog = false },
            onColorConfirm = { color ->
                eventColor = color
                SettingsManager.saveWidgetEventColor(context, color)
                refreshUI()
                showEventColorDialog = false
            }
        )
    }

    if (showBgColorDialog) {
        AdvancedColorPickerDialog(
            initialColor = backgroundColor,
            onDismissRequest = { showBgColorDialog = false },
            onColorConfirm = { color ->
                backgroundColor = color
                SettingsManager.saveWidgetBackgroundColor(context, color)
                refreshUI()
                showBgColorDialog = false
            }
        )
    }

    if (showFontDialog) {
        FontFamilySelectionDialog(
            currentSelection = fontFamilyKey,
            currentBold = fontBold,
            onConfirm = { family, bold ->
                fontFamilyKey = family
                fontBold = bold
                SettingsManager.saveWidgetFontFamily(context, family)
                SettingsManager.saveWidgetFontBold(context, bold)
                refreshUI()
                showFontDialog = false
            },
            onDismiss = { showFontDialog = false }
        )
    }
}

@Composable
private fun ColorOptionRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
        val isBgDark = ColorUtils.calculateLuminance(CalendarioTheme.colors.fondoSecciones.toArgb()) < 0.5
        val borderColor = if (isBgDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, borderColor, CircleShape)
        )
    }
}
