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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
fun ColorThemeScreen(
    onBackPress: () -> Unit,
    onThemeUpdated: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val groupedItems = ColorThemeConfig.colorThemeItems.groupBy { it.category }

    val pendingChanges = remember { mutableStateMapOf<String, Color>() }
    var showAdvancedColorDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<Triple<String, Color, String>?>(null) } // key, color, label

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalizar Colores", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary) } },
                actions = {
                    FilledIconButton(
                        onClick = { 
                            if (pendingChanges.isNotEmpty()) {
                                prefs.edit { pendingChanges.forEach { (key, color) -> putInt(key, color.toArgb()) } }
                                pendingChanges.clear()
                                onThemeUpdated()
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text("Claro", modifier = Modifier.width(64.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Oscuro", modifier = Modifier.width(64.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            groupedItems.forEach { (category, items) ->
                SectionTitle(text = category)
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)
                ) {
                    items.forEach { item ->
                        val lightColor = pendingChanges[item.lightThemeKey] ?: Color(prefs.getInt(item.lightThemeKey, item.defaultLight.toArgb()))
                        val darkColor = pendingChanges[item.darkThemeKey] ?: Color(prefs.getInt(item.darkThemeKey, item.defaultDark.toArgb()))

                        ColorThemeRow(
                            item = item,
                            lightColor = lightColor,
                            darkColor = darkColor,
                            onLightColorClick = {
                                colorToEdit = Triple(item.lightThemeKey, lightColor, "${item.label} (Claro)")
                                showAdvancedColorDialog = true
                            },
                            onDarkColorClick = {
                                colorToEdit = Triple(item.darkThemeKey, darkColor, "${item.label} (Oscuro)")
                                showAdvancedColorDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAdvancedColorDialog && colorToEdit != null) {
        AdvancedColorPickerDialog(
            initialColor = colorToEdit!!.second,
            onDismissRequest = { showAdvancedColorDialog = false },
            onColorConfirm = { newColor ->
                val key = colorToEdit!!.first
                pendingChanges[key] = newColor
                showAdvancedColorDialog = false
            }
        )
    }
}

@Composable
private fun ColorThemeRow(
    item: ColorThemeItem,
    lightColor: Color,
    darkColor: Color,
    onLightColorClick: () -> Unit,
    onDarkColorClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.End) {
            ColorBox(color = lightColor, onClick = onLightColorClick)
            Spacer(modifier = Modifier.width(32.dp))
            ColorBox(color = darkColor, onClick = onDarkColorClick)
        }
    }
}

@Composable
private fun ColorBox(color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape).clickable(onClick = onClick))
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