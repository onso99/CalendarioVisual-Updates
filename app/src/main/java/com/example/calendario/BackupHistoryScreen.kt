package com.example.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val history = remember { BackupHistoryManager.loadEntries(context) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_section_title_label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CalendarioTheme.colors.cabecera,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_logs_found),
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { entry ->
                    val dateStr = Instant.ofEpochMilli(entry.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                    
                    val sourceStr = when (entry.source) {
                        BackupSource.DRIVE -> stringResource(R.string.drive_label)
                        BackupSource.LOCAL -> stringResource(R.string.preferences_backup_label)
                    }

                    val actionStr = when (entry.action) {
                        BackupAction.SAVE -> stringResource(R.string.guardar_label)
                        BackupAction.RESTORE -> stringResource(R.string.restaurar_label)
                        BackupAction.SYNC -> stringResource(R.string.sincronizar_label)
                    }

                    val contentStr = if (entry.isSuccess) {
                        val parts = mutableListOf<String>()
                        if (entry.eventsCount > 0) parts.add("${entry.eventsCount} ${stringResource(R.string.restore_events_label)}")
                        if (entry.notesCount > 0) parts.add("${entry.notesCount} ${stringResource(R.string.restore_notes_label)}")
                        if (entry.includePrefs) parts.add(stringResource(R.string.restore_prefs_label))
                        
                        val base = parts.joinToString(", ")
                        val size = if (entry.sizeBytes > 0) {
                            " (%.2f MB)".format(Locale.US, entry.sizeBytes / (1024.0 * 1024.0))
                        } else ""
                        ": $base.$size"
                    } else {
                        val errorBase = stringResource(R.string.error)
                        val detail = entry.errorMessageRes?.let { stringResource(it) } 
                            ?: entry.technicalError?.let { " ($it)" } ?: ""
                        ": $errorBase$detail"
                    }

                    Column {
                        Text(
                            text = "$dateStr - $sourceStr",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalendarioTheme.colors.textSystem
                        )
                        Text(
                            text = "$actionStr$contentStr",
                            fontSize = 14.sp,
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
