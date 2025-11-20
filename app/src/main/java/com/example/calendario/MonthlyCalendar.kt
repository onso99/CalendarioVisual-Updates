package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit,
    onEmptyDayClick: (date: LocalDate) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")

    val prevMonth = currentMonth.minusMonths(1)
    val nextMonth = currentMonth.plusMonths(1)

    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // 0 para Lunes

    val daysInPrevMonth = prevMonth.lengthOfMonth()
    val daysInCurrentMonth = currentMonth.lengthOfMonth()

    val visibleDays = mutableListOf<Pair<LocalDate, Boolean>>()

    for (i in 0 until firstDayOfWeekIndex) {
        val day = daysInPrevMonth - firstDayOfWeekIndex + 1 + i
        visibleDays.add(prevMonth.atDay(day) to false)
    }

    for (i in 1..daysInCurrentMonth) {
        visibleDays.add(currentMonth.atDay(i) to true)
    }

    val cellsSoFar = visibleDays.size
    val remainingCellsInWeek = if (cellsSoFar % 7 == 0) 0 else 7 - (cellsSoFar % 7)
    for (i in 1..remainingCellsInWeek) {
        visibleDays.add(nextMonth.atDay(i) to true)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(1.dp)
                        .background(CalendarioTheme.colors.monthlyCalendarHeaderBackground),
                    Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalendarioTheme.colors.monthlyCalendarHeaderText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        val weeksToDisplay = visibleDays.chunked(7).filter { week ->
            week.any { it.second }
        }

        weeksToDisplay.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (date, isCurrentMonth) ->
                    val isToday = date == today && isCurrentMonth

                    val dayEvents = if (isCurrentMonth) eventsByDate[date].orEmpty() else emptyList()
                    val dayHasEventsWithTitle = dayEvents.any { it.title.isNotBlank() }
                    val hasOtherEventsPoint = isCurrentMonth && dayEvents.any { !it.isFromHolidaySource && it.title.isNotBlank() }

                    val dayColor = when {
                        !isCurrentMonth -> CalendarioTheme.colors.monthlyCalendarDayNumberGhost
                        else -> {
                            val isHoliday = dayEvents.any { it.isFromHolidaySource && it.title.isNotBlank() }
                            val isSundayNonHoliday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY && !isHoliday
                            when {
                                isHoliday -> CalendarioTheme.colors.monthlyCalendarDayNumberHoliday
                                isSundayNonHoliday -> CalendarioTheme.colors.monthlyCalendarDayNumberSunday
                                else -> CalendarioTheme.colors.monthlyCalendarDayNumberNormal
                            }
                        }
                    }
                    val cellBackground = if (isCurrentMonth) {
                        CalendarioTheme.colors.monthlyCalendarDayCellBackground
                    } else {
                        CalendarioTheme.colors.monthlyCalendarEmptyCellBackground
                    }

                    val borderModifier = if (isToday) {
                        Modifier.border(
                            width = 3.dp,
                            color = CalendarioTheme.colors.monthlyCalendarTodayCellBorder,
                            shape = RoundedCornerShape(4.dp)
                        )
                    } else {
                        Modifier
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .background(cellBackground, RoundedCornerShape(4.dp))
                            .then(borderModifier)
                            .clickable(enabled = isCurrentMonth) {
                                if (dayHasEventsWithTitle) {
                                    onDayClick(date, dayEvents.filter { it.title.isNotBlank() })
                                } else {
                                    onEmptyDayClick(date)
                                }
                            }
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            fontWeight = if (!isCurrentMonth) FontWeight.Normal else if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = dayColor,
                            fontSize = 22.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        if (hasOtherEventsPoint) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                                    .size(6.dp)
                                    .background(
                                        color = CalendarioTheme.colors.monthlyCalendarEventIndicator,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}