package com.example.calendario

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager // NUEVO: Para PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log // NUEVO: Para Logging
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId // NUEVO: Para ZoneId
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
    var showSelectCalendarsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    val today = LocalDate.now()

    var eventsByDate by remember { mutableStateOf(loadEventsFromPrefs(context)) }
    var selectedCalendarIds by remember { mutableStateOf(loadSelectedCalendarIds(context)) }
    var availableCalendars by remember { mutableStateOf(listOf<CalendarInfo>()) }

    var hasCalendarPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            loadAvailableCalendars(context) { calendars -> // Asegúrate que es CAL02_loadAvailableCalendars si lo renombras
                availableCalendars = calendars
                showSelectCalendarsDialog = true
            }
            readFestivosFromCalendars(context, selectedCalendarIds) { festivos ->
                eventsByDate = festivos
                saveEventsToPrefs(context, festivos)
            }
        } else {
            Toast.makeText(context, "Permiso de calendario denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            Log.d("CalendarioScreen", "Permiso concedido. Cargando calendarios y eventos.")
            loadAvailableCalendars(context) { calendars -> // Asegúrate que es CAL02_loadAvailableCalendars si lo renombras
                availableCalendars = calendars
                val validSelectedIds = selectedCalendarIds.filter { selectedId -> calendars.any { it.id == selectedId } }.toSet()
                if (validSelectedIds != selectedCalendarIds) {
                    selectedCalendarIds = validSelectedIds
                    saveSelectedCalendarIds(context, validSelectedIds)
                }
            }
            readFestivosFromCalendars(context, selectedCalendarIds) { festivos ->
                eventsByDate = festivos
            }
        } else {
            Log.d("CalendarioScreen", "Permiso NO concedido.")
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(azulFijo).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Text(
                            "Calendario (CAL02 Debug)", // Identificador visual opcional
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
                                            loadAvailableCalendars(context) { calendars -> // Asegúrate que es CAL02_loadAvailableCalendars
                                                availableCalendars = calendars
                                                showSelectCalendarsDialog = true
                                            }
                                        } else {
                                            requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ayuda", fontSize = 18.sp, modifier = Modifier.padding(8.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showHelpDialog = true
                                    }
                                )
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

            if (viewMode == CalendarViewMode.MONTHLY) {
                MonthlyCalendar(currentMonth, today, eventsByDate)
                val monthFestivos = eventsByDate
                    .filter { it.key.month == currentMonth.month && it.key.year == currentMonth.year }
                    .toSortedMap()
                if (monthFestivos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()).heightIn(max=150.dp)) {
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

            if (showSelectCalendarsDialog) {
                SelectCalendarsDialog(
                    availableCalendars = availableCalendars,
                    initiallySelectedCalendarIds = selectedCalendarIds,
                    onDismissRequest = { showSelectCalendarsDialog = false },
                    onApplySelection = { newSelectedIds ->
                        selectedCalendarIds = newSelectedIds
                        saveSelectedCalendarIds(context, newSelectedIds)
                        readFestivosFromCalendars(context, newSelectedIds) { festivos ->
                            eventsByDate = festivos
                            saveEventsToPrefs(context, eventsByDate)
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
                            Text("Calendario vCAL02-Debug", fontSize = 16.sp)
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
                            Text("Calendario mensual y anual. Las flechas permiten navegar.", fontSize = 16.sp)
                            Text("Pulsando sobre el mes/año cambia la vista.", fontSize = 16.sp)
                            Text("Desde el menú (⋮) puedes seleccionar calendarios.", fontSize = 16.sp)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cerrar", fontSize = 16.sp) } }
                )
            }
        }
    }
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
fun saveSelectedCalendarIds(context: Context, selectedIds: Set<Long>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("selected_calendar_ids", selectedIds.map { it.toString() }.toSet()).apply()
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("selected_calendar_ids", emptySet())
        ?.mapNotNull { try { it.toLong() } catch (e: NumberFormatException) { null } }
        ?.toSet() ?: emptySet()
}


// --- LOAD AVAILABLE CALENDARS (VERSIÓN CAL02 CON LOGGING DETALLADO) ---
fun loadAvailableCalendars(context: Context, callback: (List<CalendarInfo>) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        callback(emptyList())
        Log.w("CalendarAccess", "READ_CALENDAR permission not granted when trying to load calendars.")
        return
    }

    val calendarsList = mutableListOf<CalendarInfo>()
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.ACCOUNT_TYPE,
        CalendarContract.Calendars.OWNER_ACCOUNT,      // Para depuración
        CalendarContract.Calendars.SYNC_EVENTS,       // Para depuración
        CalendarContract.Calendars.VISIBLE            // Para depuración
    )

    val cursor: Cursor? = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        null, // No selection, get all
        null, // No selection args
        "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
    )

    Log.d("ALL_CALENDARS_DEBUG", "Querying available calendars...")
    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val displayNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val accountNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
        val colorColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
        val accountTypeColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
        val ownerAccountColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
        val syncEventsColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.SYNC_EVENTS)
        val visibleColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)

        Log.d("ALL_CALENDARS_DEBUG", "Cursor has ${it.count} rows.")
        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val displayName = it.getString(displayNameColumn) ?: "Calendario sin nombre"
            val accountName = it.getString(accountNameColumn) ?: "Cuenta desconocida"
            val colorInt = try { it.getInt(colorColumn) } catch (e: Exception) { null }
            val accountType = it.getString(accountTypeColumn) ?: "Tipo desconocido"
            val ownerAccount = it.getString(ownerAccountColumn) ?: "Owner desconocido"
            val syncEvents = it.getInt(syncEventsColumn) // 1 para true, 0 para false
            val visible = it.getInt(visibleColumn) // 1 para true, 0 para false

            // ---- INICIO DE LOG DETALLADO ----
            Log.i("ALL_CALENDARS_DEBUG", "------------------------------------")
            Log.i("ALL_CALENDARS_DEBUG", "ID             : $id")
            Log.i("ALL_CALENDARS_DEBUG", "DisplayName    : $displayName")
            Log.i("ALL_CALENDARS_DEBUG", "AccountName    : $accountName")
            Log.i("ALL_CALENDARS_DEBUG", "AccountType    : $accountType")
            Log.i("ALL_CALENDARS_DEBUG", "OwnerAccount   : $ownerAccount")
            Log.i("ALL_CALENDARS_DEBUG", "Color          : $colorInt (Hex: ${colorInt?.let { String.format("#%08X", it) }})")
            Log.i("ALL_CALENDARS_DEBUG", "SyncEvents     : $syncEvents (1=true, 0=false)")
            Log.i("ALL_CALENDARS_DEBUG", "Visible        : $visible (1=true, 0=false)")
            Log.i("ALL_CALENDARS_DEBUG", "------------------------------------")
            // ---- FIN DE LOG DETALLADO ----

            calendarsList.add(CalendarInfo(id, displayName, accountName, colorInt))
        }
    }
    if (calendarsList.isEmpty()) {
        Log.w("ALL_CALENDARS_DEBUG", "No calendars found or accessible after processing cursor.")
    } else {
        Log.d("ALL_CALENDARS_DEBUG", "Finished processing. Found ${calendarsList.size} calendars.")
    }
    callback(calendarsList)
}


// --- READ FESTIVOS ---
fun readFestivosFromCalendars(
    context: Context,
    selectedCalendarIds: Set<Long>,
    callback: (Map<LocalDate, List<Festivo>>) -> Unit
) {
    if (selectedCalendarIds.isEmpty()) {
        callback(emptyMap())
        Log.d("ReadFestivos", "No calendars selected, returning empty map.")
        return
    }
    val resolver = context.contentResolver
    val map = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val startMillis = LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endMillis = LocalDate.now().plusYears(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val uri = CalendarContract.Events.CONTENT_URI
    val projection = arrayOf(
        CalendarContract.Events.CALENDAR_ID,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.TITLE
    )
    val selection = """
        ${CalendarContract.Events.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})
        AND ${CalendarContract.Events.DTSTART} >= ?
        AND ${CalendarContract.Events.DTSTART} <= ? 
    """.trimIndent()
    val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
    val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

    Log.d("ReadFestivos", "Querying with selection: $selection, args: ${selectionArgs.joinToString()}")
    val cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
    cursor?.use { c ->
        val calIdColumn = c.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
        val dtStartColumn = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
        val titleColumn = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
        Log.d("ReadFestivos", "Cursor has ${c.count} rows.")
        while (c.moveToNext()) {
            val calId = c.getLong(calIdColumn)
            val dtStart = c.getLong(dtStartColumn)
            val title = c.getString(titleColumn) ?: ""
            val date = Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
            val list = map.getOrPut(date) { mutableListOf() }
            list.add(Festivo(date, title, calId))
            Log.d("ReadFestivos", "Added event: $title on $date for calendar ID $calId")
        }
    }
    callback(map)
}

// --- SELECT CALENDARS DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCalendarsDialog(
    availableCalendars: List<CalendarInfo>,
    initiallySelectedCalendarIds: Set<Long>,
    onDismissRequest: () -> Unit,
    onApplySelection: (selectedIds: Set<Long>) -> Unit
) {
    var currentSelectedIds by remember { mutableStateOf(initiallySelectedCalendarIds) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Seleccionar Calendarios", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            if (availableCalendars.isEmpty()) {
                Text("No se encontraron calendarios o no se concedió el permiso para acceder a ellos.", fontSize = 16.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth()) {
                    items(availableCalendars, key = { it.id }) { calendar ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = currentSelectedIds.toMutableSet()
                                    if (newSet.contains(calendar.id)) newSet.remove(calendar.id)
                                    else newSet.add(calendar.id)
                                    currentSelectedIds = newSet
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentSelectedIds.contains(calendar.id),
                                onCheckedChange = { isChecked ->
                                    val newSet = currentSelectedIds.toMutableSet()
                                    if (isChecked) newSet.add(calendar.id)
                                    else newSet.remove(calendar.id)
                                    currentSelectedIds = newSet
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
                onClick = { onApplySelection(currentSelectedIds) },
                enabled = availableCalendars.isNotEmpty()
            ) { Text("Aplicar", fontSize = 16.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar", fontSize = 16.sp) }
        }
    )
}

// --- CALENDARS UI ---
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

    daysOfWeek.forEach { day ->
        cells.add {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFadd1fa)).border(1.dp, Color(0xFFCCCCCC)),
                contentAlignment = Alignment.Center
            ) { Text(day, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
    for (i in 0 until firstDayOfWeek) cells.add { Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFCCCCCC))) }
    for (dayNum in 1..daysInMonth) {
        val thisDate = currentMonth.atDay(dayNum)
        val isToday = thisDate == today
        val isSunday = thisDate.dayOfWeek.value % 7 == 0
        val dayEvents = eventsByDate[thisDate].orEmpty()
        val azulCabecera = Color(0xFF2196F3)
        cells.add {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White)
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
    val remainder = cells.size % 7
    if (remainder != 0) for (i in 0 until (7 - remainder)) cells.add { Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFCCCCCC))) }

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
                        ) { cells[rowIndex * 7 + colIndex].invoke() }
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
        months.chunked(2).forEach { monthPair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                monthPair.forEach { month ->
                    Box(
                        modifier = Modifier.weight(1f).padding(4.dp).clickable { onMonthSelected(month) },
                        contentAlignment = Alignment.Center
                    ) { MiniMonthCalendar(month, today, eventsByDate) }
                }
                if (monthPair.size == 1) Spacer(modifier = Modifier.weight(1f).padding(4.dp))
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
    val totalCellsToDisplay = ((firstDayOfWeekIndex + daysInMonth + 6) / 7) * 7

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(6.dp)).padding(vertical = 3.dp, horizontal = 2.dp)
    ) {
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
            fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp)
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
        val dayCells = remember(month, eventsByDate, today) {
            List(totalCellsToDisplay) { cellIndex ->
                val dayNumber = cellIndex - firstDayOfWeekIndex + 1
                if (dayNumber in 1..daysInMonth) month.atDay(dayNumber) else null
            }
        }
        dayCells.chunked(7).forEach { weekDates ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weekDates.forEach { date ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val dayEvents = eventsByDate[date].orEmpty()
                            val isToday = date == today
                            val azulCabecera = Color(0xFF2196F3)
                            Box(
                                modifier = Modifier.fillMaxSize().clip(CircleShape.copy(all = CornerSize(2.dp)))
                                    .background(if (isToday) azulCabecera.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = if (isToday) 1.dp else 0.dp,
                                        color = if (isToday) azulCabecera else Color.Transparent,
                                        shape = CircleShape.copy(all = CornerSize(2.dp))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}", fontSize = 9.sp,
                                    fontWeight = if (dayEvents.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        dayEvents.isNotEmpty() -> Color.Red
                                        date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color.Red.copy(alpha = 0.8f)
                                        isToday -> Color.Blue
                                        else -> Color.Black.copy(alpha = 0.8f)
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
