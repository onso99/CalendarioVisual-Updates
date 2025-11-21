package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    onBackPress: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onColorThemeClick: () -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    // --- Estados Originales ---
    val originalUseDarkTheme = remember { isDarkTheme }
    val originalHighlightColor = remember { Color(appPrefs.getInt(AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, 0xFFE9E9E9.toInt())) }
    val originalEventCount = remember { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    val originalUseLargeFont = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    val originalEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) }
    val originalTodayEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) }

    // --- Estados de Cambios Pendientes ---
    var pendingUseDarkTheme by remember { mutableStateOf(originalUseDarkTheme) }
    var pendingHighlightColor by remember { mutableStateOf(originalHighlightColor) }
    var pendingEventCount by remember { mutableFloatStateOf(originalEventCount.toFloat()) }
    var pendingUseLargeFont by remember { mutableStateOf(originalUseLargeFont) }
    var pendingEventColor by remember { mutableStateOf(originalEventColor) }
    var pendingTodayEventColor by remember { mutableStateOf(originalTodayEventColor) }

    // --- Control de visibilidad de diálogos ---
    var showHighlightColorPalette by remember { mutableStateOf(false) }
    var showWidgetEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetTodayEventColorPalette by remember { mutableStateOf(false) }

    val hasPendingChanges by remember {
        derivedStateOf {
            pendingUseDarkTheme != originalUseDarkTheme ||
            pendingHighlightColor != originalHighlightColor ||
            pendingEventCount.roundToInt() != originalEventCount ||
            pendingUseLargeFont != originalUseLargeFont ||
            pendingEventColor != originalEventColor ||
            pendingTodayEventColor != originalTodayEventColor
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opciones", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary) } },
                actions = {
                    FilledIconButton(
                        onClick = {
                            if (hasPendingChanges) {
                                appPrefs.edit { putInt(AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, pendingHighlightColor.toArgb()) }
                                widgetPrefs.edit {
                                    putInt(WidgetConstants.KEY_EVENT_COUNT, pendingEventCount.roundToInt())
                                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, pendingUseLargeFont)
                                    putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, pendingEventColor.toArgb())
                                    putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, pendingTodayEventColor.toArgb())
                                }
                                if (pendingUseDarkTheme != originalUseDarkTheme) {
                                    onThemeToggle(pendingUseDarkTheme)
                                }
                                notifyCalendarWidgetsConfigurationChangedMainActivity(context)
                            }
                            onBackPress()
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, "Aplicar")
                    }
                    Spacer(modifier = Modifier.width(48.dp)) // Espacio para alinear con la otra pantalla
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("Programa")
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Modo oscuro", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = pendingUseDarkTheme, onCheckedChange = { pendingUseDarkTheme = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ColorPickerRow("Resaltado eventos de hoy", pendingHighlightColor) { showHighlightColorPalette = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(modifier = Modifier.fillMaxWidth().clickable { onColorThemeClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Personalizar colores del tema", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionTitle("Widget")
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Text("Número de eventos: ${pendingEventCount.roundToInt()}", fontSize = 16.sp, modifier = Modifier.padding(top=16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = pendingEventCount, onValueChange = { pendingEventCount = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)))
                Row(modifier = Modifier.fillMaxWidth().clickable { pendingUseLargeFont = !pendingUseLargeFont }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Letra grande", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = pendingUseLargeFont, onCheckedChange = { pendingUseLargeFont = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ColorPickerRow("Color eventos", pendingEventColor) { showWidgetEventColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow("Color eventos de hoy", pendingTodayEventColor) { showWidgetTodayEventColorPalette = true }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showHighlightColorPalette) {
        AdvancedColorPickerDialog(initialColor = pendingHighlightColor, onDismissRequest = { showHighlightColorPalette = false }, onColorConfirm = { pendingHighlightColor = it; showHighlightColorPalette = false })
    }
    if (showWidgetEventColorPalette) {
        AdvancedColorPickerDialog(initialColor = pendingEventColor, onDismissRequest = { showWidgetEventColorPalette = false }, onColorConfirm = { pendingEventColor = it; showWidgetEventColorPalette = false })
    }
    if (showWidgetTodayEventColorPalette) {
        AdvancedColorPickerDialog(initialColor = pendingTodayEventColor, onDismissRequest = { showWidgetTodayEventColorPalette = false }, onColorConfirm = { pendingTodayEventColor = it; showWidgetTodayEventColorPalette = false })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ColorPickerRow(label: String, currentColor: Color, onColorBoxClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(modifier = Modifier.size(32.dp).background(currentColor, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape).clickable(onClick = onColorBoxClick))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorConfirm: (Color) -> Unit
) {
    var red by remember { mutableStateOf(initialColor.red * 255) }
    var green by remember { mutableStateOf(initialColor.green * 255) }
    var blue by remember { mutableStateOf(initialColor.blue * 255) }
    var alpha by remember { mutableStateOf(initialColor.alpha * 255) }
    
    val currentColor by remember { derivedStateOf { Color(red / 255f, green / 255f, blue / 255f, alpha / 255f) } }
    var hexCode by remember(currentColor) { mutableStateOf(String.format("#%02X%02X%02X%02X", alpha.roundToInt(), red.roundToInt(), green.roundToInt(), blue.roundToInt())) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Seleccionar Color", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth().height(60.dp).border(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(initialColor))
                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(currentColor))
                }
                Spacer(Modifier.height(16.dp))
                ColorSlider(label = "A", value = alpha, onValueChange = { alpha = it })
                ColorSlider(label = "R", value = red, onValueChange = { red = it })
                ColorSlider(label = "G", value = green, onValueChange = { green = it })
                ColorSlider(label = "B", value = blue, onValueChange = { blue = it })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = hexCode,
                    onValueChange = { 
                        val newHex = if (it.startsWith("#")) it else "#$it"
                        hexCode = newHex
                        if (newHex.length == 9) {
                            try {
                                val parsedColor = Color(android.graphics.Color.parseColor(newHex))
                                alpha = parsedColor.alpha * 255
                                red = parsedColor.red * 255
                                green = parsedColor.green * 255
                                blue = parsedColor.blue * 255
                            } catch (e: Exception) { /* No-op, color inválido */ }
                        }
                    },
                    label = { Text("Hex (ARGB)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onColorConfirm(currentColor) })
                )
            }
        },
        confirmButton = { Button(onClick = { onColorConfirm(currentColor) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..255f, modifier = Modifier.weight(1f))
        Text(value.roundToInt().toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}