package com.example.calendario

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageCalendarsScreen(
    onBackPress: () -> Unit,
    availableCalendars: List<CalendarInfo>,
    initialSelectedIds: Set<Long>,
    favoriteCalendarId: Long?,
    onApplySelection: (Set<Long>) -> Unit,
    onSetFavorite: (Long) -> Unit
) {
    var currentIds by remember(initialSelectedIds) { mutableStateOf(initialSelectedIds) }
    var currentFavoriteId by remember(favoriteCalendarId) { mutableStateOf(favoriteCalendarId) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    
    val hintMsg = stringResource(id = R.string.hint_long_press_favorite)
    val favUpdatedMsg = stringResource(id = R.string.favorite_updated)
    val readOnlyMsg = stringResource(id = R.string.calendar_read_only_error)

    // Inicialización y mensaje de ayuda
    LaunchedEffect(Unit) {
        if (currentFavoriteId == null) {
            findBestCalendarCandidate(availableCalendars)?.let { candidate ->
                onSetFavorite(candidate.id)
                currentFavoriteId = candidate.id
                currentIds = currentIds + candidate.id
            }
        }
        infoMessage = hintMsg
    }

    LaunchedEffect(infoMessage) {
        if (infoMessage != null) {
            delay(5000.milliseconds)
            infoMessage = null
        }
    }

    val sortedCalendars = remember(availableCalendars, currentIds, currentFavoriteId) {
        availableCalendars.sortedWith(
            compareByDescending<CalendarInfo> { it.id == currentFavoriteId }
                .thenByDescending { currentIds.contains(it.id) }
                .thenBy { it.accountName }
                .thenBy { it.displayName }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.calendars), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { 
                        onApplySelection(currentIds)
                        onBackPress() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            InfoBanner(
                message = infoMessage,
                isVisible = infoMessage != null,
                icon = if (infoMessage == favUpdatedMsg) Icons.Default.Star else Icons.Default.Info,
                iconColor = CalendarioTheme.colors.cabecera
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = AppLayout.ScreenHorizontalPadding, end = AppLayout.ScreenHorizontalPadding, bottom = 16.dp, top = AppLayout.TopToSectionPadding) 
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarioTheme.colors.fondoSecciones)
            ) {
                items(sortedCalendars) { cal ->
                    val isFavorite = cal.id == currentFavoriteId
                    val isSelected = currentIds.contains(cal.id) || isFavorite

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { 
                                    if (!isFavorite) {
                                        val set = currentIds.toMutableSet()
                                        if (set.contains(cal.id)) set.remove(cal.id) else set.add(cal.id)
                                        currentIds = set 
                                    }
                                },
                                onLongClick = {
                                    if (cal.canModify) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSetFavorite(cal.id)
                                        currentFavoriteId = cal.id
                                        currentIds = currentIds + cal.id
                                        infoMessage = favUpdatedMsg
                                    } else {
                                        infoMessage = readOnlyMsg
                                    }
                                }
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) { 
                            Text(
                                text = cal.displayName, 
                                fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 16.sp,
                                color = if (isFavorite) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem
                            )
                            Text(
                                text = cal.accountName, 
                                fontSize = 13.sp,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                            ) 
                        }
                        
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.cabecera,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.cabecera,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    if (cal != sortedCalendars.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CalendarioTheme.colors.settingsBackground,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}
