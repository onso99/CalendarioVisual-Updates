package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearlyCalendar(
    currentYear: Year,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(currentYear.value, it) }
    Column(
        Modifier
            .fillMaxSize()
            .background(CalendarioTheme.colors.settingsBackground)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        months.chunked(3).forEachIndexed { rowIndex, monthRow ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                monthRow.forEach { month ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMonthSelected(month) },
                        Alignment.Center
                    ) {
                        MiniMonthCalendar(
                            month = month,
                            today = today,
                            eventsByDate = eventsByDate,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                repeat(3 - monthRow.size) { Spacer(Modifier
                    .weight(1f)
                    .aspectRatio(1f)) }
            }
            if (rowIndex < months.chunked(3).size - 1) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun MiniMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    modifier: Modifier = Modifier
) {
    val daysOfWeekShort = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = month.lengthOfMonth()

    val compactTextStyle = LocalTextStyle.current.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val monthNameFontSize = 12.sp
    val dayHeadersFontSize = 8.sp
    val dayNumberFontSize = 9.sp

    Column(
        modifier.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar(Char::titlecase),
            fontSize = monthNameFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = compactTextStyle.copy(lineHeight = monthNameFontSize * 0.95f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            daysOfWeekShort.forEach { 
                Box(
                    Modifier
                        .weight(1f),
                    Alignment.Center
                ) {
                    Text(
                        it,
                        fontSize = dayHeadersFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = compactTextStyle.copy(lineHeight = dayHeadersFontSize * 0.95f)
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            val dayCellsData = remember(month) {
                List(6 * 7) {
                    val day = it - firstDayOfWeekIndex + 1
                    if (day in 1..daysInMonth) month.atDay(day) else null
                }
            }
            dayCellsData.chunked(7).forEach { weekDates ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    Arrangement.SpaceAround,
                    Alignment.CenterVertically
                ) {
                    weekDates.forEach { date ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            Alignment.Center
                        ) {
                            if (date != null) {
                                val dayEvents = eventsByDate[date].orEmpty()
                                val isToday = date == today
                                val isHoliday = dayEvents.any { it.isFromHolidaySource && it.title.isNotBlank() }
                                val isBirthday = dayEvents.any { it.title.contains("cumpleaños", true) || it.title.contains("aniversario", true) } && !isHoliday
                                val isSundayNonHoliday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY && !isHoliday && !isBirthday

                                val textColor = when {
                                    isHoliday -> CalendarioTheme.colors.textSundayHoliday
                                    isBirthday -> CalendarioTheme.colors.textBirthday
                                    isSundayNonHoliday -> CalendarioTheme.colors.textSundayHoliday
                                    else -> CalendarioTheme.colors.miniMonthDayNumberNormal
                                }
                                val fontWeightText = if (isToday) FontWeight.Bold else FontWeight.Normal

                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    if (isToday) {
                                        Box(
                                            Modifier
                                                .size((dayNumberFontSize.value * 2.1f).dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(CalendarioTheme.colors.miniMonthTodayHighlightBackground)
                                        )
                                    }
                                    Text(
                                        "${date.dayOfMonth}",
                                        fontSize = dayNumberFontSize,
                                        fontWeight = fontWeightText,
                                        color = textColor,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        style = compactTextStyle.copy(lineHeight = dayNumberFontSize * 0.95f)
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