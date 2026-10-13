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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
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
    val pulse5: Boolean = false,
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

    val effectType = remember(themeColors) { SettingsManager.getMonthlyEffectType(context) }
    val showWeekNumber = remember(themeColors) { SettingsManager.isWeekNumberVisible(context) }
    val weekFields = remember(locale) { WeekFields.of(locale) }
    val isAppDark = ColorUtils.calculateLuminance(themeColors.settingsBackground.toArgb()) < 0.5

    // --- ANIMACIÓN DE PARPADEO (v3.0.21-F4) ---
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

    // --- CÁLCULO DE LA CUADRÍCULA (MEMORIZADO DE V3.8.11) ---
    val weeksToDisplay = remember(currentMonth, startOfWeek) {
        val days = mutableListOf<Pair<LocalDate, Boolean>>()
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeekIndex = daysOfWeek.indexOf(firstDayOfMonth.dayOfWeek)
        val prevMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        val daysInCurrentMonth = currentMonth.lengthOfMonth()

        for (i in 0 until firstDayOfWeekIndex) {
            val day = (daysInPrevMonth - firstDayOfWeekIndex + 1) + i
            days.add(prevMonth.atDay(day) to false)
        }
        for (i in 1..daysInCurrentMonth) {
            days.add(currentMonth.atDay(i) to true)
        }
        val remainingCells = if (days.size % 7 == 0) 0 else 7 - (days.size % 7)
        for (i in 1..remainingCells) {
            days.add(nextMonth.atDay(i) to false)
        }

        days.chunked(7).filter { week -> week.any { it.second } }
    }

    // --- COLORES DE CABECERA (RESTAURADO EXACTO DE V3.8.11) ---
    val (headerBg, onHeaderColor) = remember(themeColors, effectType, isAppDark) {
        val startColor = themeColors.monthlyCalendarGridBackground
        val midColor = themeColors.monthlyCalendarGridEffect
        val colorBehind = when (effectType) {
            "gradient" -> lerp(startColor, midColor, 0.02f)
            "sweep" -> lerp(startColor, midColor, 0.04f)
            else -> startColor
        }
        
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colorBehind.toArgb(), hsl)
        
        if (isAppDark) {
            hsl[2] = (hsl[2] + 0.10f).coerceAtMost(1f)
            hsl[1] = (hsl[1] + 0.05f).coerceAtMost(1f)
        } else {
            val darkenFactor = when {
                hsl[2] > 0.60f -> 0.20f
                hsl[2] > 0.45f -> 0.10f
                else -> 0.05f
            }
            hsl[2] = (hsl[2] - darkenFactor).coerceAtLeast(0f)
            hsl[1] = (hsl[1] + 0.10f).coerceAtMost(1f)
        }
        val bg = Color(ColorUtils.HSLToColor(hsl))

        val hslText = FloatArray(3)
        ColorUtils.colorToHSL(bg.toArgb(), hslText)
        val isBgDark = hslText[2] < 0.75f
        hslText[2] = if (isBgDark) 0.85f else 0.15f
        bg to Color(ColorUtils.HSLToColor(hslText))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        // CABECERA: FORMA EXACTA v3.8.11 / v1.8.943
        Row(Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(1.dp)
                        .background(headerBg),
                    Alignment.Center
                ) {
                    val dayText = remember(day, locale) {
                        val shortText = day.getDisplayName(TextStyle.SHORT, locale)
                        if (shortText.length >= 2) {
                            shortText.take(2).replaceFirstChar { it.titlecase(locale) }
                        } else {
                            shortText.uppercase(locale)
                        }
                    }

                    Text(
                        text = dayText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = onHeaderColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        MonthGridView(
            weeksToDisplay = weeksToDisplay,
            today = today,
            eventsByDate = eventsByDate,
            dailyNotes = dailyNotes,
            showWeekNumber = showWeekNumber,
            weekFields = weekFields,
            isAppDark = isAppDark,
            themeColors = themeColors,
            eventSettings = eventSettings,
            pulseAlpha = pulseAlpha,
            workingDayDates = workingDayDates,
            availableCalendars = availableCalendars,
            onDayClick = onDayClick,
            onEmptyDayClick = onEmptyDayClick
        )
    }
}

@Composable
private fun MonthGridView(
    weeksToDisplay: List<List<Pair<LocalDate, Boolean>>>,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    dailyNotes: Map<String, DailyNote>,
    showWeekNumber: Boolean,
    weekFields: WeekFields,
    isAppDark: Boolean,
    themeColors: CustomColors,
    eventSettings: EventKeywordSettings,
    pulseAlpha: Float,
    workingDayDates: Set<LocalDate>,
    availableCalendars: List<CalendarInfo>,
    onDayClick: (LocalDate, List<Festivo>) -> Unit,
    onEmptyDayClick: (LocalDate) -> Unit
) {
    weeksToDisplay.forEach { week ->
        val weekContainsToday = week.any { it.first == today && it.second }
        Row(
            Modifier
                .fillMaxWidth()
                .zIndex(if (weekContainsToday) 2f else 0f)
        ) {
            week.forEachIndexed { indexInWeek, (date, isCurrentMonth) ->
                val isToday = date == today && isCurrentMonth
                val isPastDay = isCurrentMonth && date.isBefore(today)
                val baseCellBackground = themeColors.monthlyCalendarDayCellBackground

                // SUAVIZADO DE FONDO (RESTAURADO EXACTO V3.8.11)
                val cellBackground = remember(isPastDay, isCurrentMonth, baseCellBackground) {
                    when {
                        isPastDay -> {
                            val overlay = baseCellBackground.getContrastColor(Color.Black)
                            val alpha = if (overlay == Color.White) 0.04f else 0.02f
                            overlay.copy(alpha = alpha).compositeOver(baseCellBackground)
                        }
                        !isCurrentMonth -> {
                            val overlay = baseCellBackground.getContrastColor(Color.Black)
                            overlay.copy(alpha = 0.10f).compositeOver(baseCellBackground)
                        }
                        else -> baseCellBackground
                    }
                }

                val isInactive = !isCurrentMonth || isPastDay
                val dayEvents = if (isCurrentMonth) eventsByDate[date].orEmpty() else emptyList()
                val dayHasEventsWithTitle = remember(dayEvents) { dayEvents.any { it.title.isNotBlank() } }

                val dayColor = remember(date, isInactive, dayEvents, workingDayDates, themeColors) {
                    val isForcedWorkingDay = workingDayDates.contains(date)
                    val isHoliday = !isForcedWorkingDay && dayEvents.any { it.isFromHolidaySource && it.title.isNotBlank() }
                    val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY

                    val baseColor = when {
                        isHoliday || isSunday -> themeColors.textSundayHoliday
                        else -> themeColors.textSystem
                    }

                    if (isInactive) baseColor.copy(alpha = 0.5f) else baseColor
                }

                val todayBorderColor = remember(cellBackground) { 
                    if (ColorUtils.calculateLuminance(cellBackground.toArgb()) > 0.5) Color.Black else Color.White 
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .zIndex(if (isToday) 10f else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    MonthlyDayCell(
                        date = date,
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        isInactive = isInactive,
                        dayEvents = dayEvents,
                        dayHasEventsWithTitle = dayHasEventsWithTitle,
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
                        availableCalendars = availableCalendars,
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
    isInactive: Boolean,
    dayEvents: List<Festivo>,
    dayHasEventsWithTitle: Boolean,
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
    availableCalendars: List<CalendarInfo>,
    onDayClick: (LocalDate, List<Festivo>) -> Unit,
    onEmptyDayClick: (LocalDate) -> Unit
) {
    val fixedLaneWidth = 6.5.dp

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
        // --- RENDERIZADO DE CARRILES MULTI-DÍA (RESTAURADO EXACTO V3.8.11 CON OPACIDAD INACTIVA) ---
        val longPeriods = dayEvents.filter { it.isLongPeriod && it.lane != null }
        if (isCurrentMonth && longPeriods.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .align(Alignment.Center)
            ) {
                repeat(5) { laneIndex ->
                    val period = longPeriods.find { it.lane == laneIndex }
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (period != null) {
                            val cal = availableCalendars.find { it.id == period.calendarId }
                            val baseColor = if (period.customColor != null) Color(period.customColor)
                                       else if (cal != null) Color(cal.color)
                                       else themeColors.textSystem
                            val color = if (isInactive) baseColor.copy(alpha = 0.4f) else baseColor

                            Box(modifier = Modifier.fillMaxWidth().height(fixedLaneWidth)) {
                                if (period.currentDay > 1) {
                                    val isLastDay = period.currentDay == period.totalDays
                                    val innerRadius = if (isLastDay) 10.dp else 0.dp
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(6.dp)
                                            .fillMaxHeight()
                                            .background(
                                                color,
                                                RoundedCornerShape(
                                                    topEnd = innerRadius,
                                                    bottomEnd = innerRadius,
                                                    topStart = 0.dp,
                                                    bottomStart = 0.dp
                                                )
                                            )
                                    )
                                }
                                if (period.currentDay < period.totalDays) {
                                    val isFirstDay = period.currentDay == 1
                                    val innerRadius = if (isFirstDay) 10.dp else 0.dp
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(6.dp)
                                            .fillMaxHeight()
                                            .background(
                                                color,
                                                RoundedCornerShape(
                                                    topStart = innerRadius,
                                                    bottomStart = innerRadius,
                                                    topEnd = 0.dp,
                                                    bottomEnd = 0.dp
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // NÚMERO DE SEMANA
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

        // --- INDICADORES POR PRIORIDAD (Propuesta 3) ---
        val indicatorColors = remember(dayEvents, isCurrentMonth, isAppDark, eventSettings, themeColors) {
            if (!isCurrentMonth) return@remember emptyList()
            
            val indicators = mutableListOf<Color>()
            val filtered = dayEvents.filter { !it.isFromHolidaySource && it.title.isNotBlank() }
            
            if (filtered.isEmpty()) return@remember emptyList()

            // 1. PRIORIDAD 1: Eventos especiales (1 al 5)
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

            // 2. PRIORIDAD 2: Cumpleaños
            if (filtered.any { it.isBirthday }) {
                if (indicators.size < 5) {
                    indicators.add(themeColors.textBirthday)
                }
            }

            // 3. PRIORIDAD 3: Eventos normales (1 punto único en color gris a la derecha, si hay hueco < 5 y no es evento largo)
            val hasNormalEvent = filtered.any { event ->
                !event.isLongPeriod && eventSettings.isNormalEvent(event.title, event.isBirthday)
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
