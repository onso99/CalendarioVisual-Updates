package com.example.calendario

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// --- GESTIÓN DE BORRADOS (Tombstones) ---

fun markEventAsDeleted(context: Context, eventId: Long) {
    SettingsManager.markEventAsDeleted(context, eventId)
}

fun getDeletedEventIds(context: Context): Set<Long> {
    return SettingsManager.getDeletedEventIds(context)
}

fun clearDeletedEventIds(context: Context) {
    SettingsManager.clearDeletedEventIds(context)
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
    SettingsManager.saveSelectedCalendarIds(context, ids)
}

fun loadSelectedCalendarIds(context: Context): Set<Long> {
    return SettingsManager.getSelectedCalendarIds(context)
}

fun savePeriodColor(context: Context, eventId: Long, colorInt: Int?) {
    SettingsManager.savePeriodColor(context, eventId, colorInt)
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
        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        CalendarContract.Calendars.VISIBLE,
        CalendarContract.Calendars.SYNC_EVENTS
    )

    try {
        // Filtramos directamente en la consulta: 
        // Solo calendarios marcados como VISIBLES y que estén activados para SINCRONIZAR
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1"

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
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
    SettingsManager.saveHolidayAdjustments(context, adjustments)
}

fun loadHolidayAdjustments(context: Context): List<HolidayAdjustment> {
    return SettingsManager.getHolidayAdjustments(context)
}

fun readFestivosFromCalendarsSync(
    context: Context,
    selectedCalendarIds: Set<Long>
): Map<LocalDate, List<Festivo>> {
    val finalMap = mutableMapOf<LocalDate, MutableList<Festivo>>()
    val holidayAdjustments = loadHolidayAdjustments(context)
    val workingDayIds = holidayAdjustments.filter { it.type == HolidayAdjustmentType.WORKING_DAY }.mapNotNull { it.originalEventId }.toSet()
    val workingDayDates = holidayAdjustments.filter { it.type == HolidayAdjustmentType.WORKING_DAY && it.originalEventId == null }.map { it.date }.toSet()
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
            val birthYearMap = mutableMapOf<Long, Int>() // Mapa recuperado
            val descMap = mutableMapOf<Long, String>()
            val technicalBirthdayIds = mutableSetOf<Long>()
            val customColorMap = mutableMapOf<Long, Int?>()

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
                        val internalColor = SettingsManager.getPeriodColor(context, id)
                        val systemColor = if (colorCol != -1 && !cursor.isNull(colorCol)) cursor.getInt(colorCol) else null
                        customColorMap[id] = internalColor ?: systemColor
                        val s1 = if (s1Col != -1) cursor.getStringOrNull(s1Col)?.lowercase() ?: "" else ""
                        val pkg = if (pkgCol != -1) cursor.getStringOrNull(pkgCol)?.lowercase() ?: "" else ""
                        val org = if (orgCol != -1) cursor.getStringOrNull(orgCol)?.lowercase() ?: "" else ""
                        if (pkg.contains("contacts") || org.contains("contacts") || s1.contains("birthday") || pkg.contains("gms") || org.contains("contacts@google.com")) technicalBirthdayIds.add(id)
                        
                        // RECUPERACIÓN DEL AÑO DE NACIMIENTO (Para cálculo de edad)
                        var bYear: Int? = null
                        val s2 = if (s2Col != -1) cursor.getStringOrNull(s2Col) ?: "" else ""
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
            
            val event1Keyword = SettingsManager.getEvent1Keyword(context).unaccent().trim().lowercase()
            val event2Keyword = SettingsManager.getEvent2Keyword(context).unaccent().trim().lowercase()

            // --- GESTIÓN DE CARRILES (Lanes) ---
            val laneAssignments = mutableMapOf<String, Int>()
            val laneOccupancy = mutableMapOf<LocalDate, BooleanArray>()

            val multiDayInstances = tempInstancesData.mapNotNull { data ->
                val eventId = data["eventId"] as Long
                val beginM = data["begin"] as Long
                val endM = data["end"] as Long
                val isAllDay = data["isAllDay"] as Boolean
                
                val startZ = if (isAllDay) Instant.ofEpochMilli(beginM).atZone(java.time.ZoneOffset.UTC) else Instant.ofEpochMilli(beginM).atZone(systemZoneId)
                val endZ = if (isAllDay) Instant.ofEpochMilli(endM).atZone(java.time.ZoneOffset.UTC) else Instant.ofEpochMilli(endM).atZone(systemZoneId)
                val startD = startZ.toLocalDate()
                var endD = endZ.toLocalDate()
                if (endM > beginM && endZ.toLocalTime() == LocalTime.MIDNIGHT) endD = endD.minusDays(1)

                // Lógica de Carriles Unificada (v3.1.34): Solo asignamos carril si el evento es >= 24h
                val duration = java.time.Duration.between(startZ, endZ)
                val isLong = duration.toHours() >= 24

                if (isLong && endD.isAfter(startD)) {
                    val title = (data["title"] as String).lowercase()
                    val organizer = data["organizer"] as String
                    val isH = organizer.contains("#holiday") || organizer.contains("#festivo")
                    val isB = technicalBirthdayIds.contains(eventId) || organizer.contains("contacts@google.com") || (birthdayKeywords.any { title.contains(it) })
                    
                    if (!isH && !isB && !workingDayIds.contains(eventId) && !workingDayDates.contains(startD)) {
                        val uniqueKey = "${eventId}_${beginM}"
                        Triple(uniqueKey, startD, endD)
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
                
                // AJUSTE: Si el ID o la FECHA están marcados como "Laborable", ignoramos el evento por completo
                if (workingDayIds.contains(eventId) || workingDayDates.contains(startDate)) {
                    return@forEach 
                }

                val isFromHoliday = organizer.contains("#holiday") || organizer.contains("#festivo")
                val isTechnicalBirthday = technicalBirthdayIds.contains(eventId) || organizer.contains("contacts@google.com")
                val hasBirthdayWord = birthdayKeywords.any { title.lowercase().contains(it) }
                val hasGreetingWord = greetingKeywords.any { title.lowercase().contains(it) }
                
                // DETECCCIÓN DE EVENTOS PROPIOS (Punto 1 Optimización + Etiquetas Editables)
                val cleanTitleForMatch = title.unaccent().trim().lowercase()
                val finalIsEvent1 = event1Keyword.isNotBlank() && cleanTitleForMatch.contains(event1Keyword)
                val finalIsEvent2 = event2Keyword.isNotBlank() && cleanTitleForMatch.contains(event2Keyword)

                val finalIsBirthday = (isTechnicalBirthday || hasBirthdayWord || hasGreetingWord) && !isFromHoliday && !finalIsEvent1 && !finalIsEvent2
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
                
                // REGLA DE ORO (Corregida v3.1.34): Un evento es periodo largo si dura 24h o más,
                // atraviesa al menos dos días diferentes y no es especial (cumple/festivo)
                val duration = java.time.Duration.between(startZdt, endZdt)
                val isLongPeriod = duration.toHours() >= 24 && endDate.isAfter(startDate) && !finalIsBirthday && !isFromHoliday
                
                // El carril solo se asigna si realmente es un periodo largo
                val assignedLane = if (isLongPeriod) laneAssignments[uniqueKey] else null

                val totalDaysCount = if (isLongPeriod) (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1) else 1
                var currentLoopDate = startDate
                var dayIndex = 1
                
                while (currentLoopDate.isBefore(endDate.plusDays(1))) {
                    val age = if (finalIsBirthday && birthYear != null) (currentLoopDate.year - birthYear) else null
                    val startTimeForAdn = if (currentLoopDate == startDate) startTime else null
                    
                    finalMap.getOrPut(currentLoopDate) { mutableListOf() }.add(Festivo(
                        id = eventId,
                        title = title,
                        description = descMap[eventId],
                        date = currentLoopDate,
                        startTime = startTimeForAdn,
                        endTime = if (currentLoopDate == endDate) endTime else null,
                        isAllDay = isAllDay || (currentLoopDate != startDate && currentLoopDate != endDate),
                        calendarId = calendarId,
                        isFromHolidaySource = isFromHoliday,
                        rrule = rruleMap[eventId],
                        age = age,
                        isBirthday = finalIsBirthday,
                        originalBirthDate = if (finalIsBirthday) birthYear?.let { y -> startDate.withYear(y) } else null,
                        isLongPeriod = isLongPeriod,
                        lane = assignedLane,
                        totalDays = totalDaysCount,
                        currentDay = dayIndex,
                        customColor = customColorMap[eventId],
                        fullStartMillis = beginMillis,
                        fullEndMillis = endMillis,
                        adn = Festivo.generateAdn(currentLoopDate, title, startTimeForAdn)
                    ))
                    currentLoopDate = currentLoopDate.plusDays(1)
                    dayIndex++
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
    
    // Cargar ajustes para filtrado de laborables en la fusión
    val holidayAdjustments = loadHolidayAdjustments(context)
    val workingDayIds = holidayAdjustments.filter { it.type == HolidayAdjustmentType.WORKING_DAY }.mapNotNull { it.originalEventId }.toSet()
    val workingDayDates = holidayAdjustments.filter { it.type == HolidayAdjustmentType.WORKING_DAY && it.originalEventId == null }.map { it.date }.toSet()
    val manualHolidaysAdns = holidayAdjustments.filter { it.type == HolidayAdjustmentType.HOLIDAY && it.originalEventId == null }.associate { it.date to Festivo.generateAdn(it.date, it.title, null) }

    val systemKeys = systemEvents.asSequence().map { it.adn }.toSet()
    val fuzzySystemMap = systemEvents.associateBy { it.fuzzyAdn }
    
    // MAPA DE IDENTIDAD DEL SISTEMA: Para detectar si un registro de Room es obsoleto
    val systemCurrentAdnMap = systemEvents.filter { it.id > 0 }.associateBy { "${it.id}_${it.date}" }
    val systemIds = systemEvents.filter { it.id > 0 }.map { it.id }.toSet()

    (systemEvents + cachedHistory).asSequence()
        .distinctBy { it.adn } // Cada día de evento largo o manual mantiene su ADN único
        .filter { event ->
            // A) Filtro de Seguridad
            if (event.isDeleted || (event.id in deletedIds)) return@filter false
            
            // B) Filtro de Laborables
            if (workingDayIds.contains(event.id) || workingDayDates.contains(event.date)) return@filter false

            // C) SANEAMIENTO DE DUPLICADOS Y SEGMENTOS (v3.1.34):
            if (event.id > 0) {
                // Si el ID existe en el sistema pero este día/ADN concreto NO, es basura de Room (evento acortado)
                if (systemIds.contains(event.id) && !systemKeys.contains(event.adn)) {
                    return@filter false
                }
                
                // Si la versión del sistema para hoy tiene un ADN distinto, Room es obsoleto
                val currentSystemVersion = systemCurrentAdnMap["${event.id}_${event.date}"]
                if (currentSystemVersion != null && currentSystemVersion.adn != event.adn) {
                    return@filter false
                }
            }
            
            // D) SANEAMIENTO DE FESTIVOS MANUALES (ID -1):
            // Evitamos que queden "zombies" de festivos manuales si el ADN ha cambiado (ej. cambio de nombre)
            if (event.id == -1L && event.isFromHolidaySource) {
                val currentManualAdn = manualHolidaysAdns[event.date]
                if (currentManualAdn != null && currentManualAdn != event.adn) {
                    return@filter false
                }
            }

            // E) Saneamiento de huérfanos manuales
            if (!systemKeys.contains(event.adn) && !fuzzySystemMap.containsKey(event.fuzzyAdn)) {
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
