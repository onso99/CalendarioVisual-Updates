package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyEventList(
    modifier: Modifier = Modifier,
    finalEventsToList: List<Pair<LocalDate, List<Festivo>>>,
    lazyListState: LazyListState,
    isCurrentMonthView: Boolean,
    showAllEvents: Boolean,
    today: LocalDate,
    onEventClick: (Festivo) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val event1Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "") ?: "" }
    val event2Keyword = remember { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "") ?: "" }

    Box(modifier = modifier) {
        if (finalEventsToList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCurrentMonthView && !showAllEvents) stringResource(id = R.string.no_pending_events_this_month) else stringResource(id = R.string.no_events_this_month),
                    fontSize = 16.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                itemsIndexed(finalEventsToList, key = { _, (date, festivos) -> date.toString() + festivos.firstOrNull()?.id }) { _, (date, festivos) ->
                    val isTodayEvents = isCurrentMonthView && date == today
                    festivos.forEach { festivo ->

                        val normalizedTitle = festivo.title.unaccent().lowercase()
                        val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
                        val esCumpleanos = festivo.isBirthday && !esFestivo
                        val esEvento1 = event1Keyword.isNotBlank() && normalizedTitle.contains(event1Keyword.unaccent().lowercase())
                        val esEvento2 = event2Keyword.isNotBlank() && normalizedTitle.contains(event2Keyword.unaccent().lowercase())

                        val textColor = if (isTodayEvents) {
                            val highlightColor = CalendarioTheme.colors.todayHighlightColor
                            val backgroundColor = MaterialTheme.colorScheme.background
                            if (isColorDark(highlightColor, backgroundColor)) Color.White else Color.Black
                        } else {
                            when {
                                esEvento1 -> CalendarioTheme.colors.textEvent1
                                esEvento2 -> CalendarioTheme.colors.textEvent2
                                esFestivo -> CalendarioTheme.colors.textSundayHoliday
                                esCumpleanos -> CalendarioTheme.colors.textBirthday
                                else -> CalendarioTheme.colors.textEventDefault
                            }
                        }
                        
                        val iconColor = if (isTodayEvents) textColor else CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)

                        val noTitle = stringResource(id = R.string.no_title)
                        val allDayEvent = stringResource(id = R.string.all_day_event)
                        val baseDesc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { noTitle }}"
                        else festivo.title.ifEmpty { if (festivo.isAllDay) allDayEvent else "" }

                        val displayDesc = if (festivo.age != null) "$baseDesc (${festivo.age})" else baseDesc

                        if (displayDesc.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .then(
                                            if (isTodayEvents) {
                                                Modifier.background(CalendarioTheme.colors.todayHighlightColor)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onEventClick(festivo) }
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            String.format(Locale.getDefault(), "%02d", date.dayOfMonth),
                                            color = textColor,
                                            fontWeight = if (isTodayEvents) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            displayDesc,
                                            color = textColor,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    if (festivo.rrule != null) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(id = R.string.repeated_event),
                                            tint = iconColor,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(16.dp)
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
}