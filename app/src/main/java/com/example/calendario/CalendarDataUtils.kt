package com.example.calendario

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
    selectedCalendarIds: Set<Long>
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
        android.content.ContentUris.appendId(it, startRangeMillis)
        android.content.ContentUris.appendId(it, endRangeMillis)
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
        val eventIds = mutableSetOf<Long>()

        resolver.query(instancesUri, instancesProjection, instancesSelection, null, null)?.use { cursor ->
            val eventIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)

            while (cursor.moveToNext() && continuation.isActive) {
                eventIds.add(cursor.getLong(eventIdColumn))
            }
        }

        val rruleMap = mutableMapOf<Long, String>()
        if (eventIds.isNotEmpty()) {
            val eventsProjection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.RRULE)
            val eventsSelection = "${CalendarContract.Events._ID} IN (${eventIds.joinToString(",")})"
            resolver.query(CalendarContract.Events.CONTENT_URI, eventsProjection, eventsSelection, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val rruleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(idColumn)
                    val rrule = cursor.getStringOrNull(rruleColumn)
                    if (rrule != null) {
                        rruleMap[eventId] = rrule
                    }
                }
            }
        }
        
        resolver.query(instancesUri, instancesProjection, instancesSelection, null, null)?.use { cursor ->
            val eventIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calendarIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext() && continuation.isActive) {
                val eventId = cursor.getLong(eventIdColumn)
                val calendarId = cursor.getLong(calendarIdColumn)
                val beginMillis = cursor.getLong(beginColumn)
                val endMillis = cursor.getLong(endColumn)
                val title = cursor.getStringOrNull(titleColumn)?.trim() ?: ""
                val isAllDay = cursor.getInt(allDayColumn) == 1

                if (title.isNotBlank() || isAllDay) { // Heuristic to filter out empty/invalid events
                    val startInstant = Instant.ofEpochMilli(beginMillis)
                    val endInstant = Instant.ofEpochMilli(endMillis)
                    val startDate = startInstant.atZone(systemZoneId).toLocalDate()
                    val startTime = if (isAllDay) null else startInstant.atZone(systemZoneId).toLocalTime()
                    val endTime = if (isAllDay) null else endInstant.atZone(systemZoneId).toLocalTime()
                    
                    val festivo = Festivo(
                        id = eventId,
                        title = title,
                        description = null, // Will be fetched later if needed
                        date = startDate,
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = isAllDay,
                        calendarId = calendarId,
                        isFromHolidaySource = false,
                        rrule = rruleMap[eventId],
                        age = null,
                        isBirthday = false 
                    )
                    finalMap.getOrPut(startDate) { mutableListOf() }.add(festivo)
                }
            }
        }

        if (continuation.isActive) {
            continuation.resume(finalMap)
        }

    } catch (e: Exception) {
        if (continuation.isActive) {
            Log.e("CalendarDataUtils", "Error al leer festivos del calendario", e)
            continuation.resumeWithException(e)
        }
    }
}
