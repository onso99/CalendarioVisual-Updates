@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    viewModel: CalendarioViewModel,
    onColorThemeClick: () -> Unit,
    onHolidayManagerClick: () -> Unit,
    onThemeUpdated: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onLogClick: () -> Unit = {},
    onBackupHistoryClick: () -> Unit = {},
    onNavigateToDate: (LocalDate) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing = uiState.isSyncing

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var showWidgetCalendarDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showUnlinkAccountDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isChangingLanguage by remember { mutableStateOf(false) }
    var showRestoreSelectDialog by remember { mutableStateOf(false) }
    var restoreSource by remember { mutableStateOf<String?>(null) }
    var pendingLocalUri by remember { mutableStateOf<Uri?>(null) }
    var showWidgetColorExpand by remember { mutableStateOf(false) }
    var showBackupActionsExpand by remember { mutableStateOf(false) }
    var showLocalBackupExpand by remember { mutableStateOf(false) }

    var showDataCleaningDialog by remember { mutableStateOf(false) }

    // Estado para el tema cargado desde archivo (pero aún no aplicado)
    var importedThemeData by remember { mutableStateOf<Pair<ParsedTheme, String>?>(null) }

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
                                // En lugar de aplicar, guardamos en el estado temporal
                                importedThemeData = importResult.parsedTheme to fileName
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
                            appPrefs.edit {
                                putString(AppConstants.KEY_LIGHT_THEME_NAME, newName)
                                putString(AppConstants.KEY_DARK_THEME_NAME, newName)
                            }
                            importedThemeData = null // Limpiar memoria temporal al guardar
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
                    try {
                        BackupManager.exportFullBackup(context, uri)
                        Toast.makeText(context, R.string.backup_exported_successfully, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val errorMsg = context.applicationContext.getString(R.string.error_exporting_backup, e.message ?: "Unknown error")
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val importFullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    pendingLocalUri = uri
                    restoreSource = "local"
                    showRestoreSelectDialog = true
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
    val lastBackupTimestamp = remember(permissionsUpdateTrigger) { 
        try { appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_TIME, 0L) } 
        catch (_: Exception) { (appPrefs.all[AppConstants.KEY_LAST_BACKUP_TIME] as? Number)?.toLong() ?: 0L }
    }
    val lastBackupSize = remember(permissionsUpdateTrigger) { 
        try { appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_SIZE, 0L) } 
        catch (_: Exception) { (appPrefs.all[AppConstants.KEY_LAST_BACKUP_SIZE] as? Number)?.toLong() ?: 0L }
    }

    var pendingShowWeekNumber by remember { 
        val v = try { appPrefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false) } 
                catch (_: Exception) { appPrefs.all[AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW]?.toString()?.toBoolean() ?: false }
        mutableStateOf(v) 
    }
    var pendingStartOfWeekKey by remember { 
        val raw = appPrefs.getString(AppConstants.KEY_START_OF_WEEK, StartOfWeekOption.SYSTEM.key) ?: StartOfWeekOption.SYSTEM.key
        mutableStateOf(StartOfWeekOption.fromKey(raw).key)
    }
    var pendingEventCount by remember { 
        val v = try { widgetPrefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) } catch (_: Exception) { WidgetConstants.DEFAULT_EVENT_COUNT }
        mutableFloatStateOf(v.toFloat())
    }
    var pendingTextBoost by remember {
        val v = try { widgetPrefs.getFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, 0f) } catch (_: Exception) { 0f }
        mutableFloatStateOf(v)
    }
    var pendingEventColor by remember { 
        val c = try { widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB) } catch (_: Exception) { WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB }
        mutableStateOf(Color(c))
    }
    var pendingTodayEventColor by remember {
        val c = try { widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB) } catch (_: Exception) { WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB }
        mutableStateOf(Color(c))
    }
    var pendingWidgetBackgroundColor by remember {
        val c = try { widgetPrefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB) } catch (_: Exception) { WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB }
        mutableStateOf(Color(c))
    }
    var pendingFontFamily by remember { mutableStateOf(widgetPrefs.getString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) ?: WidgetConstants.DEFAULT_WIDGET_FONT_FAMILY) }
    var pendingFontBold by remember { 
        val v = try { widgetPrefs.getBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, WidgetConstants.DEFAULT_WIDGET_FONT_BOLD) } 
                catch (_: Exception) { widgetPrefs.all[WidgetConstants.KEY_WIDGET_FONT_BOLD]?.toString()?.toBoolean() ?: WidgetConstants.DEFAULT_WIDGET_FONT_BOLD }
        mutableStateOf(v) 
    }
    var pendingWidgetCalendarIds by remember {
        val ids = try {
            widgetPrefs.getStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, emptySet())
        } catch (_: ClassCastException) {
            emptySet()
        }?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        mutableStateOf(ids)
    }
    val currentWidgetIds = pendingWidgetCalendarIds.ifEmpty { uiState.selectedCalendarIds }
    val widgetCalendarSummary = "(${currentWidgetIds.size})"
    
    var pendingAlarmOffset by remember { 
        val v = try { appPrefs.getInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, 30) } 
                catch (_: Exception) { (appPrefs.all[AppConstants.KEY_DEFAULT_ALARM_OFFSET] as? Number)?.toInt() ?: appPrefs.all[AppConstants.KEY_DEFAULT_ALARM_OFFSET]?.toString()?.toIntOrNull() ?: 30 }
        mutableFloatStateOf(v.toFloat()) 
    }
    var pendingSnoozeInterval by remember { 
        val v = try { appPrefs.getInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, 10) } 
                catch (_: Exception) { (appPrefs.all[AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL] as? Number)?.toInt() ?: appPrefs.all[AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL]?.toString()?.toIntOrNull() ?: 10 }
        mutableFloatStateOf(v.toFloat()) 
    }
    var pendingBackupFreq by remember { 
        val auto = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false)
        val f = if (!auto) "manual" else appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "manual") ?: "manual"
        mutableStateOf(f)
    }

    var showWidgetEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetTodayEventColorPalette by remember { mutableStateOf(false) }
    var showWidgetBackgroundColorPalette by remember { mutableStateOf(false) }

    // Auxiliares
    val updateAppPrefs = { block: android.content.SharedPreferences.Editor.() -> Unit ->
        appPrefs.edit { block() }
        CalendarAppWidgetProvider.triggerWidgetUpdate(context)
        WidgetStateManager.refreshWithCurrentEvents(context)
    }
    val updateWidgetPrefs = { block: android.content.SharedPreferences.Editor.() -> Unit ->
        widgetPrefs.edit(commit = true) { block() }
        CalendarAppWidgetProvider.triggerWidgetUpdate(context)
        WidgetStateManager.refreshWithCurrentEvents(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionsUpdateTrigger++ }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary) } },
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
            SectionTitle(text = stringResource(id = R.string.general))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showPermissionsDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.system_permissions), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(10.dp).background(permissionPointColor, CircleShape))
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showWeekConfigDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.semana_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val startDayName = stringResource(id = StartOfWeekOption.fromKey(pendingStartOfWeekKey).displayNameRes)
                    Text(text = "$startDayName${if (pendingShowWeekNumber) " (123)" else ""}", color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showAlarmConfigDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.alarm), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_alarm_anticipation), null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        Text(text = "${if (pendingAlarmOffset > 0) "-" else ""}${pendingAlarmOffset.roundToInt()}'", modifier = Modifier.padding(start = 2.dp), color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(id = R.drawable.ic_alarm_snooze), null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        Text(text = "${pendingSnoozeInterval.roundToInt()}'", modifier = Modifier.padding(start = 2.dp), color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onHolidayManagerClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.holidays_section), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showLanguageDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.language), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val curLangs = AppCompatDelegate.getApplicationLocales()
                    val langSetting = AppLanguageSetting.fromCode(if (curLangs.isEmpty) null else curLangs.get(0)?.language)
                    Text(text = stringResource(id = langSetting.displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showThemeDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.mode), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = stringResource(id = themeSetting.displayNameRes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showBundledThemesDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.predefined_themes), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val currentThemeId = lightThemeName ?: "theme_1"
                    val bundledTheme = BundledThemes.themes.find { t -> val manifest = t["themeManifest"] as Map<*,*>; manifest["id"] == currentThemeId.removeSuffix("***") || manifest["name"] == currentThemeId.removeSuffix("***") }
                    val finalName = if (bundledTheme != null) stringResource(id = (bundledTheme["themeManifest"] as Map<*,*>)["nameRes"] as Int) + (if (currentThemeId.endsWith("***")) "***" else "") else currentThemeId
                    Text(text = truncateThemeName(finalName, 20), color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                ActionRow(text = stringResource(id = R.string.customize_colors), onClick = onColorThemeClick)
            }

            WidgetSectionTitle()
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showWidgetCalendarDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.widget_selected_calendars_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = widgetCalendarSummary, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(text = stringResource(id = R.string.widget_event_count), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = pendingEventCount, 
                            onValueChange = { pendingEventCount = it; updateWidgetPrefs { putInt(WidgetConstants.KEY_EVENT_COUNT, it.roundToInt()) } }, 
                            valueRange = 1f..12f, 
                            steps = 10, 
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CalendarioTheme.colors.cabecera,
                                activeTrackColor = CalendarioTheme.colors.cabecera,
                                inactiveTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.24f)
                            )
                        )
                        Text(text = pendingEventCount.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(id = R.string.widget_text_adjustment), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = pendingTextBoost, 
                            onValueChange = { pendingTextBoost = it; updateWidgetPrefs { putFloat(WidgetConstants.KEY_WIDGET_TEXT_BOOST, it) } }, 
                            valueRange = -4f..4f, 
                            steps = 7, 
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CalendarioTheme.colors.cabecera,
                                activeTrackColor = CalendarioTheme.colors.cabecera,
                                inactiveTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.24f)
                            )
                        )
                        Text(text = (if (pendingTextBoost.roundToInt() > 0) "+" else "") + pendingTextBoost.roundToInt().toString(), modifier = Modifier.width(40.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showFontFamilyDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.font), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(if (pendingFontBold) CalendarioTheme.colors.cabecera.copy(alpha = 0.12f) else Color.Transparent).border(1.dp, if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.15f), CircleShape).clickable { pendingFontBold = !pendingFontBold; updateWidgetPrefs { putBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, pendingFontBold) } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.FormatBold, null, tint = if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), modifier = Modifier.size(34.dp)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    val display = when(pendingFontFamily) { WidgetConstants.FONT_FAMILY_SERIF -> stringResource(id = R.string.font_serif); WidgetConstants.FONT_FAMILY_MONOSPACE -> stringResource(id = R.string.font_monospace); WidgetConstants.FONT_FAMILY_CONDENSED -> stringResource(id = R.string.font_condensed); WidgetConstants.FONT_FAMILY_SANS_SERIF -> stringResource(id = R.string.font_sans_serif); else -> stringResource(id = R.string.font_system) }
                    Text(text = display, color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = if (pendingFontBold) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                Column {
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showWidgetColorExpand = !showWidgetColorExpand }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.widget_colors_label), fontSize = 16.sp, modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val border = if (isColorDark(CalendarioTheme.colors.fondoSecciones, Color.White)) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pendingWidgetBackgroundColor).border(0.5.dp, border, CircleShape))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pendingEventColor).border(0.5.dp, border, CircleShape))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pendingTodayEventColor).border(0.5.dp, border, CircleShape))
                        }
                        Icon(if (showWidgetColorExpand) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.padding(start = 8.dp).size(20.dp))
                    }
                    if (showWidgetColorExpand) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WidgetColorChip(stringResource(id = R.string.widget_color_fondo), pendingWidgetBackgroundColor, Modifier.weight(1f)) { showWidgetBackgroundColorPalette = true }
                            WidgetColorChip(stringResource(id = R.string.widget_color_evento), pendingEventColor, Modifier.weight(1f)) { showWidgetEventColorPalette = true }
                            WidgetColorChip(stringResource(id = R.string.widget_color_hoy), pendingTodayEventColor, Modifier.weight(1f)) { showWidgetTodayEventColorPalette = true }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle(text = stringResource(id = R.string.backup_section_title_label), modifier = Modifier.weight(1f))
                IconButton(onClick = onBackupHistoryClick, modifier = Modifier.padding(top = 16.dp).size(24.dp)) { 
                    Icon(
                        imageVector = Icons.Default.History, 
                        contentDescription = null, 
                        tint = lerp(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.textSystem, 0.4f)
                    )
                }
            }
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val email = remember(permissionsUpdateTrigger) { appPrefs.getString("google_account_email", null) }
                if (email == null) {
                    ActionRow(text = stringResource(id = R.string.link_google_account)) {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(Scope(DriveScopes.DRIVE_APPDATA)).build()
                        googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showUnlinkAccountDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.drive_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(text = email, color = CalendarioTheme.colors.textSystem, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                    val freqLabel = when(pendingBackupFreq) { "manual" -> stringResource(R.string.frequency_manual); "daily" -> stringResource(R.string.frequency_daily); "weekly" -> stringResource(R.string.frequency_weekly); "monthly" -> stringResource(R.string.frequency_monthly); else -> pendingBackupFreq }
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showFrequencyDialog = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.backup_frequency), color = CalendarioTheme.colors.textSystem, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Text(text = freqLabel, color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                    Column {
                        var dateFontSize by remember { mutableStateOf(14.sp) }
                        Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showBackupActionsExpand = !showBackupActionsExpand }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.last_backup_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            val lastStr = if (lastBackupTimestamp == 0L) stringResource(R.string.never) else DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(lastBackupTimestamp))
                            val sizeStr = if (lastBackupSize > 0) " · ${"%.2f".format(Locale.US, lastBackupSize / (1024.0 * 1024.0))}MB" else ""
                            Text(text = "$lastStr$sizeStr", color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), fontSize = dateFontSize, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, onTextLayout = { if (it.hasVisualOverflow && dateFontSize > 11.sp) dateFontSize = (dateFontSize.value - 1f).sp })
                            Icon(if (showBackupActionsExpand) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.padding(start = 8.dp).size(20.dp))
                        }
                        if (showBackupActionsExpand) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val bg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                                SettingsActionChip(text = stringResource(id = R.string.restaurar_label), icon = painterResource(id = R.drawable.ic_restore_custom), modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { restoreSource = "drive"; showRestoreSelectDialog = true })
                                SettingsActionChip(text = stringResource(id = R.string.sincronizar_label), icon = Icons.Default.Sync, isIconRotating = isSyncing, modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { viewModel.syncHistoryToDrive(context) { if (it.success) { permissionsUpdateTrigger++; Toast.makeText(context, context.applicationContext.getString(R.string.sync_success_detailed, it.totalEvents), Toast.LENGTH_LONG).show() } else { Toast.makeText(context, R.string.sync_error_drive, Toast.LENGTH_SHORT).show() } } })
                            }
                        }
                    }
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                Column {
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showLocalBackupExpand = !showLocalBackupExpand }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.preferences_backup_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Icon(if (showLocalBackupExpand) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                    if (showLocalBackupExpand) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val bg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                            SettingsActionChip(text = stringResource(id = R.string.restaurar_label), icon = painterResource(id = R.drawable.ic_restore_custom), modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { importFullBackupLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) })
                            SettingsActionChip(text = stringResource(id = R.string.guardar_label), icon = Icons.Default.Save, modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { val suggested = "calendariovisual_backup_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.json"; exportFullBackupLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, suggested) }) })
                        }
                    }
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showDataCleaningDialog = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.data_cleaning_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.cleaningCandidates.isNotEmpty()) {
                        Text(
                            text = "(${uiState.cleaningCandidates.size})",
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.about), style = typography.titleMedium, fontWeight = FontWeight.Bold, color = lerp(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.textSystem, 0.4f))
                val haptic = LocalHapticFeedback.current
                var loggingEnabledInternal by remember { mutableStateOf(LogCollector.isLoggingEnabled(context)) }
                var debugClickCount by remember { mutableIntStateOf(0) }
                IconButton(onClick = { if (loggingEnabledInternal) onLogClick() else { debugClickCount++; if (debugClickCount >= 7) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); loggingEnabledInternal = true; LogCollector.setLoggingEnabled(context, true); debugClickCount = 0 } } }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.BugReport, null, tint = if (loggingEnabledInternal) CalendarioTheme.colors.textSystem else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.combinedClickable(onClick = { if (loggingEnabledInternal) onLogClick() else { debugClickCount++; if (debugClickCount >= 7) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); loggingEnabledInternal = true; LogCollector.setLoggingEnabled(context, true); debugClickCount = 0 } } }, onLongClick = { if (loggingEnabledInternal) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); loggingEnabledInternal = false; LogCollector.setLoggingEnabled(context, false); debugClickCount = 0 } })) }
            }
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(16.dp)) {
                Text("${stringResource(id = R.string.app_name)} ${AboutInfo.getVersionName(context)}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${AboutInfo.LINE_3_AUTHOR} > ${AboutInfo.getFormattedDate()} > ", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem)
                    Text(text = stringResource(id = R.string.history), fontSize = 16.sp, color = Color(0xFF2196F3), modifier = Modifier.clickable { onHistoryClick() } )
                }
            }
        }

        // --- Dialogs ---
        if (showDataCleaningDialog) {
            DataCleaningDialog(
                candidates = uiState.cleaningCandidates,
                onDismiss = { showDataCleaningDialog = false },
                onNavigateToDate = { date ->
                    showDataCleaningDialog = false
                    onBackPress() // Salir de ajustes
                    onNavigateToDate(date)
                },
                onDeleteCandidate = { viewModel.deleteCleaningCandidate(it) },
                onDeleteAll = { viewModel.deleteAllCleaningCandidates() },
                onScan = { onComplete -> viewModel.refreshData { onComplete() } }
            )
        }

        if (showLanguageDialog) {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            LanguageSelectionDialog(currentLanguageCode = if (currentLocales.isEmpty) null else currentLocales.get(0)?.language, onLanguageSelected = { newCode -> scope.launch { isChangingLanguage = true; delay(1000.milliseconds); AppCompatDelegate.setApplicationLocales(if (newCode == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(newCode)); showLanguageDialog = false } }, onDismiss = { showLanguageDialog = false })
        }
        if (showThemeDialog) { ThemeSelectionDialog(currentTheme = themeSetting, onThemeSelected = { themeManager.setTheme(it); showThemeDialog = false }, onDismiss = { showThemeDialog = false }) }
        if (showWeekConfigDialog) { WeekConfigDialog(currentSelectionKey = pendingStartOfWeekKey, showWeekNumber = pendingShowWeekNumber, onConfirm = { key, show -> pendingStartOfWeekKey = key; pendingShowWeekNumber = show; updateAppPrefs { putString(AppConstants.KEY_START_OF_WEEK, key); putBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, show) }; showWeekConfigDialog = false }, onDismiss = { showWeekConfigDialog = false }) }
        if (showAlarmConfigDialog) { AlarmConfigDialog(anticipation = pendingAlarmOffset, snooze = pendingSnoozeInterval, onConfirm = { offset, interval -> pendingAlarmOffset = offset; pendingSnoozeInterval = interval; updateAppPrefs { putInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, offset.roundToInt()); putInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, interval.roundToInt()) }; showAlarmConfigDialog = false }, onDismiss = { showAlarmConfigDialog = false }) }
        if (showFontFamilyDialog) { FontFamilySelectionDialog(currentSelection = pendingFontFamily, onConfirm = { family -> pendingFontFamily = family; updateWidgetPrefs { putString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, family) }; showFontFamilyDialog = false }, onDismiss = { showFontFamilyDialog = false }) }
        if (showFrequencyDialog) { BackupFrequencyDialog(selection = pendingBackupFreq, onConfirm = { freq -> pendingBackupFreq = freq; updateAppPrefs { val enabled = freq != "manual"; putBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, enabled); putString(AppConstants.KEY_BACKUP_FREQUENCY, freq) }; if (freq != "manual") BackupScheduler.scheduleBackup(context, freq) else BackupScheduler.cancelBackup(context); showFrequencyDialog = false }, onDismiss = { showFrequencyDialog = false }) }
        if (showBundledThemesDialog) { 
            BundledThemesDialog(
                currentThemeId = lightThemeName, 
                importedTheme = importedThemeData,
                onDismiss = { 
                    showBundledThemesDialog = false
                    importedThemeData = null 
                }, 
                onThemeSelected = { theme, id -> 
                    showBundledThemesDialog = false
                    ThemePersistence.applyTheme(context, theme, id)
                    onThemeImported()
                    CalendarAppWidgetProvider.triggerWidgetUpdate(context)
                    WidgetStateManager.refreshWithCurrentEvents(context)
                    importedThemeData = null
                }, 
                onLoadClick = { 
                    importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { 
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json" 
                    }) 
                }, 
                onSaveClick = { showExportDialog = true }
            ) 
        }
        if (showWidgetEventColorPalette) { AdvancedColorPickerDialog(initialColor = pendingEventColor, onDismissRequest = { showWidgetEventColorPalette = false }, onColorConfirm = { pendingEventColor = it; updateWidgetPrefs { putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, it.toArgb()) }; showWidgetEventColorPalette = false }) }
        if (showWidgetTodayEventColorPalette) { AdvancedColorPickerDialog(initialColor = pendingTodayEventColor, onDismissRequest = { showWidgetTodayEventColorPalette = false }, onColorConfirm = { pendingTodayEventColor = it; updateWidgetPrefs { putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, it.toArgb()) }; showWidgetTodayEventColorPalette = false }) }
        if (showWidgetBackgroundColorPalette) { AdvancedColorPickerDialog(initialColor = pendingWidgetBackgroundColor, onDismissRequest = { showWidgetBackgroundColorPalette = false }, onColorConfirm = { pendingWidgetBackgroundColor = it; updateWidgetPrefs { putInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, it.toArgb()) }; showWidgetBackgroundColorPalette = false }) }
        if (showDiscardChangesDialog) { AlertDialog(onDismissRequest = { showDiscardChangesDialog = false }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Text(stringResource(id = R.string.discard_changes_confirmation)) }, confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.discard), onClick = { showDiscardChangesDialog = false; onBackPress() }, color = Color.Red) }, dismissButton = { DialogDismissButton(onDismiss = { showDiscardChangesDialog = false }) }) }
        if (showUnlinkAccountDialog) { AlertDialog(onDismissRequest = { showUnlinkAccountDialog = false }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.unlink_google_account), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Text(stringResource(id = R.string.unlink_account_confirmation)) }, confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.unlink_action), onClick = { val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(); GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener { appPrefs.edit { remove("google_account_email") }; permissionsUpdateTrigger++; showUnlinkAccountDialog = false } }, color = Color.Red) }, dismissButton = { DialogDismissButton(onDismiss = { showUnlinkAccountDialog = false }) }) }
        if (showPermissionsDialog) { LaunchedEffect(Unit) { permissionsUpdateTrigger++; delay(500.milliseconds); permissionsUpdateTrigger++ }; PermissionsDialog(calStatus = calStatus, notifStatus = notifStatus, alarmStatus = alarmStatus, driveStatus = driveStatus, batteryStatus = batteryStatus, onDismiss = { showPermissionsDialog = false }, onFix = { type -> when (type) { "drive" -> { val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(Scope(DriveScopes.DRIVE_APPDATA)).build(); googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent) }; "battery" -> context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); else -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }) } }) }
        if (showRestoreSelectDialog) { 
            RestoreSelectDialog(
                onDismiss = { 
                    showRestoreSelectDialog = false
                    restoreSource = null
                    pendingLocalUri = null 
                }, 
                onConfirm = { prefs, holidays, notes, events -> 
                    showRestoreSelectDialog = false
                    when (restoreSource) {
                        "drive" -> {
                            viewModel.restoreHistoryFromDrive(context, prefs, holidays, notes, events) { success -> 
                                if (success) { 
                                    Toast.makeText(context.applicationContext, R.string.restore_success, Toast.LENGTH_SHORT).show()
                                    if (prefs) {
                                        (context as? Activity)?.let { a -> a.finish(); a.startActivity(a.intent) } 
                                    } else {
                                        permissionsUpdateTrigger++
                                    }
                                } else {
                                    Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show() 
                                }
                            }
                        }
                        "local" -> {
                            if (pendingLocalUri != null) {
                                viewModel.restoreFromLocal(context, pendingLocalUri!!, prefs, holidays, notes, events) { success -> 
                                    if (success) { 
                                        Toast.makeText(context.applicationContext, R.string.restore_success, Toast.LENGTH_SHORT).show()
                                        if (prefs) {
                                            (context as? Activity)?.let { a -> a.finish(); a.startActivity(a.intent) } 
                                        } else {
                                            permissionsUpdateTrigger++
                                        }
                                    } else {
                                        Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show() 
                                    }
                                }
                            }
                        }
                    }
                    restoreSource = null
                    pendingLocalUri = null 
                }
            ) 
        }
        if (showExportDialog) { ExportThemeDialog(onDismissRequest = { showExportDialog = false }, onConfirm = { newName -> showExportDialog = false; appPrefs.edit { putString("temp_export_name", newName) }; exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "${newName}.json") }) }) }
        if (showWidgetCalendarDialog) { SelectWidgetCalendarsDialog(appActiveCalendars = uiState.availableCalendars.filter { uiState.selectedCalendarIds.contains(it.id) }, initialSelectedIds = pendingWidgetCalendarIds.ifEmpty { uiState.selectedCalendarIds }, currentFavoriteId = uiState.favoriteCalendarId, onApply = { newIds -> pendingWidgetCalendarIds = newIds; updateWidgetPrefs { putStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, newIds.map { it.toString() }.toSet()) }; showWidgetCalendarDialog = false }, onDismissRequest = { showWidgetCalendarDialog = false }) }

        if (isChangingLanguage) {
            val transition = rememberInfiniteTransition(label = "lang_rotation")
            val rot by transition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rotation")
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) { Icon(Icons.Default.Language, null, modifier = Modifier.size(48.dp).graphicsLayer { rotationZ = rot }, tint = Color.White) }
        }
    }
}

@Composable
private fun ExportThemeDialog(onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.export_theme_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(id = R.string.theme_name)) }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) }, confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.export), onClick = { onConfirm(text.ifBlank { "nuevo_tema" }) }, enabled = text.isNotBlank()) }, dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) })
}

@Composable
private fun RestoreSelectDialog(onDismiss: () -> Unit, onConfirm: (prefs: Boolean, holidays: Boolean, notes: Boolean, events: Boolean) -> Unit) {
    var restorePrefs by remember { mutableStateOf(true) }
    var restoreHolidays by remember { mutableStateOf(true) }
    var restoreNotes by remember { mutableStateOf(true) }
    var restoreEvents by remember { mutableStateOf(true) }
    val anySelected = restorePrefs || restoreHolidays || restoreNotes || restoreEvents
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, title = { Text(text = stringResource(id = R.string.restaurar_label), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { RestoreOptionRow(stringResource(id = R.string.restore_prefs_label), restorePrefs) { restorePrefs = it }; RestoreOptionRow(stringResource(id = R.string.restore_holidays_label), restoreHolidays) { restoreHolidays = it }; RestoreOptionRow(stringResource(id = R.string.restore_notes_label), restoreNotes) { restoreNotes = it }; RestoreOptionRow(stringResource(id = R.string.restore_events_label), restoreEvents) { restoreEvents = it } } }, confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.apply), onClick = { onConfirm(restorePrefs, restoreHolidays, restoreNotes, restoreEvents) }, enabled = anySelected) }, dismissButton = { DialogDismissButton(onDismiss = onDismiss) })
}

@Composable
private fun RestoreOptionRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { 
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Text(text = label, color = CalendarioTheme.colors.textSystem, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp) 
    }
}

@Composable
private fun FontFamilySelectionDialog(currentSelection: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tempSelection by remember { mutableStateOf(currentSelection) }
    val options = listOf(WidgetConstants.FONT_FAMILY_SYSTEM to R.string.font_system, WidgetConstants.FONT_FAMILY_SANS_SERIF to R.string.font_sans_serif, WidgetConstants.FONT_FAMILY_SERIF to R.string.font_serif, WidgetConstants.FONT_FAMILY_MONOSPACE to R.string.font_monospace, WidgetConstants.FONT_FAMILY_CONDENSED to R.string.font_condensed)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.font), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Column { options.forEach { (key, labelRes) -> val family = when(key) { WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif; WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace; WidgetConstants.FONT_FAMILY_CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL)); WidgetConstants.FONT_FAMILY_SANS_SERIF -> FontFamily.SansSerif; else -> FontFamily.Default }; Row(Modifier.fillMaxWidth().clickable { tempSelection = key }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { val isSelected = key == tempSelection; Text(text = stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = family, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal); if (isSelected) { Icon(Icons.Default.Check, null, tint = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) CalendarioTheme.colors.textSystem else CalendarioTheme.colors.cabecera) } } } } }, confirmButton = { AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempSelection) }, onDismiss = onDismiss) })
}

@Composable
private fun AlarmConfigDialog(anticipation: Float, snooze: Float, onConfirm: (Float, Float) -> Unit, onDismiss: () -> Unit) {
    var tempAnticipation by remember { mutableFloatStateOf(anticipation) }
    var tempSnooze by remember { mutableFloatStateOf(snooze) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem, 
        title = { Text(stringResource(id = R.string.alarm), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
        text = { 
            Column { 
                Text(stringResource(id = R.string.alarm_anticipation_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    Slider(
                        value = tempAnticipation, 
                        onValueChange = { tempAnticipation = it }, 
                        valueRange = 0f..60f, 
                        steps = 5,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CalendarioTheme.colors.cabecera,
                            activeTrackColor = CalendarioTheme.colors.cabecera,
                            inactiveTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.24f)
                        )
                    )
                    Text(text = "${if (tempAnticipation.roundToInt() > 0) "-" else ""}${tempAnticipation.roundToInt()}'", modifier = Modifier.width(52.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp, fontWeight = FontWeight.Medium) 
                }
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                Text(stringResource(id = R.string.alarm_snooze_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    Slider(
                        value = tempSnooze, 
                        onValueChange = { tempSnooze = it }, 
                        valueRange = 10f..60f, 
                        steps = 4,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CalendarioTheme.colors.cabecera,
                            activeTrackColor = CalendarioTheme.colors.cabecera,
                            inactiveTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.24f)
                        )
                    )
                    Text(text = "${tempSnooze.roundToInt()}'", modifier = Modifier.width(44.dp).padding(start = 8.dp), color = CalendarioTheme.colors.textSystem, textAlign = TextAlign.End, fontSize = 16.sp, fontWeight = FontWeight.Medium) 
                }
            }
        }, 
        confirmButton = { 
            AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempAnticipation, tempSnooze) }, onDismiss = onDismiss) 
        }
    )
}

@Composable
private fun WeekConfigDialog(currentSelectionKey: String, showWeekNumber: Boolean, onConfirm: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    var tempKey by remember { mutableStateOf(currentSelectionKey) }
    var tempShowWeek by remember { mutableStateOf(showWeekNumber) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem, 
        title = { Text(stringResource(id = R.string.semana_label), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
        text = { 
            Column { 
                StartOfWeekOption.entries.forEach { option -> 
                    Row(Modifier.fillMaxWidth().clickable { tempKey = option.key }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { 
                        val isSelected = option.key == tempKey
                        Text(text = stringResource(id = option.displayNameRes), modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                        if (isSelected) { 
                            Icon(Icons.Default.Check, null, tint = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) CalendarioTheme.colors.textSystem else CalendarioTheme.colors.cabecera) 
                        } 
                    } 
                }
                HorizontalDivider(color = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth().height(56.dp).clickable { tempShowWeek = !tempShowWeek }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { 
                    Text(stringResource(id = R.string.week_in_year_view), fontSize = 16.sp)
                    Switch(
                        checked = tempShowWeek, 
                        onCheckedChange = { tempShowWeek = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CalendarioTheme.colors.cabecera,
                            uncheckedThumbColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                            uncheckedTrackColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.12f),
                            uncheckedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.2f)
                        )
                    )
                } 
            } 
        }, 
        confirmButton = { 
            AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempKey, tempShowWeek) }, onDismiss = onDismiss) 
        }
    )
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun BundledThemesDialog(
    currentThemeId: String?,
    importedTheme: Pair<ParsedTheme, String>?, // Nuevo: Tema cargado de archivo
    onDismiss: () -> Unit,
    onThemeSelected: (ParsedTheme, String) -> Unit, // Cambiado: Ahora devuelve ParsedTheme + ID
    onLoadClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val effectiveId = currentThemeId ?: "theme_1"
    val cleanId = effectiveId.removeSuffix("***")
    
    // Verificar si el tema actual es uno de los predefinidos
    val isCurrentBundled = remember(cleanId) {
        BundledThemes.themes.any { (it["themeManifest"] as Map<*, *>)["id"] == cleanId }
    }
    
    // El ID seleccionado puede ser un ID de tema bundled, el ID de un tema ya activo, o "imported_temp"
    // Usamos cleanId como clave para que se resetee si el tema del sistema cambia (ej: al guardar)
    var tempSelectionId by remember(cleanId) { mutableStateOf(cleanId) }
    
    // Si cargamos un archivo nuevo, lo seleccionamos automáticamente
    LaunchedEffect(importedTheme) {
        if (importedTheme != null) {
            tempSelectionId = "imported_temp"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(text = stringResource(id = R.string.themes_v6), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // La celda personalizada aparece si hay un tema importado nuevo O si el tema actual ya es personalizado
                val hasCustomEntry = importedTheme != null || !isCurrentBundled
                val gridHeight = if (hasCustomEntry) 264.dp else 210.dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(gridHeight)
                ) {
                    // 1. Temas predefinidos
                    gridItems(BundledThemes.themes) { theme: Map<String, Any> ->
                        val themeManifest = theme["themeManifest"] as Map<*, *>
                        val themeId = themeManifest["id"] as String
                        val themeResId = themeManifest["nameRes"] as Int
                        val isSelected = tempSelectionId == themeId
                        
                        ThemeChip(
                            name = stringResource(id = themeResId),
                            isSelected = isSelected,
                            onClick = { tempSelectionId = themeId }
                        )
                    }
                    
                    // 2. Celda Inteligente de Tema Personalizado
                    if (hasCustomEntry) {
                        item {
                            val customName = when {
                                importedTheme != null -> importedTheme.second
                                !isCurrentBundled -> cleanId
                                else -> ""
                            }
                            // El ID de selección es "imported_temp" si es una carga fresca, 
                            // o el cleanId original si es el tema ya activo.
                            val targetId = if (importedTheme != null) "imported_temp" else cleanId
                            
                            ThemeChip(
                                name = customName,
                                isSelected = tempSelectionId == targetId,
                                onClick = { tempSelectionId = targetId }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(18.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val backupButtonBg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                    SettingsActionChip(text = stringResource(id = R.string.cargar_label), icon = Icons.Default.FolderOpen, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), containerColor = backupButtonBg, onClick = onLoadClick)
                    SettingsActionChip(text = stringResource(id = R.string.guardar_label), icon = Icons.Default.Save, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), containerColor = backupButtonBg, onClick = onSaveClick)
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.accept),
                onConfirm = { 
                    when (tempSelectionId) {
                        "imported_temp" -> {
                            importedTheme?.let { onThemeSelected(it.first, it.second) }
                        }
                        cleanId -> {
                            if (!isCurrentBundled) {
                                // El usuario ha vuelto a seleccionar el tema personalizado que ya tenía
                                onDismiss()
                            } else {
                                // Es el tema bundled que estaba activo
                                BundledThemes.themes.find { (it["themeManifest"] as Map<*, *>)["id"] == tempSelectionId }?.let { theme ->
                                    val manifestObj = theme["themeManifest"] as Map<*, *>
                                    val manifest = JSONObject(manifestObj)
                                    val light = theme["lightTheme"]?.let { JSONObject(it as Map<*, *>) }
                                    val dark = theme["darkTheme"]?.let { JSONObject(it as Map<*, *>) }
                                    val id = manifestObj["id"] as String
                                    onThemeSelected(ParsedTheme(manifest, light, dark), id)
                                }
                            }
                        }
                        else -> {
                            // Buscar y aplicar el tema predefinido seleccionado
                            BundledThemes.themes.find { (it["themeManifest"] as Map<*, *>)["id"] == tempSelectionId }?.let { theme ->
                                val manifestObj = theme["themeManifest"] as Map<*, *>
                                val manifest = JSONObject(manifestObj)
                                val light = theme["lightTheme"]?.let { JSONObject(it as Map<*, *>) }
                                val dark = theme["darkTheme"]?.let { JSONObject(it as Map<*, *>) }
                                val id = manifestObj["id"] as String
                                onThemeSelected(ParsedTheme(manifest, light, dark), id)
                            }
                        }
                    }
                },
                onDismiss = onDismiss
            )
        }
    )
}

@Composable
private fun ThemeChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
    val bgColor = if (isSelected) CalendarioTheme.colors.cabecera.copy(alpha = 0.08f) else Color.Transparent
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
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

private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "nombre_desconocido"; context.contentResolver.query(uri, null, null, null, null)?.use { cursor -> if (cursor.moveToFirst()) { val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (nameIndex != -1) fileName = cursor.getString(nameIndex) } }; return fileName.substringBeforeLast('.')
}

fun truncateThemeName(name: String, limit: Int): String = if (name.length > limit) name.take(limit - 3) + "..." else name

@Composable
private fun WidgetColorChip(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val textColor = if (isColorDark(color, CalendarioTheme.colors.settingsBackground)) Color.White else Color.Black; val borderColor = if (isColorDark(CalendarioTheme.colors.fondoSecciones, Color.White)) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f); Box(modifier = modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).background(color).border(0.5.dp, borderColor, RoundedCornerShape(10.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp)) }
}

@Composable
private fun PermissionsDialog(calStatus: PermissionStatus, notifStatus: PermissionStatus, alarmStatus: PermissionStatus, driveStatus: PermissionStatus, batteryStatus: PermissionStatus, onDismiss: () -> Unit, onFix: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.permissions_dialog_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { PermissionRow(label = stringResource(id = R.string.calendar_permission_label), status = calStatus, fixLabel = if (calStatus == PermissionStatus.GRANTED) stringResource(R.string.status_granted) else stringResource(R.string.status_denied), onFix = { onFix("calendar") }); PermissionRow(label = stringResource(id = R.string.notifications_permission_label), status = notifStatus, fixLabel = if (notifStatus == PermissionStatus.GRANTED) stringResource(R.string.status_granted_f) else stringResource(R.string.status_denied), onFix = { onFix("notifications") }); PermissionRow(label = stringResource(id = R.string.alarms_permission_label), status = alarmStatus, fixLabel = if (alarmStatus == PermissionStatus.GRANTED) stringResource(R.string.status_full_screen) else stringResource(R.string.status_no_full_screen), onFix = { onFix("alarms") }); PermissionRow(label = stringResource(id = R.string.google_drive_permission_label), status = driveStatus, fixLabel = if (driveStatus == PermissionStatus.GRANTED) stringResource(R.string.status_linked) else stringResource(R.string.status_unlinked), onFix = { onFix("drive") }); PermissionRow(label = stringResource(id = R.string.battery_optimization_label), status = batteryStatus, fixLabel = if (batteryStatus == PermissionStatus.GRANTED) stringResource(R.string.status_unrestricted) else stringResource(R.string.status_optimized), onFix = { onFix("battery") }) } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.close), color = CalendarioTheme.colors.textSystem) } })
}

@Composable
private fun BackupFrequencyDialog(selection: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tempSelection by remember { mutableStateOf(selection) }; val options = listOf("manual" to R.string.frequency_manual, "daily" to R.string.frequency_daily, "weekly" to R.string.frequency_weekly, "monthly" to R.string.frequency_monthly); AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.backup_frequency), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Column { options.forEach { (key, labelRes) -> Row(Modifier.fillMaxWidth().clickable { tempSelection = key }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { val isSelected = key == tempSelection; Text(text = stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal); if (isSelected) Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera) } } } }, confirmButton = { AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempSelection) }, onDismiss = onDismiss) })
}

@Composable
private fun PermissionRow(label: String, status: PermissionStatus, fixLabel: String, onFix: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(modifier = Modifier.padding(top = 6.dp).size(10.dp).background(if (status == PermissionStatus.GRANTED) Color.Green else Color.Red, CircleShape)); Column(modifier = Modifier.weight(1f)) { Text(text = label, color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, lineHeight = 20.sp); Text(text = fixLabel, color = if (status == PermissionStatus.DENIED) MaterialTheme.colorScheme.primary else CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), fontWeight = if (status == PermissionStatus.DENIED) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, modifier = Modifier.clickable { onFix() }.padding(vertical = 2.dp)) } }
}

@Composable
private fun DataCleaningDialog(
    candidates: List<SearchItem>,
    onDismiss: () -> Unit,
    onNavigateToDate: (LocalDate) -> Unit,
    onDeleteCandidate: (SearchItem) -> Unit,
    onDeleteAll: () -> Unit,
    onScan: (() -> Unit) -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var scanFinishedTrigger by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var lastKnownMessage by remember { mutableStateOf("") }
    
    if (statusMessage != null) lastKnownMessage = statusMessage!!

    val analyzingDataMsg = stringResource(id = R.string.analyzing_data)
    val analysisFinishedMsg = stringResource(id = R.string.analysis_finished)

    LaunchedEffect(isScanning, scanFinishedTrigger) {
        if (isScanning) {
            statusMessage = analyzingDataMsg
        } else if (scanFinishedTrigger > 0) {
            statusMessage = analysisFinishedMsg
            delay(5000.milliseconds)
            statusMessage = null
        }
    }

    val messageAlpha by animateFloatAsState(
        targetValue = if (statusMessage != null) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "statusAlpha"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(id = R.string.data_cleaning_label), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 20.sp, 
                    textAlign = TextAlign.Start
                )
                // Mensaje fijo que no aumenta la altura del diálogo
                Text(
                    text = lastKnownMessage,
                    fontSize = 12.sp,
                    color = if (isScanning) CalendarioTheme.colors.textSystem.copy(alpha = 0.6f * messageAlpha) else CalendarioTheme.colors.cabecera.copy(alpha = messageAlpha),
                    modifier = Modifier.offset(y = (-16).dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val offsetPx = 48.dp.roundToPx() // Sincronizado con SelectCalendarsDialog
                    layout(placeable.width, placeable.height - offsetPx) {
                        placeable.placeRelative(0, -offsetPx)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        val ghostCount = candidates.count { it is SearchItem.Event }
                        val noteCount = candidates.count { it is SearchItem.Note }

                        if (ghostCount > 0) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ghost_24),
                                contentDescription = null,
                                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " ($ghostCount)",
                                fontSize = 14.sp,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }

                        if (noteCount > 0) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " ($noteCount)",
                                fontSize = 14.sp,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                            )
                        }

                    }

                    TextButton(
                        onClick = { 
                            isScanning = true
                            onScan { 
                                isScanning = false 
                                scanFinishedTrigger++
                            } 
                        },
                        enabled = !isScanning
                    ) {
                        Text(
                            text = stringResource(id = R.string.scan_label), 
                            color = if (isScanning) Color.Gray else CalendarioTheme.colors.cabecera,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (candidates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.cabecera,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.no_cleaning_results),
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates) { item ->
                            CleaningCandidateRow(
                                item = item,
                                onClick = { onNavigateToDate(item.date) },
                                onDelete = { onDeleteCandidate(item) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.delete_all_events_option).substringBefore(" "), // Reciclando "Eliminar"
                confirmColor = Color.Red,
                confirmEnabled = candidates.isNotEmpty(),
                onConfirm = onDeleteAll,
                onDismiss = onDismiss
            )
        }
    )
}

@Composable
private fun CleaningCandidateRow(
    item: SearchItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CalendarioTheme.colors.fondoSecciones)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono según tipo
        if (item is SearchItem.Event) {
            Icon(
                painter = painterResource(id = R.drawable.ic_ghost_24),
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val title = when (item) {
                is SearchItem.Event -> item.festivo.title.ifBlank { stringResource(id = R.string.no_title) }
                is SearchItem.Note -> stringResource(id = R.string.note_label)
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = CalendarioTheme.colors.textSystem,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.date.format(fmt),
                fontSize = 12.sp,
                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
            )
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
