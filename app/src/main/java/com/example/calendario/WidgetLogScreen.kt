package com.example.calendario

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var logText by remember { mutableStateOf(LogCollector.getLogs()) }
    val nextRefresh = LogCollector.getNextRefreshTime(context).let {
        when (it) {
            "OFF" -> stringResource(id = R.string.system_default).uppercase()
            "PENDING" -> stringResource(id = R.string.next_refresh_pending)
            "NOT_SCHEDULED" -> stringResource(id = R.string.not_scheduled)
            else -> it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(id = R.string.debug_title),
                            fontSize = 18.sp,
                        )
                        Text(
                            stringResource(id = R.string.next_refresh_label, nextRefresh),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            LogCollector.addLog(">>> MANUAL: Sincronización de alarmas forzada por el usuario.")
                            val purged = AlarmUtils.rescheduleAllAlarms(context)
                            logText = LogCollector.getLogs() // Refrescar pantalla
                            val msg = context.applicationContext.getString(R.string.sync_alarms_success, purged)
                    context.showToast(msg, Toast.LENGTH_LONG)
                        },
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = stringResource(id = R.string.sync_alarms),
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("logs", logText),
                                    ),
                                )
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(id = R.string.copy_action),
                        )
                    }
                    IconButton(
                        onClick = {
                            LogCollector.clear()
                            logText = ""
                        },
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.clear),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (logText.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        stringResource(id = R.string.no_logs_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(logText.split("\n")) { line ->
                        Text(
                            text = line,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
