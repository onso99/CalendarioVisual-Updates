package com.example.calendario

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val calendarioViewModel: CalendarioViewModel by viewModels()
    private var calendarObserver: ContentObserver? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Initialize the observer
        calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Refresh data whenever the calendar content changes
                calendarioViewModel.refreshData()
            }
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
        // Register the observer only if we have permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            calendarObserver?.let {
                contentResolver.registerContentObserver(
                    CalendarContract.Events.CONTENT_URI,
                    true,
                    it
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the observer to avoid memory leaks
        calendarObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
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
    showAll: Boolean
): List<Pair<LocalDate, List<Festivo>>> {
    val now = LocalDateTime.now()

    // 1. Obtener todos los eventos del mes que estamos viendo.
    var monthEvents = allEvents.values.flatten().filter {
        it.date.year == currentMonth.year && it.date.month == currentMonth.month
    }

    // 2. Si 'showAll' es false, filtramos para mostrar solo los eventos futuros.
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
            events.sortedWith(
                compareBy { it.startTime }
            )
        }
        .toList()
        .sortedBy { it.first }
}

fun processEventsForWidget(allEvents: Map<LocalDate, List<Festivo>>, limit: Int): List<Festivo> {
    val now = LocalDateTime.now()
    val allFutureEvents = allEvents.values.flatten().filter { event ->
        val eventEndDateTime = if (event.isAllDay) {
            event.date.plusDays(1).atStartOfDay()
        } else {
            val endTime = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.MAX
            LocalDateTime.of(event.date, endTime)
        }
        eventEndDateTime.isAfter(now)
    }.sortedWith(compareBy({ it.date }, { it.startTime }))

    return allFutureEvents.take(limit)
}
