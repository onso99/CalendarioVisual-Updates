package com.example.calendario

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val localCalendarsLabel = stringResource(id = R.string.local_calendars_label)

    // Agrupación de calendarios por cuenta
    val rawGroupedCalendars = remember(availableCalendars) {
        availableCalendars.groupBy { cal ->
            if (cal.accountName.isNotBlank()) cal.accountName else localCalendarsLabel
        }
    }

    // Ordenar cuentas: Cuentas Google primero, luego las demás, luego locales
    val sortedAccountEntries = remember(rawGroupedCalendars, localCalendarsLabel) {
        rawGroupedCalendars.entries.sortedWith(
            compareByDescending<Map.Entry<String, List<CalendarInfo>>> { entry ->
                val name = entry.key
                name.contains("gmail.com", ignoreCase = true) || name.contains("google", ignoreCase = true)
            }.thenBy { entry ->
                if (entry.key == localCalendarsLabel) "z_local" else entry.key.lowercase()
            }
        )
    }

    // Cuentas desplegadas por defecto: Cuentas de Google (o la primera si no hay Google)
    var expandedAccounts by remember(sortedAccountEntries) {
        val defaultExpanded = sortedAccountEntries.filter { entry ->
            val name = entry.key
            name.contains("gmail.com", ignoreCase = true) || name.contains("google", ignoreCase = true)
        }.map { it.key }.toSet()

        mutableStateOf(
            if (defaultExpanded.isNotEmpty()) defaultExpanded
            else sortedAccountEntries.firstOrNull()?.let { setOf(it.key) } ?: emptySet()
        )
    }

    AppScreen(
        title = stringResource(id = R.string.calendars),
        onBackClick = { 
            onApplySelection(currentIds)
            onBackPress() 
        },
        bannerMessage = infoMessage,
        isBannerVisible = infoMessage != null,
        bannerIcon = if (infoMessage == favUpdatedMsg) Icons.Default.Star else Icons.Default.Info,
        bannerIconColor = CalendarioTheme.colors.cabecera,
        scrollable = false
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AppLayout.ScreenHorizontalPadding, end = AppLayout.ScreenHorizontalPadding, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sortedAccountEntries.forEach { (accountName, calendarsInAccount) ->
                val isExpanded = expandedAccounts.contains(accountName)
                val sortedAccountCalendars = calendarsInAccount.sortedWith(
                    compareByDescending<CalendarInfo> { it.id == currentFavoriteId }
                        .thenByDescending { currentIds.contains(it.id) }
                        .thenBy { it.displayName }
                )

                item(key = "account_card_$accountName") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CalendarioTheme.colors.fondoSecciones)
                    ) {
                        // CABECERA DE CUENTA (16.sp Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedAccounts = if (isExpanded) {
                                        expandedAccounts - accountName
                                    } else {
                                        expandedAccounts + accountName
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (accountName == localCalendarsLabel) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.cabecera,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = accountName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CalendarioTheme.colors.textSystem,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "(${calendarsInAccount.size})",
                                fontSize = 14.sp,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // FILAS DE CALENDARIOS DENTRO DE LA CUENTA
                        if (isExpanded) {
                            HorizontalDivider(
                                color = CalendarioTheme.colors.settingsBackground,
                                thickness = 1.dp
                            )

                            sortedAccountCalendars.forEachIndexed { index, cal ->
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
                                        .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(cal.color))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cal.displayName, 
                                            fontWeight = if (isFavorite) FontWeight.Bold else if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = if (isFavorite) CalendarioTheme.colors.cabecera else CalendarioTheme.colors.textSystem,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!cal.canModify) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = stringResource(id = R.string.calendar_read_only_error),
                                                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    if (isFavorite) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = CalendarioTheme.colors.cabecera,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CalendarioTheme.colors.cabecera,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                if (index < sortedAccountCalendars.size - 1) {
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
        }
    }
}
