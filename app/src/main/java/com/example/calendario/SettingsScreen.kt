package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import org.json.JSONObject
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
    onHolidayManagerClick: () -> Unit,
    onRefreshData: () -> Unit,
    onThemeUpdated: () -> Unit
) {
    val context = LocalContext.current
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
    var showBundledThemesDialog by remember { mutableStateOf(false) }
    var showFontFamilyDialog by remember { mutableStateOf(false) }

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
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error processing theme import", e)
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
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error exporting theme", e)
                        Toast.makeText(context, R.string.error_saving_theme_file, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val importHolidaysLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val success = importHolidaysFromJson(context, uri)
                        if (success) {
                            Toast.makeText(context, R.string.holidays_imported_successfully, Toast.LENGTH_SHORT).show()
                            onRefreshData()
                        } else {
                            Toast.makeText(context, R.string.error_reading_holidays_file, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error importing holidays", e)
                        Toast.makeText(context, R.string.error_reading_holidays_file, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val exportHolidaysLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        exportHolidaysToJson(context, uri)
                        Toast.makeText(context, R.string.theme_exported_successfully, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error exporting holidays", e)
                        Toast.makeText(context, R.string.error_saving_holidays_file, Toast.LENGTH_LONG).show()
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
    val originalTextBoost = remember { widgetPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f) }
    val originalEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)) }
    val originalTodayEventColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)) }
    val originalWidgetBackgroundColor = remember { Color(widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)) }
    val originalFontFamily = remember { widgetPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY }

    var pendingShowWeekNumber by remember { mutableStateOf(originalShowWeekNumber) }
    var pendingStartOfWeekKey by remember { mutableStateOf(originalStartOfWeekKey) }
    var pendingEventCount by remember { mutableFloatStateOf(originalEventCount.toFloat()) }
    var pendingTextBoost by remember { mutableFloatStateOf(originalTextBoost) }
    var pendingEventColor by remember { mutableStateOf(originalEventColor) }
    var pendingTodayEventColor by remember { mutableStateOf(originalTodayEventColor) }
    var pendingWidgetBackgroundColor by remember { mutableStateOf(originalWidgetBackgroundColor) }
    var pendingFontFamily by remember { mutableStateOf(originalFontFamily) }

    var showWidgetEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetTodayEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetBackgroundColorPalette by remember { mutableStateOf(false) }

    val hasPendingChanges by remember {
        derivedStateOf {
            pendingShowWeekNumber != originalShowWeekNumber ||
                    pendingStartOfWeekKey != originalStartOfWeekKey ||
                    pendingEventCount.roundToInt() != originalEventCount ||
                    pendingTextBoost != originalTextBoost ||
                    pendingEventColor != originalEventColor ||
                    pendingTodayEventColor != originalTodayEventColor ||
                    pendingWidgetBackgroundColor != originalWidgetBackgroundColor ||
                    pendingFontFamily != originalFontFamily
        }
    }

    val backAction = {
        if (hasPendingChanges) {
            showDiscardChangesDialog = true
        } else {
            onBackPress()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = backAction) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary) } },
                actions = {
                    if (hasPendingChanges) {
                        IconButton(onClick = {
                            appPrefs.edit {
                                putBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, pendingShowWeekNumber)
                                putString(AppConstants.KEY_START_OF_WEEK, pendingStartOfWeekKey)
                            }
                            widgetPrefs.edit {
                                putInt(WidgetConstants.KEY_EVENT_COUNT, pendingEventCount.roundToInt())
                                putFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, pendingTextBoost)
                                putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, pendingEventColor.toArgb())
                                putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, pendingTodayEventColor.toArgb())
                                putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, pendingWidgetBackgroundColor.toArgb())
                                putString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, pendingFontFamily)
                            }
                            CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                            onBackPress()
                        }) {
                            Icon(Icons.Default.Check, stringResource(id = R.string.apply_changes), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- 1. General Section ---
            SectionTitle(text = stringResource(id = R.string.general))
            Column(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
                .padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { showStartDayOfWeekDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.start_of_week), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(id = StartOfWeekOption.fromKey(pendingStartOfWeekKey).displayNameRes),
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
                            uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                            uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // --- 2. Appearance Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleColor = lerp(
                    start = CalendarioTheme.colors.cabecera,
                    stop = CalendarioTheme.colors.textSystem,
                    fraction = 0.4f
                )
                Text(
                    text = stringResource(id = R.string.customize_theme),
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))

                val lightThemeName = appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, null)
                val darkThemeName = appPrefs.getString(AppConstants.KEY_DARK_THEME_NAME, null)

                if (lightThemeName != null && lightThemeName == darkThemeName) {
                    Text(
                        text = lightThemeName,
                        color = titleColor,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp),
                        fontSize = 13.sp
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 16.dp)) {
                        lightThemeName?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.light_theme_prefix), color = titleColor, fontSize = 13.sp)
                                Text(it, color = titleColor, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                        darkThemeName?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.dark_theme_prefix), color = titleColor, fontSize = 13.sp)
                                Text(it, color = titleColor, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
                .padding(horizontal = 16.dp)) {
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { showThemeDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.mode), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(id = themeSetting.displayNameRes),
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                ActionRow(text = stringResource(id = R.string.customize_colors), onClick = onColorThemeClick)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(text = stringResource(id = R.string.predefined_themes)) { showBundledThemesDialog = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(stringResource(id = R.string.import_theme)) { importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(stringResource(id = R.string.export_theme)) { showExportDialog = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(stringResource(id = R.string.restore_default_colors)) { showRestoreDialog = true }
            }

            // --- 3. Holidays Section ---
            SectionTitle(text = stringResource(id = R.string.holidays_section))
            Column(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
                .padding(horizontal = 16.dp)) {
                ActionRow(text = stringResource(id = R.string.holiday_manager_title), onClick = onHolidayManagerClick)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(text = stringResource(id = R.string.import_holidays)) { 
                    importHolidaysLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ActionRow(text = stringResource(id = R.string.export_holidays)) { 
                    exportHolidaysLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { 
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "festivos_locales.json")
                    })
                }
            }

            WidgetSectionTitle()
            Column(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CalendarioTheme.colors.fondoSecciones)
                .padding(horizontal = 16.dp)) {
                Text(stringResource(id = R.string.widget_event_count, pendingEventCount.roundToInt()), fontSize = 16.sp, modifier = Modifier.padding(top=16.dp), color = CalendarioTheme.colors.textSystem)
                Slider(value = pendingEventCount, onValueChange = { pendingEventCount = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                
                val textBoostValue = pendingTextBoost.roundToInt()
                val textBoostLabel = when {
                    textBoostValue > 0 -> "+${textBoostValue}"
                    else -> textBoostValue.toString()
                }
                Text("${stringResource(id = R.string.widget_text_adjustment)}: $textBoostLabel", fontSize = 16.sp, modifier = Modifier.padding(top=8.dp), color = CalendarioTheme.colors.textSystem)
                
                Slider(value = pendingTextBoost, onValueChange = { pendingTextBoost = it }, valueRange = -2f..2f, steps = 3, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { showFontFamilyDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.font), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val fontFamilyDisplay = when(pendingFontFamily) {
                        WidgetConstants.FONT_FAMILY_SERIF -> stringResource(id = R.string.font_serif)
                        WidgetConstants.FONT_FAMILY_MONOSPACE -> stringResource(id = R.string.font_monospace)
                        WidgetConstants.FONT_FAMILY_CONDENSED -> stringResource(id = R.string.font_condensed)
                        WidgetConstants.FONT_FAMILY_SANS_SERIF -> stringResource(id = R.string.font_sans_serif)
                        else -> stringResource(id = R.string.font_system)
                    }
                    Text(
                        text = fontFamilyDisplay,
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                Text("${AboutInfo.LINE_1} ${AboutInfo.getVersionName(context)}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Text(AboutInfo.LINE_2, fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Text(AboutInfo.getLine3(), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeSetting,
            onThemeSelected = { themeManager.setTheme(it); showThemeDialog = false },
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

    if (showFontFamilyDialog) {
        FontFamilySelectionDialog(
            currentSelection = pendingFontFamily,
            onOptionSelected = {
                pendingFontFamily = it
                showFontFamilyDialog = false
            },
            onDismiss = { showFontFamilyDialog = false }
        )
    }

    if (showBundledThemesDialog) {
        BundledThemesDialog(
            currentThemeName = lightThemeName, 
            onDismiss = { showBundledThemesDialog = false },
            onThemeSelected = { theme ->
                showBundledThemesDialog = false
                val manifest = JSONObject(theme["themeManifest"] as Map<*, *>)
                val lightTheme = theme["lightTheme"]?.let { JSONObject(it as Map<*, *>) }
                val darkTheme = theme["darkTheme"]?.let { JSONObject(it as Map<*, *>) }
                val parsedTheme = ParsedTheme(manifest, lightTheme, darkTheme)
                val themeName = manifest.optString("name", "")

                ThemePersistence.applyTheme(context, parsedTheme, themeName)
                onThemeImported()
                Toast.makeText(context, R.string.theme_imported_successfully, Toast.LENGTH_SHORT).show()
            }
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
                // --- Restore App Theme ---
                appPrefs.edit(commit = true) {
                    val keysToRemove = appPrefs.all.keys.filter { it.startsWith("light_") || it.startsWith("dark_") }
                    for (key in keysToRemove) {
                        remove(key)
                    }
                    remove(AppConstants.KEY_LIGHT_THEME_NAME)
                    remove(AppConstants.KEY_DARK_THEME_NAME)
                    putString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient")
                }
                onThemeUpdated() // This recomposes the whole app with default colors

                // --- Restore Widget Theme (UI Only) ---
                pendingEventColor = Color(WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
                pendingTodayEventColor = Color(WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
                pendingWidgetBackgroundColor = Color(WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)
                pendingFontFamily = WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option.key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = option.displayNameRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        if (option.key == currentSelectionKey) {
                            val checkColor = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) {
                                CalendarioTheme.colors.textSystem
                            } else {
                                CalendarioTheme.colors.cabecera
                            }
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.custom_selected), tint = checkColor)
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

@Composable
private fun FontFamilySelectionDialog(
    currentSelection: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        WidgetConstants.FONT_FAMILY_SYSTEM to R.string.font_system,
        WidgetConstants.FONT_FAMILY_SANS_SERIF to R.string.font_sans_serif,
        WidgetConstants.FONT_FAMILY_SERIF to R.string.font_serif,
        WidgetConstants.FONT_FAMILY_MONOSPACE to R.string.font_monospace,
        WidgetConstants.FONT_FAMILY_CONDENSED to R.string.font_condensed
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.font), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (key, labelRes) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = labelRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        if (key == currentSelection) {
                            val checkColor = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) {
                                CalendarioTheme.colors.textSystem
                            } else {
                                CalendarioTheme.colors.cabecera
                            }
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.custom_selected), tint = checkColor)
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

@Suppress("UNCHECKED_CAST")
@Composable
private fun BundledThemesDialog(
    currentThemeName: String?,
    onDismiss: () -> Unit,
    onThemeSelected: (Map<String, Any>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.themes_v6), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                items(BundledThemes.themes) { theme ->
                    val themeManifest = theme["themeManifest"] as Map<String, Any>
                    val themeName = themeManifest["name"] as String
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = themeName,
                            modifier = Modifier.weight(1f),
                            fontSize = 18.sp
                        )
                        if (themeName == currentThemeName) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.custom_selected), tint = CalendarioTheme.colors.textSystem)
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
