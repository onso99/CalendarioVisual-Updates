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
    showAll: Boolean
): List<Pair<LocalDate, List<Festivo>>> {
    val now = LocalDateTime.now()

    // 1. Obtener todos los eventos del mes que estamos viendo.
    var monthEvents = allEvents.values.flatten().filter {
        it.date.year == currentMonth.year && it.date.month == currentMonth.month
    }

    // 2. Si 'showAll' es false, filtramos para mostrar solo los eventos futuros.
    //    Esto se usará para la vista "Pendientes" y para el widget.
    if (!showAll) {
        monthEvents = monthEvents.filter { event ->
            val eventEndDateTime = if (event.isAllDay) {
                event.date.plusDays(1).atStartOfDay()
            } else {
                val endTime = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.MAX
                LocalDateTime.of(event.date, endTime)
            }
            eventEndDateTime.isAfter(now)
        }
    }

    // 3. Agrupar por fecha y ordenar para la lista.
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
