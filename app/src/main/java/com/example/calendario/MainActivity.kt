package com.example.calendario

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.CalendarContract
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import androidx.core.content.ContextCompat

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

// --- ENUMS ---
enum class CalendarViewMode { MONTHLY, YEARLY }

// --- DATA CLASSES ---
data class CalendarData(val id: Long, val displayName: String)
data class Festivo(
    val date: LocalDate,
    val description: String,
    val calendarId: Long,
    val isHolidayCalendar: Boolean = false
)
data class FestivoDto(val desc: String, val id: Long)

// --- CALENDARIO APP ---
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

// --- CALENDARIO SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen() {
    val context = LocalContext.current
    val azulFijo = Color(0xFF2196F3)

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentYear by remember { mutableStateOf(Year.now()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showCalendarsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    val today = LocalDate.now()

    var eventsByDate by remember { mutableStateOf(loadEventsFromPrefs(context)) }
    var selectedCalendars by remember { mutableStateOf(loadSelectedCalendars(context)) }
    var availableCalendars by remember { mutableStateOf(listOf<CalendarData>()) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadCalendars(context) { calendars ->
                availableCalendars = calendars
                showCalendarsDialog = true
            }
        } else {
            Toast.makeText(context, "Permiso de calendario denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(showCalendarsDialog) {
        if (showCalendarsDialog &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            loadCalendars(context) { calendars -> availableCalendars = calendars }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(azulFijo).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "Calendario",
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
                                // 1. Calendario de festivos
                                DropdownMenuItem(
                                    text = { Text("Calendario de festivos", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.READ_CALENDAR
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        ) {
                                            showCalendarsDialog = true
                                        } else requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                                    }
                                )
                                // 2. Ayuda
                                DropdownMenuItem(
                                    text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showHelpDialog = true
                                    }
                                )
                                // 3. Acerca de
                                DropdownMenuItem(
                                    text = { Text("Acerca de", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showAboutDialog = true
                                    }
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CONTROLES DE MES/AÑO ---
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
                        fontSize = 20.sp,
                        color = Color.Black
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

            Spacer(modifier = Modifier.height(16.dp))

            // --- CALENDARIOS ---
            if (viewMode == CalendarViewMode.MONTHLY) {
                MonthlyCalendar(currentMonth, today, eventsByDate)

                // --- LISTA DE FESTIVOS DEL MES ---
                val monthFestivos = eventsByDate
                    .filter { it.key.month == currentMonth.month && it.key.year == currentMonth.year }
                    .toSortedMap()

                if (monthFestivos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        monthFestivos.forEach { (date, festivos) ->
                            festivos.forEach { festivo ->
                                Row {
                                    Text("${date.dayOfMonth}", color = Color.Red, fontWeight = FontWeight.Bold)
                                    Text(": ${festivo.description}", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            } else {
                YearlyCalendar(currentYear, today, eventsByDate) { selectedMonth ->
                    currentMonth = selectedMonth
                    viewMode = CalendarViewMode.MONTHLY
                }
            }

            // --- DIALOGOS ---
            if (showCalendarsDialog) {
                FestivosDialog(
                    availableCalendars = availableCalendars,
                    selectedCalendars = selectedCalendars,
                    onSelectedChange = { newSet ->
                        selectedCalendars = newSet
                        saveSelectedCalendars(context, selectedCalendars)
                    },
                    onApply = {
                        readFestivosFromCalendars(context, selectedCalendars) { festivos ->
                            eventsByDate = festivos
                            saveEventsToPrefs(context, eventsByDate)
                            showCalendarsDialog = false
                        }
                    },
                    onDismiss = { showCalendarsDialog = false }
                )
            }

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text("Acerca de", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    text = {
                        Column {
                            Text("Calendario v1.01", fontSize = 16.sp)
                            Text("Android Studio/ChatGPT", fontSize = 16.sp)
                            Text("Onso/agosto 2025", fontSize = 16.sp)
                            Text("MA52a", fontSize = 16.sp)
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
                            Text("Calendario mensual y anual. Las flechas permiten navegar por meses o años.", fontSize = 16.sp)
                            Text("Pulsando sobre el nombre  del mes aparecerá el calendario anual y viceversa.", fontSize = 16.sp)
                            Text("Sólo se acepta el calendario de festivos de Google calendar.", fontSize = 16.sp)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
                )
            }
        }
    }
}

// --- FESTIVOS DIALOG ---
@Composable
fun FestivosDialog(
    availableCalendars: List<CalendarData>,
    selectedCalendars: Set<Long>,
    onSelectedChange: (Set<Long>) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    // Filtrar solo los calendarios que contengan "Festivo"
    val festivoCalendars = availableCalendars.filter { it.displayName.contains("Festivo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calendario de festivos", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 400.dp)
            ) {
                festivoCalendars.forEach { cal ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSet = selectedCalendars.toMutableSet()
                                if (newSet.contains(cal.id)) newSet.remove(cal.id) else newSet.add(cal.id)
                                onSelectedChange(newSet)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selectedCalendars.contains(cal.id),
                            onCheckedChange = { checked ->
                                val newSet = selectedCalendars.toMutableSet()
                                if (checked) newSet.add(cal.id) else newSet.remove(cal.id)
                                onSelectedChange(newSet)
                            }
                        )

                        Text(cal.displayName, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onApply) { Text("Aplicar", fontSize = 16.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", fontSize = 16.sp) } }
    )
}

// --- EVENTS STORAGE ---
fun saveEventsToPrefs(context: Context, eventsByDate: Map<LocalDate, List<Festivo>>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val mapString = eventsByDate.mapKeys { it.key.toString() }
        .mapValues { entry -> entry.value.map { FestivoDto(it.description, it.calendarId) } }
    prefs.edit().putString("events", gson.toJson(mapString)).apply()
}

fun loadEventsFromPrefs(context: Context): Map<LocalDate, List<Festivo>> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("events", null) ?: return emptyMap()
    val gson = Gson()
    val type = object : TypeToken<Map<String, List<FestivoDto>>>() {}.type
    val mapString: Map<String, List<FestivoDto>> = try { gson.fromJson(json, type) } catch (e: Exception) { e.printStackTrace(); return emptyMap() }

    return mapString.mapNotNull { (key, list) ->
        val date = try { LocalDate.parse(key) } catch (e: Exception) { null }
        if (date != null) date to list.map { Festivo(date, it.desc, it.id) } else null
    }.toMap()
}

// --- SELECTED CALENDARS ---
fun saveSelectedCalendars(context: Context, selectedCalendars: Set<Long>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("selectedCalendars", selectedCalendars.map { it.toString() }.toSet()).apply()
}

fun loadSelectedCalendars(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("selectedCalendars", emptySet())?.map { it.toLong() }?.toSet() ?: emptySet()
}

// --- LOAD CALENDARS ---
fun loadCalendars(context: Context, callback: (List<CalendarData>) -> Unit) {
    val resolver = context.contentResolver
    val uri = CalendarContract.Calendars.CONTENT_URI
    val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
    val list = mutableListOf<CalendarData>()
    val cursor = resolver.query(uri, projection, null, null, null)
    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getLong(0)
            val name = it.getString(1)
            list.add(CalendarData(id, name))
        }
    }
    callback(list)
}

// --- READ FESTIVOS ---
fun readFestivosFromCalendars(
    context: Context,
    selectedCalendars: Set<Long>,
    callback: (Map<LocalDate, List<Festivo>>) -> Unit
) {
    if (selectedCalendars.isEmpty()) {
        callback(emptyMap())
        return
    }

    val resolver = context.contentResolver
    val map = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val startMillis = LocalDate.now().minusYears(1).atStartOfDay().atZone(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli()
    val endMillis = LocalDate.now().plusYears(2).atStartOfDay().atZone(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli()
    val uri = CalendarContract.Events.CONTENT_URI
    val projection = arrayOf(CalendarContract.Events.CALENDAR_ID, CalendarContract.Events.DTSTART, CalendarContract.Events.TITLE)
    val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${selectedCalendars.joinToString(",")})"
    val cursor = resolver.query(uri, projection, selection, null, null)

    cursor?.use { c ->
        while (c.moveToNext()) {
            val calId = c.getLong(0)
            val dtStart = c.getLong(1)
            val title = c.getString(2) ?: ""
            val date = Instant.ofEpochMilli(dtStart).atZone(TimeZone.getDefault().toZoneId()).toLocalDate()
            val list = map.getOrPut(date) { mutableListOf() }
            list.add(Festivo(date, title, calId))
        }
    }

    callback(map)
}

// --- CALENDARS UI ---
// MonthlyCalendar, YearlyCalendar y MiniMonthCalendar siguen igual,
// pero eliminando referencias a `calendarColors`.

// --- MONTHLY CALENDAR ---
@Composable
fun MonthlyCalendar(
    currentMonth: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    val cells = mutableListOf<@Composable () -> Unit>()

    // Encabezado de días
    daysOfWeek.forEach { day ->
        cells.add {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFadd1fa)) // <- Aquí cambias el fondo
                    .border(1.dp, Color(0xFFCCCCCC)),
                contentAlignment = Alignment.Center
            ) {
                Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Celdas vacías antes del primer día
    for (i in 0 until firstDayOfWeek) cells.add { Spacer(modifier = Modifier) }

    // Días del mes
    for (dayNum in 1..daysInMonth) {
        val thisDate = currentMonth.atDay(dayNum)
        val isToday = thisDate == today
        val isSunday = thisDate.dayOfWeek.value % 7 == 0
        val dayEvents = eventsByDate[thisDate].orEmpty()
        val azulCabecera = Color(0xFF2196F3)
        cells.add {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .border(
                        width = if (isToday) 2.dp else 1.dp,
                        color = if (isToday) azulCabecera else Color(0xFFCCCCCC),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$dayNum",
                        fontWeight = if (dayEvents.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                        color = if (dayEvents.isNotEmpty()) Color.Red else if (isSunday) Color.Red else Color.Black,
                        fontSize = 22.sp
                    )
                }
            }
        }
    }

    // Completar última fila
    val remainder = cells.size % 7
    if (remainder != 0) for (i in 0 until (7 - remainder)) cells.add { Spacer(modifier = Modifier) }

    // --- MARCO CON FONDO GRIS CLARO ---
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFf1f7fe), RoundedCornerShape(8.dp)) // fondo más claro
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)) // borde más claro
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
                        ) { cells[rowIndex * 7 + colIndex].invoke() }
                    }
                }
            }
        }
    }
}

// --- YEARLY CALENDAR ---
@Composable
fun YearlyCalendar(
    currentYear: Year,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>,
    onMonthSelected: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(currentYear.value, it) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (col in 0 until 2) {
                    val index = row * 2 + col
                    if (index < months.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
                                .clickable { onMonthSelected(months[index]) },
                            contentAlignment = Alignment.Center
                        ) {
                            MiniMonthCalendar(months[index], today, eventsByDate)
                        }
                    } else Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// --- MINI MONTH CALENDAR ---
@Composable
fun MiniMonthCalendar(
    month: YearMonth,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<Festivo>>
) {
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val adjustedFirstDay = if (firstDayOfWeek == 0) 6 else firstDayOfWeek - 1
    val daysInMonth = month.lengthOfMonth()
    val totalCells = ((adjustedFirstDay + daysInMonth + 6) / 7) * 7

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        // Nombre del mes
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        /// Encabezado días de la semana con fondo gris compacto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFeaeaea)) // gris claro
                .padding(vertical = 0.0.dp), // menos alto
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 9.sp, fontWeight = FontWeight.Bold) // fuente un poco más pequeña
                }
            }
        }



        // Celdas del mes
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (weekStart in 0 until totalCells step 7) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    for (dayIndex in 0 until 7) {
                        val cellIndex = weekStart + dayIndex
                        val dateNumber = cellIndex - adjustedFirstDay + 1
                        if (dateNumber in 1..daysInMonth) {
                            val thisDate = month.atDay(dateNumber)
                            val dayEvents = eventsByDate[thisDate].orEmpty()
                            val isToday = thisDate == today
                            val azulCabecera = Color(0xFF2196F3)
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Cuadrado azul (solo si es hoy)
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .offset(y = 2.dp) // <- desplaza el borde azul hacia abajo
                                            .border(
                                                width = 2.dp,
                                                color = azulCabecera,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }

                                // Número del día y pequeños indicadores
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$dateNumber",
                                        fontSize = 12.sp,
                                        fontWeight = if (dayEvents.any { it.description.isNotEmpty() }) FontWeight.Bold else FontWeight.Normal,
                                        color = if (dayEvents.any { it.description.isNotEmpty() }) Color.Red // festivos en rojo
                                        else if (thisDate.dayOfWeek.value % 7 == 0) Color.Red
                                        else Color.Black
                                    )
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        dayEvents.forEach { event ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(Color.Red, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }

                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
