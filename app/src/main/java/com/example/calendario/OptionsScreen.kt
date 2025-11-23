package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    onBackPress: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onColorThemeClick: () -> Unit,
    onThemeUpdated: () -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    // --- Theme States & Launchers ---
    var showRestoreDialog by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> exportThemeToJson(context, uri) }
            }
        }
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let {
                    uri -> importThemeFromJson(context, uri) { onThemeUpdated() }
                    Toast.makeText(context, "Tema importado. Vuelve a entrar en Opciones para ver los cambios.", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // --- Widget States ---
    val originalUseDarkTheme = remember { isDarkTheme }
    val originalEventCount = remember { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    val originalUseLargeFont = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    val originalEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) }
    val originalTodayEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) }
    val originalWidgetBackgroundColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)) }

    // --- Pending Changes States ---
    var pendingUseDarkTheme by remember { mutableStateOf(originalUseDarkTheme) }
    var pendingEventCount by remember { mutableFloatStateOf(originalEventCount.toFloat()) }
    var pendingUseLargeFont by remember { mutableStateOf(originalUseLargeFont) }
    var pendingEventColor by remember { mutableStateOf(originalEventColor) }
    var pendingTodayEventColor by remember { mutableStateOf(originalTodayEventColor) }
    var pendingWidgetBackgroundColor by remember { mutableStateOf(originalWidgetBackgroundColor) }

    // --- Dialog Visibility ---
    var showWidgetEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetTodayEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetBackgroundColorPalette by remember { mutableStateOf(false) }

    val hasPendingChanges by remember {
        derivedStateOf {
            pendingUseDarkTheme != originalUseDarkTheme ||
            pendingEventCount.roundToInt() != originalEventCount ||
            pendingUseLargeFont != originalUseLargeFont ||
            pendingEventColor != originalEventColor ||
            pendingTodayEventColor != originalTodayEventColor ||
            pendingWidgetBackgroundColor != originalWidgetBackgroundColor
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
                                widgetPrefs.edit {
                                    putInt(WidgetConstants.KEY_EVENT_COUNT, pendingEventCount.roundToInt())
                                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, pendingUseLargeFont)
                                    putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, pendingEventColor.toArgb())
                                    putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, pendingTodayEventColor.toArgb())
                                    putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, pendingWidgetBackgroundColor.toArgb())
                                }
                                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                                if (pendingUseDarkTheme != originalUseDarkTheme) {
                                    onThemeToggle(pendingUseDarkTheme)
                                }
                            }
                            onBackPress()
                        },
                        modifier = Modifier.padding(end = 8.dp).size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, "Aplicar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("Personalizar Tema")
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Modo oscuro", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Switch(checked = pendingUseDarkTheme, onCheckedChange = { pendingUseDarkTheme = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(text = "Personalizar colores", onClick = onColorThemeClick)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Importar tema...") { 
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
                    importLauncher.launch(intent)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Exportar tema...") { 
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "calendario_theme.json") }
                    exportLauncher.launch(intent)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Restaurar colores por defecto") { showRestoreDialog = true }
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
                ColorPickerRow("Color de fondo", pendingWidgetBackgroundColor) { showWidgetBackgroundColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow("Color eventos", pendingEventColor) { showWidgetEventColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow("Color eventos de hoy", pendingTodayEventColor) { showWidgetTodayEventColorPalette = true }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showWidgetEventColorPalette) {
        AdvancedColorPickerDialog(initialColor = pendingEventColor, onDismissRequest = { showWidgetEventColorPalette = false }, onColorConfirm = { pendingEventColor = it; showWidgetEventColorPalette = false })
    }
    if (showWidgetTodayEventColorPalette) {
        AdvancedColorPickerDialog(initialColor = pendingTodayEventColor, onDismissRequest = { showWidgetTodayEventColorPalette = false }, onColorConfirm = { pendingTodayEventColor = it; showWidgetTodayEventColorPalette = false })
    }
    if (showWidgetBackgroundColorPalette) {
        AdvancedColorPickerDialog(initialColor = pendingWidgetBackgroundColor, onDismissRequest = { showWidgetBackgroundColorPalette = false }, onColorConfirm = { pendingWidgetBackgroundColor = it; showWidgetBackgroundColorPalette = false })
    }
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Restaurar Colores", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = { Text("¿Estás seguro de que quieres restaurar todos los colores a sus valores por defecto?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(onClick = {
                    val editor = appPrefs.edit()
                    ColorThemeConfig.colorThemeItems.forEach {
                        if (it.lightThemeKey.isNotBlank()) editor.remove(it.lightThemeKey)
                        if (it.darkThemeKey.isNotBlank()) editor.remove(it.darkThemeKey)
                    }
                    editor.apply()
                    onThemeUpdated()
                    Toast.makeText(context, "Los colores han sido restaurados.", Toast.LENGTH_SHORT).show()
                    showRestoreDialog = false
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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

private fun exportThemeToJson(context: Context, uri: Uri) {
    val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val json = JSONObject()
    val lightTheme = JSONObject()
    val darkTheme = JSONObject()

    ColorThemeConfig.colorThemeItems.forEach {
        if (it.lightThemeKey.isNotBlank()) {
            val color = Color(prefs.getInt(it.lightThemeKey, it.defaultLight.toArgb()))
            lightTheme.put(it.lightThemeKey, String.format("#%08X", color.toArgb()))
        }
        if (it.darkThemeKey.isNotBlank()) {
            val color = Color(prefs.getInt(it.darkThemeKey, it.defaultDark.toArgb()))
            darkTheme.put(it.darkThemeKey, String.format("#%08X", color.toArgb()))
        }
    }
    json.put("lightTheme", lightTheme)
    json.put("darkTheme", darkTheme)

    try {
        context.contentResolver.openOutputStream(uri)?.use { 
            it.write(json.toString(4).toByteArray())
        }
        Toast.makeText(context, "Tema exportado correctamente", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al exportar el tema", Toast.LENGTH_SHORT).show()
    }
}

private fun importThemeFromJson(context: Context, uri: Uri, onFinished: () -> Unit) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE).edit()
            
            val lightTheme = json.optJSONObject("lightTheme")
            lightTheme?.keys()?.forEach { key ->
                try {
                    val colorInt = android.graphics.Color.parseColor(lightTheme.getString(key))
                    prefs.putInt(key, colorInt)
                } catch (e: IllegalArgumentException) {}
            }

            val darkTheme = json.optJSONObject("darkTheme")
            darkTheme?.keys()?.forEach { key ->
                try {
                    val colorInt = android.graphics.Color.parseColor(darkTheme.getString(key))
                    prefs.putInt(key, colorInt)
                } catch (e: IllegalArgumentException) {}
            }
            prefs.apply()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al importar el tema", Toast.LENGTH_SHORT).show()
    }
    onFinished()
}