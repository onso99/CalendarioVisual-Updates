package com.example.calendario.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EventEntity::class, NoteEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de 7 a 8: Añadir columna hasIncident a la tabla notes (v3.3.05)
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN hasIncident INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendario_database"
                )
                .addMigrations(MIGRATION_7_8)
                // Eliminamos la migración destructiva automática para proteger los datos (Fase 4.2)
                // .fallbackToDestructiveMigration() 
                .allowMainThreadQueries() 
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
