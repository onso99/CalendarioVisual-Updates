package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import org.json.JSONObject
import java.util.Locale
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
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val appPrefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    // --- Dialog States ---
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showThemeMixerDialog by remember { mutableStateOf(false) }
    var showCompatibilityDialog by remember { mutableStateOf<CompatibilityDialogInfo?>(null) }

    // --- Launchers ---
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importThemeFromJson(context, uri, onThemeUpdated) { dialogInfo ->
                        showCompatibilityDialog = dialogInfo
                    }
                }
            }
        }
    )
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> exportThemeToJson(context, uri) }
            }
        }
    )

    // --- Theme & Widget States ---
    val lightThemeName = appPrefs.getString(AppThemeSetup.KEY_LIGHT_THEME_NAME, null)
    val darkThemeName = appPrefs.getString(AppThemeSetup.KEY_DARK_THEME_NAME, null)

    val originalUseDarkTheme = remember { isDarkTheme }
    val originalEventCount = remember { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    val originalUseLargeFont = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    val originalEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) }
    val originalTodayEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) }
    val originalWidgetBackgroundColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)) }

    var pendingUseDarkTheme by remember { mutableStateOf(originalUseDarkTheme) }
    var pendingEventCount by remember { mutableFloatStateOf(originalEventCount.toFloat()) }
    var pendingUseLargeFont by remember { mutableStateOf(originalUseLargeFont) }
    var pendingEventColor by remember { mutableStateOf(originalEventColor) }
    var pendingTodayEventColor by remember { mutableStateOf(originalTodayEventColor) }
    var pendingWidgetBackgroundColor by remember { mutableStateOf(originalWidgetBackgroundColor) }

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
                title = { Text("Opciones", color = colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = colorScheme.onPrimary) } },
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
                            containerColor = colorScheme.onPrimary.copy(alpha = 0.2f),
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, "Aplicar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
             Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personalizar Tema",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                val themeNameColor = run {
                    val settingsBackgroundColor = CalendarioTheme.colors.settingsBackground
                    val hsl = FloatArray(3)
                    ColorUtils.colorToHSL(settingsBackgroundColor.toArgb(), hsl)
                    val isDark = hsl[2] < 0.5f
                    hsl[2] = if (isDark) (hsl[2] + 0.2f).coerceAtMost(1f) else (hsl[2] - 0.2f).coerceAtLeast(0f)
                    Color(ColorUtils.HSLToColor(hsl))
                }

                if (lightThemeName != null && lightThemeName == darkThemeName) {
                    Text(
                        text = lightThemeName,
                        color = themeNameColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp).weight(1f)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        lightThemeName?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Claro: ", color = themeNameColor, fontSize = 14.sp)
                                Text(it, color = themeNameColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            }
                        }
                        darkThemeName?.let {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Oscuro: ", color = themeNameColor, fontSize = 14.sp)
                                Text(it, color = themeNameColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Modo oscuro", color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Switch(checked = pendingUseDarkTheme, onCheckedChange = { pendingUseDarkTheme = it }, colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary, checkedTrackColor = colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = colorScheme.outline, uncheckedTrackColor = colorScheme.surfaceVariant, uncheckedBorderColor = colorScheme.outline.copy(alpha = 0.5f)))
                }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(text = "Personalizar colores", onClick = onColorThemeClick)
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Importar tema...") { importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Exportar tema...") { exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "calendario_theme.json") }) }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Mezclar temas...") { showThemeMixerDialog = true }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Restaurar colores por defecto") { showRestoreDialog = true }
            }

            WidgetSectionTitle()
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Text("Número de eventos: ${pendingEventCount.roundToInt()}", fontSize = 16.sp, modifier = Modifier.padding(top=16.dp), color = colorScheme.onSurfaceVariant)
                Slider(value = pendingEventCount, onValueChange = { pendingEventCount = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), colors = SliderDefaults.colors(thumbColor = colorScheme.primary, activeTrackColor = colorScheme.primary, inactiveTrackColor = colorScheme.onSurfaceVariant.copy(alpha = 0.24f)))
                Row(modifier = Modifier.fillMaxWidth().clickable { pendingUseLargeFont = !pendingUseLargeFont }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Letra grande", fontSize = 16.sp, color = colorScheme.onSurfaceVariant)
                    Switch(checked = pendingUseLargeFont, onCheckedChange = { pendingUseLargeFont = it }, colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary, checkedTrackColor = colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = colorScheme.outline, uncheckedTrackColor = colorScheme.surfaceVariant, uncheckedBorderColor = colorScheme.outline.copy(alpha = 0.5f)))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colorScheme.outline.copy(alpha = 0.3f))
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
        val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            title = { Text("Restaurar Colores", fontWeight = FontWeight.Bold, color = onFondoDialogos) },
            text = { Text("¿Estás seguro de que quieres restaurar todos los colores y palabras clave a sus valores por defecto?", color = onFondoDialogos) },
            confirmButton = {
                Button(
                    onClick = {
                        appPrefs.edit {
                            ColorThemeConfig.colorThemeItems.forEach { item ->
                                if (item.lightThemeKey.isNotBlank()) remove(item.lightThemeKey)
                                if (item.darkThemeKey.isNotBlank()) remove(item.darkThemeKey)
                            }
                            remove(AppThemeSetup.KEY_EVENT_1_KEYWORD)
                            remove(AppThemeSetup.KEY_EVENT_2_KEYWORD)
                            remove(AppThemeSetup.KEY_LIGHT_THEME_NAME)
                            remove(AppThemeSetup.KEY_DARK_THEME_NAME)
                        }
                        onThemeUpdated()
                        Toast.makeText(context, "Los colores y palabras clave han sido restaurados.", Toast.LENGTH_SHORT).show()
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancelar", color = onFondoDialogos) } }
        )
    }

    if (showThemeMixerDialog) {
        ThemeMixerDialog(
            onDismissRequest = { showThemeMixerDialog = false },
            onThemeMixed = { 
                showThemeMixerDialog = false
                onThemeUpdated()
            }
        )
    }

    showCompatibilityDialog?.let { dialogInfo ->
        val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
        CompatibilityAlertDialog(
            info = dialogInfo, 
            onDismiss = { showCompatibilityDialog = null },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            onContainerColor = onFondoDialogos
        )
    }
}

data class CompatibilityDialogInfo(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

@Composable
private fun CompatibilityAlertDialog(
    info: CompatibilityDialogInfo,
    onDismiss: () -> Unit,
    containerColor: Color,
    onContainerColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        title = { Text(info.title, fontWeight = FontWeight.Bold, color = onContainerColor) },
        text = { Text(info.message, color = onContainerColor) },
        confirmButton = {
            Button(
                onClick = { 
                    info.onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) { Text("Continuar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = onContainerColor) } }
    )
}

@Composable
private fun WidgetSectionTitle() {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(text = "Widget", style = typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp), fontWeight = FontWeight.Bold, color = colorScheme.primary)
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColorPickerRow(label: String, currentColor: Color, onColorBoxClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f), color = colorScheme.onSurfaceVariant)
        Box(modifier = Modifier.size(32.dp).background(currentColor, CircleShape).border(1.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape).clickable(onClick = onColorBoxClick))
    }
}

private fun exportThemeToJson(context: Context, uri: Uri) {
    try {
        val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val allPrefs = prefs.all

        val themeData = JSONObject()
        val manifest = JSONObject()
        manifest.put("version", AppThemeSetup.CURRENT_THEME_VERSION)
        manifest.put("appName", AppThemeSetup.APP_SIGNATURE)
        themeData.put("themeManifest", manifest)

        val lightTheme = JSONObject()
        val darkTheme = JSONObject()

        ColorThemeConfig.colorThemeItems.forEach { item ->
            // Light Theme
            if (item.lightThemeKey.isNotBlank()) {
                val colorString = when (val value = allPrefs[item.lightThemeKey]) {
                    is Int -> String.format("#%08X", value)
                    is String -> value
                    else -> String.format("#%08X", item.defaultLight.toArgb()) // Fallback to default
                }
                lightTheme.put(item.lightThemeKey, colorString)
            }
            
            // Dark Theme
            if (item.darkThemeKey.isNotBlank()) {
                 val colorString = when (val value = allPrefs[item.darkThemeKey]) {
                    is Int -> String.format("#%08X", value)
                    is String -> value
                    else -> String.format("#%08X", item.defaultDark.toArgb()) // Fallback to default
                }
                darkTheme.put(item.darkThemeKey, colorString)
            }
        }

        if(lightTheme.length() > 0) themeData.put("lightTheme", lightTheme)
        if(darkTheme.length() > 0) themeData.put("darkTheme", darkTheme)

        context.contentResolver.openOutputStream(uri)?.use { 
            it.write(themeData.toString(4).toByteArray())
        }
        Toast.makeText(context, "Tema exportado con éxito", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar el tema: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

internal fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (colIndex > -1) {
                    result = cursor.getString(colIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

private fun importThemeFromJson(
    context: Context,
    uri: Uri,
    onThemeImported: () -> Unit,
    showDialog: (CompatibilityDialogInfo) -> Unit
) {
    when (val importResult = ThemeImportManager.processThemeImport(context, uri)) {
        is ImportResult.Success -> {
            val parsedTheme = importResult.parsedTheme
            val manifest = parsedTheme.manifest
            val fileVersion = manifest?.optInt("version", 1) ?: 1

            val applyChanges = { themeToApply: ParsedTheme ->
                val prefs = context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                val themeName = getFileNameFromUri(context, uri)?.removeSuffix(".json")?.replace('_', ' ')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Tema importado"

                prefs.edit {
                    val allKnownKeys = ColorThemeConfig.colorThemeItems.flatMap { listOf(it.lightThemeKey, it.darkThemeKey) }.toSet()

                    themeToApply.lightTheme?.let {
                        for (key in it.keys()) {
                            if(allKnownKeys.contains(key)) putString(key, it.getString(key))
                        }
                    }
                    themeToApply.darkTheme?.let {
                        for (key in it.keys()) {
                            if(allKnownKeys.contains(key)) putString(key, it.getString(key))
                        }
                    }
                    
                    putString(AppThemeSetup.KEY_LIGHT_THEME_NAME, themeName)
                    putString(AppThemeSetup.KEY_DARK_THEME_NAME, themeName)
                }
                onThemeImported()
                Toast.makeText(context, "Tema '${themeName}' importado con éxito.", Toast.LENGTH_SHORT).show()
            }

            when {
                fileVersion == AppThemeSetup.CURRENT_THEME_VERSION -> applyChanges(parsedTheme)
                fileVersion < AppThemeSetup.CURRENT_THEME_VERSION -> {
                    showDialog(CompatibilityDialogInfo(
                        title = "Tema Antiguo Detectado",
                        message = "Este tema es de una versión anterior y no contiene todas las opciones de color. Los colores que falten se rellenarán con los valores por defecto.",
                        onConfirm = { applyChanges(parsedTheme) }
                    ))
                }
                else -> {
                     showDialog(CompatibilityDialogInfo(
                        title = "Tema Incompatible Detectado",
                        message = "Este tema es de una versión más nueva. Se importarán solo los colores compatibles con tu versión actual.",
                        onConfirm = { applyChanges(parsedTheme) }
                    ))
                }
            }
        }
        is ImportResult.Failure -> {
            Toast.makeText(context, importResult.errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
