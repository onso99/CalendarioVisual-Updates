package com.example.calendario

import android.content.ContentUris
import android.content.Context
import androidx.core.content.edit
import android.content.pm.PackageManager
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

// --- PERSISTENCIA HISTÓRICA (JSON) ---

private const val HISTORY_FILE_NAME = "calendar_history_v2.json"

fun saveHistoryToDisk(context: Context, events: List<Festivo>) {
    try {
        val gson = Gson()
        val json = gson.toJson(events.map { it.toDto() })
        context.openFileOutput(HISTORY_FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error saving history", e)
    }
}

fun loadHistoryFromDisk(context: Context): List<Festivo> {
    return try {
        val file = context.getFileStreamPath(HISTORY_FILE_NAME)
        if (!file.exists()) return emptyList()
        val json = context.openFileInput(HISTORY_FILE_NAME).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<FestivoDto>>() {}.type
        val dtos: List<FestivoDto> = Gson().fromJson(json, type) ?: emptyList()
        dtos.map { it.toFestivo() }
    } catch (e: Exception) {
        Log.e("CalendarDataUtils", "Error loading history", e)
        emptyList()
    }
}

// --- CONVERSORES DTO ---

fun Festivo.toDto() = FestivoDto(
    id = this.id,
    calendarId = this.calendarId,
    title = this.title,
    description = this.description,
    startTimeStr = this.startTime?.toString(),
    endTimeStr = this.endTime?.toString(),
    isAllDay = this.isAllDay,
    rrule = this.rrule,
    age = this.age,
    isBirthday = this.isBirthday,
    isFromHolidaySource = this.isFromHolidaySource,
    isLongPeriod = this.isLongPeriod,
    lane = this.lane,
    totalDays = this.totalDays,
    currentDay = this.currentDay,
    customColor = this.customColor,
    fullStartMillis = this.fullStartMillis,
    fullEndMillis = this.fullEndMillis,
    repeatCount = this.repeatCount,
    lastModified = this.lastModified,
    isDeleted = this.isDeleted
)

fun FestivoDto.toFestivo() = Festivo(
    id = this.id,
    calendarId = this.calendarId ?: 0L,
    title = this.title ?: "",
    description = this.description,
    date = LocalDate.now(),
    startTime = this.startTimeStr?.let { LocalTime.parse(it) },
    endTime = this.endTimeStr?.let { LocalTime.parse(it) },
    isAllDay = this.isAllDay,
    isFromHolidaySource = this.isFromHolidaySource ?: false,
    rrule = this.rrule,
    age = this.age,
    isBirthday = this.isBirthday ?: false,
    isLongPeriod = this.isLongPeriod ?: false,
    lane = this.lane,
    totalDays = this.totalDays ?: 1,
    currentDay = this.currentDay ?: 1,
    customColor = this.customColor,
    fullStartMillis = this.fullStartMillis,
    fullEndMillis = this.fullEndMillis,
    repeatCount = this.repeatCount,
    lastModified = this.lastModified ?: System.currentTimeMillis(),
    isDeleted = this.isDeleted ?: false
).let { 
    if (this.fullStartMillis != null) {
        it.copy(date = Instant.ofEpochMilli(this.fullStartMillis).atZone(ZoneId.systemDefault()).toLocalDate())
    } else it
}

// --- GESTIÓN DE BORRADOS (Tombstones) ---

private const val DELETED_EVENTS_PREFS = "deleted_events_prefs"

fun markEventAsDeleted(context: Context, eventId: Long) {
    val prefs = context.getSharedPreferences(DELETED_EVENTS_PREFS, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(eventId.toString(), true) }
}

fun getDeletedEventIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences(DELETED_EVENTS_PREFS, Context.MODE_PRIVATE)
    return prefs.all.keys.mapNotNull { it.toLongOrNull() }.toSet()
}

fun clearDeletedEventIds(context: Context) {
    context.getSharedPreferences(DELETED_EVENTS_PREFS, Context.MODE_PRIVATE).edit { clear() }
}

/**
 * Fusión Inteligente (Incremental): Combina local y remoto.
 * Devuelve un par con la lista final y el número de borrados detectados.
 */
fun mergeHistoryLists(context: Context, local: List<Festivo>, remote: List<Festivo>): Pair<List<Festivo>, Int> {
    val deletedIds = getDeletedEventIds(context)
    val allEvents = (local + remote).groupBy { it.id }
    
    val result = mutableListOf<Festivo>()
    var purgedCount = 0

    allEvents.forEach { (id, versions) ->
        val newest = versions.maxByOrNull { it.lastModified }
        if (newest != null) {
            if (newest.isDeleted || id in deletedIds) {
                purgedCount++
            } else {
                result.add(newest)
            }
        }
    }
    return Pair(result, purgedCount)
}

// --- PERSISTENCIA COMPATIBILIDAD (SharedPreferences) ---

fun saveEventsToPrefs(context: Context, eventsMap: Map<LocalDate, List<Festivo>>) {
    val prefs = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val dtoMap = eventsMap.mapKeys { it.key.toString() }.mapValues { entry ->
        entry.value.map { it.toDto() }
    }
    prefs.edit { putString("events", gson.toJson(dtoMap)) }
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
            validDate to dtoList.map { it.toFestivo().copy(date = validDate) }
        }
    }.toMap()
}

fun saveSelectedCalendarIds(context: Context, ids: Set<Long>) {
    val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    prefs.edit { putStringSet("selected_ids", ids.map { it.toString() }.toSet()) }
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("selected_ids", emptySet())?.map { it.toLong() }?.toSet() ?: emptySet()
}

// --- PERSISTENCIA DE COLORES DE PERIODOS ---

fun savePeriodColor(context: Context, eventId: Long, colorInt: Int?) {
    val prefs = context.getSharedPreferences(AppConstants.PERIOD_COLOR_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        if (colorInt == null) remove(eventId.toString())
        else putInt(eventId.toString(), colorInt)
    }
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
    val prefs = context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()

    // UNICIDAD ABSOLUTA POR FECHA: Un solo ajuste por día. El último gana.
    val uniqueAdjustments = adjustments.asReversed().distinctBy { it.date }.reversed()

    val dtoList = uniqueAdjustments.map { adj ->
        HolidayAdjustmentDto(
            dateStr = adj.date.toString(),
            title = adj.title,
            type = adj.type.name,
            originalEventId = adj.originalEventId
        )
    }
    prefs.edit { putString(AppConstants.KEY_HOLIDAY_ADJUSTMENTS, gson.toJson(dtoList)) }
}

fun loadHolidayAdjustments(context: Context): List<HolidayAdjustment> {
    val prefs = context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(AppConstants.KEY_HOLIDAY_ADJUSTMENTS, null) ?: return emptyList()
    val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
    val dtoList: List<HolidayAdjustmentDto> = try { Gson().fromJson(json, type) } catch (_: Exception) { emptyList() }
    
    val adjustments = dtoList.mapNotNull { dto ->
        try {
            HolidayAdjustment(
                date = LocalDate.parse(dto.dateStr),
                title = dto.title,
                type = HolidayAdjustmentType.valueOf(dto.type),
                originalEventId = dto.originalEventId
            )
        } catch (_: Exception) { null }
    }

    // AUTOLIMPIEZA: Asegurar que el disco esté sano al cargar
    val cleaned = adjustments.asReversed().distinctBy { it.date }.reversed()

    if (cleaned.size < adjustments.size) {
        saveHolidayAdjustments(context, cleaned)
    }

    return cleaned
}



// --- LECTURA DE EVENTOS (MÉTODO MAESTRO) ---

fun readFestivosFromCalendarsSync(
    context: Context,
    selectedCalendarIds: Set<Long>
): Map<LocalDate, List<Festivo>> {
    val finalMap = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val holidayAdjustments = loadHolidayAdjustments(context)
    val workingDayIds = holidayAdjustments.filter { it.type == HolidayAdjustmentType.WORKING_DAY }.mapNotNull { it.originalEventId }.toSet()
    val manualHolidays = holidayAdjustments.filter { it.type == HolidayAdjustmentType.HOLIDAY && it.originalEventId == null }

    if (selectedCalendarIds.isEmpty() && manualHolidays.isEmpty()) return finalMap
    
    val resolver = context.contentResolver
    val systemZoneId = ZoneId.systemDefault()
    val today = LocalDate.now()
    
    if (selectedCalendarIds.isNotEmpty()) {
        val instancesProjection = arrayOf(
            CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.ORGANIZER
        )
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"
        val tempInstancesMap = mutableMapOf<String, Map<String, Any>>()
        var windowStart = today.minusYears(1)
        val totalEnd = today.plusYears(5)
        
        while (windowStart.isBefore(totalEnd)) {
            val windowEnd = windowStart.plusMonths(3).run { if (isAfter(totalEnd)) totalEnd else this }
            val startMillis = windowStart.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
            val endMillis = windowEnd.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
            val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon().run {
                ContentUris.appendId(this, startMillis)
                ContentUris.appendId(this, endMillis)
                build()
            }
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
                    if (workingDayIds.contains(eventId)) continue
                    val startM = cursor.getLong(beginCol)
                    val uniqueKey = "${eventId}_${startM}"
                    if (!tempInstancesMap.containsKey(uniqueKey)) {
                        tempInstancesMap[uniqueKey] = mapOf(
                            "eventId" to eventId,
                            "calendarId" to cursor.getLong(calIdCol),
                            "title" to (cursor.getStringOrNull(titleCol) ?: ""),
                            "begin" to startM,
                            "end" to cursor.getLong(endCol),
                            "isAllDay" to (cursor.getInt(allDayCol) == 1),
                            "organizer" to (cursor.getStringOrNull(orgCol)?.lowercase() ?: "")
                        )
                    }
                }
            }
            windowStart = windowEnd
        }

        if (tempInstancesMap.isNotEmpty()) {
            val tempInstancesData = tempInstancesMap.values
            val uniqueEventIds = tempInstancesData.map { it["eventId"] as Long }.distinct()
            val rruleMap = mutableMapOf<Long, String>()
            val birthYearMap = mutableMapOf<Long, Int>()
            val descMap = mutableMapOf<Long, String>()
            val technicalBirthdayIds = mutableSetOf<Long>()
            val customColorMap = mutableMapOf<Long, Int?>()
            val internalColorsPrefs = context.getSharedPreferences(AppConstants.PERIOD_COLOR_PREFS_NAME, Context.MODE_PRIVATE)

            uniqueEventIds.chunked(400).forEach { chunk ->
                val eventSelection = "${CalendarContract.Events._ID} IN (${chunk.joinToString(",")})"
                resolver.query(CalendarContract.Events.CONTENT_URI, null, eventSelection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val rruleCol = cursor.getColumnIndex(CalendarContract.Events.RRULE)
                    val startCol = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                    val descCol = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                    val colorCol = cursor.getColumnIndex(CalendarContract.Events.EVENT_COLOR)
                    val s1Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA1)
                    val s2Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA2)
                    val pkgCol = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                    val orgCol = cursor.getColumnIndex(CalendarContract.Events.ORGANIZER)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        if (rruleCol != -1) cursor.getStringOrNull(rruleCol)?.let { rruleMap[id] = it }
                        if (descCol != -1) descMap[id] = cursor.getStringOrNull(descCol) ?: ""
                        val internalColor = if (internalColorsPrefs.contains(id.toString())) internalColorsPrefs.getInt(id.toString(), 0) else null
                        val systemColor = if (colorCol != -1 && !cursor.isNull(colorCol)) cursor.getInt(colorCol) else null
                        customColorMap[id] = internalColor ?: systemColor
                        val s1 = if (s1Col != -1) cursor.getStringOrNull(s1Col)?.lowercase() ?: "" else ""
                        val s2 = if (s2Col != -1) cursor.getStringOrNull(s2Col) ?: "" else ""
                        val pkg = if (pkgCol != -1) cursor.getStringOrNull(pkgCol)?.lowercase() ?: "" else ""
                        val org = if (orgCol != -1) cursor.getStringOrNull(orgCol)?.lowercase() ?: "" else ""
                        if (pkg.contains("contacts") || org.contains("contacts") || s1.contains("birthday") || pkg.contains("gms") || org.contains("contacts@google.com")) technicalBirthdayIds.add(id)
                        var bYear: Int? = null
                        if (s2.length >= 4) bYear = Regex("\\b(19|20)\\d{2}\\b").find(s2)?.value?.toIntOrNull()
                        if ((bYear == null || bYear < 1850) && startCol != -1) {
                            val dtStartValue = cursor.getLong(startCol)
                            val year = Instant.ofEpochMilli(dtStartValue).atZone(ZoneId.of("UTC")).toLocalDate().year
                            if (year in 1850..LocalDate.now().year) bYear = year
                        }
                        if (bYear != null && bYear > 1850) birthYearMap[id] = bYear
                    }
                }
            }

            val birthdayKeywords = context.getString(R.string.birthday_keywords).split(",").map { it.trim().lowercase() }
            val greetingKeywords = context.getString(R.string.greeting_keywords).split(",").map { it.trim().lowercase() }
            val laneAssignments = mutableMapOf<String, Int>()
            val laneOccupancy = mutableMapOf<LocalDate, BooleanArray>()

            val multiDayInstances = tempInstancesData.mapNotNull { data ->
                val beginMillis = data["begin"] as Long
                val endMillis = data["end"] as Long
                val isAllDay = data["isAllDay"] as Boolean
                val startZdt = if (isAllDay) Instant.ofEpochMilli(beginMillis).atZone(java.time.ZoneOffset.UTC) else Instant.ofEpochMilli(beginMillis).atZone(systemZoneId)
                val endZdt = if (isAllDay) Instant.ofEpochMilli(endMillis).atZone(java.time.ZoneOffset.UTC) else Instant.ofEpochMilli(endMillis).atZone(systemZoneId)
                val startDate = startZdt.toLocalDate()
                var endDate = endZdt.toLocalDate()
                if (endMillis > beginMillis && endZdt.toLocalTime() == LocalTime.MIDNIGHT) endDate = endDate.minusDays(1)
                if (endDate.isAfter(startDate)) {
                    val eventId = data["eventId"] as Long
                    val organizer = data["organizer"] as String
                    val title = (data["title"] as String).lowercase()
                    val isHoliday = organizer.contains("#holiday") || organizer.contains("#festivo")
                    val isBirthday = (technicalBirthdayIds.contains(eventId) || organizer.contains("contacts@google.com") || (isAllDay && birthdayKeywords.any { title.contains(it) }))
                    if (!isHoliday && !isBirthday) {
                        val uniqueKey = "${eventId}_${beginMillis}"
                        Triple(uniqueKey, startDate, endDate)
                    } else null
                } else null
            }.sortedWith(compareBy({ it.second }, { it.third }, { it.first }))

            multiDayInstances.forEach { (uniqueKey, start, end) ->
                var chosenLane = -1
                for (l in 0..4) {
                    var isFree = true
                    var d = start
                    while (!d.isAfter(end)) {
                        if (laneOccupancy[d]?.get(l) == true) { isFree = false; break }
                        d = d.plusDays(1)
                    }
                    if (isFree) { chosenLane = l; break }
                }
                if (chosenLane != -1) {
                    laneAssignments[uniqueKey] = chosenLane
                    var d = start
                    while (!d.isAfter(end)) {
                        laneOccupancy.getOrPut(d) { BooleanArray(5) }[chosenLane] = true
                        d = d.plusDays(1)
                    }
                }
            }

            tempInstancesData.forEach { data ->
                val eventId = data["eventId"] as Long
                val calendarId = data["calendarId"] as Long
                val title = data["title"] as String
                val beginMillis = data["begin"] as Long
                val endMillis = data["end"] as Long
                val isAllDay = data["isAllDay"] as Boolean
                val organizer = data["organizer"] as String
                val startZdt = if (isAllDay) Instant.ofEpochMilli(beginMillis).atZone(java.time.ZoneOffset.UTC) else Instant.ofEpochMilli(beginMillis).atZone(systemZoneId)
                val endZdt = if (isAllDay) Instant.ofEpochMilli(endMillis).atZone(java.time.ZoneOffset.UTC) else Instant.ofEpochMilli(endMillis).atZone(systemZoneId)
                val startDate = startZdt.toLocalDate()
                var endDate = endZdt.toLocalDate()
                if (endMillis > beginMillis && endZdt.toLocalTime() == LocalTime.MIDNIGHT) endDate = endDate.minusDays(1)
                val startTime = if (isAllDay) null else startZdt.toLocalTime()
                val endTime = if (isAllDay) null else endZdt.toLocalTime()
                val isFromHoliday = organizer.contains("#holiday") || organizer.contains("#festivo")
                val isTechnicalBirthday = technicalBirthdayIds.contains(eventId) || organizer.contains("contacts@google.com")
                val hasBirthdayWord = birthdayKeywords.any { title.lowercase().contains(it) }
                val hasGreetingWord = greetingKeywords.any { title.lowercase().contains(it) }
                val finalIsBirthday = (isTechnicalBirthday || (isAllDay && (hasBirthdayWord || hasGreetingWord))) && !isFromHoliday
                var birthYear = birthYearMap[eventId]
                if (finalIsBirthday) {
                    if (hasGreetingWord && !isTechnicalBirthday) birthYear = null
                    if (birthYear == null) {
                        val yearInTitle = Regex("\\b(19|20)\\d{2}\\b").find(title)?.value?.toIntOrNull()
                        val yearInDesc = Regex("\\b(19|20)\\d{2}\\b").find(descMap[eventId] ?: "")?.value?.toIntOrNull()
                        birthYear = yearInTitle ?: yearInDesc
                    }
                    if (birthYear != null && birthYear >= startDate.year) birthYear = null
                }
                val uniqueKey = "${eventId}_${beginMillis}"
                val assignedLane = laneAssignments[uniqueKey]
                val assignedRrule = rruleMap[eventId]
                val extractedCount = assignedRrule?.let { if (it.contains("COUNT=")) it.substringAfter("COUNT=").substringBefore(";").toIntOrNull() else null }
                
                // REGLA DE ORO: Un evento solo es periodo largo si dura mÃ¡s de 24 horas y no es cumpleaÃ±os ni festivo
                val duration = java.time.Duration.between(startZdt, endZdt)
                val isLongPeriod = duration.toHours() > 24 && !finalIsBirthday && !isFromHoliday

                val totalDaysCount = if (isLongPeriod) (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1) else 1
                var currentLoopDate = startDate
                var dayIndex = 1
                while (!currentLoopDate.isAfter(endDate)) {
                    val age = if (finalIsBirthday && birthYear != null) (currentLoopDate.year - birthYear) else null
                    finalMap.getOrPut(currentLoopDate) { mutableListOf() }.add(Festivo(
                        id = eventId, title = title, description = descMap[eventId],
                        date = currentLoopDate, startTime = if (currentLoopDate == startDate) startTime else null, 
                        endTime = if (currentLoopDate == endDate) endTime else null,
                        isAllDay = isAllDay || (currentLoopDate != startDate && currentLoopDate != endDate),
                        calendarId = calendarId, isFromHolidaySource = isFromHoliday,
                        rrule = rruleMap[eventId], age = age, isBirthday = finalIsBirthday,
                        isLongPeriod = isLongPeriod, lane = assignedLane, totalDays = totalDaysCount, currentDay = dayIndex,
                        customColor = customColorMap[eventId], fullStartMillis = beginMillis, fullEndMillis = endMillis, repeatCount = extractedCount
                    ))
                    currentLoopDate = currentLoopDate.plusDays(1)
                    dayIndex++
                }
            }
        }
    }

    manualHolidays.forEach { manual ->
        finalMap.getOrPut(manual.date) { mutableListOf() }.add(Festivo(
            id = -100L - manual.date.toEpochDay() - manual.title.hashCode().toLong(),
            title = manual.title, description = "Festivo manual", date = manual.date,
            startTime = null, endTime = null, isAllDay = true, calendarId = -1L,
            isFromHolidaySource = true, rrule = null, age = null, isBirthday = false
        ))
    }
    
    finalMap.values.forEach { list ->
        list.sortWith(compareBy<Festivo> { !it.isAllDay }.thenBy { it.startTime }.thenBy { it.title })
    }
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
