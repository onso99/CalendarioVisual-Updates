@file:Suppress("DEPRECATION")

package com.example.calendario

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Save
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
import androidx.core.graphics.ColorUtils
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    viewModel: CalendarioViewModel,
    onColorThemeClick: () -> Unit,
    onThemeUpdated: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onLogClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isChangingLanguage by remember { mutableStateOf(false) }
    var showWidgetColorExpand by remember { mutableStateOf(false) }
    var showFontExpand by remember { mutableStateOf(false) }

    // Estado para el tema cargado desde archivo (pero aún no aplicado)
    var importedThemeData by remember { mutableStateOf<Pair<ParsedTheme, String>?>(null) }

    // --- Launchers ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                permissionsUpdateTrigger++
                context.showToast(R.string.account_linked_success)
            }
        }
    )

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
                                context.showToast(R.string.theme_imported_successfully)
                            }
                            is ImportResult.Failure -> {
                                context.showToast(importResult.errorMessage, Toast.LENGTH_LONG)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error processing theme import", e)
                        context.showToast(R.string.could_not_read_file, Toast.LENGTH_LONG)
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
                            importedThemeData = null 
                            onThemeImported()
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error exporting theme", e)
                        context.showToast(R.string.error_saving_theme_file, Toast.LENGTH_LONG)
                    }
                }
            }
        }
    )

    // --- States ---
    val themeSetting by themeManager.themeSetting.collectAsState()

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
    val currentWidgetIds = pendingWidgetCalendarIds.filter { id -> uiState.availableCalendars.any { it.id == id } }.ifEmpty { uiState.selectedCalendarIds }
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
            SectionTitle(text = stringResource(id = R.string.general), topPadding = 8.dp)
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
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { showFontExpand = !showFontExpand }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.font), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (showFontExpand) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (showFontExpand) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón Chip "Negrita" (Estilo Tema)
                            val chipBg = if (pendingFontBold) CalendarioTheme.colors.cabecera.copy(alpha = 0.12f) else CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                            val chipBorder = if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)
                            val chipTextColor = if (pendingFontBold) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem
                            
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 90.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(18.dp))
                                    .clickable { 
                                        pendingFontBold = !pendingFontBold
                                        updateWidgetPrefs { putBoolean(WidgetConstants.KEY_WIDGET_FONT_BOLD, pendingFontBold) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.font_bold),
                                    color = chipTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (pendingFontBold) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Nombre de la fuente (Clickable para abrir diálogo)
                            val fontDisplayName = when(pendingFontFamily) {
                                WidgetConstants.FONT_FAMILY_SERIF -> stringResource(id = R.string.font_serif)
                                WidgetConstants.FONT_FAMILY_MONOSPACE -> stringResource(id = R.string.font_monospace)
                                WidgetConstants.FONT_FAMILY_CONDENSED -> stringResource(id = R.string.font_condensed)
                                WidgetConstants.FONT_FAMILY_SANS_SERIF -> stringResource(id = R.string.font_sans_serif)
                                else -> stringResource(id = R.string.font_system)
                            }

                            val previewFontFamily = when(pendingFontFamily) {
                                WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif
                                WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace
                                WidgetConstants.FONT_FAMILY_CONDENSED -> {
                                    val style = if (pendingFontBold) Typeface.BOLD else Typeface.NORMAL
                                    FontFamily(Typeface.create("sans-serif-condensed", style))
                                }
                                WidgetConstants.FONT_FAMILY_SANS_SERIF -> FontFamily.SansSerif
                                else -> FontFamily.Default
                            }
                            
                            Text(
                                text = fontDisplayName,
                                color = CalendarioTheme.colors.textSystem,
                                fontSize = 13.sp, // Reducido ligeramente para acomodar nombres largos
                                fontFamily = previewFontFamily,
                                // Si es condensada, el peso ya va en la familia arriba, para las demás usamos FontWeight.Bold
                                fontWeight = if (pendingFontBold && pendingFontFamily != WidgetConstants.FONT_FAMILY_CONDENSED) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showFontFamilyDialog = true }
                            )
                        }
                    }
                }
                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)
                Column {
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).clickable { showWidgetColorExpand = !showWidgetColorExpand }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.widget_colors_label), fontSize = 16.sp, modifier = Modifier.weight(1f), color = CalendarioTheme.colors.textSystem)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val isBgDark = ColorUtils.calculateLuminance(CalendarioTheme.colors.fondoSecciones.toArgb()) < 0.5
                            val borderColor = if (isBgDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f)
                            
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pendingWidgetBackgroundColor).border(0.5.dp, borderColor, CircleShape))
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pendingEventColor).border(0.5.dp, borderColor, CircleShape))
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pendingTodayEventColor).border(0.5.dp, borderColor, CircleShape))
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

            SectionTitle(text = stringResource(id = R.string.about))
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${stringResource(id = R.string.app_name)} ${AboutInfo.getVersionName(context)}", 
                        fontSize = 16.sp, 
                        color = CalendarioTheme.colors.textSystem,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onHistoryClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.History, 
                            contentDescription = null, 
                            tint = lerp(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.textSystem, 0.4f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                val haptic = LocalHapticFeedback.current
                var loggingEnabledInternal by remember { mutableStateOf(LogCollector.isLoggingEnabled(context)) }
                var debugClickCount by remember { mutableIntStateOf(0) }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${AboutInfo.LINE_3_AUTHOR} > ${AboutInfo.getFormattedDate()}", fontSize = 16.sp, color = CalendarioTheme.colors.textSystem, modifier = Modifier.weight(1f))
                    
                    Icon(
                        imageVector = Icons.Default.BugReport, 
                        contentDescription = null, 
                        tint = if (loggingEnabledInternal) CalendarioTheme.colors.textSystem else Color.Gray.copy(alpha = 0.4f), 
                        modifier = Modifier
                            .size(24.dp)
                            .combinedClickable(
                                onClick = { 
                                    if (loggingEnabledInternal) onLogClick() 
                                    else { 
                                        debugClickCount++
                                        if (debugClickCount >= 7) { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            loggingEnabledInternal = true
                                            LogCollector.setLoggingEnabled(context, true)
                                            debugClickCount = 0 
                                        } 
                                    } 
                                },
                                onLongClick = { 
                                    if (loggingEnabledInternal) { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        loggingEnabledInternal = false
                                        LogCollector.setLoggingEnabled(context, false)
                                        debugClickCount = 0 
                                    } 
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    )
                }
            }
        }

        // --- Dialogs ---
        if (showLanguageDialog) {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            LanguageSelectionDialog(currentLanguageCode = if (currentLocales.isEmpty) null else currentLocales.get(0)?.language, onLanguageSelected = { newCode -> scope.launch { isChangingLanguage = true; delay(1000.milliseconds); AppCompatDelegate.setApplicationLocales(if (newCode == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(newCode)); showLanguageDialog = false } }, onDismiss = { showLanguageDialog = false })
        }
        if (showThemeDialog) { ThemeSelectionDialog(currentTheme = themeSetting, onThemeSelected = { themeManager.setTheme(it); showThemeDialog = false }, onDismiss = { showThemeDialog = false }) }
        if (showWeekConfigDialog) { WeekConfigDialog(currentSelectionKey = pendingStartOfWeekKey, showWeekNumber = pendingShowWeekNumber, onConfirm = { key, show -> pendingStartOfWeekKey = key; pendingShowWeekNumber = show; updateAppPrefs { putString(AppConstants.KEY_START_OF_WEEK, key); putBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, show) }; showWeekConfigDialog = false }, onDismiss = { showWeekConfigDialog = false }) }
        if (showAlarmConfigDialog) { AlarmConfigDialog(anticipation = pendingAlarmOffset, snooze = pendingSnoozeInterval, onConfirm = { offset, interval -> pendingAlarmOffset = offset; pendingSnoozeInterval = interval; updateAppPrefs { putInt(AppConstants.KEY_DEFAULT_ALARM_OFFSET, offset.roundToInt()); putInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, interval.roundToInt()) }; showAlarmConfigDialog = false }, onDismiss = { showAlarmConfigDialog = false }) }
        if (showFontFamilyDialog) { FontFamilySelectionDialog(currentSelection = pendingFontFamily, onConfirm = { family -> pendingFontFamily = family; updateWidgetPrefs { putString(WidgetConstants.KEY_WIDGET_FONT_FAMILY, family) }; showFontFamilyDialog = false }, onDismiss = { showFontFamilyDialog = false }) }
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
        if (showDiscardChangesDialog) { AlertDialog(onDismissRequest = { showDiscardChangesDialog = false }, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Text(stringResource(id = R.string.discard_changes_confirmation)) }, confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.discard), onClick = { showDiscardChangesDialog = false; onBackPress() }, color = Color.Red) }, dismissButton = { DialogDismissButton(onDismiss = { showDiscardChangesDialog = false }) } ) }
        if (showExportDialog) { 
            val cleanId = lightThemeName?.removeSuffix("***") ?: "theme_1"
            val bundled = remember(cleanId) {
                BundledThemes.themes.find { (it["themeManifest"] as Map<*, *>)["id"] == cleanId }
            }
            val suggestedName = if (bundled != null) {
                stringResource(id = (bundled["themeManifest"] as Map<*, *>)["nameRes"] as Int)
            } else {
                cleanId
            }

            ExportThemeDialog(
                initialName = suggestedName,
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
        if (showWidgetCalendarDialog) { 
            val appActiveCalendars = uiState.availableCalendars.filter { uiState.selectedCalendarIds.contains(it.id) }
            val sanitizedInitialIds = pendingWidgetCalendarIds.filter { id -> appActiveCalendars.any { it.id == id } }.toSet()
            
            SelectWidgetCalendarsDialog(
                appActiveCalendars = appActiveCalendars, 
                initialSelectedIds = sanitizedInitialIds.ifEmpty { uiState.selectedCalendarIds }, 
                currentFavoriteId = uiState.favoriteCalendarId, 
                onApply = { newIds -> 
                    pendingWidgetCalendarIds = newIds
                    updateWidgetPrefs { putStringSet(WidgetConstants.KEY_WIDGET_SELECTED_CALENDARS, newIds.map { it.toString() }.toSet()) }
                    showWidgetCalendarDialog = false 
                }, 
                onDismissRequest = { showWidgetCalendarDialog = false }
            ) 
        }

        if (showPermissionsDialog) {
            PermissionsDialog(
                calStatus = PermissionChecker.getCalendarStatus(context),
                notifStatus = PermissionChecker.getNotificationsStatus(context),
                alarmStatus = PermissionChecker.getAlarmsStatus(context),
                driveStatus = PermissionChecker.getGoogleDriveStatus(context),
                batteryStatus = PermissionChecker.getBatteryOptimizationStatus(context),
                onDismiss = { showPermissionsDialog = false },
                onFix = { type ->
                    when (type) {
                        "calendar", "notifications" -> {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
                            context.startActivity(intent)
                        }
                        "alarms" -> {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                alarmManager?.canScheduleExactAlarms() ?: false
                            } else true

                            if (!canScheduleExact) {
                                // Caso 1: Falta permiso de alarmas exactas (Android 12+)
                                @SuppressLint("NewApi")
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.fromParts("package", context.packageName, null) }
                                context.startActivity(intent)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                // Caso 2: Alarmas exactas OK, pero falta Full Screen Intent (Android 14+)
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply { data = Uri.fromParts("package", context.packageName, null) }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    // Fallback preventivo a la info de la app
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
                                    context.startActivity(intent)
                                }
                            }
                        }
                        "drive" -> {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                                .build()
                            googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        }
                        "battery" -> {
                            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                // Si ya está concedido, abrimos los ajustes generales para que el usuario pueda verlo/cambiarlo
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } else {
                                // Si no está concedido, lanzamos la petición directa (el diálogo blanco)
                                @SuppressLint("BatteryLife")
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.fromParts("package", context.packageName, null) }
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            )
        }

        if (isChangingLanguage) {
            val transition = rememberInfiniteTransition(label = "lang_rotation")
            val rot by transition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rotation")
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) { Icon(Icons.Default.Language, null, modifier = Modifier.size(48.dp).graphicsLayer { rotationZ = rot }, tint = Color.White) }
        }
    }
}

@Composable
private fun ExportThemeDialog(initialName: String, onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(onDismissRequest = onDismissRequest, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.export_theme_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(id = R.string.theme_name)) }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) }, confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.export), onClick = { onConfirm(text.ifBlank { "nuevo_tema" }) }, enabled = text.isNotBlank()) }, dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) })
}

@Composable
private fun FontFamilySelectionDialog(currentSelection: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tempSelection by remember { mutableStateOf(currentSelection) }
    val options = listOf(WidgetConstants.FONT_FAMILY_SYSTEM to R.string.font_system, WidgetConstants.FONT_FAMILY_SANS_SERIF to R.string.font_sans_serif, WidgetConstants.FONT_FAMILY_SERIF to R.string.font_serif, WidgetConstants.FONT_FAMILY_MONOSPACE to R.string.font_monospace, WidgetConstants.FONT_FAMILY_CONDENSED to R.string.font_condensed)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CalendarioTheme.colors.fondoDialogos, titleContentColor = CalendarioTheme.colors.textSystem, textContentColor = CalendarioTheme.colors.textSystem, title = { Text(stringResource(id = R.string.font), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, text = { Column { options.forEach { (key, labelRes) -> val family = when(key) { WidgetConstants.FONT_FAMILY_SERIF -> FontFamily.Serif; WidgetConstants.FONT_FAMILY_MONOSPACE -> FontFamily.Monospace; WidgetConstants.FONT_FAMILY_CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL)); WidgetConstants.FONT_FAMILY_SANS_SERIF -> FontFamily.SansSerif; else -> FontFamily.Default }; Row(Modifier.fillMaxWidth().clickable { tempSelection = key }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { val isSelected = key == tempSelection; Text(text = stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = family, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal); if (isSelected) { Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.fondoDialogos.getContrastColor(MaterialTheme.colorScheme.background)) } } } } }, confirmButton = { AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempSelection) }, onDismiss = onDismiss) })
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
                            Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.fondoDialogos.getContrastColor(MaterialTheme.colorScheme.background)) 
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
    importedTheme: Pair<ParsedTheme, String>?, 
    onDismiss: () -> Unit,
    onThemeSelected: (ParsedTheme, String) -> Unit, 
    onLoadClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val effectiveId = currentThemeId ?: "theme_1"
    val cleanId = effectiveId.removeSuffix("***")
    
    val isCurrentBundled = remember(cleanId) {
        BundledThemes.themes.any { (it["themeManifest"] as Map<*, *>)["id"] == cleanId }
    }
    
    var tempSelectionId by remember(cleanId) { mutableStateOf(cleanId) }
    
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
                val hasCustomEntry = importedTheme != null || !isCurrentBundled
                val gridHeight = if (hasCustomEntry) 264.dp else 210.dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(gridHeight)
                ) {
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
                    
                    if (hasCustomEntry) {
                        item {
                            val customName = when {
                                importedTheme != null -> importedTheme.second
                                !isCurrentBundled -> cleanId
                                else -> ""
                            }
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
                    SettingsActionChip(
                        text = stringResource(id = R.string.cargar_label), 
                        icon = painterResource(id = R.drawable.ic_folder_open_custom), 
                        modifier = Modifier.weight(1f).height(44.dp), 
                        shape = RoundedCornerShape(12.dp), 
                        containerColor = backupButtonBg, 
                        onClick = onLoadClick
                    )
                    SettingsActionChip(
                        text = stringResource(id = R.string.guardar_label), 
                        icon = Icons.Outlined.Save, 
                        modifier = Modifier.weight(1f).height(44.dp), 
                        shape = RoundedCornerShape(12.dp), 
                        containerColor = backupButtonBg, 
                        onClick = onSaveClick
                    )
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
                                onDismiss()
                            } else {
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
    val textColor = color.getContrastColor(CalendarioTheme.colors.settingsBackground)
    val borderColor = CalendarioTheme.colors.fondoSecciones.getContrastColor(Color.White).copy(alpha = 0.2f)
    Box(modifier = modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).background(color).border(0.5.dp, borderColor, RoundedCornerShape(10.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp)) }
}

@Composable
private fun PermissionsDialog(calStatus: PermissionStatus, notifStatus: PermissionStatus, alarmStatus: PermissionStatus, driveStatus: PermissionStatus, batteryStatus: PermissionStatus, onDismiss: () -> Unit, onFix: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        titleContentColor = CalendarioTheme.colors.textSystem, 
        textContentColor = CalendarioTheme.colors.textSystem, 
        title = { Text(stringResource(id = R.string.permissions_dialog_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                PermissionRow(label = stringResource(id = R.string.calendar_permission_label), status = calStatus, fixLabel = if (calStatus == PermissionStatus.GRANTED) stringResource(R.string.status_granted) else stringResource(R.string.status_denied), onFix = { onFix("calendar") })
                PermissionRow(label = stringResource(id = R.string.notifications_permission_label), status = notifStatus, fixLabel = if (notifStatus == PermissionStatus.GRANTED) stringResource(R.string.status_granted_f) else stringResource(R.string.status_denied), onFix = { onFix("notifications") })
                PermissionRow(label = stringResource(id = R.string.alarms_permission_label), status = alarmStatus, fixLabel = if (alarmStatus == PermissionStatus.GRANTED) stringResource(R.string.status_full_screen) else stringResource(R.string.status_no_full_screen), onFix = { onFix("alarms") })
                PermissionRow(label = stringResource(id = R.string.google_drive_permission_label), status = driveStatus, fixLabel = if (driveStatus == PermissionStatus.GRANTED) stringResource(R.string.status_linked) else stringResource(R.string.status_unlinked), onFix = { onFix("drive") })
                PermissionRow(label = stringResource(id = R.string.battery_optimization_label), status = batteryStatus, fixLabel = if (batteryStatus == PermissionStatus.GRANTED) stringResource(R.string.status_unrestricted) else stringResource(R.string.status_optimized), onFix = { onFix("battery") }) 
            } 
        }, 
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.close), color = CalendarioTheme.colors.textSystem) } }
    )
}

@Composable
private fun PermissionRow(label: String, status: PermissionStatus, fixLabel: String, onFix: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) { 
        Box(modifier = Modifier.padding(top = 6.dp).size(10.dp).background(if (status == PermissionStatus.GRANTED) Color.Green else Color.Red, CircleShape))
        Column(modifier = Modifier.weight(1f)) { 
            Text(text = label, color = CalendarioTheme.colors.textSystem, fontSize = 15.sp, lineHeight = 20.sp)
            Text(text = fixLabel, color = if (status == PermissionStatus.DENIED) MaterialTheme.colorScheme.primary else CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), fontWeight = if (status == PermissionStatus.DENIED) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, modifier = Modifier.clickable { onFix() }.padding(vertical = 2.dp)) 
        } 
    }
}


