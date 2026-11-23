package com.example.calendario

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme

@Composable
fun WidgetColorScreen(
    onBackPress: () -> Unit,
    initialEventColor: Color,
    initialTodayEventColor: Color,
    initialBackgroundColor: Color,
    onEventColorChange: (Color) -> Unit,
    onTodayEventColorChange: (Color) -> Unit,
    onBackgroundColorChange: (Color) -> Unit
) {
    var eventColor by remember { mutableStateOf(initialEventColor) }
    var todayEventColor by remember { mutableStateOf(initialTodayEventColor) }
    var backgroundColor by remember { mutableStateOf(initialBackgroundColor) }

    var showEventColorDialog by remember { mutableStateOf(false) }
    var showTodayEventColorDialog by remember { mutableStateOf(false) }
    var showBgColorDialog by remember { mutableStateOf(false) }

    AppScreen(
        title = stringResource(id = R.string.widget_color_screen_title),
        onBackClick = onBackPress
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- 1. SIMULACIÓN DE WIDGET ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Evento de Hoy simulado
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(todayEventColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "12 Mar • Reunión importante de trabajo",
                            color = todayEventColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Evento normal simulado
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(eventColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "14 Mar • Gimnasio y natación",
                            color = eventColor,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // --- 2. OPCIONES DE COLOR ---
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                // Evento Hoy
                ColorOptionRow(
                    label = stringResource(id = R.string.widget_color_today_event),
                    color = todayEventColor,
                    onClick = { showTodayEventColorDialog = true }
                )
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                // Evento
                ColorOptionRow(
                    label = stringResource(id = R.string.widget_color_event),
                    color = eventColor,
                    onClick = { showEventColorDialog = true }
                )
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                // Fondo
                ColorOptionRow(
                    label = stringResource(id = R.string.widget_color_background),
                    color = backgroundColor,
                    onClick = { showBgColorDialog = true }
                )
            }
        }
    }

    if (showTodayEventColorDialog) {
        AdvancedColorPickerDialog(
            initialColor = todayEventColor,
            onDismissRequest = { showTodayEventColorDialog = false },
            onColorConfirm = { color ->
                todayEventColor = color
                onTodayEventColorChange(color)
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
                onEventColorChange(color)
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
                onBackgroundColorChange(color)
                showBgColorDialog = false
            }
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
