package com.example.calendario

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val calendarioViewModel: CalendarioViewModel by viewModels()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContent {
            val themeManager = rememberThemeManager()
            val themeSetting by themeManager.themeSetting.collectAsState()
            var themeUpdateTrigger by remember { mutableIntStateOf(0) }
            val onThemeUpdated = { themeUpdateTrigger += 1 }

            val useDarkTheme = when (themeSetting) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            CalendarioTheme(darkTheme = useDarkTheme, themeUpdateTrigger = themeUpdateTrigger) {
                CalendarioApp(
                    themeManager = themeManager,
                    onThemeUpdated = onThemeUpdated,
                    viewModel = calendarioViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        calendarioViewModel.refreshData()
    }

    override fun attachBaseContext(newBase: Context) {
        val newConfig = Configuration(newBase.resources.configuration)
        if (newConfig.fontScale > 1.3f) {
            newConfig.fontScale = 1.3f
        }
        val context = newBase.createConfigurationContext(newConfig)
        super.attachBaseContext(context)
    }
}

fun processEventsForDisplay(
    allEvents: Map<LocalDate, List<Festivo>>,
    currentMonth: LocalDate,
    today: LocalDate,
    showAll: Boolean // Solo aplica al mes actual
): List<Pair<LocalDate, List<Festivo>>> {
    val now = LocalDateTime.now()

    // 1. Primero, obtener TODOS los eventos futuros, sin importar el mes. Esta es la corrección clave.
    val allUpcomingEvents = allEvents.values.flatten().filter { event ->
        val eventEndDateTime = if (event.isAllDay) {
            event.date.plusDays(1).atStartOfDay() // Eventos de día completo terminan al inicio del día siguiente
        } else {
            val endTime = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.MAX
            LocalDateTime.of(event.date, endTime)
        }
        eventEndDateTime.isAfter(now) // La regla universal: si no ha terminado, se muestra.
    }

    // 2. Filtrar los eventos futuros para el mes que estamos viendo.
    var monthEvents = allUpcomingEvents.filter {
        it.date.year == currentMonth.year && it.date.month == currentMonth.month
    }

    // 3. Si estamos en el mes actual y showAll es false, filtramos solo los de hoy en adelante.
    val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
    if (isCurrentMonthView && !showAll) {
        monthEvents = monthEvents.filter { it.date.isAfter(today.minusDays(1)) }
    }

    // 4. Agrupar por fecha y ordenar para la lista.
    return monthEvents
        .groupBy { it.date }
        .mapValues { (_, events) ->
            // Reordenar por si el filtrado alteró el orden original.
            events.sortedWith(
                compareBy<Festivo> { it.startTime }
            )
        }
        .toList()
        .sortedBy { it.first }
}
