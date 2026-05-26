package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun YearlyCalendar(
    currentYear: Year,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onMonthSelected: (YearMonth) -> Unit,
    showWeekNumber: Boolean,
    startOfWeek: DayOfWeek
) {
    val months = (1..12).map { YearMonth.of(currentYear.value, it) }
    val fontScale = LocalConfiguration.current.fontScale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .background(CalendarioTheme.colors.settingsBackground)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        months.chunked(3).forEach { monthRow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                            showWeekNumber = showWeekNumber,
                            startOfWeek = startOfWeek,
                            modifier = Modifier.fillMaxSize(),
                            fontScale = fontScale
                        )
                    }
                }
                repeat(3 - monthRow.size) { 
                    Spacer(Modifier.weight(1f).aspectRatio(1f)) 
                }
            }
        }
    }
}

@Composable
fun MiniMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    showWeekNumber: Boolean,
    startOfWeek: DayOfWeek,
    modifier: Modifier = Modifier,
    fontScale: Float
) {
    val daysOfWeek = remember(startOfWeek) {
        val days = DayOfWeek.entries
        val startDayIndex = days.indexOf(startOfWeek)
        days.subList(startDayIndex, days.size) + days.subList(0, startDayIndex)
    }
    
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = daysOfWeek.indexOf(firstDayOfMonth.dayOfWeek)
    val daysInMonth = month.lengthOfMonth()

    val compactTextStyle = LocalTextStyle.current.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val monthNameFontSize = 13.sp
    val dayHeadersFontSize = 8.sp
    val dayNumberFontSize = (9f / fontScale).sp
    val weekNumberFontSize = 8.sp
    val weekNumberColumnWidth = if (showWeekNumber) 14.dp else 0.dp

    Column(
        modifier.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar(Char::titlecase),
            fontSize = monthNameFontSize,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = CalendarioTheme.colors.textSystem,
            style = compactTextStyle.copy(lineHeight = monthNameFontSize * 0.95f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showWeekNumber) {
                Spacer(modifier = Modifier.width(weekNumberColumnWidth))
            }
            daysOfWeek.forEach { day ->
                Box(
                    Modifier.weight(1f),
                    Alignment.Center
                ) {
                    val dayText = remember(day, Locale.getDefault()) {
                        val narrowText = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                        if (narrowText.isNotEmpty()) {
                            narrowText.first().uppercase()
                        } else {
                            ""
                        }
                    }

                    Text(
                        text = dayText,
                        fontSize = dayHeadersFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f),
                        style = compactTextStyle.copy(lineHeight = dayHeadersFontSize * 0.95f)
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            val dayCellsData = remember(month, startOfWeek) {
                List(6 * 7) {
                    val day = it - firstDayOfWeekIndex + 1
                    if (day in 1..daysInMonth) month.atDay(day) else null
                }
            }
            val weekFields = remember { WeekFields.of(Locale.getDefault()) }
            dayCellsData.chunked(7).forEach { weekDates ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    Arrangement.SpaceAround,
                    Alignment.CenterVertically
                ) {
                    if (showWeekNumber) {
                        val weekNumber = weekDates.firstOrNull { it != null }?.get(weekFields.weekOfWeekBasedYear()) ?: ""
                        Box(
                            modifier = Modifier.width(weekNumberColumnWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = weekNumber.toString(),
                                fontSize = weekNumberFontSize,
                                color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                                style = compactTextStyle.copy(lineHeight = weekNumberFontSize * 0.95f)
                            )
                        }
                    }
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
                                val isSundayNonHoliday = date.dayOfWeek == DayOfWeek.SUNDAY && !isHoliday

                                val textColor = when {
                                    isToday -> CalendarioTheme.colors.textSystem
                                    isHoliday || isSundayNonHoliday -> CalendarioTheme.colors.textSundayHoliday
                                    else -> CalendarioTheme.colors.textSystem // Unificado con eventos normales
                                }
                                val fontWeightText = if (isToday) FontWeight.Bold else FontWeight.Normal

                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    if (isToday) {
                                        Box(
                                            Modifier
                                                .size((dayNumberFontSize.value * 2.1f).dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = CalendarioTheme.colors.miniMonthTodayCellBorder,
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                    Text(
                                        text = date.dayOfMonth.toString(),
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
