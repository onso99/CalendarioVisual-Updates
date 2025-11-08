package com.example.calendario

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
                    endTimeStr = festivo.endTime?.toString(),
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
            remove("events")
        }
        return emptyMap()
    }

    return mapFromString.mapNotNull { (dateStr, dtoList) ->
        val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { Log.e("CalendarDataUtils", "Error parseando fecha: '$dateStr'", e); null }
        if (date != null) {
            date to dtoList.map { dto ->
                Festivo(
                    id = -1L,
                    title = dto.desc.takeIf { it.isNotBlank() }?.take(40)?.trim() ?: "(Evento guardado)",
                    description = dto.desc,
                    date = date,
                    startTime = dto.startTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { Log.e("CalendarDataUtils", "Error parseando LocalTime en load (startTime): '$it'", e); null } },
                    endTime = dto.endTimeStr?.let { try { LocalTime.parse(it) } catch (e: Exception) { Log.e("CalendarDataUtils", "Error parseando LocalTime en load (endTime): '$it'", e); null } },
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

fun processEventsForDisplay(
    eventsByDate: Map<LocalDate, List<Festivo>>,
    targetMonth: YearMonth,
    today: LocalDate,
    showAll: Boolean
): List<Pair<LocalDate, List<Festivo>>> {
    val isCurrentMonthView = targetMonth.year == today.year && targetMonth.month == today.month
    val now = LocalTime.now()

    return eventsByDate
        .filterKeys { date -> date.month == targetMonth.month && date.year == targetMonth.year }
        .mapValues { (date, festivosOnDate) ->
            festivosOnDate.filter { festivo ->
                if (date.isEqual(today)) {
                    if (!festivo.isAllDay && festivo.startTime != null && festivo.endTime != null) {
                        now.isBefore(festivo.endTime)
                    } else {
                        true
                    }
                } else {
                    true
                }
            }
        }
        .let { eventsInMonth ->
            if (isCurrentMonthView && !showAll) {
                eventsInMonth.filterKeys { date -> !date.isBefore(today) }
            } else {
                eventsInMonth
            }
        }
        .filterValues { festivos -> festivos.isNotEmpty() }
        .toSortedMap()
        .map { entry ->
            entry.key to entry.value.sortedWith(
                compareBy<Festivo> { it.isAllDay }.reversed()
                    .thenBy(nullsLast()) { it.startTime }
            )
        }
}

suspend fun loadAvailableCalendarsSuspend(context: Context): List<CalendarInfo> {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        Log.w("CalendarDataUtils", "Permiso denegado en loadAvailableCalendarsSuspend. Devolviendo lista vacía.")
        return emptyList()
    }

    return withContext(Dispatchers.IO) {
        val calendarsList = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val displayNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val isPrimaryColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
                val accessLevelColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val displayName = it.getString(displayNameColumn) ?: "Calendario sin nombre"
                    val accountName = it.getString(accountNameColumn) ?: "Cuenta desconocida"
                    val colorInt = try {
                        if (it.isNull(colorColumn)) null else it.getInt(colorColumn)
                    } catch (_: Exception) { null }
                    val isPrimary = it.getInt(isPrimaryColumn) == 1
                    val accessLevel = it.getInt(accessLevelColumn)
                    val canModify = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                    calendarsList.add(
                        CalendarInfo(
                            id = id,
                            displayName = displayName,
                            accountName = accountName,
                            color = colorInt,
                            isPrimary = isPrimary,
                            canModify = canModify
                        )
                    )
                }
            } ?: Log.w("LoadCalendars", "El cursor de calendarios del ContentResolver fue nulo.")

            calendarsList
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
    availableCalendars: List<CalendarInfo>
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
    val systemZoneId = ZoneId.systemDefault()

    val today = LocalDate.now()
    val startRangeDate = today.minusYears(1).withDayOfYear(1)
    val endRangeDate = today.plusYears(2).withDayOfYear(today.plusYears(2).lengthOfYear())

    val startRangeMillis = startRangeDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
    val endRangeMillis = endRangeDate.plusDays(1).atStartOfDay(systemZoneId).toInstant().toEpochMilli()

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, startRangeMillis)
    ContentUris.appendId(builder, endRangeMillis)
    val instancesUri = builder.build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
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
            val eventOriginalIdColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calIdColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val beginColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayColumn = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            Log.d("ReadFestivos", "Procesando ${c.count} instancias de eventos del provider.")

            while (c.moveToNext()) {
                if (!continuation.isActive) break

                val eventOriginalId = c.getLong(eventOriginalIdColumn)
                val eventCalId = c.getLong(calIdColumn)
                val beginMillis = c.getLong(beginColumn)
                val endMillis = c.getLong(endColumn)

                val eventTitleFromProvider = c.getStringOrNull(titleColumn)?.trim()
                val finalEventTitle = if (eventTitleFromProvider.isNullOrBlank()) "(Sin título)" else eventTitleFromProvider

                val isAllDayEvent = c.getInt(allDayColumn) == 1

                val calendarInfo = availableCalendars.find { it.id == eventCalId }
                val calendarDisplayName = calendarInfo?.displayName ?: "DESCONOCIDO_CAL_ID_$eventCalId"

                var isEventFromHolidaySource = false
                if (calendarDisplayName != "DESCONOCIDO_CAL_ID_$eventCalId") {
                    val lowerCaseDisplayName = calendarDisplayName.lowercase()
                    val holidayKeywords = listOf(
                        "festivo", "festivos", "holiday", "holidays",
                        "fiesta", "fiestas", "feriado", "feriados",
                        "national", "nacional"
                    )
                    if (holidayKeywords.any { keyword -> lowerCaseDisplayName.contains(keyword) }) {
                        isEventFromHolidaySource = true
                    }
                }

                if (isEventFromHolidaySource || calendarDisplayName.lowercase().contains("festivo")) {
                    Log.i("FestivoLogic", "Evento: '$finalEventTitle' (ID: $eventOriginalId) | Cal: '$calendarDisplayName' (ID: $eventCalId) | EsFuente: $isEventFromHolidaySource")
                }

                val beginInstant = Instant.ofEpochMilli(beginMillis)
                val beginDateTimeInSystemZone = beginInstant.atZone(systemZoneId)
                val eventDate = beginDateTimeInSystemZone.toLocalDate()

                var actualStartTime: LocalTime? = null
                var actualEndTime: LocalTime? = null

                if (!isAllDayEvent) {
                    actualStartTime = beginDateTimeInSystemZone.toLocalTime()
                    val endInstant = Instant.ofEpochMilli(endMillis)
                    val endDateTimeInSystemZone = endInstant.atZone(systemZoneId)
                    actualEndTime = endDateTimeInSystemZone.toLocalTime()
                }

                val eventDescriptionToUse = finalEventTitle

                val festivoEntry = Festivo(
                    id = eventOriginalId,
                    title = finalEventTitle,
                    description = eventDescriptionToUse,
                    date = eventDate,
                    startTime = actualStartTime,
                    endTime = actualEndTime,
                    isAllDay = isAllDayEvent,
                    calendarId = eventCalId,
                    isFromHolidaySource = isEventFromHolidaySource
                )

                val listForDate = map.getOrPut(eventDate) { mutableListOf() }
                listForDate.add(festivoEntry)
            }
        } ?: Log.w("ReadFestivos", "El cursor de instancias de eventos fue nulo.")

        if (continuation.isActive) {
            Log.d("ReadFestivos", "Lectura de instancias completada. Total días con eventos en mapa: ${map.size}, Total Festivos individuales: ${map.values.sumOf { it.size }}")
            continuation.resume(map)
        }

    } catch (e: SecurityException) {
        Log.e("ReadFestivosError", "Excepción de seguridad al leer instancias: ${e.message}", e)
        if (continuation.isActive) continuation.resumeWithException(e)
    } catch (e: Exception) {
        Log.e("ReadFestivosError", "Error general al leer instancias de eventos: ${e.message}", e)
        if (continuation.isActive) continuation.resumeWithException(e)
    }
}
