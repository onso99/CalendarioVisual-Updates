package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    isDarkTheme: Boolean,
    onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")

    // --- LÓGICA REVISADA PARA CALCULAR LOS DÍAS VISIBLES ---
    val prevMonth = currentMonth.minusMonths(1)
    val nextMonth = currentMonth.plusMonths(1)

    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // 0 para Lunes

    val daysInPrevMonth = prevMonth.lengthOfMonth()
    val daysInCurrentMonth = currentMonth.lengthOfMonth()

    val visibleDays = mutableListOf<Pair<LocalDate, Boolean>>()

    // Añadir días del mes anterior
    for (i in 0 until firstDayOfWeekIndex) {
        val day = daysInPrevMonth - firstDayOfWeekIndex + 1 + i
        visibleDays.add(prevMonth.atDay(day) to false)
    }

    // Añadir días del mes actual
    for (i in 1..daysInCurrentMonth) {
        visibleDays.add(currentMonth.atDay(i) to true)
    }

    // Añadir días del mes siguiente para completar la última semana
    val cellsSoFar = visibleDays.size
    val remainingCellsInWeek = if (cellsSoFar % 7 == 0) 0 else 7 - (cellsSoFar % 7)
    for (i in 1..remainingCellsInWeek) {
        visibleDays.add(nextMonth.atDay(i) to false)
    }
    // --- FIN DE LA LÓGICA REVISADA ---

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarGridBackground else AppThemeSetup.LightColors.monthlyCalendarGridBackground,
                RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        // Cabecera con los días de la semana
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(1.dp)
                        .background(if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarHeaderBackground else AppThemeSetup.LightColors.monthlyCalendarHeaderBackground),
                    Alignment.Center
                ) {
                    Text(
                        text = day, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarHeaderText else AppThemeSetup.LightColors.monthlyCalendarHeaderText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // --- FILTRADO DE SEMANAS AÑADIDO AQUÍ ---
        val weeksToDisplay = visibleDays.chunked(7).filter { week ->
            week.any { it.second } // Solo mostrar la semana si contiene algún día del mes actual (pair.second == true)
        }

        // Cuadrícula de días
        weeksToDisplay.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (date, isCurrentMonth) ->
                    val isToday = date == today && isCurrentMonth

                    val dayEvents = if (isCurrentMonth) eventsByDate[date].orEmpty() else emptyList()
                    val dayEventsConAlgunaInfo = dayEvents.any { it.description.ifEmpty { if (it.isAllDay) "(Todo el día)" else "" }.isNotBlank() }
                    val hasOtherEventsPoint = isCurrentMonth && dayEvents.any { !it.isFromHolidaySource && (it.startTime != null && !it.isAllDay || it.description.isNotBlank()) }

                    val dayColor = when {
                        !isCurrentMonth -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberGhost else AppThemeSetup.LightColors.monthlyCalendarDayNumberGhost
                        else -> {
                            val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
                            val isSundayNonHoliday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY && !isHoliday
                            when {
                                isHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberHoliday else AppThemeSetup.LightColors.monthlyCalendarDayNumberHoliday
                                isSundayNonHoliday -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberSunday else AppThemeSetup.LightColors.monthlyCalendarDayNumberSunday
                                else -> if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayNumberNormal else AppThemeSetup.LightColors.monthlyCalendarDayNumberNormal
                            }
                        }
                    }
                    val cellBackground = if (isCurrentMonth) {
                        if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarDayCellBackground else AppThemeSetup.LightColors.monthlyCalendarDayCellBackground
                    } else {
                        if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarEmptyCellBackground else AppThemeSetup.LightColors.monthlyCalendarEmptyCellBackground
                    }

                    val borderModifier = if (isToday) {
                        Modifier.border(
                            width = 3.dp,
                            color = if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarTodayCellBorder else AppThemeSetup.LightColors.monthlyCalendarTodayCellBorder,
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
                            .clickable(enabled = dayEventsConAlgunaInfo) {
                                if (isCurrentMonth) {
                                    onDayClick(
                                        date,
                                        dayEvents.filter {
                                            it.description.ifEmpty { if (it.isAllDay) "(Todo el día)" else "" }
                                                .isNotBlank()
                                        })
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
                                        color = if (isDarkTheme) AppThemeSetup.DarkColors.monthlyCalendarEventIndicator else AppThemeSetup.LightColors.monthlyCalendarEventIndicator,
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
