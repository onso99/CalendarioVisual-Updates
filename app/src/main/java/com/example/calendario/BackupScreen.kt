package com.example.calendario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBackPress: () -> Unit,
    viewModel: CalendarioViewModel,
    onHistoryClick: () -> Unit = {},
    onNavigateToDate: (LocalDate) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing = uiState.isSyncing
    val isRestoring = uiState.isRestoring

    val context = LocalContext.current
    val appPrefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    var permissionsUpdateTrigger by remember { mutableIntStateOf(0) }

    // --- Dialog States ---
    var showUnlinkAccountDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var restoreSource by remember { mutableStateOf<String?>(null) }
    var pendingLocalUri by remember { mutableStateOf<Uri?>(null) }

    // --- Mantenimiento States ---
    var isScanning by remember { mutableStateOf(false) }
    var scanFinished by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val analyzingDataMsg = stringResource(id = R.string.analyzing_data)
    val analysisFinishedMsg = stringResource(id = R.string.analysis_finished)

    LaunchedEffect(isScanning) {
        if (isScanning) {
            statusMessage = analyzingDataMsg
            viewModel.refreshData {
                isScanning = false
                scanFinished = true
                statusMessage = analysisFinishedMsg
            }
        }
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage == analysisFinishedMsg) {
            delay(3000.milliseconds)
            statusMessage = null
        }
    }

    // --- Launchers ---
    val exportFullBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val selIds = uiState.selectedCalendarIds
                        val favId = uiState.favoriteCalendarId
                        BackupManager.exportFullBackup(context, uri, selIds, favId)
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
                    showConfirmRestoreDialog = true
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
    val lastBackupTimestamp = remember(permissionsUpdateTrigger, isSyncing) { 
        appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_TIME, 0L)
    }
    val lastBackupSize = remember(permissionsUpdateTrigger, isSyncing) { 
        appPrefs.getLong(AppConstants.KEY_LAST_BACKUP_SIZE, 0L)
    }

    var pendingBackupFreq by remember { 
        val auto = appPrefs.getBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, false)
        val f = if (!auto) "manual" else appPrefs.getString(AppConstants.KEY_BACKUP_FREQUENCY, "manual") ?: "manual"
        mutableStateOf(f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.backup_section_title_label), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { 
                    IconButton(onClick = onBackPress) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary) 
                    } 
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onPrimary)
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
            // --- SECCIÓN GOOGLE DRIVE ---
            SectionTitle(text = stringResource(id = R.string.drive_label))
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        var dateFontSize by remember { mutableStateOf(14.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.last_backup_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            val lastStr = if (lastBackupTimestamp == 0L) stringResource(R.string.never) else DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(lastBackupTimestamp))
                            val sizeStr = if (lastBackupSize > 0) " · ${"%.2f".format(Locale.US, lastBackupSize / (1024.0 * 1024.0))}MB" else ""
                            Text(text = "$lastStr$sizeStr", color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), fontSize = dateFontSize, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, onTextLayout = { if (it.hasVisualOverflow && dateFontSize > 11.sp) dateFontSize = (dateFontSize.value - 1f).sp })
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val bg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                            SettingsActionChip(text = stringResource(id = R.string.restaurar_label), icon = painterResource(id = R.drawable.ic_restore_custom), isIconRotating = isRestoring, reverseRotation = true, modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { restoreSource = "drive"; showConfirmRestoreDialog = true })
                            SettingsActionChip(text = stringResource(id = R.string.sincronizar_label), icon = Icons.Default.Sync, isIconRotating = isSyncing, modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { viewModel.syncHistoryToDrive(context) { if (it.success) { permissionsUpdateTrigger++; Toast.makeText(context, context.applicationContext.getString(R.string.sync_success_detailed, it.totalEvents), Toast.LENGTH_LONG).show() } else { Toast.makeText(context, R.string.sync_error_drive, Toast.LENGTH_SHORT).show() } } } )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN COPIA LOCAL (Acción Directa) ---
            SectionTitle(text = stringResource(id = R.string.preferences_backup_label))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val bg = CalendarioTheme.colors.textSystem.copy(alpha = 0.05f)
                    SettingsActionChip(text = stringResource(id = R.string.restaurar_label), icon = painterResource(id = R.drawable.ic_restore_custom), isIconRotating = isRestoring, reverseRotation = true, modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { importFullBackupLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }) })
                    SettingsActionChip(text = stringResource(id = R.string.guardar_label), icon = Icons.Default.Save, modifier = Modifier.weight(1f).height(44.dp), containerColor = bg, onClick = { 
                        val suggested = "calendariovisual_backup_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.json"
                        exportFullBackupLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, suggested) }) 
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN MANTENIMIENTO (Integrada v3.1.09) ---
            SectionTitle(text = stringResource(id = R.string.maintenance_section))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(id = R.string.data_cleaning_label), color = CalendarioTheme.colors.textSystem, fontSize = 16.sp)
                        statusMessage?.let {
                            Text(text = it, fontSize = 12.sp, color = CalendarioTheme.colors.cabecera, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    if (uiState.cleaningCandidates.isNotEmpty()) {
                        Text(
                            text = "(${uiState.cleaningCandidates.size})",
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Button(
                        onClick = { isScanning = true },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.12f), contentColor = CalendarioTheme.colors.cabecera),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(stringResource(id = R.string.scan_label), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (scanFinished) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (uiState.cleaningCandidates.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.no_cleaning_results), color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                    } else {
                        // Lista de candidatos integrada (sin LazyColumn para evitar conflictos de scroll)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.cleaningCandidates.take(10).forEach { item ->
                                InternalCleaningCandidateRow(
                                    item = item,
                                    onClick = { onNavigateToDate(item.date) },
                                    onDelete = { viewModel.deleteCleaningCandidate(item) }
                                )
                            }
                            if (uiState.cleaningCandidates.size > 10) {
                                Text(
                                    text = "... y ${uiState.cleaningCandidates.size - 10} más",
                                    fontSize = 12.sp,
                                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 40.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { viewModel.deleteAllCleaningCandidates() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(id = R.string.delete_all_events_option).substringBefore(" "), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Diálogos ---
        if (showUnlinkAccountDialog) { 
            AlertDialog(
                onDismissRequest = { showUnlinkAccountDialog = false }, 
                containerColor = CalendarioTheme.colors.fondoDialogos, 
                titleContentColor = CalendarioTheme.colors.textSystem, 
                textContentColor = CalendarioTheme.colors.textSystem, 
                title = { Text(stringResource(id = R.string.unlink_google_account), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
                text = { Text(stringResource(id = R.string.unlink_account_confirmation)) }, 
                confirmButton = { 
                    DialogConfirmButton(
                        text = stringResource(id = R.string.unlink_action), 
                        onClick = { 
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener { 
                                appPrefs.edit { remove("google_account_email") }; 
                                permissionsUpdateTrigger++; 
                                showUnlinkAccountDialog = false 
                            } 
                        }, 
                        color = Color.Red
                    ) 
                }, 
                dismissButton = { DialogDismissButton(onDismiss = { showUnlinkAccountDialog = false }) }
            ) 
        }

        if (showFrequencyDialog) { 
            BackupFrequencyDialog(
                selection = pendingBackupFreq, 
                onConfirm = { freq -> 
                    pendingBackupFreq = freq; 
                    appPrefs.edit { 
                        val enabled = freq != "manual"; 
                        putBoolean(AppConstants.KEY_AUTO_BACKUP_DRIVE, enabled); 
                        putString(AppConstants.KEY_BACKUP_FREQUENCY, freq) 
                    }; 
                    if (freq != "manual") BackupScheduler.scheduleBackup(context, freq) else BackupScheduler.cancelBackup(context); 
                    showFrequencyDialog = false 
                }, 
                onDismiss = { showFrequencyDialog = false }
            ) 
        }

        if (showConfirmRestoreDialog) { 
            ConfirmRestoreDialog(
                onDismiss = { 
                    showConfirmRestoreDialog = false
                    restoreSource = null
                    pendingLocalUri = null 
                }, 
                onConfirm = { 
                    showConfirmRestoreDialog = false
                    val callback: (Boolean) -> Unit = { success -> 
                        if (success) { 
                            Toast.makeText(context.applicationContext, R.string.restore_success, Toast.LENGTH_SHORT).show()
                            (context as? Activity)?.let { a -> a.finish(); a.startActivity(a.intent) } 
                        } else {
                            Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show() 
                        }
                    }
                    when (restoreSource) {
                        "drive" -> viewModel.restoreHistoryFromDrive(
                            context = context,
                            restorePrefs = true,
                            restoreHolidays = true,
                            restoreNotes = true,
                            restoreEvents = true,
                            onComplete = callback
                        )
                        "local" -> {
                            if (pendingLocalUri != null) {
                                viewModel.restoreFromLocal(
                                    context = context,
                                    uri = pendingLocalUri!!,
                                    restorePrefs = true,
                                    restoreHolidays = true,
                                    restoreNotes = true,
                                    restoreEvents = true,
                                    onComplete = callback
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun InternalCleaningCandidateRow(
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
            .background(CalendarioTheme.colors.settingsBackground) // Fondo más profundo
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item is SearchItem.Event) {
            Icon(painterResource(id = R.drawable.ic_ghost_24), null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.AutoMirrored.Filled.StickyNote2, null, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val title = when (item) {
                is SearchItem.Event -> item.festivo.title.ifBlank { stringResource(id = R.string.no_title) }
                is SearchItem.Note -> stringResource(id = R.string.note_label)
            }
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CalendarioTheme.colors.textSystem, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = item.date.format(fmt), fontSize = 11.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f))
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BackupFrequencyDialog(selection: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tempSelection by remember { mutableStateOf(selection) }
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
        title = { Text(stringResource(id = R.string.backup_frequency), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }, 
        text = { 
            Column { 
                options.forEach { (key, labelRes) -> 
                    Row(Modifier.fillMaxWidth().clickable { tempSelection = key }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { 
                        val isSelected = key == tempSelection
                        Text(text = stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                        if (isSelected) Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera) 
                    } 
                } 
            } 
        }, 
        confirmButton = { 
            AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempSelection) }, onDismiss = onDismiss) 
        }
    )
}

@Composable
private fun ConfirmRestoreDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = CalendarioTheme.colors.fondoDialogos, 
        title = { 
            Text(
                text = stringResource(id = R.string.confirm_restore_title), 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp, 
                modifier = Modifier.fillMaxWidth(), 
                textAlign = TextAlign.Start
            ) 
        }, 
        text = { 
            Text(
                text = stringResource(id = R.string.restore_total_confirmation),
                color = CalendarioTheme.colors.textSystem,
                fontSize = 16.sp
            ) 
        }, 
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.accept), 
                onClick = onConfirm
            ) 
        }, 
        dismissButton = { 
            DialogDismissButton(onDismiss = onDismiss) 
        }
    )
}
