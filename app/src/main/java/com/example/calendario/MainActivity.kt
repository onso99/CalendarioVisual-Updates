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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
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

    // --- Paleta de Colores Principal de la Pantalla ---
    val azulFijo = Color(0xFF2196F3) // Azul primario para TopAppBar y punto de evento en el calendario

    // Define el color de fondo de la pantalla aquí para fácil modificación
    val colorDeFondoPantalla = Color(0xFFfafafa) // Ejemplo: Azul cielo claro
    // Alternativas que puedes probar descomentando:
    // val colorDeFondoPantalla = Color.Black
    // val colorDeFondoPantalla = Color(0xFF001f3f) // Azul marino oscuro
    // val colorDeFondoPantalla = Color(0xFF121212) // Gris oscuro estándar para temas dark
    // val colorDeFondoPantalla = Color(0xFF263238) // Azul grisáceo oscuro

    // Colores para elementos sobre el colorDeFondoPantalla
    val colorTextoNormalSobreFondo = Color.Black
    val colorTextoSecundarioSobreFondo = Color.DarkGray // Para mensajes como "lista vacía"
    val colorResaltadoFestivos = Color.Red // Para festivos en la lista
    val colorResaltadoEventosHoyLista = azulFijo // Eventos de hoy en la lista (tu azul primario)

    // Colores para los botones de navegación (flechas, mes/año)
    val colorFondoBotonesNavegacion = Color(0xFFffbb77) // Gris claro
    val colorContenidoBotonesNavegacion = Color.Black    // Iconos/texto negro
    // --- Fin de la Paleta de Colores ---


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
            Column(modifier = Modifier
                .background(azulFijo) // El TopAppBar sigue siendo azulFijo
                .statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "Calendario Visual",
                            fontSize = 20.sp,
                            color = Color.White, // Texto del TopAppBar es blanco
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
                                modifier = Modifier.background(Color.White) // Fondo del menú desplegable claro
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Calendarios", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
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
                                    text = { Text("Widget", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showWidgetConfigDialog = true
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
                .background(colorDeFondoPantalla) // Usando la variable para el fondo de pantalla
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Controles de Navegación y Título del Mes/Año ---
            Row(
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
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorFondoBotonesNavegacion,
                        contentColor = colorContenidoBotonesNavegacion
                    )
                ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Anterior") }

                Button(
                    onClick = {
                        viewMode = if (viewMode == CalendarViewMode.MONTHLY) CalendarViewMode.YEARLY
                        else { currentMonth = YearMonth.now(); CalendarViewMode.MONTHLY }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorFondoBotonesNavegacion,
                        contentColor = colorContenidoBotonesNavegacion
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        if (viewMode == CalendarViewMode.MONTHLY)
                            "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}"
                        else "${currentYear.value}",
                        fontSize = 20.sp // El color se hereda de colorContenidoBotonesNavegacion
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.plusMonths(1)
                        else currentYear = currentYear.plusYears(1)
                    },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorFondoBotonesNavegacion,
                        contentColor = colorContenidoBotonesNavegacion
                    )
                ) { Icon(Icons.Filled.ArrowForward, contentDescription = "Siguiente") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewMode == CalendarViewMode.MONTHLY) {
                MonthlyCalendar( // MonthlyCalendar mantiene su apariencia clara interna
                    currentMonth = currentMonth,
                    today = today,
                    eventsByDate = eventsByDate,
                    puntoEventoColor = azulFijo, // Punto de evento en calendario sigue siendo azulFijo
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

                Text(
                    listTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                    color = colorTextoNormalSobreFondo // Usando variable
                )

                if (finalEventsToList.isEmpty()) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center) {
                        val emptyListMessage = if (isCurrentMonthView) "No hay eventos pendientes para este mes." else "No hay eventos para este mes."
                        Text(
                            emptyListMessage,
                            fontSize = 16.sp,
                            color = colorTextoSecundarioSobreFondo // Usando variable
                        )
                    }
                } else {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp)) {
                        finalEventsToList.forEach { (date, festivos) ->
                            val isTodayEvents = isCurrentMonthView && date == today

                            festivos.forEach { festivo ->
                                val baseDayTextColor = if (festivo.isFromHolidaySource) colorResaltadoFestivos else colorTextoNormalSobreFondo
                                val baseDescriptionTextColor = if (festivo.isFromHolidaySource) colorResaltadoFestivos else colorTextoNormalSobreFondo

                                val currentDayNumberColor = if (isTodayEvents && !festivo.isFromHolidaySource) colorResaltadoEventosHoyLista else baseDayTextColor
                                val currentDescriptionColor = if (isTodayEvents && !festivo.isFromHolidaySource) colorResaltadoEventosHoyLista else baseDescriptionTextColor

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
                                            color = currentDayNumberColor, // Usando variable aplicada
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = ": $displayDescription",
                                            color = currentDescriptionColor, // Usando variable aplicada
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
                YearlyCalendar( // YearlyCalendar también mantiene su apariencia clara interna
                    currentYear,
                    today,
                    eventsByDate
                ) { selectedMonth ->
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
                            saveEventsToPrefs(context, festivos)
                            notifyCalendarWidgetsDataChanged(context) // Notificar widgets
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
                            Text("Calendario Visual V1.2.55.1", fontSize = 16.sp)
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
                            Text(
                                "El botón con el mes alterna entre calendario mensual y anual.",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                "Las flechas laterales permiten navegar entre los meses y los años.",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                "Pulsando en un día con eventos se mostrará una lista de ellos. ",
                                fontSize = 16.sp
                            )
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
                        "Letra grande",
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
    availableCalendars: List<CalendarInfo>, // Esta lista debe ser proporcionada por una función como loadAvailableCalendars
    callback: (Map<LocalDate, List<Festivo>>) -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        Log.w("ReadFestivos", "Permiso READ_CALENDAR no concedido.")
        callback(emptyMap()); return
    }
    if (selectedCalendarIds.isEmpty()) {
        Log.i("ReadFestivos", "No hay calendarios seleccionados por el usuario.")
        callback(emptyMap()); return
    }

    // Identificar el ID del calendario que se considera la fuente principal de festivos.
    // Según tus logs y aclaraciones, este es el ID 2 para "Festivos en España".
    val specificHolidaySourceCalendarId = 2L
    val holidayCalendarIds = if (selectedCalendarIds.contains(specificHolidaySourceCalendarId)) {
        setOf(specificHolidaySourceCalendarId)
    } else {
        emptySet<Long>()
    }

    if (holidayCalendarIds.isEmpty() && selectedCalendarIds.contains(specificHolidaySourceCalendarId)) {
        Log.w("ReadFestivos", "El calendario fuente de festivos (ID $specificHolidaySourceCalendarId) fue seleccionado pero no se pudo identificar como tal. Revisa la lógica si esto ocurre.")
    } else if (holidayCalendarIds.isEmpty() && selectedCalendarIds.isNotEmpty()) {
        Log.i("ReadFestivos", "El calendario fuente de festivos (ID $specificHolidaySourceCalendarId) no está entre los seleccionados o no se identificó ninguno.")
    }

    val resolver = context.contentResolver
    val map = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val now = Instant.now()

    val daysInTwoYears = 730L // Aproximadamente +/- 2 años
    val startRangeMillis = now.minus(Duration.ofDays(daysInTwoYears)).toEpochMilli()
    val endRangeMillis = now.plus(Duration.ofDays(daysInTwoYears)).toEpochMilli()

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

    // Consultar solo los calendarios que el usuario ha seleccionado
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
                val eventCalId = c.getLong(calIdColumn)
                val beginMillis = c.getLong(beginColumn)
                val title = c.getString(titleColumn)?.trim() ?: "(Sin título)"
                val isAllDayEvent = c.getInt(allDayColumn) == 1

                val systemZoneId = ZoneId.systemDefault()
                val beginInstant = Instant.ofEpochMilli(beginMillis)
                val beginDateTimeAtSystemZone = beginInstant.atZone(systemZoneId)

                val isFromHolidaySource = holidayCalendarIds.contains(eventCalId)
                val actualStartTime = if (isAllDayEvent) null else beginDateTimeAtSystemZone.toLocalTime()

                val endInstant = Instant.ofEpochMilli(c.getLong(endColumn))
                var currentDateIterator = beginDateTimeAtSystemZone.toLocalDate()

                val loopEndDate = if (isAllDayEvent && Duration.between(beginInstant, endInstant).toDays() >= 1L) {
                    endInstant.atZone(systemZoneId).toLocalDate().minusDays(1L)
                } else {
                    beginDateTimeAtSystemZone.toLocalDate()
                }

                while (!currentDateIterator.isAfter(loopEndDate)) {
                    val list = map.getOrPut(currentDateIterator) { mutableListOf() }
                    list.add(
                        Festivo(
                            date = currentDateIterator,
                            description = title,
                            calendarId = eventCalId,
                            isFromHolidaySource = isFromHolidaySource,
                            startTime = actualStartTime,
                            isAllDay = isAllDayEvent
                        )
                    )
                    currentDateIterator = currentDateIterator.plusDays(1L)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ReadFestivos", "Error al consultar las instancias del calendario", e)
    }

    val totalFestivosEnMapa = map.values.flatten().count { it.isFromHolidaySource }
    Log.i("ReadFestivos", "Callback con mapa. Días con eventos: ${map.size}. Festivos (isFromHolidaySource=true): $totalFestivosEnMapa.")

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
                LazyColumn(modifier = Modifier
                    .heightIn(max = 400.dp)
                    .fillMaxWidth()) {
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
                                        .border(
                                            1.dp,
                                            Color.DarkGray.copy(alpha = 0.5f),
                                            CircleShape
                                        )
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFadd1fa))
                    .border(1.dp, Color(0xFFCCCCCC)),
                contentAlignment = Alignment.Center
            ) { Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
    for (i in 0 until firstDayOfWeek) {
        cells.add { Box(Modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFFCCCCCC))) }
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

        // --- Corrección Aplicada Aquí ---
        // 1. Determinar el fontWeight basado SOLAMENTE en las condiciones de contenido (ej. festivo)
        val currentFontWeight: FontWeight = if (isHoliday) { // Solo 'isHoliday' causa negrita ahora
            FontWeight.Bold
        } else {
            FontWeight.Normal
        }

        // 2. Determinar el textColor
        val textColor: Color = when {
            isHoliday -> Color.Red // Festivos en rojo
            isSunday -> Color.Red.copy(alpha = 0.7f) // Domingos en rojo claro
            // Opcional: si quieres que "hoy" tenga un color de texto diferente SI NO ES FESTIVO NI DOMINGO
            // isToday && !isHoliday && !isSunday -> MaterialTheme.colorScheme.primary // Por ejemplo, el color primario
            else -> Color.Black // Días normales
        }
        // --- Fin de la Corrección ---

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
                            val desc =
                                festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }
                            desc.isNotBlank()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "$dayNum",
                        fontWeight = currentFontWeight, // fontWeight corregido
                        color = textColor,             // textColor determinado
                        fontSize = 22.sp
                    )
                    if (hasOtherEvents) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier
                            .size(6.dp)
                            .background(puntoEventoColor, CircleShape))
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
            cells.add { Box(Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFCCCCCC))) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFf1f7fe), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            for (rowIndex in 0 until cells.size / 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (colIndex in 0 until 7) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp),
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

    val horizontalSpacingBetweenMonths = 4.dp
    val verticalSpacingBetweenMonthRows = 4.dp

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        months.chunked(3).forEachIndexed { rowIndex, monthRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacingBetweenMonths)
            ) {
                monthRow.forEach { month ->
                    // Este Box exterior toma el peso, define la forma (cuadrada) y es clickeable
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f) // FORZAR CELDA CUADRADA
                            .clickable { onMonthSelected(month) },
                        contentAlignment = Alignment.Center // Centrar MiniMonthCalendar dentro de la celda cuadrada
                    ) {
                        // MiniMonthCalendar ahora debe usar fillMaxSize()
                        MiniMonthCalendar(
                            month = month,
                            today = today,
                            eventsByDate = eventsByDate,
                            // Pasamos un Modifier para que MiniMonthCalendar sepa que debe llenar el espacio
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                // Rellenar las celdas restantes en la fila si es necesario
                if (monthRow.size < 3) {
                    for (i in 0 until (3 - monthRow.size)) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f)) // Mantener proporción también para los spacers
                    }
                }
            }

            // Espaciador vertical entre las filas de meses
            if (rowIndex < months.chunked(3).size - 1) {
                Spacer(modifier = Modifier.height(verticalSpacingBetweenMonthRows))
            }
        }
    }
}




@Composable
fun MiniMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    modifier: Modifier = Modifier
) {
    val daysOfWeekShort = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val totalCellsToDisplay = 6 * 7

    val monthNameFontSize = 9.sp
    val dayHeadersFontSize = 7.sp
    val dayNumberFontSize = 8.sp

    val compactTextStyle = LocalTextStyle.current.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Column(
        modifier = modifier
            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(4.dp))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            fontSize = monthNameFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = compactTextStyle.copy(lineHeight = monthNameFontSize * 0.95f),
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeekShort.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = dayHeadersFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        style = compactTextStyle.copy(lineHeight = dayHeadersFontSize * 0.95f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f), // Columna de la cuadrícula de días toma espacio vertical
            // Ya no necesitamos verticalArrangement aquí si cada Row toma weight
        ) {
            val dayCellsData = remember(month, daysInMonth, firstDayOfWeekIndex) {
                List(totalCellsToDisplay) { cellIndex ->
                    val dayNumber = cellIndex - firstDayOfWeekIndex + 1
                    if (dayNumber in 1..daysInMonth) month.atDay(dayNumber) else null
                }
            }

            dayCellsData.chunked(7).forEach { weekDates -> // Esto itera 6 veces
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // <--- CADA FILA DE SEMANA TOMA 1/6 DE LA ALTURA DISPONIBLE PARA LA CUADRÍCULA
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically // Centrar celdas de día verticalmente en la Row
                ) {
                    weekDates.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f) // Esto ahora se basa en el ancho Y altura dados por los weights
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                val isToday = date == today
                                val dayEvents = eventsByDate[date].orEmpty()
                                val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
                                val azulResaltadoHoy = Color(0xFF2196F3)

                                val baseTextColor = when {
                                    isHoliday -> Color.Red
                                    date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color.Red.copy(alpha = 0.7f)
                                    else -> Color.Black.copy(alpha = 0.9f)
                                }
                                val finalTextColor = if (isToday && !isHoliday) Color.Blue.copy(alpha = 0.9f) else baseTextColor
                                val currentFontWeight = if (isHoliday || isToday) FontWeight.Bold else FontWeight.Normal

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isToday && !isHoliday) {
                                        Box(
                                            modifier = Modifier
                                                .size((dayNumberFontSize.value * 2.2f).dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(azulResaltadoHoy.copy(alpha = 0.15f))
                                        )
                                    }
                                    Text(
                                        text = "${date.dayOfMonth}",
                                        fontSize = dayNumberFontSize,
                                        fontWeight = currentFontWeight,
                                        color = finalTextColor,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        style = compactTextStyle.copy(lineHeight = dayNumberFontSize * 0.95f)
                                    )
                                }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val itemColor = if (festivo.isFromHolidaySource) Color.Red else Color.Black
                            val calendarInfo = availableCalendars.find { it.id == festivo.calendarId }
                            val eventColorInt = calendarInfo?.color

                            eventColorInt?.let { colorInt ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(colorInt), CircleShape)
                                        .border(
                                            0.5.dp,
                                            Color.DarkGray.copy(alpha = 0.5f),
                                            CircleShape
                                        )
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
