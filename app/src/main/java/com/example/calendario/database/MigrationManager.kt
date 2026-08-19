package com.example.calendario.database

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.calendario.AppConstants
import com.example.calendario.loadHistoryFromDisk
import com.example.calendario.loadNotesFromDisk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encargado de trasvasar los datos del antiguo sistema JSON a la nueva base de datos Room.
 */
object MigrationManager {
    private const val KEY_JSON_TO_ROOM_MIGRATED = "json_to_room_migrated"

    suspend fun checkAndMigrate(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val isMigrated = prefs.getBoolean(KEY_JSON_TO_ROOM_MIGRATED, false)

        if (!isMigrated) {
            Log.d("MigrationManager", "Iniciando trasvase de datos JSON a Room...")
            try {
                val dao = database.calendarDao()

                // 1. Migrar Eventos
                val legacyEvents = loadHistoryFromDisk(context)
                if (legacyEvents.isNotEmpty()) {
                    val entities = legacyEvents.map { festivo ->
                        EventEntity(
                            googleId = festivo.id,
                            title = festivo.title,
                            description = festivo.description,
                            date = festivo.date.toString(),
                            startTime = festivo.startTime?.toString(),
                            endTime = festivo.endTime?.toString(),
                            isAllDay = festivo.isAllDay,
                            calendarId = festivo.calendarId,
                            isFromHolidaySource = festivo.isFromHolidaySource,
                            rrule = festivo.rrule,
                            age = festivo.age,
                            isBirthday = festivo.isBirthday,
                            isLongPeriod = festivo.isLongPeriod,
                            lane = festivo.lane,
                            totalDays = festivo.totalDays,
                            currentDay = festivo.currentDay,
                            customColor = festivo.customColor,
                            fullStartMillis = festivo.fullStartMillis,
                            fullEndMillis = festivo.fullEndMillis,
                            repeatCount = festivo.repeatCount,
                            adn = festivo.adn,
                            lastModified = festivo.lastModified,
                            isDeleted = festivo.isDeleted,
                            isGhost = festivo.isGhost
                        )
                    }
                    dao.insertEvents(entities)
                    Log.d("MigrationManager", "Migrados ${entities.size} eventos.")
                }

                // 2. Migrar Notas
                val legacyNotes = loadNotesFromDisk(context)
                if (legacyNotes.isNotEmpty()) {
                    legacyNotes.forEach { note ->
                        dao.insertNote(NoteEntity(
                            dateStr = note.dateStr,
                            content = note.content,
                            lastModified = note.lastModified,
                            isDeleted = note.isDeleted
                        ))
                    }
                    Log.d("MigrationManager", "Migradas ${legacyNotes.size} notas.")
                }

                // 3. Marcar como completado
                prefs.edit(commit = true) {
                    putBoolean(KEY_JSON_TO_ROOM_MIGRATED, true)
                }
                Log.d("MigrationManager", "Trasvase finalizado con éxito.")

            } catch (e: Exception) {
                Log.e("MigrationManager", "Error durante la migración", e)
            }
        }
    }
}
