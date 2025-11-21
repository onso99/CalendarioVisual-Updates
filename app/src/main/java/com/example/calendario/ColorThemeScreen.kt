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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import org.json.JSONObject
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
    val categories = remember { ColorThemeConfig.colorThemeItems.map { it.category }.distinct() }

    val pendingChanges = remember { mutableStateMapOf<String, Color>() }

    var showMenu by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAdvancedColorDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<Triple<String, Color, String>?>(null) } // key, color, label

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> exportThemeToJson(context, uri, pendingChanges) }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importThemeFromJson(context, uri, pendingChanges, onThemeUpdated)
                }
            }
        }
    )

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
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, "Aplicar")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menú", tint = MaterialTheme.colorScheme.onPrimary) }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.background(CalendarioTheme.colors.dropdownMenuBackground)
                        ) {
                            DropdownMenuItem(text = { Text("Importar tema...", color = CalendarioTheme.colors.onScreenTextNormal) }, onClick = { 
                                showMenu = false
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
                                importLauncher.launch(intent)
                            })
                            DropdownMenuItem(text = { Text("Exportar tema...", color = CalendarioTheme.colors.onScreenTextNormal) }, onClick = { 
                                showMenu = false
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "calendario_theme.json") }
                                exportLauncher.launch(intent)
                            })
                            DropdownMenuItem(text = { Text("Restaurar colores por defecto", color = CalendarioTheme.colors.onScreenTextNormal) }, onClick = { showMenu = false; showRestoreDialog = true })
                        }
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
            groupedItems.forEach { (category, items) ->
                SectionTitle(text = category)
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)
                ) {
                    items.forEach { item ->
                        val lightColor = if (item.lightThemeKey.isNotBlank()) {
                            pendingChanges[item.lightThemeKey] ?: Color(prefs.getInt(item.lightThemeKey, item.defaultLight.toArgb()))
                        } else null

                        val darkColor = if (item.darkThemeKey.isNotBlank()) {
                            pendingChanges[item.darkThemeKey] ?: Color(prefs.getInt(item.darkThemeKey, item.defaultDark.toArgb()))
                        } else null
                        
                        ColorThemeRow(
                            item = item,
                            lightColor = lightColor,
                            darkColor = darkColor,
                            onLightColorClick = { key, color -> colorToEdit = Triple(key, color, item.label) ; showAdvancedColorDialog = true },
                            onDarkColorClick = { key, color -> colorToEdit = Triple(key, color, item.label) ; showAdvancedColorDialog = true }
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

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Restaurar Colores", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = { Text("¿Estás seguro de que quieres restaurar todos los colores a sus valores por defecto?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(onClick = {
                    val editor = prefs.edit()
                    ColorThemeConfig.colorThemeItems.forEach {
                        if (it.lightThemeKey.isNotBlank()) editor.remove(it.lightThemeKey)
                        if (it.darkThemeKey.isNotBlank()) editor.remove(it.darkThemeKey)
                    }
                    editor.apply()
                    pendingChanges.clear()
                    showRestoreDialog = false
                    onThemeUpdated()
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancelar") } }
        )
    }
}

private fun exportThemeToJson(context: Context, uri: Uri, pendingChanges: Map<String, Color>) {
    val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val json = JSONObject()
    val lightTheme = JSONObject()
    val darkTheme = JSONObject()

    ColorThemeConfig.colorThemeItems.forEach {
        if (it.lightThemeKey.isNotBlank()) {
            val color = pendingChanges[it.lightThemeKey] ?: Color(prefs.getInt(it.lightThemeKey, it.defaultLight.toArgb()))
            lightTheme.put(it.lightThemeKey, String.format("#%08X", color.toArgb()))
        }
        if (it.darkThemeKey.isNotBlank()) {
            val color = pendingChanges[it.darkThemeKey] ?: Color(prefs.getInt(it.darkThemeKey, it.defaultDark.toArgb()))
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

private fun importThemeFromJson(context: Context, uri: Uri, pendingChanges: MutableMap<String, Color>, onFinished: () -> Unit) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            pendingChanges.clear()

            val lightTheme = json.optJSONObject("lightTheme")
            lightTheme?.keys()?.forEach { key ->
                try {
                    val colorInt = android.graphics.Color.parseColor(lightTheme.getString(key))
                    pendingChanges[key] = Color(colorInt)
                } catch (e: IllegalArgumentException) {}
            }

            val darkTheme = json.optJSONObject("darkTheme")
            darkTheme?.keys()?.forEach { key ->
                try {
                    val colorInt = android.graphics.Color.parseColor(darkTheme.getString(key))
                    pendingChanges[key] = Color(colorInt)
                } catch (e: IllegalArgumentException) {}
            }
        }
        Toast.makeText(context, "Tema importado. Pulsa APLICAR para guardarlo.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al importar el tema", Toast.LENGTH_SHORT).show()
    }
    onFinished()
}

@Composable
private fun ColorThemeRow(
    item: ColorThemeItem,
    lightColor: Color?,
    darkColor: Color?,
    onLightColorClick: (key: String, color: Color) -> Unit,
    onDarkColorClick: (key: String, color: Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.End) {
            if (lightColor != null) {
                ColorBox(color = lightColor) { onLightColorClick(item.lightThemeKey, lightColor) }
            } else { Spacer(modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            if (darkColor != null) {
                ColorBox(color = darkColor) { onDarkColorClick(item.darkThemeKey, darkColor) }
            } else { Spacer(modifier = Modifier.size(32.dp)) }
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
        confirmButton = { 
            Button(onClick = { onColorConfirm(currentColor) }) { 
                Text("Aceptar") 
            }
        },
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
