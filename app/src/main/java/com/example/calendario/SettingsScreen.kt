@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var permissionsUpdateTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    var lightThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, "Océano")) }
    var darkThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_DARK_THEME_NAME, "Océano")) }

    // --- Dialog States ---
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var showStartDayOfWeekDialog by remember { mutableStateOf(false) }
    var showBundledThemesDialog by remember { mutableStateOf(false) }
    var showFontFamilyDialog by remember { mutableStateOf(false) }
    var showImportHolidaysDialog by remember { mutableStateOf(false) }
    var pendingHolidaysUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showUnlinkAccountDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }

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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    val account = task.result
                    appPrefs.edit { putString("google_account_email", account?.email) }
                    permissionsUpdateTrigger++
                    Toast.makeText(context, R.string.theme_imported_successfully, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al vincular cuenta", Toast.LENGTH_SHORT).show()
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
    val originalAutoBackup = remember { appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false) }
    val originalBackupFreq = remember { appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "daily") ?: "daily" }
    val lastBackupTimestamp = remember(permissionsUpdateTrigger) { appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_TIME, 0L) }

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
    var pendingAutoBackup by remember { mutableStateOf(originalAutoBackup) }
    var pendingBackupFreq by remember { mutableStateOf(originalBackupFreq) }

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
                    pendingSnoozeInterval.roundToInt() != originalSnoozeInterval ||
                    pendingAutoBackup != originalAutoBackup ||
                    pendingBackupFreq != originalBackupFreq
        }
    }

    val backAction = {
        if (hasPendingChanges) showDiscardChangesDialog = true else onBackPress()
    }

    // --- Lógica de Refresco de Permisos al volver de Ajustes ---
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permissionsUpdateTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val calStatus = remember(permissionsUpdateTrigger) { PermissionChecker.getCalendarStatus(context) }
    val notifStatus = remember(permissionsUpdateTrigger) { PermissionChecker.getNotificationsStatus(context) }
    val alarmStatus = remember(permissionsUpdateTrigger) { PermissionChecker.getAlarmsStatus(context) }
    val driveStatus = remember(permissionsUpdateTrigger) { PermissionChecker.getGoogleDriveStatus(context) }

    val permissionPointColor = when {
        calStatus == PermissionStatus.DENIED -> Color.Red
        notifStatus == PermissionStatus.DENIED || alarmStatus == PermissionStatus.DENIED || driveStatus == PermissionStatus.DENIED -> Color(0xFFFFA500)
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
                                putBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, pendingAutoBackup)
                                putString(AppConstants.KEY_BACKUP_FREQUENCY, pendingBackupFreq)
                            }
                            if (pendingAutoBackup) BackupScheduler.scheduleBackup(context, pendingBackupFreq)
                            else BackupScheduler.cancelBackup(context)
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
                // 1. MODO
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showThemeDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.mode), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = stringResource(id = themeSetting.displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                
                // 2. PERMISOS
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showPermissionsDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.system_permissions), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(10.dp).background(permissionPointColor, CircleShape))
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 3. COMIENZA SEMANA
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showStartDayOfWeekDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.start_of_week), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = stringResource(id = StartOfWeekOption.fromKey(pendingStartOfWeekKey).displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 4. NÚMERO DE SEMANA (Switch al final)
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { pendingShowWeekNumber = !pendingShowWeekNumber }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(id = R.string.week_in_year_view), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Switch(checked = pendingShowWeekNumber, onCheckedChange = { pendingShowWeekNumber = it }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f), uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)))
                }
            }

            // --- 2. Estilo Section ---
            SectionTitle(text = stringResource(id = R.string.customize_theme))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showBundledThemesDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.predefined_themes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val titleColor = lerp(start = CalendarioTheme.colors.cabecera, stop = CalendarioTheme.colors.textSystem, fraction = 0.4f)
                    Text(text = truncateThemeName(lightThemeName!!, 20), color = titleColor, fontSize = 14.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(stringResource(id = R.string.export_theme)) { showExportDialog = true }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(stringResource(id = R.string.import_theme)) { importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(text = stringResource(id = R.string.customize_colors), onClick = onColorThemeClick)
            }

            // --- 3. Alarm Section ---
            SectionTitle(text = stringResource(id = R.string.alarm))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)) {
                    Text(text = stringResource(id = R.string.alarm_offset_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingAlarmOffset, onValueChange = { pendingAlarmOffset = it }, valueRange = 0f..60f, steps = 11, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = pendingAlarmOffset.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
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
                ActionRow(text = stringResource(id = R.string.holiday_manager_title), onClick = onHolidayManagerClick)
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(text = stringResource(id = R.string.import_holidays)) { importHolidaysLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(text = stringResource(id = R.string.export_holidays)) { exportHolidaysLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "festivos_locales.json") }) }
            }

            WidgetSectionTitle()
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)) {
                    Text(text = stringResource(id = R.string.widget_event_count), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingEventCount, onValueChange = { pendingEventCount = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = pendingEventCount.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 8.dp)) {
                    val textBoostLabel = if (pendingTextBoost.roundToInt() > 0) "+${pendingTextBoost.roundToInt()}" else pendingTextBoost.roundToInt().toString()
                    Text(stringResource(id = R.string.widget_text_adjustment), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = pendingTextBoost, onValueChange = { pendingTextBoost = it }, valueRange = -4f..4f, steps = 7, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)))
                        Text(text = textBoostLabel, modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showFontFamilyDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.font), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (pendingFontBold) CalendarioTheme.colors.cabecera.copy(alpha = 0.12f) else Color.Transparent)
                            .border(1.dp, if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.15f), CircleShape)
                            .clickable { pendingFontBold = !pendingFontBold },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FormatBold,
                            null,
                            tint = if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val fontFamilyDisplay = when(pendingFontFamily) {
                        WidgetConstants.FONT_FAMILY_SERIF -> stringResource(id = R.string.font_serif)
                        WidgetConstants.FONT_FAMILY_MONOSPACE -> stringResource(id = R.string.font_monospace)
                        WidgetConstants.FONT_FAMILY_CONDENSED -> stringResource(id = R.string.font_condensed)
                        WidgetConstants.FONT_FAMILY_SANS_SERIF -> stringResource(id = R.string.font_sans_serif)
                        else -> stringResource(id = R.string.font_system)
                    }
                    val currentFontFamily = when(pendingFontFamily) {
                        WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif
                        WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace
                        WidgetConstants.FONT_FAMILY_CONDENSED -> FontFamily(
                            android.graphics.Typeface.create(
                                "sans-serif-condensed",
                                if (pendingFontBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                            )
                        )
                        WidgetConstants.FONT_FAMILY_SANS_SERIF -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    }
                    Text(
                        text = fontFamilyDisplay,
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        textAlign = TextAlign.End,
                        fontWeight = if (pendingFontBold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = currentFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ColorPickerRow(stringResource(id = R.string.background_color), pendingWidgetBackgroundColor) { showWidgetBackgroundColorPalette = true }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ColorPickerRow(stringResource(id = R.string.event_color), pendingEventColor) { showWidgetEventColorPalette = true }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ColorPickerRow(stringResource(id = R.string.today_event_color), pendingTodayEventColor) { showWidgetTodayEventColorPalette = true }
            }

            // --- 5. Backup Section ---
            SectionTitle(text = stringResource(id = R.string.backup_section_title))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val accountEmail = remember(permissionsUpdateTrigger) { appPrefs.getString("google_account_email", null) }
                
                if (accountEmail == null) {
                    ActionRow(text = stringResource(id = R.string.link_google_account)) {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    }
                } else {
                    ActionRow(
                        text = stringResource(id = R.string.account_linked),
                        detail = accountEmail
                    ) {
                        showUnlinkAccountDialog = true
                    }
                    
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    
                    val freqLabel = when(pendingBackupFreq) {
                        "daily" -> stringResource(R.string.frequency_daily)
                        "weekly" -> stringResource(R.string.frequency_weekly)
                        "monthly" -> stringResource(R.string.frequency_monthly)
                        else -> pendingBackupFreq
                    }
                    SettingsRow(stringResource(id = R.string.backup_frequency), freqLabel) {
                        showFrequencyDialog = true
                    }
                    
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    
                    // Sincronizar Ahora con información de última copia integrada
                    val lastStr = if (lastBackupTimestamp == 0L) stringResource(R.string.never) 
                                 else java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                    .withZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.Instant.ofEpochMilli(lastBackupTimestamp))
                    
                    ActionRow(
                        text = stringResource(id = R.string.sync_now),
                        detail = stringResource(R.string.last_backup, lastStr)
                    ) {
                        scope.launch {
                            val account = GoogleSignIn.getLastSignedInAccount(context)
                            if (account != null) {
                                // 1. Poblar con datos frescos del sistema antes de subir
                                val selectedIds = withContext(kotlinx.coroutines.Dispatchers.IO) { loadSelectedCalendarIds(context) }
                                if (selectedIds.isNotEmpty()) {
                                    val freshEvents = withContext(kotlinx.coroutines.Dispatchers.IO) { 
                                        readFestivosFromCalendarsSync(context, selectedIds) 
                                    }
                                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        saveHistoryToDisk(context, freshEvents.values.flatten())
                                    }
                                }

                                // 2. Subir a Drive
                                val success = GoogleDriveHelper(context, account).uploadHistoryFile()
                                if (success) {
                                    appPrefs.edit { putLong(AppConstants.KEY_LAST_BACKUP_TIME, System.currentTimeMillis()) }
                                    permissionsUpdateTrigger++
                                    Toast.makeText(context, "Sincronizado con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error de Drive. Verifica tu cuenta.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(text = stringResource(id = R.string.export_full_backup)) { val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "copia_seguridad_calendario.json") }; exportFullBackupLauncher.launch(intent) }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(text = stringResource(id = R.string.import_full_backup)) { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }; importFullBackupLauncher.launch(intent) }
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
                Text("${stringResource(id = R.string.app_name)} ${AboutInfo.getVersionName(context)}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Text("${AboutInfo.LINE_2} > ${AboutInfo.getFormattedDate()}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${AboutInfo.LINE_3_AUTHOR} > ", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Text(stringResource(id = R.string.history), fontSize = 16.sp, color = Color(0xFF2196F3), modifier = Modifier.clickable { uriHandler.openUri(AboutInfo.URL_HISTORIAL) })
                }
            }
        }
    }

    if (showThemeDialog) { ThemeSelectionDialog(currentTheme = themeSetting, onThemeSelected = { themeManager.setTheme(it); showThemeDialog = false }, onDismiss = { showThemeDialog = false }) }
    if (showStartDayOfWeekDialog) { StartDayOfWeekDialog(currentSelectionKey = pendingStartOfWeekKey, onOptionSelected = { pendingStartOfWeekKey = it; showStartDayOfWeekDialog = false }, onDismiss = { showStartDayOfWeekDialog = false }) }
    if (showFontFamilyDialog) { FontFamilySelectionDialog(currentSelection = pendingFontFamily, onOptionSelected = { pendingFontFamily = it; showFontFamilyDialog = false }, onDismiss = { showFontFamilyDialog = false }) }
    if (showBundledThemesDialog) { BundledThemesDialog(currentThemeName = lightThemeName, onDismiss = { showBundledThemesDialog = false }, onThemeSelected = { theme -> showBundledThemesDialog = false; val manifest = JSONObject(theme["themeManifest"] as Map<*, *>); val lightTheme = theme["lightTheme"]?.let { JSONObject(it as Map<*, *>) }; val darkTheme = theme["darkTheme"]?.let { JSONObject(it as Map<*, *>) }; ThemePersistence.applyTheme(context, ParsedTheme(manifest, lightTheme, darkTheme), manifest.optString("name", "")); onThemeImported() }) }
    if (showFrequencyDialog) {
        BackupFrequencyDialog(
            selection = pendingBackupFreq,
            onSelected = { pendingBackupFreq = it; showFrequencyDialog = false },
            onDismiss = { showFrequencyDialog = false }
        )
    }
    if (showWidgetEventColorPalette) { AdvancedColorPickerDialog(initialColor = pendingEventColor, onDismissRequest = { showWidgetEventColorPalette = false }, onColorConfirm = { pendingEventColor = it; showWidgetEventColorPalette = false }) }
    if (showWidgetTodayEventColorPalette) { AdvancedColorPickerDialog(initialColor = pendingTodayEventColor, onDismissRequest = { showWidgetTodayEventColorPalette = false }, onColorConfirm = { pendingTodayEventColor = it; showWidgetTodayEventColorPalette = false }) }
    if (showWidgetBackgroundColorPalette) { AdvancedColorPickerDialog(initialColor = pendingWidgetBackgroundColor, onDismissRequest = { showWidgetBackgroundColorPalette = false }, onColorConfirm = { pendingWidgetBackgroundColor = it; showWidgetBackgroundColorPalette = false }) }
    if (showDiscardChangesDialog) { AlertDialog(onDismissRequest = { showDiscardChangesDialog = false }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold) }, text = { Text(stringResource(id = R.string.discard_changes_confirmation)) }, confirmButton = { Button(onClick = { showDiscardChangesDialog = false; onBackPress() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(stringResource(id = R.string.discard)) } }, dismissButton = { TextButton(onClick = { showDiscardChangesDialog = false }) { Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem) } }) }
    if (showUnlinkAccountDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkAccountDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.unlink_google_account), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.unlink_account_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                            appPrefs.edit { remove("google_account_email") }
                            permissionsUpdateTrigger++
                            showUnlinkAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(id = R.string.unlink_action), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkAccountDialog = false }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            }
        )
    }

    if (showPermissionsDialog) {
        PermissionsDialog(
            calStatus = calStatus,
            notifStatus = notifStatus,
            alarmStatus = alarmStatus,
            driveStatus = driveStatus,
            onDismiss = { showPermissionsDialog = false },
            onFix = { type ->
                if (type == "drive") {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
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
    driveStatus: PermissionStatus,
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
                PermissionRow(stringResource(id = R.string.google_drive_permission_label), driveStatus) { onFix("drive") }
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
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CalendarioTheme.colors.textSystem, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Text(value, color = CalendarioTheme.colors.textSystem.copy(0.7f), fontSize = 16.sp)
    }
}

@Composable
private fun BackupFrequencyDialog(
    selection: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("daily" to R.string.frequency_daily, "weekly" to R.string.frequency_weekly, "monthly" to R.string.frequency_monthly)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.backup_frequency), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (key, labelRes) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                        if (key == selection) Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera)
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
private fun PermissionRow(label: String, status: PermissionStatus, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // El punto de color se mantiene arriba junto a la primera línea de texto
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(if (status == PermissionStatus.GRANTED) Color.Green else Color.Red, CircleShape)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = CalendarioTheme.colors.textSystem,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
            
            if (status == PermissionStatus.DENIED) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.fix_permission),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onFix() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
