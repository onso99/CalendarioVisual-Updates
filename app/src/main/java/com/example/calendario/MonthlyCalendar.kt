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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit,
    onEmptyDayClick: (date: LocalDate) -> Unit,
    startOfWeek: DayOfWeek
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val themeColors = CalendarioTheme.colors
    val event1Keyword = remember(themeColors) { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "")?.trim() ?: "" }
    val event2Keyword = remember(themeColors) { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "")?.trim() ?: "" }
    val effectType = remember(themeColors) { prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient") ?: "gradient" }

    val daysOfWeek = remember(startOfWeek) {
        val days = DayOfWeek.entries
        val startDayIndex = days.indexOf(startOfWeek)
        days.slice(startDayIndex until days.size) + days.slice(0 until startDayIndex)
    }

    val prevMonth = currentMonth.minusMonths(1)
    val nextMonth = currentMonth.plusMonths(1)

    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeekIndex = daysOfWeek.indexOf(firstDayOfMonth.dayOfWeek)

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
        // MUESTREO POR POSICIÓN + SINCRONIZACIÓN MODO APP
        val headerBg = run {
            val startColor = themeColors.monthlyCalendarGridBackground
            val midColor = themeColors.monthlyCalendarGridEffect
            val endColor = if (effectType == "gradient") midColor else startColor
            
            val colorBehind = when (effectType) {
                "gradient" -> lerp(startColor, endColor, 0.02f)
                "sweep" -> lerp(startColor, midColor, 0.04f)
                else -> startColor
            }
            
            // Decisión basada en el Modo de la App (no en la luminancia local)
            val isMainBgDark = ColorUtils.calculateLuminance(themeColors.settingsBackground.toArgb()) < 0.5
            if (isMainBgDark) {
                Color.White.copy(alpha = 0.15f).compositeOver(colorBehind)
            } else {
                // Lógica dinámica: Oscurecemos 30% sobre fondos claros y 15% sobre fondos oscuros
                val isLocalDark = ColorUtils.calculateLuminance(colorBehind.toArgb()) < 0.5
                val overlayAlpha = if (isLocalDark) 0.15f else 0.30f
                Color.Black.copy(alpha = overlayAlpha).compositeOver(colorBehind)
            }
        }

        // El color del texto se adapta con umbral al 75%
        val onHeaderColor = run {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(headerBg.toArgb(), hsl)
            val isDark = hsl[2] < 0.75f
            hsl[2] = if (isDark) 0.85f else 0.15f
            Color(ColorUtils.HSLToColor(hsl))
        }

        // CABECERA: Restaurada FORMA EXACTA v1.8.943
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(1.dp)
                        .background(headerBg),
                    Alignment.Center
                ) {
                    val dayText = remember(day, Locale.getDefault()) {
                        val shortText = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        if (shortText.length >= 2) {
                            shortText.take(2).replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        } else {
                            shortText.uppercase(Locale.getDefault())
                        }
                    }

                    Text(
                        text = dayText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onHeaderColor,
                        maxLines = 1,
                        softWrap = false,
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
                    val isPastDay = isCurrentMonth && date.isBefore(today)
                    val isInactive = !isCurrentMonth || isPastDay
                    
                    val baseCellBackground = themeColors.monthlyCalendarDayCellBackground
                    
                    val cellBackground = if (isInactive) {
                        val overlay = if (isColorDark(baseCellBackground, Color.Black)) Color.White else Color.Black
                        overlay.copy(alpha = 0.10f).compositeOver(baseCellBackground)
                    } else {
                        baseCellBackground
                    }

                    val dayEvents = if (isCurrentMonth) eventsByDate[date].orEmpty() else emptyList()
                    val dayHasEventsWithTitle = dayEvents.any { it.title.isNotBlank() }

                    val dayColor = run {
                        val isHoliday = dayEvents.any { it.isFromHolidaySource && it.title.isNotBlank() }
                        val isSundayNonHoliday = date.dayOfWeek == DayOfWeek.SUNDAY && !isHoliday
                        
                        val baseColor = when {
                            isHoliday || isSundayNonHoliday -> themeColors.textSundayHoliday
                            else -> themeColors.textSystem
                        }
                        
                        if (isInactive) baseColor.copy(alpha = 0.5f) else baseColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .background(cellBackground, RoundedCornerShape(4.dp))
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
                                softWrap = false,
                                maxLines = 1,
                                style = ComposeTextStyle(
                                    fontSize = 22.sp,
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                modifier = Modifier.align(Alignment.Center)
                            )

                            val eventsForIndicators = dayEvents.filter { !it.isFromHolidaySource && it.title.isNotBlank() }
                            if (isCurrentMonth && eventsForIndicators.isNotEmpty()) {
                                val normalizedEvent1Keyword = remember(event1Keyword) { event1Keyword.unaccent().lowercase() }
                                val normalizedEvent2Keyword = remember(event2Keyword) { event2Keyword.unaccent().lowercase() }

                                val indicatorColors = mutableListOf<Color>()

                                val hasNormalEvent = eventsForIndicators.any { event ->
                                    val normalizedTitle = event.title.unaccent().lowercase()
                                    !event.isBirthday &&
                                    !(normalizedEvent1Keyword.isNotBlank() && normalizedTitle.contains(normalizedEvent1Keyword)) &&
                                    !(normalizedEvent2Keyword.isNotBlank() && normalizedTitle.contains(normalizedEvent2Keyword))
                                }
                                val hasBirthday = eventsForIndicators.any { it.isBirthday }
                                val hasEvent1 = normalizedEvent1Keyword.isNotBlank() && eventsForIndicators.any { it.title.unaccent().lowercase().contains(normalizedEvent1Keyword) }
                                val hasEvent2 = normalizedEvent2Keyword.isNotBlank() && eventsForIndicators.any { it.title.unaccent().lowercase().contains(normalizedEvent2Keyword) }

                                if (hasNormalEvent) indicatorColors.add(themeColors.textSystem)
                                if (hasBirthday) indicatorColors.add(themeColors.textBirthday)
                                if (hasEvent1) indicatorColors.add(themeColors.textEvent1)
                                if (hasEvent2) indicatorColors.add(themeColors.textEvent2)

                                val finalIndicators = if (indicatorColors.size > 3 && hasNormalEvent) {
                                    indicatorColors.filter { it != themeColors.textSystem }
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

                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 3.5.dp,
                                        color = themeColors.monthlyCalendarTodayCellBorder,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
