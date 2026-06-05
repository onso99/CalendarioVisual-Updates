package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
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
import androidx.compose.runtime.mutableIntStateOf
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit,
    themeManager: ThemeManager,
    onColorThemeClick: () -> Unit,
    onHolidayManagerClick: () -> Unit,
    onRefreshData: () -> Unit,
    onThemeUpdated: () -> Unit,
    onLogClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val typography = MaterialTheme.typography
    val appPrefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }

    var lightThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, "Océano")) }
    var darkThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_DARK_THEME_NAME, "Océano")) }

    // --- Dialog States ---
    var showLegacyThemeDialog by remember { mutableStateOf<Pair<ParsedTheme, String>?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var showStartDayOfWeekDialog by remember { mutableStateOf(false) }
    var showBundledThemesDialog by remember { mutableStateOf(false) }
    var showFontFamilyDialog by remember { mutableStateOf(false) }
    var showImportHolidaysDialog by remember { mutableStateOf(false) }
    var pendingHolidaysUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

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
                    val currentAdjustments = loadHolidayAdjustments(context)
                    if (currentAdjustments.isNotEmpty()) {
                        pendingHolidaysUri = uri
                        showImportHolidaysDialog = true
                    } else {
                        try {
                            if (importHolidaysFromJson(context, uri, replace = true)) {
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
        }
    )

    val exportHolidaysLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    result.data?.data?.let { uri ->
                        exportHolidaysToJson(context, uri)
                        Toast.makeText(context, R.string.theme_exported_successfully, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Error exporting holidays", e)
                    Toast.makeText(context, R.string.error_saving_holidays_file, Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    val exportFullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    BackupManager.exportFullBackup(context, uri)
                }
            }
        }
    )

    val importFullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    BackupManager.importFullBackup(context, uri) {
                        (context as? Activity)?.let { activity ->
                            val intent = activity.intent
                            activity.finish()
                            activity.startActivity(intent)
                        }
                    }
                }
            }
        }
    )

    // --- States ---
    val themeSetting by themeManager.themeSetting.collectAsState()
    val originalShowWeekNumber = remember { appPrefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false) }
    val originalStartOfWeekKey = remember { appPrefs.getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key) ?: StartOfWeekOption.SYSTEM.key }
    val originalEventCount = remember {
        try {
            widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
        } catch (_: ClassCastException) {
            val value = widgetPrefs.all[WidgetConstants.KEY_EVENT_COUNT]
            (value as? Number)?.toInt() ?: WidgetConstants.DEFAULT_EVENT_COUNT
        }
    }
    val originalTextBoost = remember {
        try {
            widgetPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f)
        } catch (_: ClassCastException) {
            val value = widgetPrefs.all[WidgetConstants.KEY_WIDGET_TEXT_BOOST]
            (value as? Number)?.toFloat() ?: 0f
        }
    }
    val originalEventColor = remember {
        val colorInt = try {
            widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (widgetPrefs.all[WidgetConstants.KEY_WIDGET_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB
        }
        Color(colorInt)
    }
    val originalTodayEventColor = remember {
        val colorInt = try {
            widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (widgetPrefs.all[WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB
        }
        Color(colorInt)
    }
    val originalWidgetBackgroundColor = remember {
        val colorInt = try {
            widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)
        } catch (_: ClassCastException) {
            (widgetPrefs.all[WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR] as? Number)?.toInt() ?: WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB
        }
        Color(colorInt)
    }
    val originalFontFamily = remember { widgetPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY }
    val originalFontBold = remember { widgetPrefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD) }

    val originalAlarmOffset = remember { appPrefs.getInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, 20) }
    val originalSnoozeInterval = remember { appPrefs.getInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, 10) }

    var pendingShowWeekNumber by remember { mutableStateOf(originalShowWeekNumber) }
    var pendingStartOfWeekKey by remember { mutableStateOf(originalStartOfWeekKey) }
    var pendingEventCount by remember { mutableFloatStateOf(originalEventCount.toFloat()) }
    var pendingTextBoost by remember { mutableFloatStateOf(originalTextBoost) }
    var pendingEventColor by remember { mutableStateOf(originalEventColor) }
    var pendingTodayEventColor by remember { mutableStateOf(originalTodayEventColor) }
    var pendingWidgetBackgroundColor by remember { mutableStateOf(originalWidgetBackgroundColor) }
    var pendingFontFamily by remember { mutableStateOf(originalFontFamily) }
    var pendingFontBold by remember { mutableStateOf(originalFontBold) }
    
    var pendingAlarmOffset by remember { mutableFloatStateOf(originalAlarmOffset.toFloat()) }
    var pendingSnoozeInterval by remember { mutableFloatStateOf(originalSnoozeInterval.toFloat()) }

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
                    pendingFontFamily != originalFontFamily ||
                    pendingFontBold != originalFontBold ||
                    pendingAlarmOffset.roundToInt() != originalAlarmOffset ||
                    pendingSnoozeInterval.roundToInt() != originalSnoozeInterval
        }
    }

    val backAction = {
        if (hasPendingChanges) {
            showDiscardChangesDialog = true
        } else {
            onBackPress()
        }
    }

    // --- Permisos Logic ---
    val calStatus = PermissionChecker.getCalendarStatus(context)
    val notifStatus = PermissionChecker.getNotificationsStatus(context)
    val alarmStatus = PermissionChecker.getAlarmsStatus(context)

    val permissionPointColor = when {
        calStatus == PermissionStatus.DENIED -> Color.Red
        notifStatus == PermissionStatus.DENIED || alarmStatus == PermissionStatus.DENIED -> Color(0xFFFFA500)
        else -> Color.Green
    }

    val dividerColor = CalendarioTheme.colors.settingsBackground
    val dividerThickness = 1.dp

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
                                putInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, pendingAlarmOffset.roundToInt())
                                putInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, pendingSnoozeInterval.roundToInt())
                            }
                            widgetPrefs.edit {
                                putInt(WidgetConstants.KEY_EVENT_COUNT, pendingEventCount.roundToInt())
                                putFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, pendingTextBoost)
                                putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, pendingEventColor.toArgb())
                                putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, pendingTodayEventColor.toArgb())
                                putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, pendingWidgetBackgroundColor.toArgb())
                                putString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, pendingFontFamily)
                                putBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, pendingFontBold)
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
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 12.dp).clickable { showStartDayOfWeekDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.start_of_week), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = stringResource(id = StartOfWeekOption.fromKey(pendingStartOfWeekKey).displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Row(modifier = Modifier.fillMaxWidth().clickable { pendingShowWeekNumber = !pendingShowWeekNumber }.padding(horizontal = 16.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(id = R.string.week_in_year_view), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Switch(checked = pendingShowWeekNumber, onCheckedChange = { pendingShowWeekNumber = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)))
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 12.dp).clickable { showThemeDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.mode), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = stringResource(id = themeSetting.displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 12.dp).clickable { showPermissionsDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.system_permissions), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(10.dp).background(permissionPointColor, CircleShape))
                }
            }

            // --- 2. Estilo Section ---
            SectionTitle(text = stringResource(id = R.string.customize_theme))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 12.dp).clickable { showBundledThemesDialog = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.predefined_themes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val titleColor = lerp(start = CalendarioTheme.colors.cabecera, stop = CalendarioTheme.colors.textSystem, fraction = 0.4f)
                    Text(text = truncateThemeName(lightThemeName!!, 20), color = titleColor, fontSize = 14.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(stringResource(id = R.string.export_theme)) { showExportDialog = true } }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(stringResource(id = R.string.import_theme)) { importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) } }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(text = stringResource(id = R.string.customize_colors), onClick = onColorThemeClick) }
            }

            // --- 3. Alarm Section ---
            SectionTitle(text = stringResource(id = R.string.alarm))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    Text(text = stringResource(id = R.string.alarm_offset_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingAlarmOffset, onValueChange = { pendingAlarmOffset = it }, valueRange = 0f..60f, steps = 11, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = pendingAlarmOffset.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                // SIN LÍNEA ENTRE LOS APARTADOS DE ALARMA PARA MANTENER SIMETRÍA
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp)) {
                    Text(text = stringResource(id = R.string.snooze_interval_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingSnoozeInterval, onValueChange = { pendingSnoozeInterval = it }, valueRange = 5f..30f, steps = 4, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = pendingSnoozeInterval.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
            }

            // --- 4. Holidays Section ---
            SectionTitle(text = stringResource(id = R.string.holidays_section))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(text = stringResource(id = R.string.holiday_manager_title), onClick = onHolidayManagerClick) }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(text = stringResource(id = R.string.import_holidays)) { importHolidaysLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) } }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(text = stringResource(id = R.string.export_holidays)) { exportHolidaysLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "festivos_locales.json") }) } }
            }

            WidgetSectionTitle()
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    Text(text = stringResource(id = R.string.widget_event_count), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingEventCount, onValueChange = { pendingEventCount = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = pendingEventCount.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
                    val textBoostLabel = if (pendingTextBoost.roundToInt() > 0) "+${pendingTextBoost.roundToInt()}" else pendingTextBoost.roundToInt().toString()
                    Text(stringResource(id = R.string.widget_text_adjustment), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingTextBoost, onValueChange = { pendingTextBoost = it }, valueRange = -4f..4f, steps = 7, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = textBoostLabel, modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.font), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    IconButton(onClick = { pendingFontBold = !pendingFontBold }, modifier = Modifier.size(36.dp).padding(start = 8.dp)) {
                        Icon(Icons.Default.FormatBold, "Bold", tint = if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val fontFamilyDisplay = when(pendingFontFamily) {
                        WidgetConstants.FONT_FAMILY_SERIF -> stringResource(id = R.string.font_serif)
                        WidgetConstants.FONT_FAMILY_MONOSPACE -> stringResource(id = R.string.font_monospace)
                        WidgetConstants.FONT_FAMILY_CONDENSED -> stringResource(id = R.string.font_condensed)
                        WidgetConstants.FONT_FAMILY_SANS_SERIF -> stringResource(id = R.string.font_sans_serif)
                        else -> stringResource(id = R.string.font_system)
                    }
                    Text(fontFamilyDisplay, color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, modifier = Modifier.weight(1f).clickable { showFontFamilyDialog = true })
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp)) { ColorPickerRow(stringResource(id = R.string.background_color), pendingWidgetBackgroundColor) { showWidgetBackgroundColorPalette = true } }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp)) { ColorPickerRow(stringResource(id = R.string.event_color), pendingEventColor) { showWidgetEventColorPalette = true } }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp)) { ColorPickerRow(stringResource(id = R.string.today_event_color), pendingTodayEventColor) { showWidgetTodayEventColorPalette = true } }
            }

            // --- 5. Backup Section ---
            SectionTitle(text = stringResource(id = R.string.backup_section_title))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(text = stringResource(id = R.string.export_full_backup)) { val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "copia_seguridad_calendario.json") }; exportFullBackupLauncher.launch(intent) } }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { ActionRow(text = stringResource(id = R.string.import_full_backup)) { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }; importFullBackupLauncher.launch(intent) } }
            }

            // --- About Section ---
            var loggingEnabled by remember { mutableStateOf(LogCollector.isLoggingEnabled(context)) }
            var debugClickCount by remember { mutableIntStateOf(0) }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val titleColor = lerp(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.textSystem, 0.4f)
                Text(stringResource(id = R.string.about), style = typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                IconButton(onClick = { if (loggingEnabled) onLogClick() else { debugClickCount++; if (debugClickCount >= 7) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); loggingEnabled = true; LogCollector.setLoggingEnabled(context, true); debugClickCount = 0 } } }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.BugReport, null, tint = if (loggingEnabled) CalendarioTheme.colors.textSystem else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.combinedClickable(onClick = { if (loggingEnabled) onLogClick() else { debugClickCount++; if (debugClickCount >= 7) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); loggingEnabled = true; LogCollector.setLoggingEnabled(context, true); debugClickCount = 0 } } }, onLongClick = { if (loggingEnabled) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); loggingEnabled = false; LogCollector.setLoggingEnabled(context, false); debugClickCount = 0 } }))
                }
            }
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(16.dp)) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Text("${AboutInfo.LINE_1} ${AboutInfo.getVersionName(context)}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Text("${AboutInfo.LINE_2} > ${AboutInfo.getFormattedDate()}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${AboutInfo.LINE_3_AUTHOR} > ", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Text(AboutInfo.HISTORY_LABEL, fontSize = 16.sp, color = Color(0xFF2196F3), modifier = Modifier.clickable { uriHandler.openUri(AboutInfo.URL_HISTORIAL) })
                }
            }
        }
    }

    if (showThemeDialog) { ThemeSelectionDialog(currentTheme = themeSetting, onThemeSelected = { themeManager.setTheme(it); showThemeDialog = false }, onDismiss = { showThemeDialog = false }) }
    if (showStartDayOfWeekDialog) { StartDayOfWeekDialog(currentSelectionKey = pendingStartOfWeekKey, onOptionSelected = { pendingStartOfWeekKey = it; showStartDayOfWeekDialog = false }, onDismiss = { showStartDayOfWeekDialog = false }) }
    if (showFontFamilyDialog) { FontFamilySelectionDialog(currentSelection = pendingFontFamily, onOptionSelected = { pendingFontFamily = it; showFontFamilyDialog = false }, onDismiss = { showFontFamilyDialog = false }) }
    if (showBundledThemesDialog) { BundledThemesDialog(currentThemeName = lightThemeName, onDismiss = { showBundledThemesDialog = false }, onThemeSelected = { theme -> showBundledThemesDialog = false; val manifest = JSONObject(theme["themeManifest"] as Map<*, *>); val lightTheme = theme["lightTheme"]?.let { JSONObject(it as Map<*, *>) }; val darkTheme = theme["darkTheme"]?.let { JSONObject(it as Map<*, *>) }; ThemePersistence.applyTheme(context, ParsedTheme(manifest, lightTheme, darkTheme), manifest.optString("name", "")); onThemeImported() }) }
    if (showWidgetEventColorPalette) { AdvancedColorPickerDialog(initialColor = pendingEventColor, onDismissRequest = { showWidgetEventColorPalette = false }, onColorConfirm = { pendingEventColor = it; showWidgetEventColorPalette = false }) }
    if (showWidgetTodayEventColorPalette) { AdvancedColorPickerDialog(initialColor = pendingTodayEventColor, onDismissRequest = { showWidgetTodayEventColorPalette = false }, onColorConfirm = { pendingTodayEventColor = it; showWidgetTodayEventColorPalette = false }) }
    if (showWidgetBackgroundColorPalette) { AdvancedColorPickerDialog(initialColor = pendingWidgetBackgroundColor, onDismissRequest = { showWidgetBackgroundColorPalette = false }, onColorConfirm = { pendingWidgetBackgroundColor = it; showWidgetBackgroundColorPalette = false }) }
    if (showDiscardChangesDialog) { AlertDialog(onDismissRequest = { showDiscardChangesDialog = false }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold) }, text = { Text(stringResource(id = R.string.discard_changes_confirmation)) }, confirmButton = { Button(onClick = { showDiscardChangesDialog = false; onBackPress() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(stringResource(id = R.string.discard)) } }, dismissButton = { TextButton(onClick = { showDiscardChangesDialog = false }) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } }) }
    if (showPermissionsDialog) {
        PermissionsDialog(
            calStatus = calStatus,
            notifStatus = notifStatus,
            alarmStatus = alarmStatus,
            onDismiss = { showPermissionsDialog = false },
            onFix = { type -> 
                when (type) { 
                    "calendar", "notifications" -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) })
                    "alarms" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.fromParts("package", context.packageName, null) }) else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }) 
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

    if (showImportHolidaysDialog && pendingHolidaysUri != null) {
        AlertDialog(
            onDismissRequest = { showImportHolidaysDialog = false; pendingHolidaysUri = null },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.import_holidays_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.import_holidays_confirm_message)) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (importHolidaysFromJson(context, pendingHolidaysUri!!, replace = false)) {
                                Toast.makeText(context, R.string.holidays_imported_successfully, Toast.LENGTH_SHORT).show()
                                onRefreshData()
                            }
                            showImportHolidaysDialog = false
                            pendingHolidaysUri = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
                    ) {
                        Text(stringResource(id = R.string.import_holidays_merge))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (importHolidaysFromJson(context, pendingHolidaysUri!!, replace = true)) {
                                Toast.makeText(context, R.string.holidays_imported_successfully, Toast.LENGTH_SHORT).show()
                                onRefreshData()
                            }
                            showImportHolidaysDialog = false
                            pendingHolidaysUri = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(stringResource(id = R.string.import_holidays_replace), color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showImportHolidaysDialog = false; pendingHolidaysUri = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                    }
                }
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
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.export_theme_title), fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(id = R.string.theme_name)) }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) }, confirmButton = { Button(onClick = { onConfirm(text.ifBlank { "nuevo_tema" }) }, enabled = text.isNotBlank()) { Text(stringResource(id = R.string.export)) } }, dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) } })
}

@Composable
private fun FontFamilySelectionDialog(
    currentSelection: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(WidgetConstants.FONT_FAMILY_SYSTEM to R.string.font_system, WidgetConstants.FONT_FAMILY_SANS_SERIF to R.string.font_sans_serif, WidgetConstants.FONT_FAMILY_SERIF to R.string.font_serif, WidgetConstants.FONT_FAMILY_MONOSPACE to R.string.font_monospace, WidgetConstants.FONT_FAMILY_CONDENSED to R.string.font_condensed)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.font), fontWeight = FontWeight.Bold) }, text = { Column { options.forEach { (key, labelRes) -> Row(Modifier.fillMaxWidth().clickable { onOptionSelected(key) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp); if (key == currentSelection) Icon(Icons.Default.Check, null, tint = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) CalendarioTheme.colors.textSystem else CalendarioTheme.colors.cabecera) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } })
}

@Composable
private fun StartDayOfWeekDialog(
    currentSelectionKey: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.start_of_week), fontWeight = FontWeight.Bold) }, text = { Column { StartOfWeekOption.entries.forEach { option -> Row(Modifier.fillMaxWidth().clickable { onOptionSelected(option.key) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(id = option.displayNameRes), modifier = Modifier.weight(1f), fontSize = 16.sp); if (option.key == currentSelectionKey) Icon(Icons.Default.Check, null, tint = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) CalendarioTheme.colors.textSystem else CalendarioTheme.colors.cabecera) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } })
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun BundledThemesDialog(
    currentThemeName: String?,
    onDismiss: () -> Unit,
    onThemeSelected: (Map<String, Any>) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.themes_v6), fontWeight = FontWeight.Bold) }, text = { LazyColumn { items(BundledThemes.themes) { theme: Map<String, Any> -> val themeManifest = theme["themeManifest"] as Map<String, Any>; val themeName = themeManifest["name"] as String; Row(Modifier.fillMaxWidth().clickable { onThemeSelected(theme) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(themeName, modifier = Modifier.weight(1f), fontSize = 18.sp); if (themeName == currentThemeName) Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.textSystem) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } })
}

private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "nombre_desconocido"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor -> if (cursor.moveToFirst()) { val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (nameIndex != -1) fileName = cursor.getString(nameIndex) } }
    return fileName.substringBeforeLast('.')
}

fun truncateThemeName(name: String, limit: Int): String = if (name.length > limit) name.take(limit - 3) + "..." else name

@Composable
private fun PermissionsDialog(
    calStatus: PermissionStatus,
    notifStatus: PermissionStatus,
    alarmStatus: PermissionStatus,
    onDismiss: () -> Unit,
    onFix: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.permissions_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionRow(stringResource(id = R.string.calendar_permission_label), calStatus) { onFix("calendar") }
                PermissionRow(stringResource(id = R.string.notifications_permission_label), notifStatus) { onFix("notifications") }
                PermissionRow(stringResource(id = R.string.alarms_permission_label), alarmStatus) { onFix("alarms") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close), color = CalendarioTheme.colors.textSystem)
            }
        }
    )
}

@Composable
private fun PermissionRow(label: String, status: PermissionStatus, onFix: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (status == PermissionStatus.GRANTED) Color.Green else Color.Red, CircleShape)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = CalendarioTheme.colors.textSystem,
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
        if (status == PermissionStatus.DENIED) {
            TextButton(onClick = onFix) {
                Text(stringResource(id = R.string.fix_permission), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
