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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
                    isAllDay = festivo.isAllDay,
                    rrule = festivo.rrule
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
        val date = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            Log.e("CalendarDataUtils", "Error parseando fecha: '$dateStr'", e); null
        }
        if (date != null) {
            date to dtoList.map { dto ->
                Festivo(
                    id = -1L,
                    title = dto.desc.takeIf { it.isNotBlank() }?.take(40)?.trim() ?: "(Evento guardado)",
                    description = dto.desc,
                    date = date,
                    startTime = dto.startTimeStr?.let {
                        try {
                            LocalTime.parse(it)
                        } catch (e: Exception) {
                            Log.e("CalendarDataUtils", "Error parseando LocalTime en load (startTime): '$it'", e); null
                        }
                    },
                    endTime = dto.endTimeStr?.let {
                        try {
                            LocalTime.parse(it)
                        } catch (e: Exception) {
                            Log.e("CalendarDataUtils", "Error parseando LocalTime en load (endTime): '$it'", e); null
                        }
                    },
                    isAllDay = dto.isAllDay,
                    calendarId = dto.id,
                    isFromHolidaySource = false,
                    rrule = dto.rrule
                )
            }
        } else {
            null
        }
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
        ?.mapNotNull { idStr ->
            try {
                idStr.toLong()
            } catch (e: NumberFormatException) {
                Log.e("CalendarDataUtils", "Error parseando ID: '$idStr'", e); null
            }
        }
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
                    } catch (_: Exception) {
                        null
                    }
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
            emptyList()
        } catch (e: Exception) {
            Log.e("LoadCalendars", "Error general en loadAvailableCalendarsSuspend: ${e.message}", e)
            emptyList()
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
    val finalMap = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val systemZoneId = ZoneId.systemDefault()

    val today = LocalDate.now()
    val startRangeDate = today.minusYears(1).withDayOfYear(1)
    val endRangeDate = today.plusYears(2).withDayOfYear(today.plusYears(2).lengthOfYear())

    val startRangeMillis = startRangeDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
    val endRangeMillis = endRangeDate.plusDays(1).atStartOfDay(systemZoneId).toInstant().toEpochMilli()

    val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon().let { 
        ContentUris.appendId(it, startRangeMillis)
        ContentUris.appendId(it, endRangeMillis)
        it.build()
    }

    val instancesProjection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.ALL_DAY
    )
    val instancesSelection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"

    try {
        val eventInstances = mutableListOf<Festivo>()
        val eventIds = mutableSetOf<Long>()

        resolver.query(instancesUri, instancesProjection, instancesSelection, null, null)?.use { c ->
            Log.d("ReadFestivos", "Paso 1: Procesando ${c.count} instancias de eventos.")
            val eventIdCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calIdCol = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endCol = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleCol = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayCol = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (c.moveToNext()) {
                val eventId = c.getLong(eventIdCol)
                eventIds.add(eventId)

                val beginMillis = c.getLong(beginCol)
                val endMillis = c.getLong(endCol)
                val calId = c.getLong(calIdCol)
                val title = c.getStringOrNull(titleCol)?.trim() ?: "(Sin título)"
                val isAllDay = c.getInt(allDayCol) == 1

                val beginInstant = Instant.ofEpochMilli(beginMillis)
                val beginDateTime = beginInstant.atZone(systemZoneId)
                val eventDate = beginDateTime.toLocalDate()
                val startTime = if (isAllDay) null else beginDateTime.toLocalTime()
                val endTime = if (isAllDay) null else Instant.ofEpochMilli(endMillis).atZone(systemZoneId).toLocalTime()
                
                val calendarInfo = availableCalendars.find { it.id == calId }
                val isHolidaySource = calendarInfo?.displayName?.lowercase()?.contains("festivo") == true ||
                        (calendarInfo?.displayName?.lowercase()?.let { name ->
                            listOf("holiday", "fiesta", "feriado", "nacional").any { keyword -> name.contains(keyword) }
                        } == true)

                eventInstances.add(Festivo(
                    id = eventId,
                    title = title,
                    description = title,
                    date = eventDate,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    calendarId = calId,
                    isFromHolidaySource = isHolidaySource,
                    rrule = null // Se llenará en el paso 2
                ))
            }
        } ?: Log.w("ReadFestivos", "El cursor de instancias de eventos fue nulo.")

        if (!continuation.isActive || eventIds.isEmpty()) {
            if (continuation.isActive) continuation.resume(emptyMap())
            return@suspendCancellableCoroutine
        }

        val rruleMap = mutableMapOf<Long, String?>()
        val eventsProjection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.RRULE)
        val eventsSelection = "${CalendarContract.Events._ID} IN (${eventIds.joinToString(",")})"
        
        resolver.query(CalendarContract.Events.CONTENT_URI, eventsProjection, eventsSelection, null, null)?.use { c ->
            Log.d("ReadFestivos", "Paso 2: Obteniendo RRULEs para ${c.count} eventos.")
            val idCol = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val rruleCol = c.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            while (c.moveToNext()) {
                val eventId = c.getLong(idCol)
                rruleMap[eventId] = c.getStringOrNull(rruleCol)
            }
        } ?: Log.w("ReadFestivos", "El cursor de eventos para RRULEs fue nulo.")
        
        eventInstances.forEach { instance ->
            val finalFestivo = instance.copy(rrule = rruleMap[instance.id])
            finalMap.getOrPut(finalFestivo.date) { mutableListOf() }.add(finalFestivo)
        }

        finalMap.values.forEach { festivos -> 
            festivos.sortWith(
                compareBy<Festivo> { it.isAllDay }.reversed()
                .thenBy(nullsLast()) { it.startTime }
            )
        }

        if (continuation.isActive) {
            Log.d("ReadFestivos", "Paso 3: Lectura completada. Total días: ${finalMap.size}, Total eventos: ${finalMap.values.sumOf { it.size }}")
            continuation.resume(finalMap)
        }

    } catch (e: Exception) {
        Log.e("ReadFestivosError", "Error general al leer eventos: ${e.message}", e)
        if (continuation.isActive) continuation.resumeWithException(e)
    }
}
