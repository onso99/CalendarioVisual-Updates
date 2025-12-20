package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.math.roundToInt

enum class StartOfWeekOption(val key: String, val displayNameRes: Int) {
    SYSTEM("SYSTEM", R.string.system_default),
    MONDAY("MONDAY", R.string.monday),
    SUNDAY("SUNDAY", R.string.sunday),
    SATURDAY("SATURDAY", R.string.saturday);

    companion object {
        fun fromKey(key: String): StartOfWeekOption {
            return entries.find { it.key == key } ?: SYSTEM
        }
    }
}

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
    var showLegacyThemeDialog by remember { mutableStateOf<Pair<ParsedTheme, String>?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var showStartDayOfWeekDialog by remember { mutableStateOf(false) }

    // --- Launchers ---
    val onThemeImported = {
        lightThemeName = appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, null)
        darkThemeName = appPrefs.getString(AppConstants.KEY_DARK_THEME_NAME, null)
        onThemeUpdated()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val fileName = getFileName(context, uri)
                        when (val importResult = ThemeImportManager.processThemeImport(context, uri)) {
                            is ImportResult.Success -> {
                                ThemePersistence.applyTheme(context, importResult.parsedTheme, fileName)
                                onThemeImported()
                                Toast.makeText(context, R.string.theme_imported_successfully, Toast.LENGTH_SHORT).show()
                            }
                            is ImportResult.LegacyThemeDetected -> {
                                showLegacyThemeDialog = importResult.parsedTheme to fileName
                            }
                            is ImportResult.Failure -> {
                                Toast.makeText(context, importResult.errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, R.string.error_reading_theme_file, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val newName = appPrefs.getString("temp_export_name", "nuevo_tema") ?: "nuevo_tema"
                        ThemePersistence.exportThemeToJson(context, uri, newName)
                    } catch (_: Exception) {
                        Toast.makeText(context, R.string.error_saving_theme_file, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    // --- States ---
    val themeSetting by themeManager.themeSetting.collectAsState()
    val originalShowWeekNumber = remember { appPrefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false) }
    val originalStartOfWeekKey = remember { appPrefs.getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key) ?: StartOfWeekOption.SYSTEM.key }
    val originalEventCount = remember { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    val originalUseLargeFont = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    val originalEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) }
    val originalTodayEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) }
    val originalWidgetBackgroundColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)) }

    var pendingShowWeekNumber by remember { mutableStateOf(originalShowWeekNumber) }
    var pendingStartOfWeekKey by remember { mutableStateOf(originalStartOfWeekKey) }
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
            pendingStartOfWeekKey != originalStartOfWeekKey ||
            pendingEventCount.roundToInt() != originalEventCount ||
            pendingUseLargeFont != originalUseLargeFont ||
            pendingEventColor != originalEventColor ||
            pendingTodayEventColor != originalTodayEventColor ||
            pendingWidgetBackgroundColor != originalWidgetBackgroundColor
        }
    }
    
    val backAction = {
        if (hasPendingChanges) {
            showDiscardChangesDialog = true
        } else {
            onBackPress()
        }
    }
    
    val versionName = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings), color = colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = backAction) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back), tint = colorScheme.onPrimary) } },
                actions = {
                    if (hasPendingChanges) {
                        IconButton(onClick = {
                            appPrefs.edit {
                                putBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, pendingShowWeekNumber)
                                putString(AppConstants.KEY_START_OF_WEEK, pendingStartOfWeekKey)
                            }
                            widgetPrefs.edit {
                                putInt(WidgetConstants.KEY_EVENT_COUNT, pendingEventCount.roundToInt())
                                putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, pendingUseLargeFont)
                                putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, pendingEventColor.toArgb())
                                putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, pendingTodayEventColor.toArgb())
                                putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, pendingWidgetBackgroundColor.toArgb())
                            }
                            CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                            onBackPress()
                        }) {
                            Icon(Icons.Default.Check, stringResource(id = R.string.apply_changes), tint = colorScheme.onPrimary)
                        }
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
            SectionTitle(text = stringResource(id = R.string.general))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { showThemeDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.mode), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Text(stringResource(id = themeSetting.displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { showStartDayOfWeekDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.start_of_week), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Text(stringResource(id = StartOfWeekOption.fromKey(pendingStartOfWeekKey).displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingShowWeekNumber = !pendingShowWeekNumber }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.week_in_year_view), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Switch(
                        checked = pendingShowWeekNumber,
                        onCheckedChange = { pendingShowWeekNumber = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primary.copy(alpha = 0.54f),
                            uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                            uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
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
                    text = stringResource(id = R.string.customize_theme),
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
                    hsl[2] = if (isDark) (hsl[2] + 0.4f).coerceAtMost(1f) else (hsl[2] - 0.4f).coerceAtLeast(0f)
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
                                Text(stringResource(id = R.string.light_theme_prefix), color = themeNameColor, fontSize = 13.sp)
                                Text(it, color = themeNameColor, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                        currentDarkThemeName?.let {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.dark_theme_prefix), color = themeNameColor, fontSize = 13.sp)
                                Text(it, color = themeNameColor, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(horizontal = 16.dp)) {
                ActionRow(text = stringResource(id = R.string.customize_colors), onClick = onColorThemeClick)
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(stringResource(id = R.string.import_theme)) { importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(stringResource(id = R.string.export_theme)) { showExportDialog = true }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(stringResource(id = R.string.restore_default_colors)) { showRestoreDialog = true }
            }

            WidgetSectionTitle()
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(horizontal = 16.dp)) {
                Text(stringResource(id = R.string.widget_event_count, pendingEventCount.roundToInt()), fontSize = 16.sp, modifier = Modifier.padding(top=16.dp), color = CalendarioTheme.colors.textSystem)
                Slider(value = pendingEventCount, onValueChange = { pendingEventCount = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), colors = SliderDefaults.colors(thumbColor = colorScheme.primary, activeTrackColor = colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                Row(modifier = Modifier.fillMaxWidth().clickable { pendingUseLargeFont = !pendingUseLargeFont }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(id = R.string.large_font), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Switch(checked = pendingUseLargeFont, onCheckedChange = { pendingUseLargeFont = it }, colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary, checkedTrackColor = colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colorScheme.outline.copy(alpha = 0.3f))
                ColorPickerRow(stringResource(id = R.string.background_color), pendingWidgetBackgroundColor) { showWidgetBackgroundColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow(stringResource(id = R.string.event_color), pendingEventColor) { showWidgetEventColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow(stringResource(id = R.string.today_event_color), pendingTodayEventColor) { showWidgetTodayEventColorPalette = true }
                Spacer(Modifier.height(16.dp))
            }
            
            // --- About Section ---
            SectionTitle(text = stringResource(id = R.string.about))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
                    .padding(16.dp)
            ) {
                Text("Calendario Visual V${versionName}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Text("Gemini / Android Studio", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Text("Onso/Diciembre 2025", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeSetting,
            onThemeSelected = { themeManager.setTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    if (showStartDayOfWeekDialog) {
        StartDayOfWeekDialog(
            currentSelectionKey = pendingStartOfWeekKey,
            onOptionSelected = { 
                pendingStartOfWeekKey = it
                showStartDayOfWeekDialog = false
            },
            onDismiss = { showStartDayOfWeekDialog = false }
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
        RestoreDefaultColorsDialog(
            onDismiss = { showRestoreDialog = false },
            onConfirm = {
                appPrefs.edit(commit = true) {
                    ColorThemeConfig.colorThemeItems.forEach { item ->
                        if (item.lightThemeKey.isNotBlank()) remove(item.lightThemeKey)
                        if (item.darkThemeKey.isNotBlank()) remove(item.darkThemeKey)
                    }
                    remove(AppConstants.KEY_LIGHT_THEME_NAME)
                    remove(AppConstants.KEY_DARK_THEME_NAME)
                }
                lightThemeName = null
                darkThemeName = null
                onThemeUpdated()
                Toast.makeText(context, R.string.colors_restored, Toast.LENGTH_SHORT).show()
                showRestoreDialog = false
            }
        )
    }

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.discard_changes_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardChangesDialog = false
                        onBackPress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            }
        )
    }

    showLegacyThemeDialog?.let { (parsedTheme, fileName) ->
        AlertDialog(
            onDismissRequest = { showLegacyThemeDialog = null },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.legacy_theme_detected_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.legacy_theme_detected_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        ThemePersistence.applyTheme(context, parsedTheme, fileName)
                        onThemeImported()
                        Toast.makeText(context, R.string.theme_imported_successfully, Toast.LENGTH_SHORT).show()
                        showLegacyThemeDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
                ) {
                    Text(stringResource(id = R.string.apply_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLegacyThemeDialog = null }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            }
        )
    }
    
    if (showExportDialog) {
        ExportThemeDialog(
            onDismissRequest = { showExportDialog = false },
            onConfirm = { newName ->
                showExportDialog = false
                appPrefs.edit { putString("temp_export_name", newName) }
                exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { 
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "${newName}.json")
                })
            }
        )
    }
}

@Composable
private fun ExportThemeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.export_theme_title), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(id = R.string.theme_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.ifBlank { "nuevo_tema" }) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(id = R.string.export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun StartDayOfWeekDialog(
    currentSelectionKey: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.start_of_week), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                StartOfWeekOption.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOptionSelected(option.key) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = option.displayNameRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        if (option.key == currentSelectionKey) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.selected), tint = CalendarioTheme.colors.cabecera)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
            }
        }
    )
}

private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "nombre_desconocido"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName.substringBeforeLast('.')
}