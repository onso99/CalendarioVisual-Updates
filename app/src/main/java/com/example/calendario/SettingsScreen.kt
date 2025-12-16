package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit,
    themeManager: ThemeManager,
    onColorThemeClick: () -> Unit,
    onThemeUpdated: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val appPrefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    var lightThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, null)) }
    var darkThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_DARK_THEME_NAME, null)) }

    // --- Dialog States ---
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showCompatibilityDialog by remember { mutableStateOf<CompatibilityDialogInfo?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // --- Launchers ---
    val onThemeImported = {
        lightThemeName = appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, null)
        darkThemeName = appPrefs.getString(AppConstants.KEY_DARK_THEME_NAME, null)
        onThemeUpdated()
    }
    
    val showDialog = { dialogInfo: CompatibilityDialogInfo -> showCompatibilityDialog = dialogInfo }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importThemeFromJson(context, uri, onThemeImported, showDialog)
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

    // --- States ---
    val themeSetting by themeManager.themeSetting.collectAsState()
    val originalShowWeekNumber = remember { appPrefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false) } // Default to false
    val originalEventCount = remember { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    val originalUseLargeFont = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    val originalEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) }
    val originalTodayEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) }
    val originalWidgetBackgroundColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)) }

    var pendingShowWeekNumber by remember { mutableStateOf(originalShowWeekNumber) }
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
            pendingShowWeekNumber != originalShowWeekNumber ||
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
                title = { Text("Ajustes", color = colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = colorScheme.onPrimary) } },
                actions = {
                    FilledIconButton(
                        onClick = {
                            if (hasPendingChanges) {
                                appPrefs.edit {
                                    if (pendingShowWeekNumber != originalShowWeekNumber) {
                                        putBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, pendingShowWeekNumber)
                                    }
                                }
                                widgetPrefs.edit {
                                    putInt(WidgetConstants.KEY_EVENT_COUNT, pendingEventCount.roundToInt())
                                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, pendingUseLargeFont)
                                    putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, pendingEventColor.toArgb())
                                    putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, pendingTodayEventColor.toArgb())
                                    putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, pendingWidgetBackgroundColor.toArgb())
                                }
                                CalendarAppWidgetProvider.triggerWidgetUpdate(context)
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
            // --- General Section ---
            SectionTitle(text = "General")
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingShowWeekNumber = !pendingShowWeekNumber }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Semana en vista anual", color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Switch(
                        checked = pendingShowWeekNumber,
                        onCheckedChange = { pendingShowWeekNumber = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primary.copy(alpha = 0.54f),
                            uncheckedThumbColor = colorScheme.outline,
                            uncheckedTrackColor = colorScheme.surfaceVariant,
                            uncheckedBorderColor = colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // --- Theme Section ---
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

                val currentLightThemeName = lightThemeName
                val currentDarkThemeName = darkThemeName

                if (currentLightThemeName != null && currentLightThemeName == currentDarkThemeName) {
                    Text(
                        text = currentLightThemeName,
                        color = themeNameColor,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp).weight(1f),
                        fontSize = 13.sp
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        currentLightThemeName?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Claro: ", color = themeNameColor, fontSize = 13.sp)
                                Text(it, color = themeNameColor, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                        currentDarkThemeName?.let {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Oscuro: ", color = themeNameColor, fontSize = 13.sp)
                                Text(it, color = themeNameColor, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { showThemeDialog = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Modo", color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    Text(themeSetting.name.lowercase().replaceFirstChar { it.titlecase() }, color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(text = "Personalizar colores", onClick = onColorThemeClick)
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Importar tema...") { importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow("Exportar tema...") { exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "calendario_theme.json") }) }
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

    if (showThemeDialog) {
        val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

        ThemeSelectionDialog(
            currentTheme = themeSetting,
            onThemeSelected = { themeManager.setTheme(it) },
            onDismiss = { showThemeDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            onContainerColor = onFondoDialogos
        )
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
            text = { Text("¿Estás seguro de que quieres restaurar todos los colores a sus valores por defecto? Las palabras clave no se verán afectadas.", color = onFondoDialogos) },
            confirmButton = {
                Button(
                    onClick = {
                        appPrefs.edit(commit = true) {
                            ColorThemeConfig.colorThemeItems.forEach { item ->
                                if (item.lightThemeKey.isNotBlank()) remove(item.lightThemeKey)
                                if (item.darkThemeKey.isNotBlank()) remove(item.darkThemeKey)
                            }
                            remove(AppConstants.KEY_LIGHT_THEME_NAME)
                            remove(AppConstants.KEY_DARK_THEME_NAME)
                        }
                        // Actualiza los nombres de los temas después de restaurar
                        lightThemeName = null
                        darkThemeName = null
                        onThemeUpdated()
                        Toast.makeText(context, "Los colores han sido restaurados.", Toast.LENGTH_SHORT).show()
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancelar", color = onFondoDialogos) } }
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
