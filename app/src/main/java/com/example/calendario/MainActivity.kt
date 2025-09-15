package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.getValue // No es necesario el import explícito con by
// import androidx.compose.runtime.setValue // No es necesario el import explícito con by
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
// import java.time.LocalTime // No usado directamente aquí
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit // Para SharedPreferences.edit
// import androidx.compose.ui.graphics.toArgb // No es necesario aquí si se usa Color(Int)
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

// Importaciones de tu proyecto
import com.example.calendario.CalendarInfo
import com.example.calendario.Festivo
import com.example.calendario.loadEventsFromPrefs // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.loadSelectedCalendarIds // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.saveSelectedCalendarIds // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.saveEventsToPrefs // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.loadAvailableCalendarsSuspend // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.readFestivosFromCalendarsSuspend // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.processEventsForDisplay // Asumo que está en CalendarDataUtils.kt
import com.example.calendario.WidgetConstants // Importante para las constantes del Widget


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

        eventsByDateState = loadEventsFromPrefs(this) // Carga inicial desde SharedPreferences de la app
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
                    saveEventsToPrefs(this, newEvents) // Guarda en SharedPreferences de la app
                    saveSelectedCalendarIds(this, newSelectedIds)
                    notifyCalendarWidgetsDataChangedMainActivity(this) // Notifica al widget que los datos de eventos cambiaron
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
    }

    override fun onResume() {
        super.onResume()
        hasCalendarPermissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (hasCalendarPermissionState) {
            refreshDataFromCalendarProviderAndUpdateStates()
        } else {
            // Limpia los datos si no hay permiso
            eventsByDateState = emptyMap()
            availableCalendarsState = emptyList()
            // No es necesario notificar al widget aquí ya que no hay datos para mostrar sin permiso
        }
    }

    private fun refreshDataFromCalendarProviderAndUpdateStates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            hasCalendarPermissionState = false
            eventsByDateState = emptyMap()
            availableCalendarsState = emptyList()
            saveEventsToPrefs(this, emptyMap())
            notifyCalendarWidgetsDataChangedMainActivity(this) // Notifica para limpiar el widget
            return
        }
        if (!hasCalendarPermissionState) hasCalendarPermissionState = true // Asegura que el estado de permiso esté actualizado

        lifecycleScope.launch {
            try {
                val currentSelectedIds = loadSelectedCalendarIds(this@MainActivity)
                val freshAvailableCalendars = loadAvailableCalendarsSuspend(this@MainActivity)
                availableCalendarsState = freshAvailableCalendars

                val validSelectedIds = currentSelectedIds.filter { sid -> freshAvailableCalendars.any { cal -> cal.id == sid } }.toSet()
                if (validSelectedIds != selectedCalendarIdsState) {
                    selectedCalendarIdsState = validSelectedIds
                    saveSelectedCalendarIds(this@MainActivity, validSelectedIds)
                }

                val freshEventsMap = if (selectedCalendarIdsState.isNotEmpty() || freshAvailableCalendars.isNotEmpty()) {
                    readFestivosFromCalendarsSuspend(this@MainActivity, selectedCalendarIdsState, freshAvailableCalendars)
                } else {
                    emptyMap()
                }
                eventsByDateState = freshEventsMap
                saveEventsToPrefs(this@MainActivity, freshEventsMap) // Guarda los nuevos eventos
                notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity) // Notifica al widget
            } catch (e: Exception) {
                Log.e("MainActivity", "Error refrescando datos del calendario", e)
                Toast.makeText(this@MainActivity, "Error al actualizar datos.", Toast.LENGTH_SHORT).show()
                // Considera limpiar estados si hay un error crítico
                eventsByDateState = emptyMap()
                availableCalendarsState = emptyList()
                saveEventsToPrefs(this@MainActivity, emptyMap())
                notifyCalendarWidgetsDataChangedMainActivity(this@MainActivity)
            }
        }
    }
}

// Notifica a los widgets que la configuración (como colores, tamaño de fuente, número de eventos) ha cambiado.
fun notifyCalendarWidgetsConfigurationChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java) // Asegúrate que este sea tu AppWidgetProvider
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
    if (appWidgetIds.isNotEmpty()) {
        // Esta notificación es clave para que el widget actualice su RemoteViewsFactory
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list) // Asegúrate que R.id.widget_event_list es el ID de tu ListView/GridView en el widget
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar widgets por CAMBIO DE CONFIGURACIÓN.")
    }
}

// Notifica a los widgets que los datos de los eventos (la lista de eventos en sí) han cambiado.
fun notifyCalendarWidgetsDataChangedMainActivity(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java) // Asegúrate que este sea tu AppWidgetProvider
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
        Log.d("MainActivityNotifier", "Notificación enviada para actualizar datos de EVENTOS en widgets.")
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

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        onPermissionUpdated(isGranted)
    }

    LaunchedEffect(hasCalendarPermissionExternal) {
        if (hasCalendarPermissionExternal) onRefreshRequest()
    }

    val azulFijo = Color(0xFF2196F3)
    val colorDeFondoPantalla = Color(0xFFfafafa)
    val colorTextoNormalSobreFondo = Color.Black
    val colorTextoSecundarioSobreFondo = Color.DarkGray
    val colorResaltadoFestivos = Color.Red
    val colorFondoBotonesNavegacion = Color(0xFFffbb77)
    val colorContenidoBotonesNavegacion = Color.Black
    val AzulMarinoCumpleanos = Color(0xFF0000ff)
    val colorGrisClaroParaFondoTitulo = Color(0xFFeaeaea)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(azulFijo).statusBarsPadding()) {
                TopAppBar(
                    title = { Text("Calendario Visual", fontSize = 20.sp, color = Color.White, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menú", tint = Color.White) }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, shape = RoundedCornerShape(12.dp), modifier = Modifier.background(Color.White)) {
                                DropdownMenuItem(text = { Text("Calendarios", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }, onClick = { menuExpanded = false; if (hasCalendarPermissionExternal) showSelectCalendarsDialog = true else requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR) })
                                DropdownMenuItem(text = { Text("Widget", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }, onClick = { menuExpanded = false; showWidgetConfigDialog = true })
                                DropdownMenuItem(text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }, onClick = { menuExpanded = false; showHelpDialog = true })
                                DropdownMenuItem(text = { Text("Acerca de", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }, onClick = { menuExpanded = false; showAboutDialog = true })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = azulFijo, scrolledContainerColor = azulFijo, titleContentColor = Color.White, actionIconContentColor = Color.White)
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(colorDeFondoPantalla).padding(paddingValues).padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = { if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.minusMonths(1) else currentYear = currentYear.minusYears(1) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorFondoBotonesNavegacion, contentColor = colorContenidoBotonesNavegacion)) { Icon(Icons.Filled.ArrowBack, "Anterior") }
                Button(
                    onClick = { if (viewMode == CalendarViewMode.MONTHLY) { currentYear = Year.of(currentMonth.year); viewMode = CalendarViewMode.YEARLY } else { currentMonth = YearMonth.now(); viewMode = CalendarViewMode.MONTHLY } },
                    colors = ButtonDefaults.buttonColors(containerColor = colorFondoBotonesNavegacion, contentColor = colorContenidoBotonesNavegacion), shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) { Text(if (viewMode == CalendarViewMode.MONTHLY) "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}" else "${currentYear.value}", fontSize = 20.sp) }
                FilledIconButton(onClick = { if (viewMode == CalendarViewMode.MONTHLY) currentMonth = currentMonth.plusMonths(1) else currentYear = currentYear.plusYears(1) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorFondoBotonesNavegacion, contentColor = colorContenidoBotonesNavegacion)) { Icon(Icons.Filled.ArrowForward, "Siguiente") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (viewMode == CalendarViewMode.MONTHLY) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MonthlyCalendar(currentMonth, today, eventsByDateExternal, azulFijo) { date, events -> selectedDateForDialog = date; eventsForDialog = events; showDayEventsDialog = true }
                        Spacer(modifier = Modifier.height(12.dp))
                        val finalEventsToList = processEventsForDisplay(eventsByDateExternal, currentMonth, today)
                        val isCurrentMonthView = currentMonth.year == today.year && currentMonth.month == today.month
                        val listTitleText = if (isCurrentMonthView) "Eventos pendientes de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}" else "Eventos de ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }}"
                        Text(listTitleText, fontSize = 18.sp, color = azulFijo, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(colorGrisClaroParaFondoTitulo, RoundedCornerShape(8.dp)).padding(vertical = 6.dp, horizontal = 12.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (finalEventsToList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text(if (isCurrentMonthView) "No hay eventos pendientes para este mes." else "No hay eventos para este mes.", fontSize = 16.sp, color = colorTextoSecundarioSobreFondo) }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                                finalEventsToList.forEach { (date, festivos) ->
                                    val isTodayEvents = isCurrentMonthView && date == today
                                    festivos.forEach { festivo ->
                                        val esCumpleanos = festivo.description.contains("cumpleaños", true) || festivo.description.contains("aniversario", true)
                                        val (colorNum, colorDesc) = when {
                                            esCumpleanos -> AzulMarinoCumpleanos to AzulMarinoCumpleanos
                                            festivo.isFromHolidaySource -> colorResaltadoFestivos to colorResaltadoFestivos
                                            else -> colorTextoNormalSobreFondo to colorTextoNormalSobreFondo
                                        }
                                        val fontWeight = if (isTodayEvents) FontWeight.Bold else FontWeight.Normal
                                        val displayDesc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty { "(Sin título)" }}"
                                        else festivo.description.ifEmpty { if (festivo.isAllDay) "(Evento todo el día)" else "" }
                                        if (displayDesc.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text(String.format("%02d:", date.dayOfMonth), color = colorNum, fontWeight = fontWeight, fontSize = 16.sp)
                                                Text(displayDesc, color = colorDesc, fontWeight = fontWeight, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else { YearlyCalendar(currentYear, today, eventsByDateExternal) { selectedMonth -> currentMonth = selectedMonth; viewMode = CalendarViewMode.MONTHLY } }
            }

            if (showSelectCalendarsDialog) { SelectCalendarsDialog({ showSelectCalendarsDialog = false }) { ids -> scope.launch { try { val avail = if (availableCalendarsExternal.isNotEmpty()) availableCalendarsExternal else loadAvailableCalendarsSuspend(context); val fest = readFestivosFromCalendarsSuspend(context, ids, avail); onCalendarDataUpdated(fest, avail, ids) } catch (e: Exception) { Log.e("CalendarioScreen", "Error aplicando selección: ${e.localizedMessage}", e); Toast.makeText(context, "Error.", Toast.LENGTH_SHORT).show() } finally { showSelectCalendarsDialog = false } } } }
            if (showAboutDialog) { AlertDialog({showAboutDialog = false}, title={Text("Acerca de", fontWeight=FontWeight.Bold, fontSize=20.sp)}, text={Column{Text("Calendario Visual V1.31",fontSize=16.sp); Text("Asistente IA / Android Studio",fontSize=16.sp); Text("Onso/agosto 2025",fontSize=16.sp)}}, confirmButton={TextButton({showAboutDialog=false}){Text("Cerrar",fontSize=16.sp)}}) }
            if (showHelpDialog) { AlertDialog({showHelpDialog = false}, title={Text("Ayuda", fontWeight=FontWeight.Bold, fontSize=20.sp)}, text={Column{Text("Botón mes alterna mensual/anual.",fontSize=16.sp,modifier=Modifier.padding(bottom=4.dp)); Text("Flechas navegan mes/año.",fontSize=16.sp,modifier=Modifier.padding(bottom=4.dp)); Text("Pulsar día con eventos muestra lista.",fontSize=16.sp)}}, confirmButton={TextButton({showHelpDialog=false}){Text("Cerrar",fontSize=16.sp)}}) }
            if (showDayEventsDialog && selectedDateForDialog != null) { DayEventsDialog(selectedDateForDialog!!, eventsForDialog, availableCalendarsExternal) { showDayEventsDialog=false; selectedDateForDialog=null; eventsForDialog=emptyList() } }
            if (showWidgetConfigDialog) { WidgetConfigScreen { showWidgetConfigDialog = false } }
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

    var eventColor by remember { mutableStateOf(Color(prefs.getInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_EVENT_COLOR_ARGB))) }
    var todayEventColor by remember { mutableStateOf(Color(prefs.getInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, WidgetConstants.DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB))) }
    var showEventColorPalette by remember { mutableStateOf(false) }
    var showTodayEventColorPalette by remember { mutableStateOf(false) }

    // Paletas base fijas con TUS colores (los que me diste)
    val baseEventColors = remember {
        listOf(
            Color(0xFFFFFFFF), Color(0xFFF4F4F4), Color(0xFFD4D4D4), Color(0xFFB4B4B4),
            Color(0xFF949494), Color(0xFF5F5F5F), Color(0xFF000000)
        )
    }
    val baseTodayEventColors = remember {
        listOf(
            Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF80FF80), Color(0xFF00FFFF),
            Color(0xFF952BFF), Color(0xFFFFFFFF), Color(0xFF000000)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Configuración del Widget", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Número de eventos: ${eventCountSliderValue.roundToInt()}", fontSize = 16.sp)
                Slider(value = eventCountSliderValue, onValueChange = { eventCountSliderValue = it }, valueRange = 1f..12f, steps = 10, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable { useLargeFontSwitchState = !useLargeFontSwitchState }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Letra grande", fontSize = 16.sp)
                    Switch(checked = useLargeFontSwitchState, onCheckedChange = { useLargeFontSwitchState = it })
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ColorPickerRow("Color eventos", eventColor) { showEventColorPalette = true }
                Spacer(Modifier.height(12.dp))
                ColorPickerRow("Color eventos de hoy", todayEventColor) { showTodayEventColorPalette = true }
            }
        },
        confirmButton = {
            Button(onClick = {
                prefs.edit {
                    putInt(WidgetConstants.KEY_EVENT_COUNT, eventCountSliderValue.roundToInt())
                    putBoolean(WidgetConstants.KEY_FONT_SIZE_LARGE, useLargeFontSwitchState)
                    putInt(WidgetConstants.KEY_WIDGET_EVENT_COLOR, eventColor.toArgb()) // eventColor es Color de Compose, toArgb() lo convierte a Int ARGB
                    putInt(WidgetConstants.KEY_WIDGET_TODAY_EVENT_COLOR, todayEventColor.toArgb())
                    apply()
                }
                notifyCalendarWidgetsConfigurationChangedMainActivity(context) // Notifica al widget
                onDismissRequest()
            }) { Text("Guardar", fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
    )

    if (showEventColorPalette) {
        ColorPaletteDialog(
            title = "Color para eventos",
            colors = baseEventColors, // Usa la paleta base directamente
            currentlySelectedColor = eventColor,
            onColorSelected = { selectedColor -> eventColor = selectedColor; showEventColorPalette = false },
            onDismiss = { showEventColorPalette = false }
        )
    }
    if (showTodayEventColorPalette) {
        ColorPaletteDialog(
            title = "Color para eventos de hoy",
            colors = baseTodayEventColors, // Usa la paleta base directamente
            currentlySelectedColor = todayEventColor,
            onColorSelected = { selectedColor -> todayEventColor = selectedColor; showTodayEventColorPalette = false },
            onDismiss = { showTodayEventColorPalette = false }
        )
    }
}

@Composable
fun ColorPickerRow(label: String, currentColor: Color, onColorBoxClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(32.dp).background(currentColor, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.7f), CircleShape).clickable(onClick = onColorBoxClick))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteDialog(title: String, colors: List<Color>, currentlySelectedColor: Color, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val colorBordeResaltado = Color.Red
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                items(colors) { colorInPalette ->
                    val isSelected = colorInPalette == currentlySelectedColor
                    Box(modifier = Modifier.size(40.dp).background(colorInPalette, CircleShape)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) colorBordeResaltado else Color.Gray.copy(alpha = 0.5f), CircleShape)
                        .clickable { onColorSelected(colorInPalette) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCalendarsDialog(onDismissRequest: () -> Unit, onApplySelection: (selectedIds: Set<Long>) -> Unit) {
    val context = LocalContext.current
    var localAvailableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var currentSelectedIdsInDialog by remember { mutableStateOf(emptySet<Long>()) }
    LaunchedEffect(Unit) {
        try {
            val calendars = loadAvailableCalendarsSuspend(context)
            localAvailableCalendars = calendars
            currentSelectedIdsInDialog = loadSelectedCalendarIds(context).filter { id -> calendars.any { cal -> cal.id == id } }.toSet()
        } catch (e: Exception) { Log.e("SelectCalendarsDialog", "Error cargando calendarios: ${e.localizedMessage}", e); localAvailableCalendars = emptyList() }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Seleccionar Calendarios", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            if (localAvailableCalendars.isEmpty()) { Text("No se encontraron calendarios.", fontSize = 16.sp) }
            else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth()) {
                    items(localAvailableCalendars, key = { it.id }) { calendar ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { val newSet = currentSelectedIdsInDialog.toMutableSet(); if (newSet.contains(calendar.id)) newSet.remove(calendar.id) else newSet.add(calendar.id); currentSelectedIdsInDialog = newSet }.padding(vertical = 6.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = currentSelectedIdsInDialog.contains(calendar.id), onCheckedChange = { isChecked -> val newSet = currentSelectedIdsInDialog.toMutableSet(); if (isChecked) newSet.add(calendar.id) else newSet.remove(calendar.id); currentSelectedIdsInDialog = newSet })
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Text(calendar.displayName, fontWeight = FontWeight.Medium, fontSize = 16.sp); Text(calendar.accountName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onApplySelection(currentSelectedIdsInDialog) }, enabled = localAvailableCalendars.isNotEmpty()) { Text("Aplicar", fontSize = 16.sp) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) } }
    )
}

@Composable
fun MonthlyCalendar(currentMonth: YearMonth, today: LocalDate, eventsByDate: Map<LocalDate, List<Festivo>>, puntoEventoColor: Color, onDayClick: (date: LocalDate, events: List<Festivo>) -> Unit) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val cells = mutableListOf<@Composable () -> Unit>()
    val colorBordeDiaActual = MaterialTheme.colorScheme.primary

    daysOfWeek.forEach { day -> cells.add { Box(Modifier.fillMaxSize().background(Color(0xFFadd1fa)).border(1.dp, Color(0xFFCCCCCC)), Alignment.Center) { Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold) } } }
    repeat(firstDayOfWeek) { cells.add { Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFCCCCCC))) } }

    (1..daysInMonth).forEach { dayNum ->
        val thisDate = currentMonth.atDay(dayNum)
        val isToday = thisDate == today
        val isSunday = thisDate.dayOfWeek == java.time.DayOfWeek.SUNDAY
        val dayEvents = eventsByDate[thisDate].orEmpty()
        val dayEventsConAlgunaInfo = dayEvents.any { it.description.ifEmpty { if (it.isAllDay) "(Todo el día)" else "" }.isNotBlank() }
        val isHoliday = dayEvents.any { it.isFromHolidaySource && it.description.isNotBlank() }
        val hasOtherEventsPoint = dayEvents.any { !it.isFromHolidaySource && (it.startTime != null && !it.isAllDay || it.description.isNotBlank()) }
        val fontWeightNum = if (isHoliday) FontWeight.Bold else FontWeight.Normal
        val colorNum = when { isHoliday -> Color.Red; isSunday -> Color.Red.copy(alpha = 0.7f); else -> Color.Black }

        cells.add {
            Box(Modifier.fillMaxSize().background(Color.White).border(if (isToday) 2.dp else 1.dp, if (isToday) colorBordeDiaActual else Color(0xFFCCCCCC), RoundedCornerShape(4.dp)).clickable(enabled = dayEventsConAlgunaInfo) { onDayClick(thisDate, dayEvents.filter { it.description.ifEmpty { if (it.isAllDay) "(Todo el día)" else "" }.isNotBlank() }) }) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("$dayNum", fontWeight = fontWeightNum, color = colorNum, fontSize = 22.sp)
                    if (hasOtherEventsPoint) { Spacer(Modifier.height(2.dp)); Box(Modifier.size(6.dp).background(puntoEventoColor, CircleShape)) }
                    else { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
    repeat((7 - cells.size % 7) % 7) { cells.add { Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFCCCCCC))) } }

    Box(Modifier.fillMaxWidth().background(Color(0xFFf1f7fe), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)).padding(4.dp)) {
        Column { cells.chunked(7).forEach { weekCells -> Row(Modifier.fillMaxWidth()) { weekCells.forEach { cell -> Box(Modifier.weight(1f).aspectRatio(1f).padding(1.dp), Alignment.Center) { cell() } } } } }
    }
}

@Composable
fun YearlyCalendar(currentYear: Year, today: LocalDate, eventsByDate: Map<LocalDate, List<Festivo>>, onMonthSelected: (YearMonth) -> Unit) {
    val months = (1..12).map { YearMonth.of(currentYear.value, it) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        months.chunked(3).forEachIndexed { rowIndex, monthRow ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                monthRow.forEach { month -> Box(Modifier.weight(1f).aspectRatio(1f).clickable { onMonthSelected(month) }, Alignment.Center) { MiniMonthCalendar(month, today, eventsByDate, Modifier.fillMaxSize()) } }
                repeat(3 - monthRow.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
            }
            if (rowIndex < months.chunked(3).size - 1) Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun MiniMonthCalendar(month: YearMonth, today: LocalDate, eventsByDate: Map<LocalDate, List<Festivo>>, modifier: Modifier = Modifier) {
    val daysOfWeekShort = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val compactTextStyle = LocalTextStyle.current.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    val monthNameFontSize = 12.sp; val dayHeadersFontSize = 8.sp; val dayNumberFontSize = 9.sp

    Column(modifier.background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(4.dp)).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar(Char::titlecase), fontSize = monthNameFontSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = compactTextStyle.copy(lineHeight = monthNameFontSize * 0.95f), modifier = Modifier.padding(bottom = 4.dp))
        Row(Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(vertical = 3.dp), Arrangement.SpaceAround) { daysOfWeekShort.forEach { Box(Modifier.weight(1f), Alignment.Center) { Text(it, fontSize = dayHeadersFontSize, fontWeight = FontWeight.Medium, maxLines = 1, style = compactTextStyle.copy(lineHeight = dayHeadersFontSize * 0.95f)) } } }
        Column(Modifier.weight(1f)) {
            val dayCellsData = remember(month) { List(6 * 7) { val day = it - firstDayOfWeekIndex + 1; if (day in 1..daysInMonth) month.atDay(day) else null } }
            dayCellsData.chunked(7).forEach { weekDates ->
                Row(Modifier.fillMaxWidth().weight(1f), Arrangement.SpaceAround, Alignment.CenterVertically) {
                    weekDates.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
                            if (date != null) {
                                val isToday = date == today
                                val isHoliday = eventsByDate[date]?.any { it.isFromHolidaySource && it.description.isNotBlank() } == true
                                val textColor = when { isToday && !isHoliday -> Color.Blue.copy(0.9f); isHoliday -> Color.Red; date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color.Red.copy(0.7f); else -> Color.Black.copy(0.9f) }
                                val fontWeight = if (isHoliday || isToday) FontWeight.Bold else FontWeight.Normal
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    if (isToday && !isHoliday) Box(Modifier.size((dayNumberFontSize.value * 2.2f).dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2196F3).copy(alpha = 0.15f)))
                                    Text("${date.dayOfMonth}", fontSize = dayNumberFontSize, fontWeight = fontWeight, color = textColor, maxLines = 1, textAlign = TextAlign.Center, style = compactTextStyle.copy(lineHeight = dayNumberFontSize * 0.95f))
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
fun DayEventsDialog(date: LocalDate, events: List<Festivo>, availableCalendars: List<CalendarInfo>, onDismissRequest: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault()) }
    val formattedDate = remember(date) { date.format(formatter).replaceFirstChar(Char::titlecase) }
    val azulMarinoCumple = Color(0xFF0000ff)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(formattedDate, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            val eventsToDisplay = events.mapNotNull { festivo ->
                val desc = if (!festivo.isAllDay && festivo.startTime != null) "${festivo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${festivo.description.ifEmpty{"(Sin título)"}}"
                else festivo.description.ifEmpty { if (festivo.isAllDay) "(Todo el día)" else "" }
                if (desc.isNotBlank()) festivo to desc else null
            }
            if (eventsToDisplay.isEmpty()) { Text("No hay eventos con detalle.", fontSize = 16.sp) }
            else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) { // Altura máxima para el LazyColumn
                    items(eventsToDisplay, key = { (f,_) -> f.calendarId.toString()+f.description+f.startTime.toString()+f.date.toString() }) { (festivo, displayDesc) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val itemColor = when { // Determina el color del texto del evento
                                festivo.description.contains("cumpleaños",true) || festivo.description.contains("aniversario",true) -> azulMarinoCumple
                                festivo.isFromHolidaySource -> Color.Red
                                else -> Color.Black // Color por defecto para otros eventos
                            }
                            // Muestra el color del calendario si está disponible
                            availableCalendars.find { it.id == festivo.calendarId }?.color?.let { colorInt ->
                                Box(Modifier.size(10.dp).background(Color(colorInt), CircleShape).border(0.5.dp, Color.DarkGray.copy(0.5f), CircleShape))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(displayDesc, color = itemColor, fontSize = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("Cerrar", fontSize = 16.sp) } }
    )
}

// Aquí termina MainActivity.kt
