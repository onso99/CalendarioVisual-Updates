package com.example.calendario

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
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
import java.nio.charset.StandardCharsets
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
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.DELETED
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.DELETED} != 1", 
                null,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.use { 
                val idColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val displayNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountNameColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val ownerAccountColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
                val colorColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val isPrimaryColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
                val accessLevelColumn = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val displayName = it.getString(displayNameColumn) ?: context.getString(R.string.unnamed_calendar)
                    val accountName = it.getString(accountNameColumn) ?: context.getString(R.string.unknown_account)
                    val ownerAccount = it.getString(ownerAccountColumn)
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
                                ownerAccount = ownerAccount,
                                color = colorInt,
                                isPrimary = isPrimary,
                                canModify = canModify,
                                accessLevel = accessLevel,
                                isDeleted = false 
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

fun saveHolidayAdjustments(context: Context, adjustments: List<HolidayAdjustment>) {
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    val dtoList = adjustments.map { 
        HolidayAdjustmentDto(
            dateStr = it.date.toString(),
            type = it.type.name,
            title = it.title,
            originalEventId = it.originalEventId
        )
    }
    prefs.edit {
        putString(AppConstants.KEY_HOLIDAY_ADJUSTMENTS, gson.toJson(dtoList))
    }
}

fun loadHolidayAdjustments(context: Context): List<HolidayAdjustment> {
    val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(AppConstants.KEY_HOLIDAY_ADJUSTMENTS, null) ?: return emptyList()
    val gson = Gson()
    val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
    val dtoList: List<HolidayAdjustmentDto> = try {
        gson.fromJson(json, type)
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error loading holiday adjustments", e)
        emptyList()
    }
    
    return dtoList.mapNotNull { dto ->
        try {
            HolidayAdjustment(
                date = LocalDate.parse(dto.dateStr),
                type = HolidayAdjustmentType.valueOf(dto.type),
                title = dto.title,
                originalEventId = dto.originalEventId
            )
        } catch (e: Exception) {
            null
        }
    }
}

fun exportHolidaysToJson(context: Context, uri: Uri) {
    val adjustments = loadHolidayAdjustments(context)
    val gson = Gson()
    val json = gson.toJson(adjustments.map { 
        HolidayAdjustmentDto(it.date.toString(), it.type.name, it.title, it.originalEventId)
    })
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
    }
}

fun importHolidaysFromJson(context: Context, uri: Uri): Boolean {
    return try {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: return false
        val gson = Gson()
        val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
        val importedDto: List<HolidayAdjustmentDto> = gson.fromJson(json, type)
        
        val currentAdjustments = loadHolidayAdjustments(context).toMutableList()
        importedDto.forEach { dto ->
            val importedAdj = HolidayAdjustment(
                date = LocalDate.parse(dto.dateStr),
                type = HolidayAdjustmentType.valueOf(dto.type),
                title = dto.title,
                originalEventId = dto.originalEventId
            )
            // Avoid duplicates by date and title (or original ID)
            currentAdjustments.removeAll { it.date == importedAdj.date && (it.originalEventId == importedAdj.originalEventId || it.title == importedAdj.title) }
            currentAdjustments.add(importedAdj)
        }
        
        saveHolidayAdjustments(context, currentAdjustments)
        true
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error importing holidays", e)
        false
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

    val holidayCalendarIds = mutableSetOf<Long>()
    val birthdayCalendarIds = mutableSetOf<Long>()
    
    // Identify special calendars by OWNER_ACCOUNT, NAME, DISPLAY_NAME and ACCOUNT_TYPE
    try {
        val calProjection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val calSelection = "${CalendarContract.Calendars._ID} IN (${selectedCalendarIds.joinToString(",")})"
        resolver.query(CalendarContract.Calendars.CONTENT_URI, calProjection, calSelection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val ownerCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
            val nameCol = cursor.getColumnIndex(CalendarContract.Calendars.NAME)
            val displayNameCol = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountTypeCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
            
            val birthdayMarkers = listOf("birthday", "contacts", "cumple", "aniv", "anniv", "gebur", "compl", "födelse", "születés", "doğum", "urodzin")

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rawOwner = cursor.getStringOrNull(ownerCol) ?: ""
                val rawName = if (nameCol != -1) cursor.getStringOrNull(nameCol) ?: "" else ""
                val rawDisplayName = if (displayNameCol != -1) cursor.getStringOrNull(displayNameCol) ?: "" else ""
                val rawAccountType = if (accountTypeCol != -1) cursor.getStringOrNull(accountTypeCol) ?: "" else ""

                val owner = rawOwner.lowercase()
                val name = rawName.lowercase()
                val displayName = rawDisplayName.lowercase()
                val accountType = rawAccountType.lowercase()

                if (owner.contains("#holiday@group.v.calendar.google.com") || 
                    owner.contains("holiday") || 
                    displayName.contains("festivo") || 
                    displayName.contains("holiday")) {
                    holidayCalendarIds.add(id)
                } else if (birthdayMarkers.any { owner.contains(it) } || 
                           birthdayMarkers.any { name.contains(it) } || 
                           birthdayMarkers.any { displayName.contains(it) } || 
                           birthdayMarkers.any { accountType.contains(it) } ||
                           accountType.contains("com.google.android.gms.birthday") ||
                           accountType.contains("com.android.contacts")) {
                    birthdayCalendarIds.add(id)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error identifying special calendars", e)
    }

    val adjustments = loadHolidayAdjustments(context)

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
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.ORGANIZER
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
            val eventsProjection = arrayOf(
                CalendarContract.Events._ID, 
                CalendarContract.Events.RRULE
            )
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
        
        // Track which adjustments have been matched to system events to avoid duplication
        val matchedAdjustmentIndices = mutableSetOf<Int>()

        resolver.query(instancesUri, instancesProjection, instancesSelection, null, null)?.use { cursor ->
            val eventIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calendarIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val organizerColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ORGANIZER)

            while (cursor.moveToNext() && continuation.isActive) {
                val eventId = cursor.getLong(eventIdColumn)
                val calendarId = cursor.getLong(calendarIdColumn)
                val beginMillis = cursor.getLong(beginColumn)
                val endMillis = cursor.getLong(endColumn)
                val title = cursor.getStringOrNull(titleColumn)?.trim() ?: ""
                val isAllDay = cursor.getInt(allDayColumn) == 1
                val organizer = cursor.getStringOrNull(organizerColumn) ?: ""

                if (title.isNotBlank() || isAllDay) { 
                    val startInstant = Instant.ofEpochMilli(beginMillis)
                    val endInstant = Instant.ofEpochMilli(endMillis)
                    val startDate = startInstant.atZone(systemZoneId).toLocalDate()
                    val startTime = if (isAllDay) null else startInstant.atZone(systemZoneId).toLocalTime()
                    val endTime = if (isAllDay) null else endInstant.atZone(systemZoneId).toLocalTime()
                    
                    val isBirthdayCalendar = birthdayCalendarIds.contains(calendarId)
                    val isSystemHolidaySource = holidayCalendarIds.contains(calendarId)
                    
                    // New per-event birthday detection
                    val isBirthdayEvent = isBirthdayCalendar || 
                                          organizer.contains("birthday", ignoreCase = true) ||
                                          organizer.contains("contacts", ignoreCase = true) ||
                                          title.contains("cumpleaños", ignoreCase = true) ||
                                          title.contains("birthday", ignoreCase = true)
                    
                    // Logic for isFromHolidaySource with adjustments (3-level hierarchical matching)
                    var isFromHoliday = isSystemHolidaySource
                    
                    // Level 1: Match by exact originalEventId (same device)
                    // Level 2: Match by Title + Date (same language, different device)
                    // Level 3: Match by "System Holiday" nature + Date (different language and device)
                    val adjIndex = adjustments.indexOfFirst { adj ->
                        adj.date == startDate && (
                            adj.originalEventId == eventId ||
                            adj.title == title ||
                            (isSystemHolidaySource && adj.originalEventId != null)
                        )
                    }
                    
                    if (adjIndex != -1) {
                        val adjustment = adjustments[adjIndex]
                        matchedAdjustmentIndices.add(adjIndex)
                        if (adjustment.type == HolidayAdjustmentType.WORKING_DAY) {
                            continue // Saltamos los eventos marcados como laborables para que no aparezcan en el calendario
                        }
                        isFromHoliday = true
                    }

                    val festivo = Festivo(
                        id = eventId,
                        title = title,
                        description = null,
                        date = startDate,
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = isAllDay,
                        calendarId = calendarId,
                        isFromHolidaySource = isFromHoliday,
                        rrule = rruleMap[eventId],
                        age = null,
                        isBirthday = isBirthdayEvent
                    )
                    finalMap.getOrPut(startDate) { mutableListOf() }.add(festivo)
                }
            }
        }
        
        // Add manual holidays from adjustments that weren't matched to existing system events
        adjustments.forEachIndexed { index, adj ->
            if (adj.type == HolidayAdjustmentType.HOLIDAY && !matchedAdjustmentIndices.contains(index)) {
                // Double check if we already added a manual holiday with this title today
                val exists = finalMap[adj.date]?.any { it.title == adj.title } ?: false
                if (!exists) {
                    val manualFestivo = Festivo(
                        id = -2L, // Artificial ID for manual entries
                        title = adj.title,
                        description = null,
                        date = adj.date,
                        startTime = null,
                        endTime = null,
                        isAllDay = true,
                        calendarId = -2L,
                        isFromHolidaySource = true,
                        rrule = null,
                        age = null,
                        isBirthday = false
                    )
                    finalMap.getOrPut(adj.date) { mutableListOf() }.add(manualFestivo)
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
