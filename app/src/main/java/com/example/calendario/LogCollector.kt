package com.example.calendario

import java.util.concurrent.ConcurrentLinkedQueue
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object LogCollector {
    private val logs = ConcurrentLinkedQueue<String>()
    private val maxLogs = 100
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun addLog(message: String) {
        val timestamp = LocalTime.now().format(formatter)
        val logEntry = "[$timestamp] $message"
        logs.add(logEntry)
        if (logs.size > maxLogs) {
            logs.poll()
        }
    }

    fun getLogs(): String {
        return logs.joinToString("\n")
    }
    
    fun clear() {
        logs.clear()
    }
}
