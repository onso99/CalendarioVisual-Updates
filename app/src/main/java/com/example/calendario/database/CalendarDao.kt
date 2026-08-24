package com.example.calendario.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    // --- EVENTOS ---
    @Query("SELECT * FROM events WHERE isDeleted = 0")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isDeleted = 0")
    fun getAllEventsSync(): List<EventEntity>

    @Query("SELECT * FROM events WHERE date = :date AND isDeleted = 0")
    fun getEventsByDate(date: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM events WHERE adn NOT IN (:validAdns) AND isGhost = 0")
    suspend fun deleteOldEvents(validAdns: List<String>)

    @Transaction
    suspend fun smartRefreshEvents(events: List<EventEntity>) {
        if (events.isEmpty()) {
            clearAllEvents()
        } else {
            // 1. Insertamos o actualizamos los nuevos (Surgical Update)
            upsertEvents(events)
            // 2. Borramos los que ya no están en la lista (Cleanup)
            deleteOldEvents(events.map { it.adn })
        }
    }

    @Transaction
    suspend fun refreshEvents(events: List<EventEntity>) {
        clearAllEvents()
        insertEvents(events)
    }

    @Query("UPDATE events SET isDeleted = 1, lastModified = :timestamp WHERE googleId = :googleId")
    suspend fun markEventAsDeleted(googleId: Long, timestamp: Long)

    @Query("UPDATE events SET isDeleted = 1, lastModified = :timestamp WHERE adn = :adn")
    suspend fun markEventAsDeletedByAdn(adn: String, timestamp: Long)

    @Query("DELETE FROM events WHERE googleId = :googleId")
    suspend fun deleteEventPermanently(googleId: Long)

    // --- NOTAS ---
    @Query("SELECT * FROM notes WHERE isDeleted = 0")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    fun getAllNotesSync(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE dateStr = :dateStr")
    suspend fun getNoteByDate(dateStr: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    @Query("DELETE FROM notes WHERE dateStr = :dateStr")
    suspend fun deleteNotePermanently(dateStr: String)
}
