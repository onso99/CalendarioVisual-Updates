package com.example.calendario

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth // ★ AÑADIDO PARA processEventsForDisplay ★
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Imports para tus clases de datos (desde DataModels.kt)
import com.example.calendario.CalendarInfo
import com.example.calendario.Festivo
import com.example.calendario.FestivoDto

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
                    date = date,
                    description = dto.desc,
                    calendarId = dto.id,
                    isFromHolidaySource = false, // Este valor se determina al leer del provider, no al cargar de prefs
                    startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { Log.e("CalendarDataUtils", "Error parseando LocalTime: '$it'", e); null } },
                    isAllDay = dto.isAllDay
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


// --- FUNCIONES DE LECTURA DE CALENDARIO (REFACTORIZADAS A SUSPEND) ---

suspend fun loadAvailableCalendarsSuspend(context: Context): List<CalendarInfo> =
    suspendCancellableCoroutine { continuation ->
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CalendarDataUtils", "Permiso denegado en loadAvailableCalendarsSuspend")
            if (continuation.isActive) continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val calendarsList = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        try {
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
                    if (!continuation.isActive) break // Verificar cancelación antes de procesar
                    val id = it.getLong(idColumn)
                    val displayName = it.getString(displayNameColumn) ?: "Calendario sin nombre"
                    val accountName = it.getString(accountNameColumn) ?: "Cuenta desconocida"
                    val colorInt = try { it.getInt(colorColumn) } catch (_: Exception) { null }
                    calendarsList.add(CalendarInfo(id, displayName, accountName, colorInt))
                }
            }
            if (continuation.isActive) {
                continuation.resume(calendarsList)
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                Log.e("CalendarDataUtils", "Error en loadAvailableCalendarsSuspend", e)
                continuation.resumeWithException(e)
            }
        }
    }

suspend fun readFestivosFromCalendarsSuspend(
    context: Context,
    selectedCalendarIds: Set<Long>,
    availableCalendars: List<CalendarInfo> // Mantenemos este parámetro por si lo usas para algo más que el ID de festivos
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

    val specificHolidaySourceCalendarId = 2L // Considera hacer esto configurable si es necesario
    val holidayCalendarIds = if (selectedCalendarIds.contains(specificHolidaySourceCalendarId)) {
        setOf(specificHolidaySourceCalendarId)
    } else {
        emptySet<Long>()
    }

    val resolver = context.contentResolver
    val map = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val now = Instant.now()
    val daysInTwoYears = 730L // Rango de eventos a leer
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
                if (!continuation.isActive) break
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
                    endInstant.atZone(systemZoneId).toLocalDate().minusDays(1L) // Para eventos de varios días, no incluir el día final si es a las 00:00
                } else {
                    beginDateTimeAtSystemZone.toLocalDate()
                }

                while (!currentDateIterator.isAfter(loopEndDate)) {
                    if (!continuation.isActive) break
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
                if (!continuation.isActive) break
            }
        }
        if (continuation.isActive) {
            continuation.resume(map)
        }
    } catch (e: Exception) {
        if (continuation.isActive) {
            Log.e("CalendarDataUtils", "Error en readFestivosFromCalendarsSuspend", e)
            continuation.resumeWithException(e)
        }
    }
}
