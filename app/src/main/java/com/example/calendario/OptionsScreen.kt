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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    onBackPress: () -> Unit,
    isDarkTheme: Boolean, // Se mantiene por ahora para el Switch
    onThemeToggle: (Boolean) -> Unit,
    onColorThemeClick: () -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    // --- PROGRAMA States ---
    var useDarkTheme by remember { mutableStateOf(isDarkTheme) }
    val initialHighlightColor = remember { Color(appPrefs.getInt(AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, 0xFFE9E9E9.toInt())) }
    var highlightColor by remember { mutableStateOf(initialHighlightColor) }
    var showHighlightColorPalette by remember { mutableStateOf(false) }
    val highlightColors = remember {
        listOf(
            Color(0xFFE9E9E9), // Gris (defecto)
            Color(0xFFFFF59D), // Amarillo claro
            Color(0xFFFFCC80), // Naranja claro
            Color(0xFFA5D6A7), // Verde claro
            Color(0xFF90CAF9), // Azul claro
            Color(0xFFCE93D8)  // Violeta claro
        )
    }

    // --- WIDGET States ---
    val initialEventCount = remember { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    var eventCountSliderValue by remember { mutableFloatStateOf(initialEventCount.toFloat()) }
    val initialUseLargeFont = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    var useLargeFontSwitchState by remember { mutableStateOf(initialUseLargeFont) }
    var eventColor by remember { mutableStateOf(Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB))) }
    var todayEventColor by remember { mutableStateOf(Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB))) }
    var showWidgetEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetTodayEventColorPalette by remember { mutableStateOf(false) }
    val baseEventColors = remember { listOf(Color(0xFFFFFFFF), Color(0xFFF4F4F4), Color(0xFFD4D4D4), Color(0xFFB4B4B4), Color(0xFF949494), Color(0xFF5F5F5F), Color(0xFF000000)) }
    val baseTodayEventColors = remember { listOf(Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF80FF80), Color(0xFF00FFFF), Color(0xFF952BFF), Color(0xFFFFFFFF), Color(0xFF000000)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opciones", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(16.dp)) {
                // --- PROGRAMA Section ---
                SectionTitle("Programa")
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Modo oscuro", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Switch(checked = useDarkTheme, onCheckedChange = { useDarkTheme = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ColorPickerRow("Resaltado eventos de hoy", highlightColor) { showHighlightColorPalette = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorThemeClick() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Personalizar colores del tema", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // --- WIDGET Section ---
                SectionTitle("Widget")
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Número de eventos: ${eventCountSliderValue.roundToInt()}", fontSize = 16.sp, modifier = Modifier.padding(top=16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = eventCountSliderValue, onValueChange = { eventCountSliderValue = it },
                        valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f))
                    )
                    Row(modifier = Modifier.fillMaxWidth().clickable { useLargeFontSwitchState = !useLargeFontSwitchState }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Letra grande", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Switch(checked = useLargeFontSwitchState, onCheckedChange = { useLargeFontSwitchState = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ColorPickerRow("Color eventos", eventColor) { showWidgetEventColorPalette = true }
                    Spacer(Modifier.height(12.dp))
                    ColorPickerRow("Color eventos de hoy", todayEventColor) { showWidgetTodayEventColorPalette = true }
                    Spacer(Modifier.height(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                 Button(onClick = {
                    appPrefs.edit { putInt(AppThemeSetup.KEY_TODAY_HIGHLIGHT_COLOR, highlightColor.toArgb()) }
                    widgetPrefs.edit {
                        putInt(WidgetConstants.KEY_EVENT_COUNT, eventCountSliderValue.roundToInt())
                        putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, useLargeFontSwitchState)
                        putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, eventColor.toArgb())
                        putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, todayEventColor.toArgb())
                    }
                    onThemeToggle(useDarkTheme)
                    notifyCalendarWidgetsConfigurationChangedMainActivity(context)
                    onBackPress()
                }) {
                    Text("GUARDAR")
                }
            }
        }
    }

    if (showHighlightColorPalette) {
        ColorPaletteDialog(title = "Color para resaltar eventos de hoy", colors = highlightColors, currentlySelectedColor = highlightColor, onColorSelected = { selectedColor -> highlightColor = selectedColor; showHighlightColorPalette = false }, onDismiss = { showHighlightColorPalette = false })
    }
    if (showWidgetEventColorPalette) {
        ColorPaletteDialog(title = "Color para eventos del widget", colors = baseEventColors, currentlySelectedColor = eventColor, onColorSelected = { selectedColor -> eventColor = selectedColor; showWidgetEventColorPalette = false }, onDismiss = { showWidgetEventColorPalette = false })
    }
    if (showWidgetTodayEventColorPalette) {
        ColorPaletteDialog(title = "Color para eventos de hoy del widget", colors = baseTodayEventColors, currentlySelectedColor = todayEventColor, onColorSelected = { selectedColor -> todayEventColor = selectedColor; showWidgetTodayEventColorPalette = false }, onDismiss = { showWidgetTodayEventColorPalette = false })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ColorPickerRow(label: String, currentColor: Color, onColorBoxClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(modifier = Modifier
            .size(32.dp)
            .background(currentColor, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onColorBoxClick))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPaletteDialog(
    title: String, colors: List<Color>, currentlySelectedColor: Color,
    onColorSelected: (Color) -> Unit, onDismiss: () -> Unit
) {
    val selectedItemBorderColor = Color.Red

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            LazyRow(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                items(colors) { colorInPalette ->
                    val isSelected = colorInPalette == currentlySelectedColor
                    Box(modifier = Modifier
                        .size(40.dp)
                        .background(colorInPalette, CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) selectedItemBorderColor else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            ),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorInPalette) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
