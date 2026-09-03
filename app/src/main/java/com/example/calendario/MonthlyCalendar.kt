package com.example.calendario

import android.content.Context
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
import com.example.calendario.ui.theme.isColorDark
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.time.format.TextStyle

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
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val themeColors = CalendarioTheme.colors
    val event1Keyword = remember(themeColors) { prefs.getString(AppConstants.KEY_EVENT_1_KEYWORD, "")?.trim() ?: "" }
    val event2Keyword = remember(themeColors) { prefs.getString(AppConstants.KEY_EVENT_2_KEYWORD, "")?.trim() ?: "" }
    val normEvent1 = remember(event1Keyword) { event1Keyword.unaccent().lowercase() }
    val normEvent2 = remember(event2Keyword) { event2Keyword.unaccent().lowercase() }
    val event1Pulse = remember(themeColors) { prefs.getBoolean(AppConstants.KEY_EVENT_1_PULSE, false) }
    val event2Pulse = remember(themeColors) { prefs.getBoolean(AppConstants.KEY_EVENT_2_PULSE, false) }

    val effectType = remember(themeColors) { prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient") ?: "gradient" }
    
    val showWeekNumber = remember(prefs) { prefs.getBoolean(AppConstants.KEY_SHOW_WEEK_NUMBER_IN_YEAR_VIEW, false) }
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
                1f at 1000 // Visible 1 segundo
                0f at 1001 // Desaparece al instante
                0f at 1500 // Invisible medio segundo
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

    // --- GROSOR DE CARRIL FIJO (MÃ¡ximo para resaltar el efecto pÃ­ldora) ---
    val fixedLaneWidth = 6.5.dp

    // --- CÁLCULO DE LA CUADRÍCULA (MEMORIZADO) ---
    val weeksToDisplay = remember(currentMonth, startOfWeek) {
        val days = mutableListOf<Pair<LocalDate, Boolean>>()
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeekIndex = daysOfWeek.indexOf(firstDayOfMonth.dayOfWeek)
        val prevMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        val daysInCurrentMonth = currentMonth.lengthOfMonth()

        // Días del mes anterior
        for (i in 0 until firstDayOfWeekIndex) {
            val day = (daysInPrevMonth - firstDayOfWeekIndex + 1) + i
            days.add(prevMonth.atDay(day) to false)
        }
        // Días del mes actual
        for (i in 1..daysInCurrentMonth) {
            days.add(currentMonth.atDay(i) to true)
        }
        // Días del mes siguiente
        val remainingCells = if (days.size % 7 == 0) 0 else 7 - (days.size % 7)
        for (i in 1..remainingCells) {
            days.add(nextMonth.atDay(i) to false)
        }

        days.chunked(7).filter { week -> week.any { it.second } }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        // --- COLORES DE CABECERA (MEMORIZADO) ---
        val (headerBg, onHeaderColor) = remember(themeColors, effectType, isAppDark) {
            val startColor = themeColors.monthlyCalendarGridBackground
            val midColor = themeColors.monthlyCalendarGridEffect
            val endColor = if (effectType == "gradient") midColor else startColor
            
            val colorBehind = when (effectType) {
                "gradient" -> lerp(startColor, endColor, 0.02f)
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

                    val cellBackground = remember(isPastDay, isCurrentMonth, baseCellBackground) {
                        when {
                            isPastDay -> {
                                // Día pasado (del propio mes visualizado): Aplicamos el tono más claro
                                val overlay = if (isColorDark(baseCellBackground, Color.Black)) Color.White else Color.Black
                                val alpha = if (isColorDark(baseCellBackground, Color.Black)) 0.04f else 0.02f
                                overlay.copy(alpha = alpha).compositeOver(baseCellBackground)
                            }
                            !isCurrentMonth -> {
                                // Día de relleno (fuera del mes): Aplicamos el tono oscuro/inactivo normal
                                val overlay = if (isColorDark(baseCellBackground, Color.Black)) Color.White else Color.Black
                                overlay.copy(alpha = 0.10f).compositeOver(baseCellBackground)
                            }
                            else -> {
                                // Día presente o futuro del mes actual: Fondo normal
                                baseCellBackground
                            }
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

                    // Determinamos el color del borde de "Hoy" según la luminancia del fondo de la celda
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
                            // ... (resto del código de carriles y textos)
                            val longPeriods = dayEvents.filter { it.isLongPeriod && it.lane != null }
                            if (isCurrentMonth && longPeriods.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f) // Reducimos altura para acercar los carriles
                                        .align(Alignment.Center)
                                ) {
                                    repeat(5) { laneIndex ->
                                        val period = longPeriods.find { it.lane == laneIndex }
                                        
                                        // Contenedor de slot fijo (1/6 de la celda) para mantener alineaciÃ³n
                                        Box(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (period != null) {
                                                val cal = availableCalendars.find { it.id == period.calendarId }
                                                val color = if (period.customColor != null) Color(period.customColor)
                                                           else if (cal != null) Color(cal.color)
                                                           else themeColors.textSystem

                                                Box(modifier = Modifier.fillMaxWidth().height(fixedLaneWidth)) {
                                                    // Guion Izquierdo (Entrante)
                                                    if (period.currentDay > 1) {
                                                        // Si es el Ãºltimo dÃ­a, el extremo derecho (interior) es redondo.
                                                        // El extremo izquierdo (frontera) SIEMPRE es plano.
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
                                                    // Guion Derecho (Saliente)
                                                    if (period.currentDay < period.totalDays) {
                                                        // Si es el primer dÃ­a, el extremo izquierdo (interior) es redondo.
                                                        // El extremo derecho (frontera) SIEMPRE es plano.
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

                            // NÚMERO DE SEMANA (Sutil, esquina superior izquierda)
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
                                        style = ComposeTextStyle(
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 3.dp, top = 2.dp)
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
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                modifier = Modifier.align(Alignment.Center)
                            )

                            // INDICADOR DE NOTA DIARIA (TriÃ¡ngulo Post-it)
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

                            val indicatorColors = remember(dayEvents, isCurrentMonth, isAppDark, normEvent1, normEvent2, themeColors) {
                                if (!isCurrentMonth) return@remember emptyList()
                                
                                val indicators = mutableListOf<Color>()
                                val filtered = dayEvents.filter { !it.isFromHolidaySource && it.title.isNotBlank() }
                                
                                if (filtered.isEmpty()) return@remember emptyList()

                                val hasNormalEvent = filtered.any { event ->
                                    val normalizedTitle = event.title.unaccent().lowercase()
                                    !event.isBirthday &&
                                    !(normEvent1.isNotBlank() && normalizedTitle.contains(normEvent1)) &&
                                    !(normEvent2.isNotBlank() && normalizedTitle.contains(normEvent2))
                                }

                                if (hasNormalEvent) {
                                    indicators.add(if (isAppDark) Color(0xFFBDBDBD) else themeColors.cabecera)
                                }

                                if (filtered.any { it.isBirthday }) {
                                    indicators.add(themeColors.textBirthday)
                                }

                                if (normEvent1.isNotBlank() && filtered.any { it.title.unaccent().lowercase().contains(normEvent1) }) {
                                    indicators.add(themeColors.textEvent1)
                                }

                                if (normEvent2.isNotBlank() && filtered.any { it.title.unaccent().lowercase().contains(normEvent2) }) {
                                    indicators.add(themeColors.textEvent2)
                                }
                                
                                indicators
                            }

                            if (indicatorColors.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    indicatorColors.forEach { color ->
                                        val isPulsing = (color == themeColors.textEvent1 && event1Pulse) || 
                                                        (color == themeColors.textEvent2 && event2Pulse)
                                        
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
                                            // En modo claro (borde negro) aumentamos el grosor para que destaque más
                                            val borderSize = (if (todayBorderColor == Color.Black) 2.8.dp else 2.dp).toPx()
                                            // Si el borde es blanco (modo oscuro), le aplicamos un 80% de opacidad
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
                }
            }
        }
    }
}
