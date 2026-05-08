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
    
    // RANGO: Ampliado a 2 meses atrás para no perder eventos históricos recientes
    val startMillis = today.minusMonths(2).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
    val endMillis = today.plusYears(2).atStartOfDay(systemZoneId).toInstant().toEpochMilli()
    
    val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon().run {
        ContentUris.appendId(this, startMillis)
        ContentUris.appendId(this, endMillis)
        build()
    }

    val instancesProjection = arrayOf(
        CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
        CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.ORGANIZER
    )
    val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"

    val tempInstancesData = mutableListOf<Map<String, Any>>()

    // 1. Obtenemos las instancias
    resolver.query(instancesUri, instancesProjection, selection, null, null)?.use { cursor ->
        val evIdCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val calIdCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
        val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val orgCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ORGANIZER)

        while (cursor.moveToNext()) {
            tempInstancesData.add(mapOf(
                "eventId" to cursor.getLong(evIdCol),
                "calendarId" to cursor.getLong(calIdCol),
                "title" to (cursor.getStringOrNull(titleCol) ?: ""),
                "begin" to cursor.getLong(beginCol),
                "end" to cursor.getLong(endCol),
                "isAllDay" to (cursor.getInt(allDayCol) == 1),
                "organizer" to (cursor.getStringOrNull(orgCol)?.lowercase() ?: "")
            ))
        }
    }

    if (tempInstancesData.isEmpty()) return finalMap

    // 2. Cargamos metadatos en BLOQUES (Diagnóstico Profundo Integrado)
    val uniqueEventIds = tempInstancesData.map { it["eventId"] as Long }.distinct()
    val rruleMap = mutableMapOf<Long, String>()
    val birthYearMap = mutableMapOf<Long, Int>()
    val descMap = mutableMapOf<Long, String>()
    val technicalBirthdayIds = mutableSetOf<Long>()

    uniqueEventIds.chunked(400).forEach { chunk ->
        val eventSelection = "${CalendarContract.Events._ID} IN (${chunk.joinToString(",")})"
        // Proyección NULL para traer TODAS las columnas posibles en el diagnóstico
        resolver.query(CalendarContract.Events.CONTENT_URI, null, eventSelection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val rruleCol = cursor.getColumnIndex(CalendarContract.Events.RRULE)
            val startCol = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            val descCol = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val s1Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA1)
            val s2Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA2)
            val pkgCol = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_PACKAGE)
            val orgCol = cursor.getColumnIndex(CalendarContract.Events.ORGANIZER)
            val titleCol = cursor.getColumnIndex(CalendarContract.Events.TITLE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val titleInRow = if (titleCol != -1) cursor.getStringOrNull(titleCol) ?: "" else ""
                
                // --- VOLCADO DE "CAJA NEGRA" (Súper Log) ---
                if (titleInRow.lowercase().contains("eda") || titleInRow.lowercase().contains("yia")) {
                    val fullData = mutableListOf<String>()
                    for (i in 0 until cursor.columnCount) {
                        fullData.add("${cursor.getColumnName(i)}=${cursor.getStringOrNull(i)}")
                    }
                    LogCollector.addLog("DIAGNÓSTICO_FULL [$titleInRow]: ${fullData.joinToString(" | ")}")
                }

                if (rruleCol != -1) cursor.getStringOrNull(rruleCol)?.let { rruleMap[id] = it }
                if (descCol != -1) descMap[id] = cursor.getStringOrNull(descCol) ?: ""
                
                val s1 = if (s1Col != -1) cursor.getStringOrNull(s1Col)?.lowercase() ?: "" else ""
                val s2 = if (s2Col != -1) cursor.getStringOrNull(s2Col) ?: "" else ""
                val pkg = if (pkgCol != -1) cursor.getStringOrNull(pkgCol)?.lowercase() ?: "" else ""
                val org = if (orgCol != -1) cursor.getStringOrNull(orgCol)?.lowercase() ?: "" else ""

                if (pkg.contains("contacts") || org.contains("contacts") || s1.contains("birthday") || pkg.contains("gms") || org.contains("contacts@google.com")) {
                    technicalBirthdayIds.add(id)
                }

                var bYear: Int? = null
                if (s2.length >= 4) bYear = Regex("\\b(19|20)\\d{2}\\b").find(s2)?.value?.toIntOrNull()
                
                if ((bYear == null || bYear < 1850) && startCol != -1) {
                    val dtStartValue = cursor.getLong(startCol)
                    // No restringimos a > 0 para permitir fechas antes de 1970 (valor negativo)
                    val year = Instant.ofEpochMilli(dtStartValue).atZone(ZoneId.of("UTC")).toLocalDate().year
                    if (year in 1850..2024) bYear = year
                }
                if (bYear != null && bYear > 1850) birthYearMap[id] = bYear
            }
        }
    }

    // 3. Procesamos instancias y aplicamos lógica final
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
        val isTechnicalBirthday = technicalBirthdayIds.contains(eventId) || organizer.contains("contacts@google.com")
        
        val birthdayLabel = context.getString(R.string.birthdays).lowercase()
        val hasBirthdayWord = title.lowercase().contains(birthdayLabel) || title.lowercase().contains("cumple")
        
        val finalIsBirthday = (isTechnicalBirthday || (isAllDay && hasBirthdayWord)) && !isFromHoliday
        
        var birthYear = birthYearMap[eventId]
        if (finalIsBirthday && birthYear == null) {
            val yearInTitle = Regex("\\b(19|20)\\d{2}\\b").find(title)?.value?.toIntOrNull()
            val yearInDesc = Regex("\\b(19|20)\\d{2}\\b").find(descMap[eventId] ?: "")?.value?.toIntOrNull()
            birthYear = yearInTitle ?: yearInDesc
            if (birthYear != null && birthYear >= startDate.year) birthYear = null
        }
        
        val age = if (finalIsBirthday && birthYear != null) (startDate.year - birthYear) else null

        // LOG DE SEGUIMIENTO PARA EDA/YIA
        if (title.lowercase().contains("eda") || title.lowercase().contains("yia")) {
            LogCollector.addLog("SEGUIMIENTO [$title]: Tech=$isTechnicalBirthday | AñoHallado=$birthYear | Edad=$age")
        }

        finalMap.getOrPut(startDate) { mutableListOf() }.add(Festivo(
            id = eventId, title = title, description = descMap[eventId],
            date = startDate, startTime = startTime, endTime = endTime,
            isAllDay = isAllDay, calendarId = calendarId, isFromHolidaySource = isFromHoliday,
            rrule = rruleMap[eventId], age = age, isBirthday = finalIsBirthday
        ))
    }
    
    LogCollector.addLog("MOTOR: Procesados ${tempInstancesData.size} eventos (Diagnóstico Activo)")
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
