@file:Suppress("DEPRECATION")

package com.example.calendario

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun BackupScreen(
    onBackPress: () -> Unit,
    viewModel: CalendarioViewModel,
    onHistoryClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing = uiState.isSyncing
    val isRestoring = uiState.isRestoring

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var permissionsUpdateTrigger by remember { mutableIntStateOf(0) }

    // --- Dialog States ---
    var showUnlinkAccountDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var showCleaningDialog by remember { mutableStateOf(false) }
    var isScanningCleaning by remember { mutableStateOf(false) }
    var cleaningStatusMessage by remember { mutableStateOf<String?>(null) }

    // --- Launchers ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    val account = task.result
                    SettingsManager.saveGoogleAccountEmail(context, account?.email)
                    SettingsManager.saveGoogleAccountPhotoUrl(context, account?.photoUrl?.toString())
                    permissionsUpdateTrigger++
                    context.showToast(R.string.account_linked_success)
                } else {
                    context.showToast(R.string.account_linked_error)
                }
            }
        }
    )

    val importCvoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.processExternalCvo(uri) { success, error, _ ->
                    if (success) {
                        context.showToast(R.string.import_success)
                    } else {
                        context.showToast(error ?: "Error al importar archivo .cvo")
                    }
                }
            }
        }
    )

    // --- States (Con escudos de seguridad v3.1.34) ---
    val lastBackupTimestamp = remember(permissionsUpdateTrigger, isSyncing, isRestoring) { 
        SettingsManager.getLastBackupTime(context)
    }
    val lastBackupSize = remember(permissionsUpdateTrigger, isSyncing, isRestoring) { 
        SettingsManager.getLastBackupSize(context)
    }

    var pendingBackupFreq by remember { 
        val auto = SettingsManager.isAutoBackupEnabled(context)
        val f = if (!auto) "manual" else SettingsManager.getBackupFrequency(context)
        mutableStateOf(f)
    }

    AppScreen(
        title = stringResource(id = R.string.data_center_screen_title),
        onBackClick = onBackPress
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            // --- SECCIÓN 1: COPIA DE SEGURIDAD ---
            SectionTitle(text = stringResource(id = R.string.backup_section_title_label), isFirst = true)
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val email = remember(permissionsUpdateTrigger) { SettingsManager.getGoogleAccountEmail(context) }
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
                    
                    // --- FILA ÚLTIMA COPIA ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onHistoryClick() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.last_backup_label), 
                            color = CalendarioTheme.colors.textSystem, 
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        val lastStr = if (lastBackupTimestamp == 0L) {
                            stringResource(R.string.never)
                        } else {
                            AppFormats.dateTimeShort(locale).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(lastBackupTimestamp))
                        }
                        
                        val sizeStr = if (lastBackupSize > 0) {
                            if (lastBackupSize < 1024 * 1024) {
                                " %.2fKB".format(Locale.US, lastBackupSize / 1024.0)
                            } else {
                                " %.2fMB".format(Locale.US, lastBackupSize / (1024.0 * 1024.0))
                            }
                        } else ""

                        Text(
                            text = "$lastStr$sizeStr", 
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), 
                            fontSize = 13.sp, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                    BackupActionRow(
                        text = stringResource(id = R.string.sincronizar_label),
                        icon = Icons.Default.Sync,
                        isRotating = isSyncing,
                        onClick = { viewModel.syncHistoryToDrive(context) { if (it.success) { permissionsUpdateTrigger++; context.showToast(context.applicationContext.getString(R.string.sync_success_detailed, it.totalEvents), Toast.LENGTH_LONG) } else { context.showToast(R.string.sync_error_drive) } } }
                    )

                    HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                    BackupActionRow(
                        text = stringResource(id = R.string.restaurar_label),
                        icon = painterResource(id = R.drawable.ic_restore_custom),
                        isRotating = isRestoring,
                        onClick = { showConfirmRestoreDialog = true }
                    )
                }
            }

            // --- SECCIÓN 2: BASE DE DATOS ---
            SectionTitle(text = stringResource(id = R.string.database_section_label))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                BackupActionRow(
                    text = stringResource(id = R.string.import_cvo_option_label),
                    icon = painterResource(id = R.drawable.ic_folder_open_custom),
                    onClick = { importCvoLauncher.launch(arrayOf("*/*")) }
                )

                HorizontalDivider(color = CalendarioTheme.colors.settingsBackground, thickness = 1.dp)

                BackupActionRow(
                    text = "Optimizar",
                    icon = Icons.Default.CleaningServices,
                    onClick = { showCleaningDialog = true }
                )
            }
        }

        // --- Diálogos ---
        if (showUnlinkAccountDialog) { 
            AppDialog(
                onDismissRequest = { showUnlinkAccountDialog = false },
                title = stringResource(id = R.string.unlink_google_account),
                confirmButton = { 
                    DialogConfirmButton(
                        text = stringResource(id = R.string.unlink_action), 
                        onClick = { 
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener { 
                                SettingsManager.saveGoogleAccountEmail(context, null)
                                SettingsManager.saveGoogleAccountPhotoUrl(context, null)
                                permissionsUpdateTrigger++
                                showUnlinkAccountDialog = false 
                            } 
                        }, 
                        color = Color.Red
                    ) 
                }, 
                dismissButton = { DialogDismissButton(onDismiss = { showUnlinkAccountDialog = false }) }
            ) {
                Text(stringResource(id = R.string.unlink_account_confirmation))
            }
        }

        if (showFrequencyDialog) { 
            BackupFrequencyDialog(
                selection = pendingBackupFreq, 
                onConfirm = { freq -> 
                    pendingBackupFreq = freq
                    SettingsManager.saveBackupFrequency(context, freq)
                    if (freq != "manual") BackupScheduler.scheduleBackup(context, freq) else BackupScheduler.cancelBackup(context)
                    showFrequencyDialog = false 
                }, 
                onDismiss = { showFrequencyDialog = false }
            ) 
        }

        if (showConfirmRestoreDialog) { 
            ConfirmRestoreDialog(
                onDismiss = { showConfirmRestoreDialog = false }, 
                onConfirm = { 
                    showConfirmRestoreDialog = false
                    val callback: (Boolean) -> Unit = { success -> 
                        if (success) { 
                            context.applicationContext.showToast(R.string.restore_success)
                            (context as? Activity)?.let { a -> a.finish(); a.startActivity(a.intent) } 
                        } else {
                            context.showToast(R.string.restore_error, Toast.LENGTH_LONG) 
                        }
                    }
                    viewModel.restoreHistoryFromDrive(
                        context = context,
                        restorePrefs = true,
                        restoreHolidays = true,
                        restoreNotes = true,
                        restoreEvents = true,
                        onComplete = callback
                    )
                }
            )
        }

        if (showCleaningDialog) {
            CleaningAssistantDialog(
                uiState = uiState,
                isScanning = isScanningCleaning,
                statusMessage = cleaningStatusMessage,
                onScan = {
                    isScanningCleaning = true
                    cleaningStatusMessage = context.applicationContext.getString(R.string.analyzing_data)
                    viewModel.updateCleaningCandidates()
                    isScanningCleaning = false
                    cleaningStatusMessage = context.applicationContext.getString(R.string.analysis_finished)
                },
                onDelete = { item -> viewModel.deleteCleaningCandidate(item) },
                onDeleteAll = { viewModel.deleteAllCleaningCandidates() },
                onNavigateToDate = { _ -> showCleaningDialog = false },
                onDismiss = { showCleaningDialog = false }
            )
        }
    }
}

@Composable
private fun BackupActionRow(
    text: String,
    icon: Any,
    isRotating: Boolean = false,
    @Suppress("SAME_PARAMETER_VALUE")
    reverseRotation: Boolean = true,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "angle"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(enabled = !isRotating, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = CalendarioTheme.colors.textSystem, fontSize = 16.sp, modifier = Modifier.weight(1f))
        
        val iconModifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                if (isRotating) rotationZ = if (reverseRotation) -rotation else rotation
            }

        when (icon) {
            is androidx.compose.ui.graphics.vector.ImageVector -> {
                Icon(imageVector = icon, contentDescription = null, modifier = iconModifier, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f))
            }
            is Painter -> {
                Icon(painter = icon, contentDescription = null, modifier = iconModifier, tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f))
            }
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
    
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = R.string.backup_frequency),
        confirmButton = { 
            AdaptiveDialogButtons(confirmText = stringResource(id = R.string.accept), onConfirm = { onConfirm(tempSelection) }, onDismiss = onDismiss) 
        }
    ) {
        Column { 
            options.forEach { (key, labelRes) -> 
                Row(Modifier.fillMaxWidth().clickable { tempSelection = key }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { 
                    val isSelected = key == tempSelection
                    Text(text = stringResource(id = labelRes), modifier = Modifier.weight(1f), fontSize = 16.sp, color = CalendarioTheme.colors.textSystem, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                    if (isSelected) Icon(Icons.Default.Check, null, tint = CalendarioTheme.colors.cabecera.getCoherentColor(CalendarioTheme.colors.fondoDialogos)) 
                } 
            } 
        } 
    }
}

@Composable
private fun ConfirmRestoreDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(id = R.string.confirm_restore_title),
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.accept), 
                onClick = onConfirm
            ) 
        }, 
        dismissButton = { 
            DialogDismissButton(onDismiss = onDismiss) 
        }
    ) {
        Text(
            text = stringResource(id = R.string.restore_total_confirmation),
            color = CalendarioTheme.colors.textSystem,
            fontSize = 16.sp
        ) 
    }
}
