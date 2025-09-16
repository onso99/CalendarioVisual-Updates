package com.example.calendario

// Android SDK & AndroidX
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor // Aunque .use lo maneja, el tipo explícito puede ser útil
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit // Para SharedPreferences
import androidx.core.database.getStringOrNull // Para leer del cursor de forma segura

// Kotlin Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext // Si mueves loadAvailableCalendars aquí

// Java Time API
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth // Para processEventsForDisplay

// Kotlin Standard Library
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
// import java.time.Duration // No usado en la última versión de readFestivosFromCalendarsSuspend

// Gson para SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Tus clases de datos y otras utilidades del proyecto
import com.example.calendario.CalendarInfo
import com.example.calendario.Festivo
import com.example.calendario.FestivoDto
import com.example.calendario.hasVisibleEvents // Asumo que esta es una función tuya

// Si tienes java.util.Locale en alguna otra parte, se quedaría, si no, no es necesario
// para la última versión de readFestivosFromCalendarsSuspend que usa .lowercase()
// import java.util.Locale

// --- FUNCIONES DE PERSISTENCIA (SIN CAMBIOS) ---

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
    Log.d("CalendarDataUtils", "Eventos guardados en SharedPreferences.")
}

fun loadEventsFromPrefs(context: Context): Map<LocalDate, List<Festivo>> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("events", null)
    if (json == null) {
        Log.d("CalendarDataUtils", "No hay eventos guardados en SharedPreferences.")
        return emptyMap()
    }
    val gson = Gson()
    val type = object : TypeToken<Map<String, List<FestivoDto>>>() {}.type
    val mapFromString: Map<String, List<FestivoDto>> = try {
        gson.fromJson(json, type)
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error al deserializar eventos desde SharedPreferences", e)
        prefs.edit {
            remove("events") // Limpiar datos corruptos
        }
        return emptyMap()
    }

    return mapFromString.mapNotNull { (dateStr, dtoList) ->
        val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { Log.e("CalendarDataUtils", "Error parseando fecha: '$dateStr'", e); null }
        if (date != null) {
            date to dtoList.map { dto ->
                Festivo(
                    id = -1L, // ID de evento no disponible desde SharedPreferences
                    title = dto.desc.takeIf { it.isNotBlank() }?.take(40)?.trim() ?: "(Evento guardado)", // Placeholder para el título
                    description = dto.desc,
                    date = date,
                    startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { Log.e("CalendarDataUtils", "Error parseando LocalTime en load: '$it'", e); null } },
                    isAllDay = dto.isAllDay,
                    calendarId = dto.id,
                    isFromHolidaySource = false
                )
            }
        } else { null }
    }.toMap().also {
        Log.d("CalendarDataUtils", "Eventos cargados: ${it.size} días.")
    }
}

fun saveSelectedCalendarIds(context: Context, selectedIds: Set<Long>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putStringSet("selected_calendar_ids", selectedIds.map { it.toString() }.toSet())
    }
    Log.d("CalendarDataUtils", "IDs de calendario seleccionados guardados: $selectedIds")
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("selected_calendar_ids", emptySet())
        ?.mapNotNull { idStr -> try { idStr.toLong() } catch (e: NumberFormatException) { Log.e("CalendarDataUtils", "Error parseando ID: '$idStr'", e); null } }
        ?.toSet() ?: emptySet()
}

// --- ★ NUEVA FUNCIÓN AÑADIDA ★ ---
/**
 * Procesa el mapa de eventos por fecha para obtener una lista de pares (Fecha, Lista de Festivos)
 * para mostrar, típicamente para un mes específico y filtrando días pasados
 * si es el mes actual.
 *
 * @param eventsByDate El mapa completo de eventos.
 * @param targetMonth El mes para el cual se quieren los eventos.
 * @param today La fecha actual, para filtrar eventos pasados del mes actual.
 * @return Una lista de pares (Fecha, Lista de Festivos) ordenada por fecha,
 *         conteniendo solo eventos relevantes para la visualización.
 */
fun processEventsForDisplay(
    eventsByDate: Map<LocalDate, List<Festivo>>,
    targetMonth: YearMonth,
    today: LocalDate
): List<Pair<LocalDate, List<Festivo>>> {
    val isCurrentMonthView = targetMonth.year == today.year && targetMonth.month == today.month

    val eventsForSelectedMonth = eventsByDate
        .filterKeys { date -> date.month == targetMonth.month && date.year == targetMonth.year }
        .let { eventsInMonth ->
            if (isCurrentMonthView) {
                // Para el mes actual, solo mostrar eventos desde hoy en adelante
                eventsInMonth.filterKeys { date -> !date.isBefore(today) }
            } else {
                // Para otros meses, mostrar todos los eventos
                eventsInMonth
            }
        }
        .filterValues { festivos -> festivos.isNotEmpty() } // Asegurarse de que solo incluimos días que realmente tienen eventos
        .toSortedMap() // Ordenar por fecha para una visualización consistente

    // Convertir el mapa ordenado a una lista de Pares para iterar fácilmente en Compose
    return eventsForSelectedMonth.toList()
}



suspend fun loadAvailableCalendarsSuspend(context: Context): List<CalendarInfo> {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        Log.w("CalendarDataUtils", "Permiso denegado en loadAvailableCalendarsSuspend. Devolviendo lista vacía.")
        return emptyList()
    }

    return withContext(Dispatchers.IO) {
        val calendarsListWithEvents = mutableListOf<CalendarInfo>()
        // Ajustamos la proyección para que NO incluya IS_PRIMARY, ya que CalendarInfo no lo tiene
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_TYPE // Lo mantenemos por si lo usabas, aunque no en CalendarInfo
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null, // selection
                null, // selectionArgs
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.use {
                Log.d("LoadCalendars", "Cursor de calendarios del sistema obtenido con ${it.count} entradas.")
                val idColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val displayNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                // val accountTypeColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val displayName = it.getString(displayNameColumn) ?: "Calendario sin nombre"
                    val accountName = it.getString(accountNameColumn) ?: "Cuenta desconocida"
                    val colorInt = try {
                        if (it.isNull(colorColumn)) null else it.getInt(colorColumn)
                    } catch (_: Exception) {
                        null
                    }
                    // val accountType = it.getString(accountTypeColumn)

                    if (hasVisibleEvents(context, id)) {
                        // --- CREACIÓN DE CalendarInfo CORREGIDA ---
                        // Ahora solo pasamos los parámetros que tu CalendarInfo realmente define.
                        calendarsListWithEvents.add(
                            CalendarInfo(
                                id = id,
                                displayName = displayName,
                                accountName = accountName,
                                color = colorInt // 'color' en CalendarInfo puede ser null
                            )
                        )
                        Log.i("LoadCalendars", "Calendario AÑADIDO (tiene eventos visibles): '$displayName' (ID: $id)")
                    } else {
                        Log.i("LoadCalendars", "Calendario IGNORADO (sin eventos visibles o error): '$displayName' (ID: $id)")
                    }
                }
            } ?: Log.w("LoadCalendars", "El cursor de calendarios del ContentResolver fue nulo.")

            Log.i("LoadCalendars", "Total de calendarios con eventos visibles que se devolverán: ${calendarsListWithEvents.size}")
            calendarsListWithEvents
        } catch (e: SecurityException) {
            Log.e("LoadCalendars", "Excepción de seguridad al cargar calendarios: ${e.message}", e)
            emptyList<CalendarInfo>()
        } catch (e: Exception) {
            Log.e("LoadCalendars", "Error general en loadAvailableCalendarsSuspend: ${e.message}", e)
            emptyList<CalendarInfo>()
        }
    }
}

suspend fun readFestivosFromCalendarsSuspend(
    context: Context,
    selectedCalendarIds: Set<Long>,
    availableCalendars: List<CalendarInfo> // Necesitamos esta lista para obtener displayName
): Map<LocalDate, List<Festivo>> = suspendCancellableCoroutine { continuation ->
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        Log.w("CalendarDataUtils", "Permiso READ_CALENDAR no concedido en readFestivosFromCalendarsSuspend.")
        if (continuation.isActive) continuation.resume(emptyMap())
        return@suspendCancellableCoroutine
    }
    if (selectedCalendarIds.isEmpty()) {
        Log.i("CalendarDataUtils", "No hay calendarios seleccionados por el usuario en readFestivosFromCalendarsSuspend.")
        if (continuation.isActive) continuation.resume(emptyMap())
        return@suspendCancellableCoroutine
    }

    val resolver = context.contentResolver
    val map = mutableMapOf<LocalDate, MutableList<Festivo>>()

    // Define un rango de fechas razonable para la consulta de instancias
    // Por ejemplo, 1 año hacia atrás y 2 años hacia adelante desde hoy.
    val today = LocalDate.now()
    val startRangeDate = today.minusYears(1).withDayOfYear(1) // Inicio del año pasado
    val endRangeDate = today.plusYears(2).withDayOfYear(today.plusYears(2).lengthOfYear()) // Fin de dentro de dos años

    val startRangeMillis = startRangeDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endRangeMillis = endRangeDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() // Exclusivo

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, startRangeMillis)
    ContentUris.appendId(builder, endRangeMillis)
    val instancesUri = builder.build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,       // ID original del evento
        CalendarContract.Instances.CALENDAR_ID,    // ID del calendario
        CalendarContract.Instances.BEGIN,          // Inicio de la instancia (UTC)
        CalendarContract.Instances.END,            // Fin de la instancia (UTC)
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.ALL_DAY
        // Nota: CalendarContract.Instances no tiene un campo DESCRIPTION directo.
        // Si necesitas la descripción original del evento, tendrías que hacer una consulta
        // separada a CalendarContract.Events usando EVENT_ID, o considerar si el título es suficiente.
    )
    val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"

    try {
        val cursor = resolver.query(instancesUri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")
        cursor?.use { c ->
            val eventOriginalIdColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calIdColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val beginColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            // val endColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.END) // No lo usamos directamente para construir Festivo aquí
            val titleColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            Log.d("ReadFestivos", "Procesando ${c.count} instancias de eventos del provider.")

            while (c.moveToNext()) {
                if (!continuation.isActive) break // Corutina cancelada

                val eventOriginalId = c.getLong(eventOriginalIdColumn)
                val eventCalId = c.getLong(calIdColumn)
                val beginMillis = c.getLong(beginColumn)

                // Título del evento
                val eventTitleFromProvider = c.getStringOrNull(titleColumn)?.trim()
                val finalEventTitle = if (eventTitleFromProvider.isNullOrBlank()) "(Sin título)" else eventTitleFromProvider

                val isAllDayEvent = c.getInt(allDayColumn) == 1

                // Obtener información del calendario (nombre y color)
                val calendarInfo = availableCalendars.find { it.id == eventCalId }
                val calendarDisplayName = calendarInfo?.displayName ?: "DESCONOCIDO_CAL_ID_$eventCalId"
                // No estamos usando el color del evento individual, así que no necesitamos calendarColor aquí

                // --- LÓGICA DE isFromHolidaySource MEJORADA ---
                var isEventFromHolidaySource = false
                if (calendarDisplayName != "DESCONOCIDO_CAL_ID_$eventCalId") {
                    val lowerCaseDisplayName = calendarDisplayName.lowercase() // Usa root locale por defecto
                    val holidayKeywords = listOf(
                        "festivo", "festivos",
                        "holiday", "holidays",
                        "fiesta", "fiestas",
                        "feriado", "feriados",
                        "national", "nacional"
                        // Puedes añadir más palabras clave si es necesario
                    )
                    if (holidayKeywords.any { keyword -> lowerCaseDisplayName.contains(keyword) }) {
                        isEventFromHolidaySource = true
                    }
                }

                // Log de depuración para la lógica de festivos
                if (isEventFromHolidaySource || calendarDisplayName.lowercase().contains("festivo")) {
                    Log.i("FestivoLogic", "Evento: '$finalEventTitle' (ID: $eventOriginalId) | Cal: '$calendarDisplayName' (ID: $eventCalId) | EsFuente: $isEventFromHolidaySource")
                }
                // --- FIN LÓGICA isFromHolidaySource ---

                // Conversión de tiempo: Instances.BEGIN está en UTC. Convertimos a la zona del sistema para LocalDate/LocalTime.
                // Para una precisión absoluta, si el evento tiene su propia EVENT_TIMEZONE, deberías usarla.
                // CalendarContract.Instances no expone directamente EVENT_TIMEZONE. Tendrías que obtenerla de CalendarContract.Events.
                // Por simplicidad aquí, convertiremos UTC a la zona por defecto del sistema.
                val beginInstant = Instant.ofEpochMilli(beginMillis)
                val beginDateTimeInSystemZone = beginInstant.atZone(ZoneId.systemDefault())

                val eventDate = beginDateTimeInSystemZone.toLocalDate()
                val actualStartTime = if (isAllDayEvent) null else beginDateTimeInSystemZone.toLocalTime()

                // Descripción: Como Instances no tiene descripción, usamos el título.
                val eventDescriptionToUse = finalEventTitle

                // Crear el objeto Festivo
                val festivoEntry = Festivo(
                    id = eventOriginalId,
                    title = finalEventTitle,
                    description = eventDescriptionToUse, // Usando título como descripción
                    date = eventDate,
                    startTime = actualStartTime,
                    isAllDay = isAllDayEvent,
                    // color = ..., // No incluimos color aquí según tu aclaración
                    calendarId = eventCalId,
                    isFromHolidaySource = isEventFromHolidaySource
                )

                // Añadir a la lista del mapa
                // CalendarContract.Instances ya expande eventos de varios días y recurrentes,
                // así que cada fila del cursor debería corresponder a una aparición en un día específico.
                val listForDate = map.getOrPut(eventDate) { mutableListOf() }
                listForDate.add(festivoEntry)

            } // Fin while (cursor.moveToNext())
        } ?: Log.w("ReadFestivos", "El cursor de instancias de eventos fue nulo.")

        if (continuation.isActive) {
            Log.d("ReadFestivos", "Lectura de instancias completada. Total días con eventos en mapa: ${map.size}, Total Festivos individuales: ${map.values.sumOf { it.size }}")
            continuation.resume(map)
        }

    } catch (e: SecurityException) {
        Log.e("ReadFestivosError", "Excepción de seguridad al leer instancias: ${e.message}", e)
        if (continuation.isActive) continuation.resumeWithException(e) // O resume(emptyMap())
    } catch (e: Exception) {
        Log.e("ReadFestivosError", "Error general al leer instancias de eventos: ${e.message}", e)
        if (continuation.isActive) continuation.resumeWithException(e) // O resume(emptyMap())
    }
}

