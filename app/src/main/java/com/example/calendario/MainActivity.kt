package com.example.calendario

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import java.time.YearMonth

class MainActivity : ComponentActivity() {

    private val calendarioViewModel: CalendarioViewModel by viewModels()
    private var calendarObserver: ContentObserver? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogCollector.init(applicationContext)
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

        handleIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data: Uri? = intent?.data

        if (Intent.ACTION_VIEW == action && data != null) {
            val event = IcsHelper.parseIcs(this, data)
            if (event != null) {
                calendarioViewModel.setImportedEvent(event)
            }
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
    val result = mutableListOf<Pair<LocalDate, List<Festivo>>>()

    // OPTIMIZACIÓN: En lugar de flatten() y filtrar miles de eventos, 
    // recorremos solo los días del mes (31 max) y los buscamos en el mapa.
    val yearMonth = YearMonth.from(currentMonth)
    for (day in 1..yearMonth.lengthOfMonth()) {
        val date = yearMonth.atDay(day)
        val dayEvents = allEvents[date] ?: continue
        
        var filteredEvents = dayEvents
        if (!showAll) {
            filteredEvents = dayEvents.filter { event ->
                val eventEndDateTime = if (event.isAllDay) {
                    event.date.plusDays(1).atStartOfDay()
                } else {
                    val endTime = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.MAX
                    LocalDateTime.of(event.date, endTime)
                }
                eventEndDateTime.isAfter(now)
            }
        }

        if (filteredEvents.isNotEmpty()) {
            val sortedEvents = filteredEvents.sortedWith(
                compareBy<Festivo> { !it.isAllDay }
                    .thenBy { it.startTime }
                    .thenBy { it.title }
            )
            result.add(date to sortedEvents)
        }
    }

    return result.sortedBy { it.first }
}

fun processEventsForWidget(allEvents: Map<LocalDate, List<Festivo>>, limit: Int): List<Festivo> {
    val now = LocalDateTime.now().withNano(0).withSecond(0)
    val today = LocalDate.now()
    LogCollector.addLog(">>> FILTRO: Analizando contra $now")
    
    val allFutureEvents = allEvents.values.flatten().filter { event ->
        val eventEndDateTime = if (event.isAllDay) {
            event.date.plusDays(1).atStartOfDay()
        } else {
            val endTime = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.MAX
            LocalDateTime.of(event.date, endTime)
        }
        
        val isFuture = eventEndDateTime.isAfter(now)
        
        // Solo logueamos detalles de los eventos de HOY para evitar saturación
        if (event.date == today) {
            if (isFuture) {
                LogCollector.addLog("MANTENIDO (futuro hoy): ${event.title} hasta $eventEndDateTime")
            } else {
                LogCollector.addLog("OCULTADO (pasado hoy): ${event.title} terminó $eventEndDateTime")
            }
        }
        
        isFuture
    }.sortedWith(compareBy({ it.date }, { it.startTime }))

    val result = allFutureEvents.take(limit)
    LogCollector.addLog(">>> FILTRO: Finalizado. Mostrando ${result.size} eventos.")
    return result
}
