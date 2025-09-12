package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
// import android.net.Uri
// import android.os.Build // No se está usando directamente
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit // KTX para SharedPreferences
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
//import androidx.compose.runtime.getValue // Ya importado
//import androidx.compose.runtime.setValue // Ya importado
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent {
            CalendarioApp()
        }
    }
}

enum class CalendarViewMode { MONTHLY, YEARLY }

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int? = null
)

data class Festivo(
    val date: LocalDate,
    val description: String,
    val calendarId: Long,
    var isFromHolidaySource: Boolean = false,
    val startTime: LocalTime? = null,
    val isAllDay: Boolean = true
)

data class FestivoDto(
    val desc: String,
    val id: Long,
    val startTimeStr: String? = null,
    val isAllDay: Boolean = true
)

@Composable
fun CalendarioApp() {
    val azul = Color(0xFF2196F3)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = azul,
            onPrimary = Color.White,
            background = Color.White,
            surface = Color.White,
            onSurface = Color.Black
        )
    ) {
        CalendarioScreen()
    }
}

// Función para notificar a los widgets sobre cambios en la configuración (desde la app)
fun notifyCalendarWidgetsConfigurationChanged(context: Context) {
    Log.d("MainActivity", "Intentando notificar a los widgets sobre cambio de configuración.")
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    if (appWidgetIds.isNotEmpty()) {
        // Esta notificación es crucial para que CalendarWidgetFactory.onDataSetChanged() se dispare.
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivity", "Notificación enviada a los widgets para actualizar por cambio de configuración.")
    } else {
        Log.d("MainActivity", "No hay widgets activos para notificar por cambio de configuración.")
    }
}

// Función para notificar a los widgets sobre cambios en los datos de eventos
fun notifyCalendarWidgetsDataChanged(context: Context) {
    Log.d("MainActivity", "Intentando notificar a los widgets sobre cambio de datos de eventos.")
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivity", "Notificación enviada a los widgets para actualizar datos de eventos.")
    } else {
        Log.d("MainActivity", "No hay widgets activos para notificar sobre cambio de datos de eventos.")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen() {
    val context = LocalContext.current
    val azulFijo = Color(0xFF2196F3)
    val colorResaltadoEventosHoy = Color(0xFF0080ff)

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentYear by remember { mutableStateOf(Year.now()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSelectCalendarsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    val today = LocalDate.now()

    var showDayEventsDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }
    var eventsForDialog by remember { mutableStateOf<List<Festivo>>(emptyList()) }

    var eventsByDate by remember { mutableStateOf(loadEventsFromPrefs(context)) }
    var selectedCalendarIds by remember { mutableStateOf(loadSelectedCalendarIds(context)) }
    var availableCalendars by remember { mutableStateOf(listOf<CalendarInfo>()) }

    var hasCalendarPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }

    // Estado para mostrar el diálogo de configuración del widget
    var showWidgetConfigDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            loadAvailableCalendars(context) { freshCalendars ->
                availableCalendars = freshCalendars
                if (selectedCalendarIds.isEmpty() && freshCalendars.isNotEmpty()) {
                    showSelectCalendarsDialog = true
                }
                readFestivosFromCalendars(context, selectedCalendarIds, freshCalendars) { festivos ->
                    eventsByDate = festivos
                    saveEventsToPrefs(context, festivos)
                    notifyCalendarWidgetsDataChanged(context)
                }
            }
        } else {
            Toast.makeText(context, "Permiso de calendario denegado", Toast.LENGTH_SHORT).show()
            eventsByDate = emptyMap()
            availableCalendars = emptyList()
            saveEventsToPrefs(context, eventsByDate)
            notifyCalendarWidgetsDataChanged(context)
        }
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            loadAvailableCalendars(context) { freshAvailableCalendars ->
                availableCalendars = freshAvailableCalendars
                val validSelectedIds = selectedCalendarIds.filter { sid -> freshAvailableCalendars.any { it.id == sid } }.toSet()
                if (validSelectedIds != selectedCalendarIds) {
                    selectedCalendarIds = validSelectedIds
                    saveSelectedCalendarIds(context, validSelectedIds)
                }
                readFestivosFromCalendars(context, selectedCalendarIds, freshAvailableCalendars) { festivos ->
                    eventsByDate = festivos
                    saveEventsToPrefs(context, festivos)
                    notifyCalendarWidgetsDataChanged(context)
                }
            }
        } else {
            eventsByDate = emptyMap()
            availableCalendars = emptyList()
            saveEventsToPrefs(context, eventsByDate)
            notifyCalendarWidgetsDataChanged(context)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(azulFijo).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "Calendario Visual", // Puedes actualizar la versión aquí si quieres
                            fontSize = 20.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Seleccionar Calendarios", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        if (hasCalendarPermission) {
                                            showSelectCalendarsDialog = true
                                        } else {
                                            requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Widget", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }, // Esta es la opción para abrir el diálogo
                                    onClick = {
                                        menuExpanded = false
                                        showWidgetConfigDialog = true // Mostrar el diálogo de configuración
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = { menuExpanded = false; showHelpDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Acerca de", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = { menuExpanded = false; showAboutDialog = true }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = azulFijo,
                        scrolledContainerColor = azulFijo,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (Resto de tu UI de CalendarioScreen sin cambios)

            Row( // Botones de Navegación
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.minusMonths(1)
                        else currentYear = currentYear.minusYears(1)
                    },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFDDDDDD))
                ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Anterior", tint = Color.Black) }

                Button(
                    onClick = {
                        viewMode = if (viewMode == CalendarViewMode.MONTHLY) CalendarViewMode.YEARLY
                        else { currentMonth = YearMonth.now(); CalendarViewMode.MONTHLY }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDDDDD)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        if (viewMode == CalendarViewMode.MONTHLY)
                            "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}"
                        else "${currentYear.value}",
                        fontSize = 20.sp, color = Color.Black
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.plusMonths(1)
                        else currentYear = currentYear.plusYears(1)
                    },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFDDDDDD))
                ) { Icon(Icons.Filled.ArrowForward, contentDescription = "Siguiente", tint = Color.Black) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewMode == CalendarViewMode.MONTHLY) {
                MonthlyCalendar(
                    currentMonth = currentMonth,
                    today = today,
                    eventsByDate = eventsByDate,
                    puntoEventoColor = azulFijo,
                    onDayClick = { date, events ->
                        selectedDateForDialog = date
                        eventsForDialog = events
                        showDayEventsDialog = true
                    }
                )

                val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
                val eventsForSelectedMonth = eventsByDate
                    .filterKeys { date -> date.month == currentMonth.month && date.year == currentMonth.year }
                    .let { eventsInMonth ->
                        if (isCurrentMonthView) eventsInMonth.filterKeys { date -> !date.isBefore(today) }
                        else eventsInMonth
                    }.toSortedMap()
                val finalEventsToList = eventsForSelectedMonth
                    .filterValues { it.isNotEmpty() }

                Spacer(modifier = Modifier.height(12.dp))
                val listTitle = if (isCurrentMonthView) "Eventos Pendientes de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"
                else "Eventos de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"
                Text(listTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))

                if (finalEventsToList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        val emptyListMessage = if (isCurrentMonthView) "No hay eventos pendientes para este mes." else "No hay eventos para este mes."
                        Text(emptyListMessage, fontSize = 16.sp, color = Color.Gray)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                        finalEventsToList.forEach { (date, festivos) ->
                            val isTodayEvents = isCurrentMonthView && date == today

                            festivos.forEach { festivo ->
                                val defaultDayTextColor = if (festivo.isFromHolidaySource) Color.Red else Color.Black
                                val defaultDescriptionTextColor = Color.Black

                                val currentDayNumberColor = if (isTodayEvents && !festivo.isFromHolidaySource) colorResaltadoEventosHoy else defaultDayTextColor
                                val currentDescriptionColor = if (isTodayEvents) colorResaltadoEventosHoy else defaultDescriptionTextColor

                                val displayDescription = if (!festivo.isAllDay && festivo.startTime != null) {
                                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty { "(Sin título)" }}"
                                } else {
                                    if (festivo.description.isNotBlank()) festivo.description else if (festivo.isAllDay) "(Evento todo el día)" else ""
                                }

                                if (displayDescription.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        val formattedDay = String.format("%02d", date.dayOfMonth)
                                        Text(
                                            text = formattedDay,
                                            color = currentDayNumberColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = ": $displayDescription",
                                            color = currentDescriptionColor,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else { // Yearly View
                YearlyCalendar(currentYear, today, eventsByDate) { selectedMonth ->
                    currentMonth = selectedMonth
                    viewMode = CalendarViewMode.MONTHLY
                }
            }

            // Diálogos
            if (showSelectCalendarsDialog) {
                SelectCalendarsDialog(
                    onDismissRequest = { showSelectCalendarsDialog = false },
                    onApplySelection = { newSelectedIds ->
                        selectedCalendarIds = newSelectedIds
                        saveSelectedCalendarIds(context, newSelectedIds)
                        readFestivosFromCalendars(context, newSelectedIds, availableCalendars) { festivos ->
                            eventsByDate = festivos
                            saveEventsToPrefs(context, eventsByDate)
                            notifyCalendarWidgetsDataChanged(context)
                        }
                        showSelectCalendarsDialog = false
                    }
                )
            }
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    text = {
                        Column {
                            Text("Calendario Visual V1.2.54", fontSize = 16.sp) // Puedes actualizar la versión aquí
                            Text("Asistente IA / Android Studio", fontSize = 16.sp)
                            Text("Onso/agosto 2025", fontSize = 16.sp)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
                )
            }
            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    title = { Text("Ayuda", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    text = {
                        Column {
                            Text("Días festivos en rojo. Eventos de hoy resaltados.", fontSize = 16.sp)
                            Text("Las flechas permiten navegar. Pulsar mes/año cambia vista.", fontSize = 16.sp)
                            Text("Desde el menú (⋮) puedes seleccionar calendarios.", fontSize = 16.sp)
                            Text("Los eventos pueden mostrar su hora de inicio.", fontSize = 16.sp)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
                )
            }

            if (showDayEventsDialog && selectedDateForDialog != null) {
                DayEventsDialog(
                    date = selectedDateForDialog!!,
                    events = eventsForDialog,
                    availableCalendars = availableCalendars,
                    onDismissRequest = {
                        showDayEventsDialog = false
                        selectedDateForDialog = null
                        eventsForDialog = emptyList()
                    }
                )
            }

            // --- MOSTRAR EL DIÁLOGO DE CONFIGURACIÓN DEL WIDGET ---
            if (showWidgetConfigDialog) {
                WidgetConfigScreen(
                    onDismissRequest = { showWidgetConfigDialog = false }
                )
            }
        }
    }
}

// --- WidgetConfigScreen Composable (MODIFICADO) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Número de eventos (Slider) ---
    val initialEventCount = remember {
        prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT)
    }
    var eventCountSliderValue by remember { mutableFloatStateOf(initialEventCount.toFloat()) }

    // --- Letra Grande (Switch) ---
    val initialUseLargeFont = remember {
        prefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false)
    }
    var useLargeFontSwitchState by remember { mutableStateOf(initialUseLargeFont) }


    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Configuración del Widget", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                // --- Configuración del Número de Eventos ---
                Text(
                    "Número de eventos a mostrar: ${eventCountSliderValue.roundToInt()}",
                    fontSize = 16.sp
                )
                Slider(
                    value = eventCountSliderValue,
                    onValueChange = { newValue ->
                        eventCountSliderValue = newValue
                    },
                    valueRange = 1f..12f,
                    steps = 10,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // --- Configuración de Letra Grande ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useLargeFontSwitchState = !useLargeFontSwitchState }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Letra grande en el widget",
                        fontSize = 16.sp
                    )
                    Switch(
                        checked = useLargeFontSwitchState,
                        onCheckedChange = { isChecked ->
                            useLargeFontSwitchState = isChecked
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newEventCount = eventCountSliderValue.roundToInt()
                val newUseLargeFont = useLargeFontSwitchState

                prefs.edit { // Usar el prefs recordado
                    putInt(WidgetConstants.KEY_EVENT_COUNT, newEventCount)
                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, newUseLargeFont)
                    apply()
                }
                Log.d("WidgetConfig", "Guardando config: Eventos=$newEventCount, LetraGrande=$newUseLargeFont")
                notifyCalendarWidgetsConfigurationChanged(context)
                onDismissRequest()
            }) {
                Text("Guardar", fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", fontSize = 16.sp)
            }
        }
    )
}


// --- FUNCIONES DE PERSISTENCIA Y LECTURA DE CALENDARIO (SIN CAMBIOS RESPECTO A TU VERSIÓN ANTERIOR) ---
// saveEventsToPrefs, loadEventsFromPrefs, saveSelectedCalendarIds, loadSelectedCalendarIds,
// loadAvailableCalendars, readFestivosFromCalendars

fun saveEventsToPrefs(context: Context, eventsByDate: Map<LocalDate, List<Festivo>>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val mapToSave = eventsByDate.mapKeys { it.key.toString() }
        .mapValues { entry ->
            entry.value.map { festivo ->
                FestivoDto(
                    desc = festivo.description,
                    id = festivo.calendarId,
                    startTimeStr = festivo.startTime?.toString(),
                    isAllDay = festivo.isAllDay
                )
            }
        }
    prefs.edit {
        putString("events", gson.toJson(mapToSave))
    }
    Log.d("Prefs", "Eventos guardados en SharedPreferences.")
}

fun loadEventsFromPrefs(context: Context): Map<LocalDate, List<Festivo>> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("events", null)
    if (json == null) {
        Log.d("Prefs", "No hay eventos guardados en SharedPreferences.")
        return emptyMap()
    }
    val gson = Gson()
    val type = object : TypeToken<Map<String, List<FestivoDto>>>() {}.type
    val mapFromString: Map<String, List<FestivoDto>> = try {
        gson.fromJson(json, type)
    } catch (e: Exception) {
        Log.e("Prefs", "Error al deserializar eventos desde SharedPreferences", e)
        prefs.edit {
            remove("events") // Limpiar datos corruptos
        }
        return emptyMap()
    }

    return mapFromString.mapNotNull { (dateStr, dtoList) ->
        val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { Log.e("LoadEvents", "Error parseando fecha: '$dateStr'", e); null }
        if (date != null) {
            date to dtoList.map { dto ->
                Festivo(
                    date = date,
                    description = dto.desc,
                    calendarId = dto.id,
                    isFromHolidaySource = false,
                    startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { Log.e("PrefsLoadTime", "Error parseando LocalTime: '$it'", e); null } },
                    isAllDay = dto.isAllDay
                )
            }
        } else { null }
    }.toMap().also {
        Log.d("Prefs", "Eventos cargados: ${it.size} días.")
    }
}


fun saveSelectedCalendarIds(context: Context, selectedIds: Set<Long>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putStringSet("selected_calendar_ids", selectedIds.map { it.toString() }.toSet())
    }
    Log.d("Prefs", "IDs de calendario seleccionados guardados: $selectedIds")
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("selected_calendar_ids", emptySet())
        ?.mapNotNull { idStr -> try { idStr.toLong() } catch (e: NumberFormatException) { Log.e("LoadSelectedIds", "Error parseando ID: '$idStr'", e); null } }
        ?.toSet() ?: emptySet()
}

fun loadAvailableCalendars(context: Context, callback: (List<CalendarInfo>) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        callback(emptyList()); Log.w("CalendarAccess", "Permiso denegado en loadAvailableCalendars"); return
    }
    val calendarsList = mutableListOf<CalendarInfo>()
    val projection = arrayOf(
        CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.ACCOUNT_TYPE
    )
    val cursor: Cursor? = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, projection, null, null,
        "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
    )
    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val displayNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val accountNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
        val colorColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val displayName = it.getString(displayNameColumn) ?: "Calendario sin nombre"
            val accountName = it.getString(accountNameColumn) ?: "Cuenta desconocida"
            val colorInt = try { it.getInt(colorColumn) } catch (_: Exception) { null }
            calendarsList.add(CalendarInfo(id, displayName, accountName, colorInt))
        }
    }
    callback(calendarsList)
}

fun readFestivosFromCalendars(
    context: Context,
    selectedCalendarIds: Set<Long>,
    availableCalendars: List<CalendarInfo>,
    callback: (Map<LocalDate, List<Festivo>>) -> Unit
) {
    if (selectedCalendarIds.isEmpty() || ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        callback(emptyMap()); return
    }

    val holidayCalendarKeywords = listOf("Festivo", "Holiday", "Vacaciones", "Cumpleaños")
    val holidayCalendarIds = availableCalendars
        .filter { calInfo ->
            selectedCalendarIds.contains(calInfo.id) &&
                    holidayCalendarKeywords.any { keyword -> calInfo.displayName.contains(keyword, ignoreCase = true) }
        }
        .map { it.id }.toSet()

    val resolver = context.contentResolver
    val map = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val now = Instant.now()
    val startRangeMillis = now.minus(Duration.ofDays(60)).toEpochMilli()
    val endRangeMillis = now.plus(Duration.ofDays(365)).toEpochMilli()

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, startRangeMillis)
    ContentUris.appendId(builder, endRangeMillis)
    val instancesUri = builder.build()

    val projection = arrayOf(
        CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.ALL_DAY
    )
    val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"

    try {
        val cursor = resolver.query(instancesUri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")
        cursor?.use { c ->
            val calIdColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val beginColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (c.moveToNext()) {
                val calId = c.getLong(calIdColumn)
                val beginMillis = c.getLong(beginColumn)
                val title = c.getString(titleColumn)?.trim() ?: ""
                val isAllDayEventFromProvider = c.getInt(allDayColumn) == 1

                val systemZoneId = ZoneId.systemDefault()
                val beginInstant = Instant.ofEpochMilli(beginMillis)
                val beginDateTimeAtSystemZone = beginInstant.atZone(systemZoneId)
                val actualStartTimeForEvent = if (isAllDayEventFromProvider) null else beginDateTimeAtSystemZone.toLocalTime()
                val isFromHolidayCal = holidayCalendarIds.contains(calId)

                val endInstant = Instant.ofEpochMilli(c.getLong(endColumn))
                var currentDateIterator = beginDateTimeAtSystemZone.toLocalDate()
                val loopEndDate = if (isAllDayEventFromProvider && Duration.between(beginInstant, endInstant).toDays() >= 1) {
                    endInstant.atZone(systemZoneId).toLocalDate().minusDays(1)
                } else {
                    currentDateIterator
                }

                while (!currentDateIterator.isAfter(loopEndDate)) {
                    val list = map.getOrPut(currentDateIterator) { mutableListOf() }
                    list.add(
                        Festivo(
                            date = currentDateIterator,
                            description = title,
                            calendarId = calId,
                            isFromHolidaySource = isFromHolidayCal,
                            startTime = actualStartTimeForEvent,
                            isAllDay = isAllDayEventFromProvider
                        )
                    )
                    currentDateIterator = currentDateIterator.plusDays(1)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ReadFestivos", "Error querying calendar instances", e)
    }
    callback(map)
}


// --- OTROS COMPOSABLES (SelectCalendarsDialog, MonthlyCalendar, YearlyCalendar, MiniMonthCalendar, DayEventsDialog) ---
// (Tu código para estos composables va aquí, sin cambios respecto a tu versión anterior)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCalendarsDialog(
    onDismissRequest: () -> Unit,
    onApplySelection: (selectedIds: Set<Long>) -> Unit
) {
    val context = LocalContext.current
    var localAvailableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var currentSelectedIdsInDialog by remember { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(Unit) {
        loadAvailableCalendars(context) { calendars ->
            localAvailableCalendars = calendars
            val previouslySelected = loadSelectedCalendarIds(context)
            currentSelectedIdsInDialog = previouslySelected.filter { id -> calendars.any { cal -> cal.id == id } }.toSet()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Seleccionar Calendarios", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            if (localAvailableCalendars.isEmpty()) {
                Text("No se encontraron calendarios o no se concedió el permiso.", fontSize = 16.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth()) {
                    items(localAvailableCalendars, key = { it.id }) { calendar ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = currentSelectedIdsInDialog.toMutableSet()
                                    if (newSet.contains(calendar.id)) newSet.remove(calendar.id)
                                    else newSet.add(calendar.id)
                                    currentSelectedIdsInDialog = newSet
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentSelectedIdsInDialog.contains(calendar.id),
                                onCheckedChange = { isChecked ->
                                    val newSet = currentSelectedIdsInDialog.toMutableSet()
                                    if (isChecked) newSet.add(calendar.id)
                                    else newSet.remove(calendar.id)
                                    currentSelectedIdsInDialog = newSet
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(calendar.displayName, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                Text(calendar.accountName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
                            }
                            calendar.color?.let {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color(it), CircleShape)
                                        .border(1.dp, Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplySelection(currentSelectedIdsInDialog) },
                enabled = localAvailableCalendars.isNotEmpty()
            ) { Text("Aplicar", fontSize = 16.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) }
        }
    )
}

@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    puntoEventoColor: Color,
    onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // Lunes = 0
    val daysInMonth = currentMonth.lengthOfMonth()
    val cells = mutableListOf<@Composable () -> Unit>()

    daysOfWeek.forEach { day ->
        cells.add {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFadd1fa)).border(1.dp, Color(0xFFCCCCCC)),
                contentAlignment = Alignment.Center
            ) { Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
    for (i in 0 until firstDayOfWeek) {
        cells.add { Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFCCCCCC))) }
    }

    for (dayNum in 1..daysInMonth) {
        val thisDate = currentMonth.atDay(dayNum)
        val isToday = thisDate == today
        val isSunday = thisDate.dayOfWeek == java.time.DayOfWeek.SUNDAY
        val dayEvents = eventsByDate[thisDate].orEmpty()
        val dayEventsConAlgunaInfo = dayEvents.any { festivo ->
            val desc = festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }
            desc.isNotBlank()
        }
        val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
        val hasOtherEvents = dayEvents.any {
            !it.isFromHolidaySource &&
                    ( (it.startTime != null && !it.isAllDay) || it.description.isNotBlank() )
        }
        val textColor: Color
        val currentFontWeight: FontWeight = if (isHoliday || isToday) FontWeight.Bold else FontWeight.Normal
        when {
            isHoliday -> textColor = Color.Red
            isSunday -> textColor = Color.Red.copy(alpha = 0.7f)
            else -> textColor = Color.Black
        }
        val azulCabeceraBorde = Color(0xFF2196F3)

        cells.add {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = if (isToday) azulCabeceraBorde else Color(0xFFCCCCCC),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = dayEventsConAlgunaInfo) {
                        onDayClick(thisDate, dayEvents.filter { festivo ->
                            val desc = festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }
                            desc.isNotBlank()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("$dayNum", fontWeight = currentFontWeight, color = textColor, fontSize = 22.sp)
                    if (hasOtherEvents) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.size(6.dp).background(puntoEventoColor, CircleShape))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
    val remainder = cells.size % 7
    if (remainder != 0) {
        for (i in 0 until (7 - remainder)) {
            cells.add { Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFCCCCCC))) }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFf1f7fe), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)).padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            for (rowIndex in 0 until cells.size / 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (colIndex in 0 until 7) {
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            cells[rowIndex * 7 + colIndex].invoke()
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun YearlyCalendar(
    currentYear: Year,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(currentYear.value, it) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        months.chunked(3).forEach { monthRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                monthRow.forEach { month ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                            .clickable { onMonthSelected(month) },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MiniMonthCalendar(month, today, eventsByDate)
                    }
                }
                if (monthRow.size < 3) {
                    for (i in 0 until (3 - monthRow.size)) {
                        Spacer(Modifier.weight(1f).padding(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val totalCellsToDisplay = 6 * 7

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(6.dp))
            .padding(vertical = 3.dp, horizontal = 2.dp)
    ) {
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        val dayCellsData = remember(month) {
            List(totalCellsToDisplay) { cellIndex ->
                val dayNumber = cellIndex - firstDayOfWeekIndex + 1
                if (dayNumber in 1..daysInMonth) month.atDay(dayNumber) else null
            }
        }
        dayCellsData.chunked(7).forEach { weekDates ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDates.forEach { date ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val isToday = date == today
                            val dayEvents = eventsByDate[date].orEmpty()
                            val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
                            val azulCabeceraBorde = Color(0xFF2196F3)
                            val textColor = when {
                                isHoliday -> Color.Red
                                date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color.Red.copy(alpha = 0.7f)
                                isToday -> Color.Blue
                                else -> Color.Black.copy(alpha = 0.8f)
                            }
                            val currentFontWeight = if (isHoliday || isToday) FontWeight.Bold else FontWeight.Normal
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isToday && !isHoliday) {
                                    Box(
                                        modifier = Modifier
                                            .offset(y = 3.dp)
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(azulCabeceraBorde.copy(alpha = 0.30f))
                                    )
                                }
                                Text(
                                    text = "${date.dayOfMonth}",
                                    fontSize = 9.sp,
                                    fontWeight = currentFontWeight,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEventsDialog(
    date: LocalDate,
    events: List<Festivo>,
    availableCalendars: List<CalendarInfo>,
    onDismissRequest: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault()) }
    val formattedDate = remember(date) { date.format(dateFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = formattedDate, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            val eventsToDisplay = events.mapNotNull { festivo ->
                val displayDescription = if (!festivo.isAllDay && festivo.startTime != null) {
                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty{"(Sin título)"}}"
                } else {
                    festivo.description.ifEmpty { if(festivo.isAllDay) "(Evento todo el día)" else "" }
                }
                if (displayDescription.isNotBlank()) festivo to displayDescription else null
            }

            if (eventsToDisplay.isEmpty()) {
                Text("No hay eventos con detalle para este día.", fontSize = 16.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(eventsToDisplay, key = { (festivo, _) -> festivo.calendarId.toString() + festivo.description + festivo.startTime.toString() + festivo.date.toString() }) { (festivo, displayDescription) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            val itemColor = if (festivo.isFromHolidaySource) Color.Red else Color.Black
                            val calendarInfo = availableCalendars.find { it.id == festivo.calendarId }
                            val eventColorInt = calendarInfo?.color

                            eventColorInt?.let { colorInt ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(colorInt), CircleShape)
                                        .border(0.5.dp, Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = displayDescription,
                                color = itemColor,
                                fontSize = 16.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("Cerrar", fontSize = 16.sp) } }
    )
}
