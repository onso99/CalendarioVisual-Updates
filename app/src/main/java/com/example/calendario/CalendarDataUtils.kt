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
                    title = festivo.title,
                    description = festivo.description,
                    id = festivo.calendarId,
                    startTimeStr = festivo.startTime?.toString(),
                    endTimeStr = festivo.endTime?.toString(),
                    isAllDay = festivo.isAllDay,
                    rrule = festivo.rrule,
                    age = festivo.age,
                    isBirthday = festivo.isBirthday
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
                    title = dto.title.takeIf { !it.isNullOrBlank() } ?: dto.description.takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.saved_event),
                    description = dto.description,
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
                    rrule = dto.rrule,
                    age = dto.age,
                    isBirthday = dto.isBirthday ?: false
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
        .let { eventsInMonth ->
            if (isCurrentMonthView && !showAll) {
                eventsInMonth
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
                    .filterKeys { date -> !date.isBefore(today) }
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
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.DELETED
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.DELETED} != 1", // Restore original selection
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
                    val displayName = it.getString(displayNameColumn) ?: context.getString(R.string.unnamed_calendar)
                    val accountName = it.getString(accountNameColumn) ?: context.getString(R.string.unknown_account)
                    val colorInt = try {
                        if (it.isNull(colorColumn)) null else it.getInt(colorColumn)
                    } catch (_: Exception) {
                        null
                    }
                    val isPrimary = it.getInt(isPrimaryColumn) == 1
                    val accessLevel = it.getInt(accessLevelColumn)
                    val canModify = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                    if (accessLevel > CalendarContract.Calendars.CAL_ACCESS_NONE) {
                        calendarsList.add(
                            CalendarInfo(
                                id = id,
                                displayName = displayName,
                                accountName = accountName,
                                color = colorInt,
                                isPrimary = isPrimary,
                                canModify = canModify,
                                accessLevel = accessLevel,
                                isDeleted = false // We are filtering out deleted calendars
                            )
                        )
                    }
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
                val title = c.getStringOrNull(titleCol)?.trim() ?: context.getString(R.string.no_title)
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
                    description = null,
                    date = eventDate,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    calendarId = calId,
                    isFromHolidaySource = isHolidaySource,
                    rrule = null, 
                    age = null 
                ))
            }
        } ?: Log.w("ReadFestivos", "El cursor de instancias de eventos fue nulo.")

        if (!continuation.isActive || eventIds.isEmpty()) {
            if (continuation.isActive) continuation.resume(emptyMap())
            return@suspendCancellableCoroutine
        }

        val extendedPropertiesMap = mutableMapOf<Long, MutableMap<String, String>>()
        if (eventIds.isNotEmpty()) {
            val propertiesProjection = arrayOf(
                CalendarContract.ExtendedProperties.EVENT_ID,
                CalendarContract.ExtendedProperties.NAME,
                CalendarContract.ExtendedProperties.VALUE
            )
            val propertiesSelection = "${CalendarContract.ExtendedProperties.EVENT_ID} IN (${eventIds.joinToString(",")}) AND ${CalendarContract.ExtendedProperties.NAME} = ?"
            val propertiesSelectionArgs = arrayOf("shared:calendarProviderEventType")

            resolver.query(CalendarContract.ExtendedProperties.CONTENT_URI, propertiesProjection, propertiesSelection, propertiesSelectionArgs, null)?.use { c ->
                val eventIdCol = c.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
                val valueCol = c.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE)

                while (c.moveToNext()) {
                    val eventId = c.getLong(eventIdCol)
                    val value = c.getStringOrNull(valueCol)
                    if (value == "BIRTHDAY") {
                         extendedPropertiesMap.getOrPut(eventId) { mutableMapOf() }["isBirthday"] = "true"
                    }
                }
            } ?: Log.w("ReadFestivos", "El cursor de propiedades extendidas fue nulo.")
        }

        data class EventExtraData(val rrule: String?, val description: String?, val dtStart: Long?)
        val eventDataMap = mutableMapOf<Long, EventExtraData>()
        val eventsProjection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.RRULE, CalendarContract.Events.DESCRIPTION, CalendarContract.Events.DTSTART)
        val eventsSelection = "${CalendarContract.Events._ID} IN (${eventIds.joinToString(",")})"
        
        resolver.query(CalendarContract.Events.CONTENT_URI, eventsProjection, eventsSelection, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val rruleCol = c.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            val descCol = c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val dtStartCol = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)

            while (c.moveToNext()) {
                val eventId = c.getLong(idCol)
                val rrule = c.getStringOrNull(rruleCol)
                val description = c.getStringOrNull(descCol)
                val dtStart = if (c.isNull(dtStartCol)) null else c.getLong(dtStartCol)
                eventDataMap[eventId] = EventExtraData(rrule, description, dtStart)
            }
        } ?: Log.w("ReadFestivos", "El cursor de eventos para datos adicionales fue nulo.")
        
        eventInstances.forEach { instance ->
            val eventData = eventDataMap[instance.id]
            val isBirthday = extendedPropertiesMap[instance.id]?.get("isBirthday") == "true"

            var age: Int? = null
            var originalBirthDate: LocalDate? = null
            if (isBirthday) {
                eventData?.dtStart?.let { dtStartMillis ->
                    val birthDate = Instant.ofEpochMilli(dtStartMillis).atZone(systemZoneId).toLocalDate()
                    age = instance.date.year - birthDate.year
                    originalBirthDate = birthDate
                }
            }

            val finalFestivo = instance.copy(
                description = eventData?.description,
                rrule = eventData?.rrule,
                age = age,
                isBirthday = isBirthday,
                originalBirthDate = originalBirthDate
            )
            finalMap.getOrPut(finalFestivo.date) { mutableListOf() }.add(finalFestivo)
        }

        finalMap.values.forEach { festivos -> 
            festivos.sortWith(
                compareBy<Festivo> { it.isAllDay }.reversed()
                    .thenBy(nullsLast()) { it.startTime }
            )
        }

        if (continuation.isActive) {
            continuation.resume(finalMap)
        }

    } catch (e: Exception) {
        Log.e("ReadFestivosError", "Error general al leer eventos: ${e.message}", e)
        if (continuation.isActive) continuation.resumeWithException(e)
    }
}
