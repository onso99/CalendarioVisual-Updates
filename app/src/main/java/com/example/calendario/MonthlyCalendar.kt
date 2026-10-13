package com.example.calendario

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.CustomColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.time.format.TextStyle

private data class EventKeywordSettings(
    val norm1: String = "",
    val norm2: String = "",
    val norm3: String = "",
    val norm4: String = "",
    val norm5: String = "",
    val pulse1: Boolean = false,
    val pulse2: Boolean = false,
    val pulse3: Boolean = false,
    val pulse4: Boolean = false,
    val pulse5: Boolean = false
) {
    fun isEventPulsing(color: Color, themeColors: CustomColors): Boolean {
        return (color == themeColors.textEvent1 && pulse1) ||
               (color == themeColors.textEvent2 && pulse2) ||
               (color == themeColors.textEvent3 && pulse3) ||
               (color == themeColors.textEvent4 && pulse4) ||
               (color == themeColors.textEvent5 && pulse5)
    }

    fun isNormalEvent(eventTitle: String, isBirthday: Boolean): Boolean {
        if (isBirthday) return false
        val norm = eventTitle.unaccent().lowercase()
        return (norm1.isBlank() || !norm.contains(norm1)) &&
               (norm2.isBlank() || !norm.contains(norm2)) &&
               (norm3.isBlank() || !norm.contains(norm3)) &&
               (norm4.isBlank() || !norm.contains(norm4)) &&
               (norm5.isBlank() || !norm.contains(norm5))
    }
}

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit,
    onEmptyDayClick: (date: LocalDate) -> Unit,
    startOfWeek: DayOfWeek,
    availableCalendars: List<CalendarInfo>,
    dailyNotes: Map<String, DailyNote> = emptyMap(),
    workingDayDates: Set<LocalDate> = emptySet()
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val themeColors = CalendarioTheme.colors
    val eventSettings = remember(themeColors) {
        EventKeywordSettings(
            norm1 = SettingsManager.getEvent1Keyword(context).unaccent().lowercase(),
            norm2 = SettingsManager.getEvent2Keyword(context).unaccent().lowercase(),
            norm3 = SettingsManager.getEvent3Keyword(context).unaccent().lowercase(),
            norm4 = SettingsManager.getEvent4Keyword(context).unaccent().lowercase(),
            norm5 = SettingsManager.getEvent5Keyword(context).unaccent().lowercase(),
            pulse1 = SettingsManager.isEvent1PulseEnabled(context),
            pulse2 = SettingsManager.isEvent2PulseEnabled(context),
            pulse3 = SettingsManager.isEvent3PulseEnabled(context),
            pulse4 = SettingsManager.isEvent4PulseEnabled(context),
            pulse5 = SettingsManager.isEvent5PulseEnabled(context)
        )
    }

    val showWeekNumber = remember(themeColors) { SettingsManager.isWeekNumberVisible(context) }
    val weekFields = remember(locale) { WeekFields.of(locale) }
    val isAppDark = ColorUtils.calculateLuminance(themeColors.settingsBackground.toArgb()) < 0.5

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                1f at 0
                1f at 1000
                0f at 1001
                0f at 1500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val daysOfWeek = remember(startOfWeek) {
        val days = DayOfWeek.entries
        val startDayIndex = days.indexOf(startOfWeek)
        days.slice(startDayIndex until days.size) + days.slice(0 until startDayIndex)
    }

    val weeksToDisplay = remember(currentMonth, startOfWeek) {
        val firstDay = currentMonth.atDay(1)
        val lastDay = currentMonth.atEndOfMonth()
        val weeks = mutableListOf<List<LocalDate>>()
        
        var current = firstDay.with(java.time.temporal.TemporalAdjusters.previousOrSame(startOfWeek))
        val endLimit = lastDay.with(java.time.temporal.TemporalAdjusters.nextOrSame(startOfWeek.plus(6)))
        
        while (!current.isAfter(endLimit)) {
            val week = (0..6).map { current.plusDays(it.toLong()) }
            weeks.add(week)
            current = current.plusWeeks(1)
        }
        weeks
    }

    val cellBackground = themeColors.monthlyCalendarDayCellBackground
    val todayBorderColor = if (isAppDark) Color.White else Color.Black

    val (headerBg, headerFg) = remember(themeColors, isAppDark) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(themeColors.cabecera.toArgb(), hsl)
        if (isAppDark) {
            hsl[1] = (hsl[1] + 0.05f).coerceAtMost(1f)
            val darkenFactor = when {
                hsl[2] > 0.60f -> 0.20f
                hsl[2] > 0.45f -> 0.10f
                else -> 0.05f
            }
            hsl[2] = (hsl[2] - darkenFactor).coerceAtLeast(0f)
            hsl[1] = (hsl[1] + 0.10f).coerceAtMost(1f)
        } else {
            val lightenFactor = when {
                hsl[2] < 0.25f -> 0.25f
                hsl[2] < 0.40f -> 0.15f
                else -> 0.08f
            }
            hsl[2] = (hsl[2] + lightenFactor).coerceAtMost(1f)
            hsl[1] = (hsl[1] * 0.70f).coerceAtLeast(0f)
        }
        val bg = Color(ColorUtils.HSLToColor(hsl))

        val hslText = FloatArray(3)
        ColorUtils.colorToHSL(bg.toArgb(), hslText)
        val isBgDark = hslText[2] < 0.75f
        hslText[2] = if (isBgDark) 0.85f else 0.15f
        bg to Color(ColorUtils.HSLToColor(hslText))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                val dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    .replaceFirstChar { it.uppercase(locale) }
                    .take(2)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(1.dp)
                        .background(headerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = headerFg
                    )
                }
            }
        }

        MonthGridView(
            weeksToDisplay = weeksToDisplay,
            currentMonth = currentMonth,
            today = today,
            eventsByDate = eventsByDate,
            dailyNotes = dailyNotes,
            showWeekNumber = showWeekNumber,
            weekFields = weekFields,
            isAppDark = isAppDark,
            themeColors = themeColors,
            eventSettings = eventSettings,
            pulseAlpha = pulseAlpha,
            cellBackground = cellBackground,
            todayBorderColor = todayBorderColor,
            onDayClick = onDayClick,
            onEmptyDayClick = onEmptyDayClick
        )
    }
}

@Composable
private fun MonthGridView(
    weeksToDisplay: List<List<LocalDate>>,
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    dailyNotes: Map<String, DailyNote>,
    showWeekNumber: Boolean,
    weekFields: WeekFields,
    isAppDark: Boolean,
    themeColors: CustomColors,
    eventSettings: EventKeywordSettings,
    pulseAlpha: Float,
    cellBackground: Color,
    todayBorderColor: Color,
    onDayClick: (LocalDate, List<Festivo>) -> Unit,
    onEmptyDayClick: (LocalDate) -> Unit
) {
    weeksToDisplay.forEach { week ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            week.forEachIndexed { indexInWeek, date ->
                val isCurrentMonth = YearMonth.from(date) == currentMonth
                val isToday = date == today
                val dayEvents = eventsByDate[date] ?: emptyList()
                val dayColor = when {
                    !isCurrentMonth -> themeColors.textSystem.copy(alpha = 0.38f)
                    date.dayOfWeek == DayOfWeek.SUNDAY || dayEvents.any { it.isFromHolidaySource } -> themeColors.textSundayHoliday
                    else -> themeColors.textSystem
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    MonthlyDayCell(
                        date = date,
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        dayEvents = dayEvents,
                        dailyNotes = dailyNotes,
                        showWeekNumber = showWeekNumber,
                        indexInWeek = indexInWeek,
                        weekFields = weekFields,
                        isAppDark = isAppDark,
                        themeColors = themeColors,
                        eventSettings = eventSettings,
                        pulseAlpha = pulseAlpha,
                        dayColor = dayColor,
                        cellBackground = cellBackground,
                        todayBorderColor = todayBorderColor,
                        onDayClick = onDayClick,
                        onEmptyDayClick = onEmptyDayClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    dayEvents: List<Festivo>,
    dailyNotes: Map<String, DailyNote>,
    showWeekNumber: Boolean,
    indexInWeek: Int,
    weekFields: WeekFields,
    isAppDark: Boolean,
    themeColors: CustomColors,
    eventSettings: EventKeywordSettings,
    pulseAlpha: Float,
    dayColor: Color,
    cellBackground: Color,
    todayBorderColor: Color,
    onDayClick: (LocalDate, List<Festivo>) -> Unit,
    onEmptyDayClick: (LocalDate) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(cellBackground)
            .clickable {
                if (dayEvents.isNotEmpty()) {
                    onDayClick(date, dayEvents)
                } else {
                    onEmptyDayClick(date)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showWeekNumber && indexInWeek == 0) {
            val weekNumber = try {
                date.get(weekFields.weekOfWeekBasedYear()).toString()
            } catch (_: Exception) {
                ""
            }
            if (weekNumber.isNotEmpty()) {
                Text(
                    text = weekNumber,
                    fontSize = 11.sp,
                    color = themeColors.textSystem.copy(alpha = 0.4f),
                    style = ComposeTextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 2.dp)
                )
            }
        }

        Text(
            text = date.dayOfMonth.toString(),
            fontWeight = if (!isCurrentMonth) FontWeight.Normal else if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = dayColor,
            softWrap = false,
            maxLines = 1,
            style = ComposeTextStyle(
                fontSize = 22.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        val hasNote = dailyNotes.containsKey(date.toString())
        if (isCurrentMonth && hasNote) {
            val noteColor = themeColors.noteIconColor
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .size(8.dp)
                    .drawBehind {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        drawPath(path, noteColor)
                    }
            )
        }

        val indicatorColors = remember(dayEvents, isCurrentMonth, isAppDark, eventSettings, themeColors) {
            if (!isCurrentMonth) return@remember emptyList()
            
            val indicators = mutableListOf<Color>()
            val filtered = dayEvents.filter { !it.isFromHolidaySource && it.title.isNotBlank() }
            
            if (filtered.isEmpty()) return@remember emptyList()

            val specialPairs = listOf(
                Pair(themeColors.textEvent1, eventSettings.norm1),
                Pair(themeColors.textEvent2, eventSettings.norm2),
                Pair(themeColors.textEvent3, eventSettings.norm3),
                Pair(themeColors.textEvent4, eventSettings.norm4),
                Pair(themeColors.textEvent5, eventSettings.norm5),
            )
            for ((color, norm) in specialPairs) {
                if (norm.isNotBlank() && filtered.any { it.title.unaccent().lowercase().contains(norm) }) {
                    if (indicators.size < 5) {
                        indicators.add(color)
                    }
                }
            }

            if (filtered.any { it.isBirthday }) {
                if (indicators.size < 5) {
                    indicators.add(themeColors.textBirthday)
                }
            }

            val hasNormalEvent = filtered.any { event ->
                eventSettings.isNormalEvent(event.title, event.isBirthday)
            }
            if (hasNormalEvent) {
                if (indicators.size < 5) {
                    indicators.add(if (isAppDark) Color(0xFFA0A0A0) else Color.Gray)
                }
            }
            
            indicators
        }

        if (indicatorColors.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                indicatorColors.forEach { color ->
                    val isPulsing = eventSettings.isEventPulsing(color, themeColors)
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .graphicsLayer {
                                alpha = if (isPulsing) pulseAlpha else 1f
                            }
                            .background(color)
                    )
                }
            }
        }

        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val borderSize = (if (todayBorderColor == Color.Black) 2.8.dp else 2.dp).toPx()
                        val finalAlpha = if (todayBorderColor == Color.White) 0.6f else 1.0f
                        drawRoundRect(
                            color = todayBorderColor,
                            topLeft = androidx.compose.ui.geometry.Offset(-borderSize / 2, -borderSize / 2),
                            size = androidx.compose.ui.geometry.Size(size.width + borderSize, size.height + borderSize),
                            cornerRadius = CornerRadius(4.dp.toPx() + borderSize / 2),
                            style = Stroke(width = borderSize),
                            alpha = finalAlpha
                        )
                    }
            )
        }
    }
}
