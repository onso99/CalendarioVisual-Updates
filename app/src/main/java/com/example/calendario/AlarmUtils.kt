package com.example.calendario

import android.app.AlarmManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmUtils {

    fun saveAlarmSetting(context: Context, eventId: Long, offsetMinutes: Int?) {
        val prefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            if (offsetMinutes == null) {
                remove(eventId.toString())
            } else {
                putInt(eventId.toString(), offsetMinutes)
            }
        }
    }

    fun getAlarmOffset(context: Context, eventId: Long): Int? {
        val prefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(eventId.toString())) {
            prefs.getInt(eventId.toString(), 20)
        } else {
            null
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, event: Festivo) {
        val offset = getAlarmOffset(context, event.id) ?: return
        if (event.isAllDay || event.startTime == null) return

        // Forzamos segundos y nanosegundos a cero para evitar el desfase de 1 minuto
        val eventDateTime = LocalDateTime.of(event.date, event.startTime)
            .withSecond(0)
            .withNano(0)

        val alarmDateTime = eventDateTime.minusMinutes(offset.toLong())
        val alarmMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (alarmMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllAlarms(context: Context) {
        val eventsMap = loadEventsFromPrefs(context)
        val allEvents = eventsMap.values.flatten()
        allEvents.forEach { event ->
            if (getAlarmOffset(context, event.id) != null) {
                scheduleAlarm(context, event)
            }
        }
    }
}
