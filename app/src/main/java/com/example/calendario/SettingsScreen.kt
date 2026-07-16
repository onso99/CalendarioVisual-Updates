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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.res.painterResource
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
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

enum class StartOfWeekOption(val key: String, val displayNameRes: Int) {
    SYSTEM("SYSTEM", R.string.system_default),
    MONDAY("MONDAY", R.string.monday),
    SUNDAY("SUNDAY", R.string.sunday),
    SATURDAY("SATURDAY", R.string.saturday);

    companion object {
        fun fromKey(key: String): StartOfWeekOption {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit,
    themeManager: ThemeManager,
    viewModel: CalendarioViewModel, // Cambiado para recibir el ViewModel
    onColorThemeClick: () -> Unit,
    onHolidayManagerClick: () -> Unit,
    onThemeUpdated: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onLogClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val typography = MaterialTheme.typography
    val appPrefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val widgetPrefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }
    var permissionsUpdateTrigger by remember { mutableIntStateOf(0) }

    var lightThemeName by remember { mutableStateOf(appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, "theme_1")) }

    // --- Dialog States ---
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var showWeekConfigDialog by remember { mutableStateOf(false) }
    var showAlarmConfigDialog by remember { mutableStateOf(false) }
    var showBundledThemesDialog by remember { mutableStateOf(false) }
    var showFontFamilyDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showUnlinkAccountDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showRestoreDriveDialog by remember { mutableStateOf(false) }
    var showPreferencesBackupDialog by remember { mutableStateOf(false) }
    var showEventBackupDialog by remember { mutableStateOf(false) }
    var showWidgetColorExpand by remember { mutableStateOf(false) }

    // --- Launchers ---
    val onThemeImported = {
        lightThemeName = appPrefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, null)
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
                        if (ThemePersistence.exportThemeToJson(context, uri, newName)) {
                            // Al guardar con éxito, el tema "oficial" pasa a ser el nuevo nombre (sin asteriscos)
                            appPrefs.edit {
                                putString(AppConstants.KEY_LIGHT_THEME_NAME, newName)
                                putString(AppConstants.KEY_DARK_THEME_NAME, newName)
                            }
                            onThemeImported()
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error exporting theme", e)
                        Toast.makeText(context, R.string.error_saving_theme_file, Toast.LENGTH_LONG).show()
                    }
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
                    Toast.makeText(context, R.string.account_linked_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.account_linked_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // --- States ---
    val themeSetting by themeManager.themeSetting.collectAsState()
    val originalShowWeekNumber = remember { appPrefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false) }
    val originalStartOfWeekKey = remember { 
        val raw = appPrefs.getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key) ?: StartOfWeekOption.SYSTEM.key
        StartOfWeekOption.fromKey(raw).key
    }
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
    
    // Si no está habilitado el backup, forzamos a que la frecuencia original se lea como "manual"
    val originalAutoBackup = remember { appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false) }
    val originalBackupFreq = remember { 
        if (!originalAutoBackup) "manual" 
        else appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "manual") ?: "manual"
    }
    val lastBackupTimestamp = remember(permissionsUpdateTrigger) { appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_TIME, 0L) }
    val lastBackupCount = remember(permissionsUpdateTrigger) { appPrefs.getInt(AppConstants.KEY_LAST_BACKUP_COUNT, 0) }

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
    val batteryStatus = remember(permissionsUpdateTrigger) { PermissionChecker.getBatteryOptimizationStatus(context) }

    val permissionPointColor = when {
        calStatus == PermissionStatus.DENIED -> Color.Red
        notifStatus == PermissionStatus.DENIED || alarmStatus == PermissionStatus.DENIED || driveStatus == PermissionStatus.DENIED || batteryStatus == PermissionStatus.DENIED -> Color(0xFFFFA500)
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
                                
                                val isNowEnabled = pendingBackupFreq != "manual"
                                putBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, isNowEnabled)
                                putString(AppConstants.KEY_BACKUP_FREQUENCY, if (isNowEnabled) pendingBackupFreq else "manual")
                            }
                            if (pendingBackupFreq != "manual") BackupScheduler.scheduleBackup(context, pendingBackupFreq)
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
                    Text(
                        text = stringResource(id = themeSetting.displayNameRes), 
                        color = CalendarioTheme.colors.textSystem, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                
                // 2. PERMISOS
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showPermissionsDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.system_permissions), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(10.dp).background(permissionPointColor, CircleShape))
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 3. SEMANA (Fila unificada - Sin flecha porque tiene valor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showWeekConfigDialog = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.semana_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val startDayName = stringResource(id = StartOfWeekOption.fromKey(pendingStartOfWeekKey).displayNameRes)
                    val weekNumberInfo = if (pendingShowWeekNumber) " (123)" else ""
                    
                    Text(
                        text = "$startDayName$weekNumberInfo", 
                        color = CalendarioTheme.colors.textSystem, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 4. ALARMA (Fila unificada - Sin flecha porque tiene valor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showAlarmConfigDialog = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.alarm), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Anticipación
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_alarm_anticipation),
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        val anticipationVal = pendingAlarmOffset.roundToInt()
                        val anticipationSign = if (anticipationVal > 0) "-" else ""
                        Text(
                            text = "$anticipationSign$anticipationVal'",
                            modifier = Modifier.padding(start = 2.dp),
                            color = CalendarioTheme.colors.textSystem,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Posponer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_alarm_snooze),
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${pendingSnoozeInterval.roundToInt()}'",
                            modifier = Modifier.padding(start = 2.dp),
                            color = CalendarioTheme.colors.textSystem,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 5. FESTIVOS (Navegación al gestor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable(onClick = onHolidayManagerClick)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.holidays_section), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 6. COPIA DE PREFERENCIAS (Navegación al diálogo)
                ActionRow(
                    text = stringResource(id = R.string.preferences_backup_label),
                    onClick = { showPreferencesBackupDialog = true }
                )
            }

            // --- 2. Estilo Section ---
            SectionTitle(text = stringResource(id = R.string.customize_theme))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showBundledThemesDialog = true }
                        .padding(horizontal = 16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.predefined_themes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val currentThemeId = lightThemeName ?: "theme_1"
                    val isModified = currentThemeId.endsWith("***")
                    val cleanId = if (isModified) currentThemeId.removeSuffix("***") else currentThemeId
                    
                    val bundledTheme = BundledThemes.themes.find { theme ->
                        val manifest = theme["themeManifest"] as Map<*, *>
                        val id = manifest["id"] as? String
                        val legacyName = manifest["name"] as? String
                        id == cleanId || legacyName == cleanId
                    }
                    
                    val finalName = if (bundledTheme != null) {
                        val resId = (bundledTheme["themeManifest"] as Map<*, *>)["nameRes"] as Int
                        stringResource(id = resId) + (if (isModified) "***" else "")
                    } else {
                        currentThemeId
                    }

                    Text(
                        text = truncateThemeName(finalName, 20), 
                        color = CalendarioTheme.colors.textSystem, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                ActionRow(text = stringResource(id = R.string.customize_colors), onClick = onColorThemeClick)
            }

            // --- 3. Widget Section ---
            WidgetSectionTitle()
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)) {
                    Text(text = stringResource(id = R.string.widget_event_count), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = pendingEventCount,
                            onValueChange = { pendingEventCount = it },
                            valueRange = 1f..12f,
                            steps = 10,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CalendarioTheme.colors.cabecera,
                                activeTrackColor = CalendarioTheme.colors.cabecera,
                                inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)
                            )
                        )
                        Text(text = pendingEventCount.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 8.dp)) {
                    val textBoostLabel = if (pendingTextBoost.roundToInt() > 0) "+${pendingTextBoost.roundToInt()}" else pendingTextBoost.roundToInt().toString()
                    Text(stringResource(id = R.string.widget_text_adjustment), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = pendingTextBoost,
                            onValueChange = { pendingTextBoost = it },
                            valueRange = -4f..4f,
                            steps = 7,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CalendarioTheme.colors.cabecera,
                                activeTrackColor = CalendarioTheme.colors.cabecera,
                                inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)
                            )
                        )
                        Text(text = textBoostLabel, modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showFontFamilyDialog = true }.padding(horizontal = 16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        fontFamily = currentFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                
                // Nueva Fila Unificada de Colores del Widget
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { showWidgetColorExpand = !showWidgetColorExpand }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.widget_colors_label),
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            color = CalendarioTheme.colors.textSystem
                        )
                        
                        // Previsualización de los 3 colores (puntos separados para claridad total)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pendingWidgetBackgroundColor).border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.1f), CircleShape))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pendingEventColor).border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.1f), CircleShape))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pendingTodayEventColor).border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.1f), CircleShape))
                        }

                        Icon(
                            imageVector = if (showWidgetColorExpand) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                            modifier = Modifier.padding(start = 8.dp).size(20.dp)
                        )
                    }

                    if (showWidgetColorExpand) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // BOTÃ“N FONDO
                            WidgetColorChip(
                                label = stringResource(id = R.string.widget_color_fondo),
                                color = pendingWidgetBackgroundColor,
                                modifier = Modifier.weight(1f),
                                onClick = { showWidgetBackgroundColorPalette = true }
                            )
                            // BOTÃ“N EVENTO
                            WidgetColorChip(
                                label = stringResource(id = R.string.widget_color_evento),
                                color = pendingEventColor,
                                modifier = Modifier.weight(1f),
                                onClick = { showWidgetEventColorPalette = true }
                            )
                            // BOTÃ“N HOY
                            WidgetColorChip(
                                label = stringResource(id = R.string.widget_color_hoy),
                                color = pendingTodayEventColor,
                                modifier = Modifier.weight(1f),
                                onClick = { showWidgetTodayEventColorPalette = true }
                            )
                        }
                    }
                }
            }


            // --- 6. Backup Section ---
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
                    // 1. CUENTA (Fila con valor truncado)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { showUnlinkAccountDialog = true }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.account_label), 
                            color = CalendarioTheme.colors.textSystem, 
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = accountEmail,
                            color = CalendarioTheme.colors.textSystem,
                            fontSize = 14.sp, // Tamaño ligeramente menor
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    
                    val freqLabel = when(pendingBackupFreq) {
                        "manual" -> stringResource(R.string.frequency_manual)
                        "daily" -> stringResource(R.string.frequency_daily)
                        "weekly" -> stringResource(R.string.frequency_weekly)
                        "monthly" -> stringResource(R.string.frequency_monthly)
                        else -> pendingBackupFreq
                    }
                    
                    // 2. FRECUENCIA + INFO (Última copia debajo)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFrequencyDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(id = R.string.backup_frequency), 
                                color = CalendarioTheme.colors.textSystem, 
                                modifier = Modifier.weight(1f), 
                                fontSize = 16.sp
                            )
                            Text(
                                text = freqLabel,
                                color = CalendarioTheme.colors.textSystem,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End
                            )
                        }
                        
                        // Información de la última copia ubicada aquí
                        val lastStr = if (lastBackupTimestamp == 0L) stringResource(R.string.never) 
                                     else java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
                                        .withZone(java.time.ZoneId.systemDefault())
                                        .format(java.time.Instant.ofEpochMilli(lastBackupTimestamp))
                        
                        val detailText = if (lastBackupTimestamp == 0L) lastStr 
                                        else stringResource(R.string.last_backup_with_count, lastStr, lastBackupCount)
                        
                        Text(
                            text = detailText,
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                    // 3. GESTIÓN DE COPIA (Abre diálogo)
                    ActionRow(
                        text = stringResource(id = R.string.manage_backup_label),
                        onClick = { showEventBackupDialog = true }
                    )
                }
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
                Text("${stringResource(id = R.string.app_name)} ${AboutInfo.getVersionName(context)}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${AboutInfo.LINE_3_AUTHOR} > ${AboutInfo.getFormattedDate()} > ", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Text(
                        text = stringResource(id = R.string.history),
                        fontSize = 16.sp,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.clickable { onHistoryClick() }
                    )
                }
            }
        }
    }

    if (showThemeDialog) { ThemeSelectionDialog(currentTheme = themeSetting, onThemeSelected = { themeManager.setTheme(it); showThemeDialog = false }, onDismiss = { showThemeDialog = false }) }
    if (showWeekConfigDialog) {
        WeekConfigDialog(
            currentSelectionKey = pendingStartOfWeekKey,
            onOptionSelected = { pendingStartOfWeekKey = it },
            showWeekNumber = pendingShowWeekNumber,
            onWeekNumberChange = { pendingShowWeekNumber = it },
            onDismiss = { showWeekConfigDialog = false }
        )
    }

    if (showAlarmConfigDialog) {
        AlarmConfigDialog(
            anticipation = pendingAlarmOffset,
            onAnticipationChange = { pendingAlarmOffset = it },
            snooze = pendingSnoozeInterval,
            onSnoozeChange = { pendingSnoozeInterval = it },
            onDismiss = { showAlarmConfigDialog = false }
        )
    }
    if (showFontFamilyDialog) { FontFamilySelectionDialog(currentSelection = pendingFontFamily, onOptionSelected = { pendingFontFamily = it; showFontFamilyDialog = false }, onDismiss = { showFontFamilyDialog = false }) }
    if (showBundledThemesDialog) { 
        BundledThemesDialog(
            currentThemeId = lightThemeName, 
            onDismiss = { showBundledThemesDialog = false }, 
            onThemeSelected = { theme -> 
                showBundledThemesDialog = false
                try {
                    val themeManifest = theme["themeManifest"] as? Map<*, *> ?: return@BundledThemesDialog
                    val manifest = JSONObject(themeManifest)
                    val lightTheme = theme["lightTheme"]?.let { JSONObject(it as Map<*, *>) }
                    val darkTheme = theme["darkTheme"]?.let { JSONObject(it as Map<*, *>) }
                    // Priorizamos el ID para evitar el cierre por NullPointerException
                    val themeId = themeManifest["id"] as? String ?: "theme_1"
                    ThemePersistence.applyTheme(context, ParsedTheme(manifest, lightTheme, darkTheme), themeId)
                    onThemeImported()
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Error applying bundled theme", e)
                }
            },
            onLoadClick = {
                importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { 
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json" 
                })
            },
            onSaveClick = {
                showExportDialog = true
            }
        ) 
    }
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
    if (showRestoreDriveDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDriveDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.restore_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.restore_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDriveDialog = false
                        viewModel.restoreHistoryFromDrive(context) { success ->
                            if (success) {
                                Toast.makeText(context, R.string.restore_success, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
                ) {
                    Text(stringResource(id = R.string.restore_from_drive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDriveDialog = false }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            }
        )
    }

    if (showPreferencesBackupDialog) {
        PreferencesBackupDialog(
            onDismiss = { showPreferencesBackupDialog = false },
            onLoadClick = {
                showPreferencesBackupDialog = false
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { 
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json" 
                }
                importFullBackupLauncher.launch(intent)
            },
            onSaveClick = {
                showPreferencesBackupDialog = false
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { 
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "ajustes_aspecto_calendario.json") 
                }
                exportFullBackupLauncher.launch(intent)
            }
        )
    }

    if (showEventBackupDialog) {
        EventBackupDialog(
            onDismiss = { showEventBackupDialog = false },
            onRestoreClick = {
                showEventBackupDialog = false
                showRestoreDriveDialog = true
            },
            onSyncClick = {
                showEventBackupDialog = false
                viewModel.syncHistoryToDrive(context) { result ->
                    if (result.success) {
                        permissionsUpdateTrigger++
                        val msg = context.applicationContext.getString(R.string.sync_success_detailed, result.totalEvents, result.deletedCount)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, R.string.sync_error_drive, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

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
        // Forzamos un refresco inmediato al abrir y tras un pequeño delay
        LaunchedEffect(Unit) {
            permissionsUpdateTrigger++
            kotlinx.coroutines.delay(500.milliseconds)
            permissionsUpdateTrigger++
        }

        PermissionsDialog(
            calStatus = calStatus,
            notifStatus = notifStatus,
            alarmStatus = alarmStatus,
            driveStatus = driveStatus,
            batteryStatus = batteryStatus,
            onDismiss = { showPermissionsDialog = false },
            onFix = { type ->
                when (type) {
                    "drive" -> {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    }
                    "battery" -> {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                    else -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
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
private fun AlarmConfigDialog(
    anticipation: Float,
    onAnticipationChange: (Float) -> Unit,
    snooze: Float,
    onSnoozeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.alarm), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Bloque AnticipaciÃ³n
                Text(stringResource(id = R.string.alarm_anticipation_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = anticipation,
                        onValueChange = onAnticipationChange,
                        valueRange = 0f..120f,
                        steps = 23,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CalendarioTheme.colors.cabecera,
                            activeTrackColor = CalendarioTheme.colors.cabecera,
                            inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)
                        )
                    )
                    val anticipationVal = anticipation.roundToInt()
                    val anticipationSign = if (anticipationVal > 0) "-" else ""
                    Text(
                        text = "$anticipationSign$anticipationVal'", 
                        modifier = Modifier.width(52.dp).padding(start = 8.dp),
                        color = CalendarioTheme.colors.textSystem, 
                        textAlign = TextAlign.End, 
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Bloque Posponer
                Text(stringResource(id = R.string.alarm_snooze_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = snooze,
                        onValueChange = onSnoozeChange,
                        valueRange = 5f..60f,
                        steps = 10, // 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60 (11 posiciones)
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CalendarioTheme.colors.cabecera,
                            activeTrackColor = CalendarioTheme.colors.cabecera,
                            inactiveTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.24f)
                        )
                    )
                    Text(
                        text = "${snooze.roundToInt()}'", 
                        modifier = Modifier.width(44.dp).padding(start = 8.dp), 
                        color = CalendarioTheme.colors.textSystem, 
                        textAlign = TextAlign.End, 
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.accept), color = CalendarioTheme.colors.cabecera)
            }
        }
    )
}

@Composable
private fun WeekConfigDialog(
    currentSelectionKey: String,
    onOptionSelected: (String) -> Unit,
    showWeekNumber: Boolean,
    onWeekNumberChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.semana_label), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Selector de dÃ­a
                StartOfWeekOption.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option.key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = option.displayNameRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        if (option.key == currentSelectionKey) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background))
                                    CalendarioTheme.colors.textSystem
                                else
                                    CalendarioTheme.colors.cabecera
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Ajuste de nÃºmero de semana
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.week_in_year_view), fontSize = 16.sp)
                    Switch(
                        checked = showWeekNumber,
                        onCheckedChange = onWeekNumberChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CalendarioTheme.colors.cabecera,
                            checkedTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.54f),
                            uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                            uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f),
                            uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.accept), color = CalendarioTheme.colors.cabecera)
            }
        }
    )
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun BundledThemesDialog(
    currentThemeId: String?,
    onDismiss: () -> Unit,
    onThemeSelected: (Map<String, Any>) -> Unit,
    onLoadClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val effectiveId = currentThemeId ?: "theme_1"

    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem, 
        title = { 
            Text(
                text = stringResource(id = R.string.themes_v6), 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        }, 
        text = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(210.dp) // Reducimos un poco para dar aire al respaldo
                ) { 
                    items(BundledThemes.themes) { theme: Map<String, Any> -> 
                        val themeManifest = theme["themeManifest"] as Map<*, *>
                        val themeId = themeManifest["id"] as String
                        val themeResId = themeManifest["nameRes"] as Int
                        
                        val isSelected = themeId == effectiveId

                        ThemeChip(
                            name = stringResource(id = themeResId),
                            isSelected = isSelected,
                            onClick = { onThemeSelected(theme) }
                        )
                    } 
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Fila integrada de Respaldo dentro del diálogo con diferenciación visual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val backupButtonBg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                    
                    SettingsActionChip(
                        text = stringResource(id = R.string.cargar_label),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = backupButtonBg,
                        onClick = onLoadClick
                    )
                    
                    SettingsActionChip(
                        text = stringResource(id = R.string.guardar_label),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = backupButtonBg,
                        onClick = onSaveClick
                    )
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
private fun ThemeChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        CalendarioTheme.colors.cabecera
    } else {
        CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
    }
    
    val bgColor = if (isSelected) {
        CalendarioTheme.colors.cabecera.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = CalendarioTheme.colors.textSystem,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun EventBackupDialog(
    onDismiss: () -> Unit,
    onRestoreClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem, 
        title = { 
            Text(
                text = stringResource(id = R.string.event_backup_dialog_title), 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        }, 
        text = { 
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val backupButtonBg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                
                SettingsActionChip(
                    text = stringResource(id = R.string.restaurar_label),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = backupButtonBg,
                    onClick = onRestoreClick
                )
                
                SettingsActionChip(
                    text = stringResource(id = R.string.sincronizar_label),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = backupButtonBg,
                    onClick = onSyncClick
                )
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
private fun PreferencesBackupDialog(
    onDismiss: () -> Unit,
    onLoadClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem, 
        title = { 
            Text(
                text = stringResource(id = R.string.preferences_title), 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        }, 
        text = { 
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val backupButtonBg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                
                SettingsActionChip(
                    text = stringResource(id = R.string.cargar_label),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = backupButtonBg,
                    onClick = onLoadClick
                )
                
                SettingsActionChip(
                    text = stringResource(id = R.string.guardar_label),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = backupButtonBg,
                    onClick = onSaveClick
                )
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
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor -> if (cursor.moveToFirst()) { val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (nameIndex != -1) fileName = cursor.getString(nameIndex) } }
    return fileName.substringBeforeLast('.')
}

fun truncateThemeName(name: String, limit: Int): String = if (name.length > limit) name.take(limit - 3) + "..." else name

@Composable
private fun WidgetColorChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Calculamos color de texto (Blanco o Negro) segÃºn oscuridad del fondo
    val textColor = if (isColorDark(color, CalendarioTheme.colors.settingsBackground)) Color.White else Color.Black

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun PermissionsDialog(
    calStatus: PermissionStatus,
    notifStatus: PermissionStatus,
    alarmStatus: PermissionStatus,
    driveStatus: PermissionStatus,
    batteryStatus: PermissionStatus,
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. CALENDARIO
                PermissionRow(
                    label = stringResource(id = R.string.calendar_permission_label),
                    status = calStatus,
                    fixLabel = if (calStatus == PermissionStatus.GRANTED) stringResource(R.string.status_granted) else stringResource(R.string.status_denied),
                    onFix = { onFix("calendar") }
                )

                // 2. NOTIFICACIONES
                PermissionRow(
                    label = stringResource(id = R.string.notifications_permission_label),
                    status = notifStatus,
                    fixLabel = if (notifStatus == PermissionStatus.GRANTED) stringResource(R.string.status_granted_f) else stringResource(R.string.status_denied),
                    onFix = { onFix("notifications") }
                )

                // 3. ALARMAS (Específico: Pantalla Completa)
                PermissionRow(
                    label = stringResource(id = R.string.alarms_permission_label),
                    status = alarmStatus,
                    fixLabel = if (alarmStatus == PermissionStatus.GRANTED) stringResource(R.string.status_full_screen) else stringResource(R.string.status_no_full_screen),
                    onFix = { onFix("alarms") }
                )

                // 4. DRIVE
                PermissionRow(
                    label = stringResource(id = R.string.google_drive_permission_label),
                    status = driveStatus,
                    fixLabel = if (driveStatus == PermissionStatus.GRANTED) stringResource(R.string.status_linked) else stringResource(R.string.status_unlinked),
                    onFix = { onFix("drive") }
                )
                
                // 5. BATERÍA
                PermissionRow(
                    label = stringResource(id = R.string.battery_optimization_label),
                    status = batteryStatus,
                    fixLabel = if (batteryStatus == PermissionStatus.GRANTED) stringResource(R.string.status_unrestricted) else stringResource(R.string.status_optimized),
                    onFix = { onFix("battery") }
                )
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
private fun BackupFrequencyDialog(
    selection: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "manual" to R.string.frequency_manual,
        "daily" to R.string.frequency_daily, 
        "weekly" to R.string.frequency_weekly, 
        "monthly" to R.string.frequency_monthly
    )
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
private fun PermissionRow(
    label: String, 
    status: PermissionStatus, 
    fixLabel: String,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // El punto de color (Verde = OK, Rojo = Acción requerida)
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
            
            Text(
                text = fixLabel,
                color = if (status == PermissionStatus.DENIED) MaterialTheme.colorScheme.primary else CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                fontWeight = if (status == PermissionStatus.DENIED) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onFix() }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
