package com.example.calendario.database

import android.content.Context
import android.util.Log
import com.example.calendario.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encargado de trasvasar los datos del antiguo sistema JSON a la nueva base de datos Room.
 */
object MigrationManager {

    suspend fun checkAndMigrate(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val isMigrated = com.example.calendario.SettingsManager.isJsonToRoomMigrated(context)

        if (!isMigrated) {
            Log.d("MigrationManager", "Iniciando trasvase de datos JSON a Room...")
            try {
                val dao = database.calendarDao()

                // 1. Migrar Eventos
                val legacyEvents = BackupManager.getEventsFromLegacyJson(context)
                if (legacyEvents.isNotEmpty()) {
                    val entities = legacyEvents.map { festivo ->
                        EventEntity(
                            adn = festivo.adn,
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
                            originalBirthDate = null, // JSON legado no guardaba este dato
                            isLongPeriod = festivo.isLongPeriod,
                            lane = festivo.lane,
                            totalDays = festivo.totalDays,
                            currentDay = festivo.currentDay,
                            customColor = festivo.customColor,
                            fullStartMillis = festivo.fullStartMillis,
                            fullEndMillis = festivo.fullEndMillis,
                            repeatCount = festivo.repeatCount,
                            lastModified = festivo.lastModified,
                            isDeleted = festivo.isDeleted,
                            isGhost = festivo.isGhost
                        )
                    }
                    dao.insertEvents(entities)
                    Log.d("MigrationManager", "Migrados ${entities.size} eventos.")
                }

                // 2. Migrar Notas
                val legacyNotes = BackupManager.getNotesFromLegacyJson(context)
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
                com.example.calendario.SettingsManager.setJsonToRoomMigrated(context)
                Log.d("MigrationManager", "Trasvase finalizado con éxito.")

            } catch (e: Exception) {
                Log.e("MigrationManager", "Error durante la migración", e)
            }
        }
    }
}
