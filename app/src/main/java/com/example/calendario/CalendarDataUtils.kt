package com.example.calendario

import android.content.ContentUris
import android.content.Context
import androidx.core.content.edit
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.core.database.getStringOrNull
import com.example.calendario.database.toEntity
import com.example.calendario.database.toFestivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

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

fun removeEventFromHistory(context: Context, eventAdn: String) {
    // Limpieza ROOM (Motor principal)
    @Suppress("OPT_IN_USAGE")
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        val database = com.example.calendario.database.AppDatabase.getDatabase(context)
        database.calendarDao().markEventAsDeletedByAdn(eventAdn, System.currentTimeMillis())
    }
}

fun removeSeriesFromHistory(context: Context, eventId: Long) {
    if (eventId <= 0) return 
    
    // Limpieza ROOM (Motor principal)
    @Suppress("OPT_IN_USAGE")
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        val database = com.example.calendario.database.AppDatabase.getDatabase(context)
        database.calendarDao().markEventAsDeleted(eventId, System.currentTimeMillis())
    }
}

fun saveSelectedCalendarIds(context: Context, ids: Set<Long>) {
    val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    prefs.edit { putStringSet("selected_ids", ids.map { it.toString() }.toSet()) }
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    val rawSet = try {
        prefs.getStringSet("selected_ids", emptySet())
    } catch (_: ClassCastException) {
        val all = prefs.all["selected_ids"]
        if (all is String) {
            all.removeSurrounding("[", "]").split(",").map { it.trim() }.toSet()
        } else emptySet()
    } ?: emptySet()
    return rawSet.mapNotNull { it.toLongOrNull() }.toSet()
}

fun savePeriodColor(context: Context, eventId: Long, colorInt: Int?) {
    val prefs = context.getSharedPreferences(AppConstants.PERIOD_COLOR_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        if (colorInt == null) remove(eventId.toString())
        else putInt(eventId.toString(), colorInt)
    }
}

fun loadAvailableCalendarsSync(context: Context): List<CalendarInfo> {
    val calendars = mutableListOf<CalendarInfo>()
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.OWNER_ACCOUNT,
        CalendarContract.Calendars.IS_PRIMARY,
        CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
    )

    try {
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val ownerCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
            val primaryCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
            val colorCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            val accessCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = cursor.getLong(idCol),
                        displayName = cursor.getString(nameCol),
                        accountName = cursor.getString(accCol),
                        ownerAccount = cursor.getString(ownerCol),
                        isPrimary = cursor.getInt(primaryCol) == 1,
                        color = cursor.getInt(colorCol),
                        canModify = cursor.getInt(accessCol) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                        accessLevel = cursor.getInt(accessCol),
                        isDeleted = false
                    )
                )
            }
        }
    } catch (_: Exception) {}
    return calendars
}

suspend fun loadAvailableCalendarsSuspend(context: Context): List<CalendarInfo> = withContext(Dispatchers.IO) {
    loadAvailableCalendarsSync(context)
}

fun saveHolidayAdjustments(context: Context, adjustments: List<HolidayAdjustment>) {
    val prefs = context.getSharedPreferences(AppConstants.HOLIDAY_ADJUSTMENTS_PREFS_NAME, Context.MODE_PRIVATE)
    val gson = com.google.gson.Gson()
    val json = gson.toJson(adjustments.map { HolidayAdjustmentDto(it.date.toString(), it.title, it.type.name, it.originalEventId) })
    prefs.edit { putString("adjustments", json) }
}

fun loadHolidayAdjustments(context: Context): List<HolidayAdjustment> {
    val prefs = context.getSharedPreferences(AppConstants.HOLIDAY_ADJUSTMENTS_PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString("adjustments", null) ?: return emptyList()
    val gson = com.google.gson.Gson()
    val type = object : com.google.gson.reflect.TypeToken<List<HolidayAdjustmentDto>>() {}.type
    val dtoList: List<HolidayAdjustmentDto> = try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
    return dtoList.map { HolidayAdjustment(LocalDate.parse(it.dateStr), it.title, HolidayAdjustmentType.valueOf(it.type), it.originalEventId) }
}

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
                            "title" to (cursor.getStringOrNull(titleCol)?.take(120) ?: ""),
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
            val descMap = mutableMapOf<Long, String>()
            val technicalBirthdayIds = mutableSetOf<Long>()
            val customColorMap = mutableMapOf<Long, Int?>()
            val internalColorsPrefs = context.getSharedPreferences(AppConstants.PERIOD_COLOR_PREFS_NAME, Context.MODE_PRIVATE)

            uniqueEventIds.chunked(400).forEach { chunk ->
                val eventSelection = "${CalendarContract.Events._ID} IN (${chunk.joinToString(",")})"
                resolver.query(CalendarContract.Events.CONTENT_URI, null, eventSelection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val rruleCol = cursor.getColumnIndex(CalendarContract.Events.RRULE)
                    val descCol = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                    val colorCol = cursor.getColumnIndex(CalendarContract.Events.EVENT_COLOR)
                    val s1Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA1)
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
                        val pkg = if (pkgCol != -1) cursor.getStringOrNull(pkgCol)?.lowercase() ?: "" else ""
                        val org = if (orgCol != -1) cursor.getStringOrNull(orgCol)?.lowercase() ?: "" else ""
                        if (pkg.contains("contacts") || org.contains("contacts") || s1.contains("birthday") || pkg.contains("gms") || org.contains("contacts@google.com")) technicalBirthdayIds.add(id)
                    }
                }
            }

            val birthdayKeywords = context.getString(R.string.birthday_keywords).split(",").map { it.trim().lowercase() }
            val greetingKeywords = context.getString(R.string.greeting_keywords).split(",").map { it.trim().lowercase() }
            
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

                val festivo = Festivo(
                    id = eventId,
                    title = title,
                    description = descMap[eventId],
                    date = startDate,
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    calendarId = calendarId,
                    isFromHolidaySource = isFromHoliday,
                    rrule = rruleMap[eventId],
                    isBirthday = isTechnicalBirthday || hasBirthdayWord || hasGreetingWord,
                    isLongPeriod = endDate.isAfter(startDate),
                    totalDays = if (endDate.isAfter(startDate)) (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1) else 1,
                    currentDay = 1,
                    customColor = customColorMap[eventId],
                    fullStartMillis = beginMillis,
                    fullEndMillis = endMillis,
                    adn = Festivo.generateAdn(startDate, title, startTime)
                )

                if (festivo.isLongPeriod) {
                    for (i in 0 until festivo.totalDays) {
                        val d = startDate.plusDays(i.toLong())
                        finalMap.getOrPut(d) { mutableListOf() }.add(festivo.copy(date = d, currentDay = i + 1))
                    }
                } else {
                    finalMap.getOrPut(startDate) { mutableListOf() }.add(festivo)
                }
            }
        }
    }
    
    manualHolidays.forEach { adj ->
        finalMap.getOrPut(adj.date) { mutableListOf() }.add(Festivo(
            id = -1,
            title = adj.title,
            description = null,
            date = adj.date,
            startTime = null,
            endTime = null,
            isAllDay = true,
            calendarId = -1,
            isFromHolidaySource = true,
            rrule = null,
            adn = Festivo.generateAdn(adj.date, adj.title, null)
        ))
    }

    return finalMap
}

fun readFestivosFromCalendarsSuspend(
    context: Context,
    selectedCalendarIds: Set<Long>
): Map<LocalDate, List<Festivo>> = readFestivosFromCalendarsSync(context, selectedCalendarIds)

suspend fun mergeHistoryWithSystemData(
    context: Context,
    cachedHistory: List<Festivo>,
    systemEvents: List<Festivo>,
    availableCalendars: List<CalendarInfo>
): List<Festivo> = withContext(Dispatchers.Default) {
    val today = LocalDate.now()
    val deletedIds = getDeletedEventIds(context)
    val systemKeys = systemEvents.asSequence().map { it.adn }.toSet()
    val fuzzySystemMap = systemEvents.associateBy { "${it.date}_${it.title.unaccent().trim().lowercase()}" }
    val systemIdMap = systemEvents.associateBy( { "${it.id}_${it.date}" }, { it.adn } )

    (systemEvents + cachedHistory).asSequence()
        .distinctBy { event ->
            val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
            when {
                event.id > 0 && systemIdMap.containsKey("${event.id}_${event.date}") -> systemIdMap["${event.id}_${event.date}"]
                systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey) -> fuzzyKey
                else -> event.adn
            }
        }
        .filter { event ->
            // A) Filtro de Seguridad: No recuperar si está marcado como borrado (físico o lógico)
            if (event.isDeleted || (event.id in deletedIds)) return@filter false
            
            // B) Saneamiento de huérfanos manuales
            if (!systemKeys.contains(event.adn) && !fuzzySystemMap.containsKey("${event.date}_${event.title.unaccent().trim().lowercase()}")) {
                if (event.id < 0) return@filter false
            }
            true
        }
        .filter { it.date.isAfter(today.minusYears(20)) && it.date.isBefore(today.plusYears(6)) }
        .map { event ->
            if (event.id > 0) {
                val fuzzyKey = "${event.date}_${event.title.unaccent().trim().lowercase()}"
                val isPresent = systemKeys.contains(event.adn) || fuzzySystemMap.containsKey(fuzzyKey)
                if (isPresent) event.copy(isGhost = false)
                else {
                    val isWithinYear = !event.date.isBefore(today) && event.date.isBefore(today.plusYears(1))
                    val calendarExists = availableCalendars.any { it.id == event.calendarId }
                    event.copy(isGhost = isWithinYear && !calendarExists && !event.isFromHolidaySource && !event.isBirthday)
                }
            } else event
        }
        .toList()
}
