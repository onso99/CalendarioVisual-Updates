package com.example.calendario

import android.content.ContentUris
import android.content.Context
import androidx.core.content.edit
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
    prefs.edit { putString("events", json) }
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
    prefs.edit { putStringSet("selected_ids", ids.map { it.toString() }.toSet()) }
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
    val prefs = context.getSharedPreferences(AppConstants.HOLIDAY_PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    
    // Unicidad inteligente:
    // 1. Si es manual (eventId null): La clave es Fecha + Título
    // 2. Si es excepción (eventId != null): La clave es Fecha + ID Evento
    val uniqueAdjustments = adjustments.distinctBy { 
        if (it.originalEventId != null) "${it.date}_ID_${it.originalEventId}"
        else "${it.date}_TITLE_${it.title}"
    }

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

fun importHolidaysFromJson(context: Context, uri: Uri, replace: Boolean): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<HolidayAdjustmentDto>>() {}.type
            val importedDtoList: List<HolidayAdjustmentDto> = Gson().fromJson(json, type)
            
            val currentYear = LocalDate.now().year
            val currentAdjustments = loadHolidayAdjustments(context)
            
            val importedAdjustments = importedDtoList.mapNotNull { dto ->
                val date = LocalDate.parse(dto.dateStr)
                if (date.year == currentYear) {
                    HolidayAdjustment(
                        date = date,
                        title = dto.title,
                        type = HolidayAdjustmentType.valueOf(dto.type),
                        originalEventId = dto.originalEventId
                    )
                } else null
            }
            
            val finalList = if (replace) {
                importedAdjustments
            } else {
                // Modo MEZCLAR: El usuario actual tiene preferencia.
                val currentDates = currentAdjustments.map { it.date }.toSet()
                val newOnly = importedAdjustments.filter { it.date !in currentDates }
                currentAdjustments + newOnly
            }

            // Guardamos
            saveHolidayAdjustments(context, finalList)
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
    
    // 0. Cargamos los ajustes de festivos (Manuales y Excepciones)
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
        
        // Mapa temporal para evitar duplicados durante la fragmentación
        // Clave: eventId + hora_inicio
        val tempInstancesMap = mutableMapOf<String, Map<String, Any>>()

        // CONSULTA FRAGMENTADA: +- 2 años en bloques de 3 meses (16 peticiones)
        var windowStart = today.minusYears(2)
        val totalEnd = today.plusYears(2)
        
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
            // 2. Cargamos metadatos en BLOQUES (Mantenemos tu lógica de chunking de 400)
            val uniqueEventIds = tempInstancesData.map { it["eventId"] as Long }.distinct()
            val rruleMap = mutableMapOf<Long, String>()
            val birthYearMap = mutableMapOf<Long, Int>()
            val descMap = mutableMapOf<Long, String>()
            val technicalBirthdayIds = mutableSetOf<Long>()

            uniqueEventIds.chunked(400).forEach { chunk ->
                val eventSelection = "${CalendarContract.Events._ID} IN (${chunk.joinToString(",")})"
                resolver.query(CalendarContract.Events.CONTENT_URI, null, eventSelection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val rruleCol = cursor.getColumnIndex(CalendarContract.Events.RRULE)
                    val startCol = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                    val descCol = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                    val s1Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA1)
                    val s2Col = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA2)
                    val pkgCol = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                    val orgCol = cursor.getColumnIndex(CalendarContract.Events.ORGANIZER)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)

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
                            val year = Instant.ofEpochMilli(dtStartValue).atZone(ZoneId.of("UTC")).toLocalDate().year
                            if (year in 1850..LocalDate.now().year) bYear = year
                        }
                        if (bYear != null && bYear > 1850) birthYearMap[id] = bYear
                    }
                }
            }

            val birthdayKeywords = context.getString(R.string.birthday_keywords).split(",").map { it.trim().lowercase() }
            val greetingKeywords = context.getString(R.string.greeting_keywords).split(",").map { it.trim().lowercase() }

            // 3. Procesamos instancias y aplicamos lógica final
            tempInstancesData.forEach { data ->
                val eventId = data["eventId"] as Long
                val calendarId = data["calendarId"] as Long
                val title = data["title"] as String
                val titleLower = title.lowercase()
                val beginMillis = data["begin"] as Long
                val endMillis = data["end"] as Long
                val isAllDay = data["isAllDay"] as Boolean
                val organizer = data["organizer"] as String
                
                val startDate = Instant.ofEpochMilli(beginMillis).atZone(systemZoneId).toLocalDate()
                val startTime = if (isAllDay) null else Instant.ofEpochMilli(beginMillis).atZone(systemZoneId).toLocalTime()
                val endTime = if (isAllDay) null else Instant.ofEpochMilli(endMillis).atZone(systemZoneId).toLocalTime()

                val isFromHoliday = organizer.contains("#holiday") || organizer.contains("#festivo")
                val isTechnicalBirthday = technicalBirthdayIds.contains(eventId) || organizer.contains("contacts@google.com")
                
                val hasBirthdayWord = birthdayKeywords.any { titleLower.contains(it) }
                val hasGreetingWord = greetingKeywords.any { titleLower.contains(it) }
                
                // Marcamos como azul si es técnico o contiene alguna palabra clave
                val finalIsBirthday = (isTechnicalBirthday || (isAllDay && (hasBirthdayWord || hasGreetingWord))) && !isFromHoliday
                
                var birthYear = birthYearMap[eventId]
                
                if (finalIsBirthday) {
                    // ESCUDO: Si es una felicitación genérica (ej: "Feliz") y NO es contacto oficial,
                    // anulamos el año automático porque suele ser la fecha de creación del evento.
                    if (hasGreetingWord && !isTechnicalBirthday) {
                        birthYear = null
                    }
                    
                    // Fallback: buscamos el año en el texto si no lo tenemos todavía
                    if (birthYear == null) {
                        val yearInTitle = Regex("\\b(19|20)\\d{2}\\b").find(title)?.value?.toIntOrNull()
                        val yearInDesc = Regex("\\b(19|20)\\d{2}\\b").find(descMap[eventId] ?: "")?.value?.toIntOrNull()
                        birthYear = yearInTitle ?: yearInDesc
                    }
                    
                    // Validación final
                    if (birthYear != null && birthYear >= startDate.year) birthYear = null
                }
                
                val age = if (finalIsBirthday && birthYear != null) (startDate.year - birthYear) else null

                finalMap.getOrPut(startDate) { mutableListOf() }.add(Festivo(
                    id = eventId, title = title, description = descMap[eventId],
                    date = startDate, startTime = startTime, endTime = endTime,
                    isAllDay = isAllDay, calendarId = calendarId, isFromHolidaySource = isFromHoliday,
                    rrule = rruleMap[eventId], age = age, isBirthday = finalIsBirthday
                ))
            }
        }
    }

    // 4. Inyectamos los Festivos Manuales del Gestor (Solo para el año en curso)
    val currentYear = today.year
    manualHolidays.forEach { manual ->
        if (manual.date.year == currentYear) {
            finalMap.getOrPut(manual.date) { mutableListOf() }.add(Festivo(
                id = -100L - manual.date.toEpochDay() - manual.title.hashCode().toLong(),
                title = manual.title,
                description = "Festivo manual",
                date = manual.date,
                startTime = null,
                endTime = null,
                isAllDay = true,
                calendarId = -1L,
                isFromHolidaySource = true,
                rrule = null,
                age = null,
                isBirthday = false
            ))
        }
    }
    
    // 5. Ordenación Final: Eventos de día completo > Hora inicio > Título
    finalMap.values.forEach { list ->
        list.sortWith(
            compareBy<Festivo> { !it.isAllDay }
                .thenBy { it.startTime }
                .thenBy { it.title }
        )
    }
    
    LogCollector.addLog("MOTOR: Carga finalizada con ${finalMap.values.flatten().size} eventos (+- 2 años)")
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
