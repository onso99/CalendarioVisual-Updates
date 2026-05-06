package com.example.calendario

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// --- PERSISTENCIA ---

fun saveEventsToPrefs(context: Context, eventsMap: Map<LocalDate, List<Festivo>>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val dtoMap = eventsMap.mapKeys { it.key.toString() }.mapValues { entry ->
        entry.value.map { festivo ->
            FestivoDto(
                id = festivo.id,
                title = festivo.title,
                description = festivo.description,
                startTimeStr = festivo.startTime?.toString(),
                endTimeStr = festivo.endTime?.toString(),
                isAllDay = festivo.isAllDay,
                rrule = festivo.rrule,
                age = festivo.age,
                isBirthday = festivo.isBirthday,
                isFromHolidaySource = festivo.isFromHolidaySource
            )
        }
    }
    val json = gson.toJson(dtoMap)
    prefs.edit().putString("events", json).apply()
}

fun loadEventsFromPrefs(context: Context): Map<LocalDate, List<Festivo>> {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("events", null) ?: return emptyMap()
    val gson = Gson()
    val type = object : TypeToken<Map<String, List<FestivoDto>>>() {}.type
    val dtoMap: Map<String, List<FestivoDto>> = try {
        gson.fromJson(json, type)
    } catch (_: Exception) {
        emptyMap()
    }

    return dtoMap.mapNotNull { (dateStr, dtoList) ->
        val date = try { LocalDate.parse(dateStr) } catch (_: Exception) { null }
        date?.let { validDate ->
            validDate to dtoList.map { dto ->
                Festivo(
                    id = dto.id,
                    title = dto.title ?: "",
                    description = dto.description,
                    date = validDate,
                    startTime = dto.startTimeStr?.let { LocalTime.parse(it) },
                    endTime = dto.endTimeStr?.let { LocalTime.parse(it) },
                    isAllDay = dto.isAllDay,
                    calendarId = -1L,
                    rrule = dto.rrule,
                    age = dto.age,
                    isBirthday = dto.isBirthday ?: false,
                    isFromHolidaySource = dto.isFromHolidaySource ?: false
                )
            }
        }
    }.toMap()
}

fun saveSelectedCalendarIds(context: Context, ids: Set<Long>) {
    val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("selected_ids", ids.map { it.toString() }.toSet()).apply()
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("selected_ids", emptySet())?.map { it.toLong() }?.toSet() ?: emptySet()
}

// --- CALENDARIOS DISPONIBLES ---

suspend fun loadAvailableCalendarsSuspend(context: Context): List<CalendarInfo> = suspendCancellableCoroutine { continuation ->
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        continuation.resume(emptyList())
        return@suspendCancellableCoroutine
    }

    val list = mutableListOf<CalendarInfo>()
    val resolver = context.contentResolver
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.OWNER_ACCOUNT,
        CalendarContract.Calendars.IS_PRIMARY,
        CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
    )

    resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val nameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val accNameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
        val ownerCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
        val primaryCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
        val colorCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
        val accessCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

        while (cursor.moveToNext()) {
            val accessLevel = cursor.getInt(accessCol)
            list.add(
                CalendarInfo(
                    id = cursor.getLong(idCol),
                    displayName = cursor.getString(nameCol),
                    accountName = cursor.getString(accNameCol),
                    ownerAccount = cursor.getString(ownerCol),
                    isPrimary = cursor.getInt(primaryCol) == 1,
                    color = cursor.getInt(colorCol),
                    canModify = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                    accessLevel = accessLevel,
                    isDeleted = false
                )
            )
        }
    }
    continuation.resume(list)
}

// --- AJUSTES Y FESTIVOS ---

fun saveHolidayAdjustments(context: Context, adjustments: List<HolidayAdjustment>) {
    val prefs = context.getSharedPreferences("holiday_adjustments", Context.MODE_PRIVATE)
    val gson = Gson()
    val dtoList = adjustments.map { adj ->
        HolidayAdjustmentDto(
            dateStr = adj.date.toString(),
            title = adj.title,
            type = adj.type.name,
            originalEventId = adj.originalEventId
        )
    }
    prefs.edit().putString("adjustments", gson.toJson(dtoList)).apply()
}

fun loadHolidayAdjustments(context: Context): List<HolidayAdjustment> {
    val prefs = context.getSharedPreferences("holiday_adjustments", Context.MODE_PRIVATE)
    val json = prefs.getString("adjustments", null) ?: return emptyList()
    val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
    val dtoList: List<HolidayAdjustmentDto> = try { Gson().fromJson(json, type) } catch (_: Exception) { emptyList() }
    return dtoList.map { dto ->
        HolidayAdjustment(
            date = LocalDate.parse(dto.dateStr),
            title = dto.title,
            type = HolidayAdjustmentType.valueOf(dto.type),
            originalEventId = dto.originalEventId
        )
    }
}

fun exportHolidaysToJson(context: Context, uri: Uri) {
    val adjustments = loadHolidayAdjustments(context)
    val gson = Gson()
    val json = gson.toJson(adjustments.map { adj ->
        HolidayAdjustmentDto(
            dateStr = adj.date.toString(),
            title = adj.title,
            type = adj.type.name,
            originalEventId = adj.originalEventId
        )
    })
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(json.toByteArray())
    }
}

fun importHolidaysFromJson(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
            val dtoList: List<HolidayAdjustmentDto> = Gson().fromJson(json, type)
            val adjustments = dtoList.map { dto ->
                HolidayAdjustment(
                    date = LocalDate.parse(dto.dateStr),
                    title = dto.title,
                    type = HolidayAdjustmentType.valueOf(dto.type),
                    originalEventId = dto.originalEventId
                )
            }
            saveHolidayAdjustments(context, adjustments)
            true
        } ?: false
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error importing holidays", e)
        false
    }
}

// --- LECTURA DE EVENTOS (MÉTODO MAESTRO) ---

fun readFestivosFromCalendarsSync(
    context: Context,
    selectedCalendarIds: Set<Long>
): Map<LocalDate, List<Festivo>> {
    val finalMap = mutableMapOf<LocalDate, MutableList<Festivo>>()
    if (selectedCalendarIds.isEmpty()) return finalMap
    
    val resolver = context.contentResolver
    val systemZoneId = ZoneId.systemDefault()
    val today = LocalDate.now()
    
    // OPTIMIZACIÓN: Solo leemos los próximos 6 meses para el widget
    val startMillis = today.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
    val endMillis = today.plusMonths(6).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
    
    val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon().run {
        ContentUris.appendId(this, startMillis)
        ContentUris.appendId(this, endMillis)
        build()
    }

    val instancesProjection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.ORGANIZER
    )
    val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"

    val instancesList = mutableListOf<Triple<Long, Long, Long>>() // eventId, begin, end
    val tempInstancesData = mutableListOf<Map<String, Any>>()

    // 1. Obtenemos las instancias del rango reducido
    resolver.query(instancesUri, instancesProjection, selection, null, null)?.use { cursor ->
        val evIdCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val calIdCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
        val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val orgCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ORGANIZER)

        while (cursor.moveToNext()) {
            val eventId = cursor.getLong(evIdCol)
            val begin = cursor.getLong(beginCol)
            val end = cursor.getLong(endCol)
            val data = mapOf(
                "eventId" to eventId,
                "calendarId" to cursor.getLong(calIdCol),
                "title" to (cursor.getStringOrNull(titleCol) ?: ""),
                "begin" to begin,
                "end" to end,
                "isAllDay" to (cursor.getInt(allDayCol) == 1),
                "organizer" to (cursor.getStringOrNull(orgCol)?.lowercase() ?: "")
            )
            tempInstancesData.add(data)
        }
    }

    if (tempInstancesData.isEmpty()) return finalMap

    // 2. Cargamos metadatos SOLO de los eventos encontrados
    val uniqueEventIds = tempInstancesData.map { it["eventId"] as Long }.distinct()
    val rruleMap = mutableMapOf<Long, String>()
    val birthYearMap = mutableMapOf<Long, Int>()
    val descMap = mutableMapOf<Long, String>()
    
    val eventSelection = "${CalendarContract.Events._ID} IN (${uniqueEventIds.joinToString(",")})"
    resolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(
        CalendarContract.Events._ID, CalendarContract.Events.RRULE, 
        CalendarContract.Events.DTSTART, CalendarContract.Events.DESCRIPTION
    ), eventSelection, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
        val rruleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
        val startCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
        val descCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            cursor.getStringOrNull(rruleCol)?.let { rruleMap[id] = it }
            cursor.getStringOrNull(descCol)?.let { descMap[id] = it }
            val dtStart = cursor.getLong(startCol)
            if (dtStart > 0) {
                try {
                    val year = Instant.ofEpochMilli(dtStart).atZone(ZoneId.of("UTC")).toLocalDate().year
                    if (year > 1900) birthYearMap[id] = year
                } catch (_: Exception) {}
            }
        }
    }

    // 3. Procesamos y aplicamos lógica de cumpleaños
    tempInstancesData.forEach { data ->
        val eventId = data["eventId"] as Long
        val calendarId = data["calendarId"] as Long
        val title = data["title"] as String
        val beginMillis = data["begin"] as Long
        val endMillis = data["end"] as Long
        val isAllDay = data["isAllDay"] as Boolean
        val organizer = data["organizer"] as String
        
        val startDate = Instant.ofEpochMilli(beginMillis).atZone(systemZoneId).toLocalDate()
        val startTime = if (isAllDay) null else Instant.ofEpochMilli(beginMillis).atZone(systemZoneId).toLocalTime()
        val endTime = if (isAllDay) null else Instant.ofEpochMilli(endMillis).atZone(systemZoneId).toLocalTime()

        val isFromHoliday = organizer.contains("#holiday") || organizer.contains("#festivo")
        val isTechnicalBirthday = organizer.contains("contacts@google.com")

        val birthdayLabel = context.getString(R.string.birthdays).lowercase()
        val hasBirthdayWord = title.lowercase().contains(birthdayLabel) || title.lowercase().contains("cumple")
        val isYearly = rruleMap[eventId]?.contains("FREQ=YEARLY") ?: false
        
        val finalIsBirthday = (isTechnicalBirthday || (isYearly && isAllDay && hasBirthdayWord)) && !isFromHoliday
        val birthYear = birthYearMap[eventId]
        val age = if (finalIsBirthday && birthYear != null) (startDate.year - birthYear) else null

        finalMap.getOrPut(startDate) { mutableListOf() }.add(Festivo(
            id = eventId, title = title, description = descMap[eventId],
            date = startDate, startTime = startTime, endTime = endTime,
            isAllDay = isAllDay, calendarId = calendarId, isFromHolidaySource = isFromHoliday,
            rrule = rruleMap[eventId], age = age, isBirthday = finalIsBirthday
        ))
    }
    
    LogCollector.addLog("OPTIMIZACIÓN: Procesados ${tempInstancesData.size} eventos (6 meses)")
    return finalMap
}

suspend fun readFestivosFromCalendarsSuspend(
    context: Context,
    selectedCalendarIds: Set<Long>
): Map<LocalDate, List<Festivo>> = suspendCancellableCoroutine { continuation ->
    try {
        val result = readFestivosFromCalendarsSync(context, selectedCalendarIds)
        if (continuation.isActive) continuation.resume(result)
    } catch (e: Exception) {
        if (continuation.isActive) continuation.resumeWithException(e)
    }
}
