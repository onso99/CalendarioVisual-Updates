package com.example.calendario

import android.content.ContentUris
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

import com.example.calendario.hasVisibleEvents

// Imports para tus clases de datos (desde DataModels.kt)

import com.example.calendario.Festivo
import com.example.calendario.FestivoDto

import com.example.calendario.hasVisibleEvents // <-- IMPORTANTE: Importa tu función de CalendarDataCheck.kt
// import com.example.calendario.CalendarInfo // Asegúrate de que CalendarInfo esté accesible/importada

// Asegúrate de tener estos imports al principio de tu archivo MainActivity.kt
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.calendario.hasVisibleEvents // De CalendarDataCheck.kt
import com.example.calendario.CalendarInfo // De DataModels.kt

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
