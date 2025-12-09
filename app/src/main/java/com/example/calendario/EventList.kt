package com.example.calendario

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.blendWithBackground
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
    Box(modifier = modifier) {
        if (finalEventsToList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCurrentMonthView && !showAllEvents) "No hay eventos pendientes para este mes." else "No hay eventos para este mes.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        val esFestivo = festivo.isFromHolidaySource && festivo.title.isNotBlank()
                        val esCumpleanos = (festivo.title.contains("cumpleaños", true) || festivo.title.contains("aniversario", true)) && !esFestivo

                        val textColor = if (isTodayEvents) {
                            val highlightColor = CalendarioTheme.colors.todayHighlightColor
                            val backgroundColor = MaterialTheme.colorScheme.background
                            val finalBlendedColor = blendWithBackground(highlightColor, backgroundColor)
                            if (isColorDark(finalBlendedColor)) Color.White else Color.Black
                        } else {
                            when {
                                esFestivo -> CalendarioTheme.colors.textSundayHoliday
                                esCumpleanos -> CalendarioTheme.colors.textBirthday
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        }
                        
                        val iconColor = if (isTodayEvents) textColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

                        val baseDesc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.title.ifEmpty { "(Sin título)" }}"
                        else festivo.title.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }

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
                                            contentDescription = "Evento repetido",
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
