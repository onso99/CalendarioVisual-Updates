package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val event1Keyword = remember { prefs.getString(AppThemeSetup.KEY_EVENT_1_KEYWORD, "") ?: "" }
    val event2Keyword = remember { prefs.getString(AppThemeSetup.KEY_EVENT_2_KEYWORD, "") ?: "" }

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
        visibleDays.add(nextMonth.atDay(i) to false)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        val onHeaderColor = if (isColorDark(CalendarioTheme.colors.monthlyCalendarHeaderBackground)) Color.White else Color.Black
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
                        color = onHeaderColor,
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

                    val dayColor = when {
                        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else -> {
                            val isHoliday = dayEvents.any { it.isFromHolidaySource && it.title.isNotBlank() }
                            val isSundayNonHoliday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY && !isHoliday
                            when {
                                isHoliday -> CalendarioTheme.colors.textSundayHoliday
                                isSundayNonHoliday -> CalendarioTheme.colors.textSundayHoliday
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
                        modifier = Modifier
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
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "${date.dayOfMonth}",
                                fontWeight = if (!isCurrentMonth) FontWeight.Normal else if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = dayColor,
                                fontSize = 22.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            val eventsForIndicators = dayEvents.filter { !it.isFromHolidaySource && it.title.isNotBlank() }
                            if (isCurrentMonth && eventsForIndicators.isNotEmpty()) {
                                val normalizedEvent1Keyword = remember(event1Keyword) { event1Keyword.unaccent().lowercase() }
                                val normalizedEvent2Keyword = remember(event2Keyword) { event2Keyword.unaccent().lowercase() }

                                val indicatorColors = mutableListOf<Color>()

                                val hasNormalEvent = eventsForIndicators.any { event ->
                                    val normalizedTitle = event.title.unaccent().lowercase()
                                    !normalizedTitle.contains("cumpleanos") &&
                                    !normalizedTitle.contains("aniversario") &&
                                    !(normalizedEvent1Keyword.isNotBlank() && normalizedTitle.contains(normalizedEvent1Keyword)) &&
                                    !(normalizedEvent2Keyword.isNotBlank() && normalizedTitle.contains(normalizedEvent2Keyword))
                                }
                                val hasBirthday = eventsForIndicators.any { it.title.unaccent().lowercase().contains("cumpleanos") || it.title.unaccent().lowercase().contains("aniversario") }
                                val hasEvent1 = normalizedEvent1Keyword.isNotBlank() && eventsForIndicators.any { it.title.unaccent().lowercase().contains(normalizedEvent1Keyword) }
                                val hasEvent2 = normalizedEvent2Keyword.isNotBlank() && eventsForIndicators.any { it.title.unaccent().lowercase().contains(normalizedEvent2Keyword) }

                                if (hasNormalEvent) indicatorColors.add(CalendarioTheme.colors.textEventDefault)
                                if (hasBirthday) indicatorColors.add(CalendarioTheme.colors.textBirthday)
                                if (hasEvent1) indicatorColors.add(CalendarioTheme.colors.textEvent1)
                                if (hasEvent2) indicatorColors.add(CalendarioTheme.colors.textEvent2)

                                val finalIndicators = if (indicatorColors.size > 3 && hasNormalEvent) {
                                    indicatorColors.filter { it != CalendarioTheme.colors.textEventDefault }
                                } else {
                                    indicatorColors
                                }.take(3)

                                if (finalIndicators.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        finalIndicators.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(color.copy(alpha = 0.6f), CircleShape)
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
}
