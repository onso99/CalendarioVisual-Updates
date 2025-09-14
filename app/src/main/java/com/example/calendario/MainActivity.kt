package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent // Necesario para el broadcast al widget
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

// ★★★ IMPORTACIONES PARA DATA CLASSES (desde DataModels.kt) ★★★
import com.example.calendario.CalendarInfo
import com.example.calendario.Festivo
// import com.example.calendario.FestivoDto

// ★★★ IMPORTACIONES PARA FUNCIONES DE UTILIDAD (desde CalendarDataUtils.kt) ★★★
import com.example.calendario.loadEventsFromPrefs
import com.example.calendario.loadSelectedCalendarIds
import com.example.calendario.saveSelectedCalendarIds
import com.example.calendario.saveEventsToPrefs
import com.example.calendario.loadAvailableCalendarsSuspend
import com.example.calendario.readFestivosFromCalendarsSuspend
import com.example.calendario.processEventsForDisplay


class MainActivity : ComponentActivity() {

    private var eventsByDateState by mutableStateOf<Map<LocalDate, List<Festivo>>>(emptyMap())
    private var availableCalendarsState by mutableStateOf<List<CalendarInfo>>(emptyList())
    private var selectedCalendarIdsState by mutableStateOf<Set<Long>>(emptySet())
    private var hasCalendarPermissionState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        eventsByDateState = loadEventsFromPrefs(this)
        selectedCalendarIdsState = loadSelectedCalendarIds(this)
        hasCalendarPermissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

        setContent {
            CalendarioApp(
                initialEventsByDate = eventsByDateState,
                initialAvailableCalendars = availableCalendarsState,
                initialSelectedCalendarIds = selectedCalendarIdsState,
                initialHasPermission = hasCalendarPermissionState,
                onRefreshRequest = {
                    refreshDataFromCalendarProviderAndUpdateStates()
                },
                onCalendarDataUpdated = { newEvents, newAvailable, newSelectedIds ->
                    eventsByDateState = newEvents
                    availableCalendarsState = newAvailable
                    selectedCalendarIdsState = newSelectedIds
                    saveEventsToPrefs(this, newEvents)
                    saveSelectedCalendarIds(this, newSelectedIds)
                    notifyCalendarWidgetsDataChangedMainActivity(this)
                },
                onPermissionUpdated = { newPermissionState ->
                    hasCalendarPermissionState = newPermissionState
                    if (newPermissionState) {
                        refreshDataFromCalendarProviderAndUpdateStates()
                    } else {
                        eventsByDateState = emptyMap()
                        availableCalendarsState = emptyList()
                        saveEventsToPrefs(this, emptyMap())
                        notifyCalendarWidgetsDataChangedMainActivity(this)
                    }
                }
            )
        }

        if (hasCalendarPermissionState) {
            refreshDataFromCalendarProviderAndUpdateStates()
        }
        Log.d("MainActivity", "onCreate - Carga inicial de datos solicitada si hay permiso.")
    }

    override fun onResume() {
        super.onResume()
        hasCalendarPermissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (hasCalendarPermissionState) {
            Log.d("MainActivity", "onResume - Recargando datos desde CalendarProvider.")
            refreshDataFromCalendarProviderAndUpdateStates()
        } else {
            Log.d("MainActivity", "onResume - No hay permiso de calendario, no se recargan datos.")
            eventsByDateState = emptyMap()
            availableCalendarsState = emptyList()
        }
    }

    private fun refreshDataFromCalendarProviderAndUpdateStates() {
        Log.d("MainActivity", "refreshData - Iniciando.")
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w("MainActivity", "refreshData - No hay permiso de calendario. Abortando refresco.")
            hasCalendarPermissionState = false
            eventsByDateState = emptyMap()
            availableCalendarsState = emptyList()
            saveEventsToPrefs(this, emptyMap())
            notifyCalendarWidgetsDataChangedMainActivity(this)
            return
        }
        if (!hasCalendarPermissionState) hasCalendarPermissionState = true

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "refreshData - Corrutina iniciada.")
                val currentSelectedIds = loadSelectedCalendarIds(this@MainActivity)

                val freshAvailableCalendars = loadAvailableCalendarsSuspend(this@MainActivity)
                availableCalendarsState = freshAvailableCalendars
                Log.d("MainActivity", "refreshData - Calendarios disponibles: ${freshAvailableCalendars.size}")

                val validSelectedIds = currentSelectedIds.filter { sid ->
                    freshAvailableCalendars.any { cal -> cal.id == sid }
                }.toSet()

                if (validSelectedIds != selectedCalendarIdsState) {
                    Log.i("MainActivity", "refreshData - IDs de calendario seleccionados actualizados a válidos: $validSelectedIds")
                    selectedCalendarIdsState = validSelectedIds
                    saveSelectedCalendarIds(this@MainActivity, validSelectedIds)
                }

                val freshEventsMap: Map<LocalDate, List<Festivo>>
                if (selectedCalendarIdsState.isNotEmpty() || freshAvailableCalendars.isNotEmpty()) {
                    freshEventsMap = readFestivosFromCalendarsSuspend(
                        this@MainActivity,
                        selectedCalendarIdsState,
                        freshAvailableCalendars
                    )
                    Log.d("MainActivity", "refreshData - Datos frescos leídos para IDs ${selectedCalendarIdsState}: ${freshEventsMap.size} días con eventos.")
                } else {
                    Log.i("MainActivity", "refreshData - No hay calendarios válidos seleccionados ni disponibles para leer eventos.")
                    freshEventsMap = emptyMap()
                }
                eventsByDateState = freshEventsMap

                saveEventsToPrefs(this@MainActivity, freshEventsMap)
                Log.i("MainActivity", "refreshData - Datos frescos guardados en SharedPreferences.")

                notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity)

            } catch (e: Exception) {
                Log.e("MainActivity", "refreshData - Error refrescando datos del calendario", e)
                Toast.makeText(this@MainActivity, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()
                eventsByDateState = emptyMap()
                availableCalendarsState = emptyList()
            }
        }
    }
}

fun notifyCalendarWidgetsConfigurationChangedMainActivity(context: Context) {
    Log.d("MainActivityNotifier", "Intentando notificar a los widgets sobre cambio de configuración.")
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivityNotifier", "Notificación enviada a los widgets para actualizar por cambio de configuración.")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets activos para notificar por cambio de configuración.")
    }
}

fun notifyCalendarWidgetsDataChangedMainActivity(context: Context) {
    Log.d("MainActivityNotifier", "Intentando notificar a los widgets sobre cambio de datos de eventos.")
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivityNotifier", "Notificación enviada a los widgets para actualizar datos de eventos.")
    } else {
        Log.d("MainActivityNotifier", "No hay widgets activos para notificar sobre cambio de datos de eventos.")
    }
}

@Composable
fun CalendarioApp(
    initialEventsByDate: Map<LocalDate, List<Festivo>>,
    initialAvailableCalendars: List<CalendarInfo>,
    initialSelectedCalendarIds: Set<Long>,
    initialHasPermission: Boolean,
    onRefreshRequest: () -> Unit,
    onCalendarDataUpdated: (Map<LocalDate, List<Festivo>>, List<CalendarInfo>, Set<Long>) -> Unit,
    onPermissionUpdated: (Boolean) -> Unit
) {
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
        CalendarioScreen(
            eventsByDateExternal = initialEventsByDate,
            availableCalendarsExternal = initialAvailableCalendars,
            selectedCalendarIdsExternal = initialSelectedCalendarIds,
            hasCalendarPermissionExternal = initialHasPermission,
            onRefreshRequest = onRefreshRequest,
            onCalendarDataUpdated = onCalendarDataUpdated,
            onPermissionUpdated = onPermissionUpdated
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    eventsByDateExternal: Map<LocalDate, List<Festivo>>,
    availableCalendarsExternal: List<CalendarInfo>,
    selectedCalendarIdsExternal: Set<Long>,
    hasCalendarPermissionExternal: Boolean,
    onRefreshRequest: () -> Unit,
    onCalendarDataUpdated: (Map<LocalDate, List<Festivo>>, List<CalendarInfo>, Set<Long>) -> Unit,
    onPermissionUpdated: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var showWidgetConfigDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionUpdated(isGranted)
    }

    LaunchedEffect(hasCalendarPermissionExternal) {
        if (hasCalendarPermissionExternal) {
            Log.d("CalendarioScreen", "LaunchedEffect(hasPermission): Permiso OK. Solicitando refresco.")
            onRefreshRequest()
        } else {
            Log.d("CalendarioScreen", "LaunchedEffect(hasPermission): Sin permiso.")
        }
    }

    // Definición de colores
    val azulFijo = Color(0xFF2196F3)
    val colorDeFondoPantalla = Color(0xFFfafafa)
    val colorTextoNormalSobreFondo = Color.Black
    val colorTextoSecundarioSobreFondo = Color.DarkGray
    val colorResaltadoFestivos = Color.Red
    val colorFondoBotonesNavegacion = Color(0xFFffbb77)
    val colorContenidoBotonesNavegacion = Color.Black
    val AzulMarinoCumpleanos = Color(0xFF0000ff)

    Scaffold(
        topBar = {
            Column(modifier = Modifier
                .background(azulFijo)
                .statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "Calendario Visual",
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
                                    text = { Text("Calendarios", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        if (hasCalendarPermissionExternal) {
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
                .background(colorDeFondoPantalla)
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        if (viewMode == CalendarViewMode.MONTHLY) {
                            currentYear = Year.of(currentMonth.year)
                            viewMode = CalendarViewMode.YEARLY
                        } else {
                            currentMonth = YearMonth.now()
                            viewMode = CalendarViewMode.MONTHLY
                        }
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
                        fontSize = 20.sp
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

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (viewMode == CalendarViewMode.MONTHLY) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MonthlyCalendar(
                            currentMonth = currentMonth,
                            today = today,
                            eventsByDate = eventsByDateExternal,
                            puntoEventoColor = azulFijo,
                            onDayClick = { date, events ->
                                selectedDateForDialog = date
                                eventsForDialog = events
                                showDayEventsDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val finalEventsToList: List<Pair<LocalDate, List<Festivo>>> =
                            processEventsForDisplay(
                                eventsByDate = eventsByDateExternal,
                                targetMonth = currentMonth,
                                today = today
                            )

                        val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
                        val listTitle = if (isCurrentMonthView) "Eventos pendientes de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"
                        else "Eventos de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"

                        Text(
                            listTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp),
                            color = azulFijo
                        )
                        if (finalEventsToList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                val emptyListMessage = if (isCurrentMonthView) "No hay eventos pendientes para este mes." else "No hay eventos para este mes."
                                Text(emptyListMessage, fontSize = 16.sp, color = colorTextoSecundarioSobreFondo)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp)
                            ) {
                                finalEventsToList.forEach { (date, festivos) ->
                                    val isTodayEvents = isCurrentMonthView && date == today

                                    festivos.forEach { festivo ->
                                        val esCumpleanos = festivo.description.contains("cumpleaños", ignoreCase = true) ||
                                                festivo.description.contains("aniversario", ignoreCase = true)

                                        val colorDelNumeroDiaLista: Color
                                        val colorDescripcionLista: Color

                                        var fontWeightNumeroDiaLista: FontWeight
                                        var fontWeightDescripcionLista: FontWeight

                                        if (esCumpleanos) {
                                            colorDelNumeroDiaLista = AzulMarinoCumpleanos
                                            colorDescripcionLista = AzulMarinoCumpleanos
                                        } else if (festivo.isFromHolidaySource) {
                                            colorDelNumeroDiaLista = colorResaltadoFestivos
                                            colorDescripcionLista = colorResaltadoFestivos
                                        } else {
                                            colorDelNumeroDiaLista = colorTextoNormalSobreFondo
                                            colorDescripcionLista = colorTextoNormalSobreFondo
                                        }

                                        if (isTodayEvents) {
                                            fontWeightNumeroDiaLista = FontWeight.Bold
                                            fontWeightDescripcionLista = FontWeight.Bold
                                        } else {
                                            fontWeightNumeroDiaLista = FontWeight.Normal // Otros días, número normal
                                            fontWeightDescripcionLista = FontWeight.Normal // Otros días, descripción normal
                                        }

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
                                                    text = "${formattedDay}:", // Dos puntos pegados al día
                                                    color = colorDelNumeroDiaLista,
                                                    fontWeight = fontWeightNumeroDiaLista,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = displayDescription, // Sin espacio inicial
                                                    color = colorDescripcionLista,
                                                    fontWeight = fontWeightDescripcionLista,
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
                    }
                } else { // Yearly View
                    YearlyCalendar(
                        currentYear = currentYear,
                        today = today,
                        eventsByDate = eventsByDateExternal,
                    ) { selectedMonth ->
                        currentMonth = selectedMonth
                        viewMode = CalendarViewMode.MONTHLY
                    }
                }
            }


            // Diálogos
            if (showSelectCalendarsDialog) {
                SelectCalendarsDialog(
                    onDismissRequest = { showSelectCalendarsDialog = false },
                    onApplySelection = { newSelectedIds ->
                        scope.launch {
                            try {
                                val currentAvailable = if (availableCalendarsExternal.isNotEmpty()) availableCalendarsExternal else loadAvailableCalendarsSuspend(context)
                                val newFestivos = readFestivosFromCalendarsSuspend(context, newSelectedIds, currentAvailable)
                                onCalendarDataUpdated(newFestivos, currentAvailable, newSelectedIds)
                            } catch (e: Exception) {
                                Log.e("CalendarioScreen", "Error aplicando selección de calendarios: ${e.localizedMessage}", e)
                                Toast.makeText(context, "Error al aplicar selección.", Toast.LENGTH_SHORT).show()
                            } finally {
                                showSelectCalendarsDialog = false
                            }
                        }
                    }
                )
            }
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    text = {
                        Column {
                            Text("Calendario Visual V1.30", fontSize = 16.sp)
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
                            Text("El botón con el mes alterna entre calendario mensual y anual.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text("Las flechas laterales permiten navegar entre los meses y los años.", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text("Pulsando en un día con eventos se mostrará una lista de ellos. ", fontSize = 16.sp)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
                )
            }
            if (showDayEventsDialog && selectedDateForDialog != null) {
                DayEventsDialog(
                    date = selectedDateForDialog!!,
                    events = eventsForDialog,
                    availableCalendars = availableCalendarsExternal,
                    onDismissRequest = {
                        showDayEventsDialog = false
                        selectedDateForDialog = null
                        eventsForDialog = emptyList()
                    }
                )
            }
            if (showWidgetConfigDialog) {
                WidgetConfigScreen(onDismissRequest = { showWidgetConfigDialog = false })
            }
        }
    }
}

enum class CalendarViewMode { MONTHLY, YEARLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE) }
    val initialEventCount = remember { prefs.getInt(WidgetConstants.KEY_EVENT_COUNT, WidgetConstants.DEFAULT_EVENT_COUNT) }
    var eventCountSliderValue by remember { mutableFloatStateOf(initialEventCount.toFloat()) }
    val initialUseLargeFont = remember { prefs.getBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, false) }
    var useLargeFontSwitchState by remember { mutableStateOf(initialUseLargeFont) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Configuración del Widget", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                Text("Número de eventos a mostrar: ${eventCountSliderValue.roundToInt()}", fontSize = 16.sp)
                Slider(
                    value = eventCountSliderValue,
                    onValueChange = { newValue -> eventCountSliderValue = newValue },
                    valueRange = 1f..12f,
                    steps = 10,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useLargeFontSwitchState = !useLargeFontSwitchState }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Letra grande", fontSize = 16.sp)
                    Switch(
                        checked = useLargeFontSwitchState,
                        onCheckedChange = { isChecked -> useLargeFontSwitchState = isChecked }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newEventCount = eventCountSliderValue.roundToInt()
                val newUseLargeFont = useLargeFontSwitchState
                prefs.edit {
                    putInt(WidgetConstants.KEY_EVENT_COUNT, newEventCount)
                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, newUseLargeFont)
                }
                Log.d("WidgetConfig", "Guardando config: Eventos=$newEventCount, LetraGrande=$newUseLargeFont")
                notifyCalendarWidgetsConfigurationChangedMainActivity(context)
                onDismissRequest()
            }) { Text("Guardar", fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
    )
}

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
        try {
            val calendars = loadAvailableCalendarsSuspend(context)
            localAvailableCalendars = calendars
            val previouslySelected = loadSelectedCalendarIds(context)
            currentSelectedIdsInDialog = previouslySelected.filter { id -> calendars.any { cal -> cal.id == id } }.toSet()
        } catch (e: Exception) {
            Log.e("SelectCalendarsDialog", "Error cargando calendarios disponibles: ${e.localizedMessage}", e)
            Toast.makeText(context, "No se pudieron cargar los calendarios.", Toast.LENGTH_SHORT).show()
            localAvailableCalendars = emptyList()
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
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
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
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val cells = mutableListOf<@Composable () -> Unit>()

    val colorBordeDiaActual = MaterialTheme.colorScheme.primary

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
            festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }.isNotBlank()
        }
        val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
        val hasOtherEventsForPoint = dayEvents.any { !it.isFromHolidaySource && ((it.startTime != null && !it.isAllDay) || it.description.isNotBlank()) }

        val numeroDiaFontWeight: FontWeight = if (isHoliday) FontWeight.Bold else FontWeight.Normal

        val numeroDiaColor: Color = when {
            isHoliday -> Color.Red
            isSunday -> Color.Red.copy(alpha = 0.7f)
            else -> Color.Black
        }

        cells.add {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = if (isToday) colorBordeDiaActual else Color(0xFFCCCCCC),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = dayEventsConAlgunaInfo) {
                        onDayClick(thisDate, dayEvents.filter { festivo ->
                            festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }.isNotBlank()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$dayNum",
                        fontWeight = numeroDiaFontWeight,
                        color = numeroDiaColor,
                        fontSize = 22.sp
                    )
                    if (hasOtherEventsForPoint) {
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
    val horizontalSpacingBetweenMonths = 4.dp
    val verticalSpacingBetweenMonthRows = 4.dp

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        months.chunked(3).forEachIndexed { rowIndex, monthRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacingBetweenMonths)
            ) {
                monthRow.forEach { month ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onMonthSelected(month) },
                        contentAlignment = Alignment.Center
                    ) {
                        MiniMonthCalendar(
                            month = month,
                            today = today,
                            eventsByDate = eventsByDate,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (monthRow.size < 3) {
                    for (i in 0 until (3 - monthRow.size)) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
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
    val monthNameFontSize = 12.sp
    val dayHeadersFontSize = 8.sp
    val dayNumberFontSize = 9.sp
    val compactTextStyle = LocalTextStyle.current.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))

    Column(
        modifier = modifier
            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(4.dp))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            fontSize = monthNameFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = compactTextStyle.copy(lineHeight = monthNameFontSize * 0.95f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeekShort.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = dayHeadersFontSize, fontWeight = FontWeight.Medium, maxLines = 1, style = compactTextStyle.copy(lineHeight = dayHeadersFontSize * 0.95f))
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            val dayCellsData = remember(month, daysInMonth, firstDayOfWeekIndex) {
                List(totalCellsToDisplay) { cellIndex ->
                    val dayNumber = cellIndex - firstDayOfWeekIndex + 1
                    if (dayNumber in 1..daysInMonth) month.atDay(dayNumber) else null
                }
            }
            dayCellsData.chunked(7).forEach { weekDates ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekDates.forEach { date ->
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(0.dp), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                val isToday = date == today
                                val dayEvents = eventsByDate[date].orEmpty()
                                val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
                                val azulResaltadoHoy = Color(0xFF2196F3) // Color para el recuadro del día actual en minimapa
                                val baseTextColor = when {
                                    isHoliday -> Color.Red
                                    date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color.Red.copy(alpha = 0.7f)
                                    else -> Color.Black.copy(alpha = 0.9f)
                                }
                                // En el minimapa, "hoy" sigue resaltado en azul si no es festivo
                                val finalTextColor = if (isToday && !isHoliday) Color.Blue.copy(alpha = 0.9f) else baseTextColor
                                val currentFontWeight = if (isHoliday || isToday) FontWeight.Bold else FontWeight.Normal
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    if (isToday && !isHoliday) {
                                        Box(modifier = Modifier.size((dayNumberFontSize.value * 2.2f).dp).clip(RoundedCornerShape(3.dp)).background(azulResaltadoHoy.copy(alpha = 0.15f)))
                                    }
                                    Text("${date.dayOfMonth}", fontSize = dayNumberFontSize, fontWeight = currentFontWeight, color = finalTextColor, maxLines = 1, textAlign = TextAlign.Center, style = compactTextStyle.copy(lineHeight = dayNumberFontSize * 0.95f))
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

    val AzulMarinoCumpleanos = Color(0xFF0000ff)
    // val today = LocalDate.now() // No es necesario aquí si no afecta la lógica
    // val isDialogForToday = date == today // No es necesario aquí si no afecta la lógica

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = formattedDate, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            val eventsToDisplay = events.mapNotNull { festivo ->
                val displayDescription = if (!festivo.isAllDay && festivo.startTime != null) {
                    "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty { "(Sin título)" }}"
                } else {
                    festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }
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
                            val itemColor: Color
                            val itemFontWeight: FontWeight = FontWeight.Normal // En el diálogo, la fuente siempre normal

                            val esCumpleanos = festivo.description.contains("cumpleaños", ignoreCase = true) ||
                                    festivo.description.contains("aniversario", ignoreCase = true)

                            itemColor = when {
                                esCumpleanos -> AzulMarinoCumpleanos
                                festivo.isFromHolidaySource -> Color.Red
                                else -> Color.Black
                            }

                            val calendarInfo = availableCalendars.find { it.id == festivo.calendarId }
                            val eventColorInt = calendarInfo?.color

                            eventColorInt?.let { colorInt ->
                                Box(modifier = Modifier.size(10.dp).background(Color(colorInt), CircleShape).border(0.5.dp, Color.DarkGray.copy(alpha = 0.5f), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = displayDescription,
                                color = itemColor,
                                fontWeight = itemFontWeight,
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

