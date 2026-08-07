package com.example.calendario

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class BackupSource { DRIVE, LOCAL }
enum class BackupAction { SAVE, RESTORE, SYNC }

data class BackupHistoryEntry(
    val timestamp: Long,
    val source: BackupSource,
    val action: BackupAction,
    val isSuccess: Boolean,
    val eventsCount: Int = 0,
    val notesCount: Int = 0,
    val includePrefs: Boolean = false,
    val sizeBytes: Long = 0,
    val errorMessageRes: Int? = null,
    val technicalError: String? = null
)

object BackupHistoryManager {
    private const val FILE_NAME = "backup_history.json"
    private const val MAX_ENTRIES = 50

    fun addEntry(context: Context, entry: BackupHistoryEntry) {
        val entries = loadEntries(context).toMutableList()
        entries.add(0, entry)
        
        val limitedEntries = entries.take(MAX_ENTRIES)
        saveEntries(context, limitedEntries)
    }

    fun loadEntries(context: Context): List<BackupHistoryEntry> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val result = mutableListOf<BackupHistoryEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(BackupHistoryEntry(
                    timestamp = obj.getLong("time"),
                    source = BackupSource.valueOf(obj.getString("src")),
                    action = BackupAction.valueOf(obj.getString("act")),
                    isSuccess = obj.getBoolean("ok"),
                    eventsCount = obj.optInt("evs", 0),
                    notesCount = obj.optInt("nts", 0),
                    includePrefs = obj.optBoolean("prf", false),
                    sizeBytes = obj.optLong("sz", 0),
                    errorMessageRes = if (obj.has("errRes")) obj.getInt("errRes") else null,
                    technicalError = if (obj.has("tech")) obj.getString("tech") else null
                ))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(context: Context, entries: List<BackupHistoryEntry>) {
        try {
            val array = JSONArray()
            entries.forEach { entry ->
                val obj = JSONObject().apply {
                    put("time", entry.timestamp)
                    put("src", entry.source.name)
                    put("act", entry.action.name)
                    put("ok", entry.isSuccess)
                    put("evs", entry.eventsCount)
                    put("nts", entry.notesCount)
                    put("prf", entry.includePrefs)
                    put("sz", entry.sizeBytes)
                    entry.errorMessageRes?.let { put("errRes", it) }
                    entry.technicalError?.let { put("tech", it) }
                }
                array.put(obj)
            }
            File(context.filesDir, FILE_NAME).writeText(array.toString())
        } catch (_: Exception) {}
    }
}
