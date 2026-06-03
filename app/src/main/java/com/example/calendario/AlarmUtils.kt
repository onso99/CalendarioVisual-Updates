package com.example.calendario

import android.app.AlarmManager
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmUtils {

    private const val TAG = "AlarmUtils"

    fun saveAlarmSetting(context: Context, eventId: Long, offsetMinutes: Int?) {
        val prefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        // Usamos commit = true para asegurar que el dato se graba antes de programar la alarma
        prefs.edit(commit = true) {
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
        
        val eventDateTime = if ((event.isAllDay || event.startTime == null)) {
            // Si es todo el día, la referencia es el inicio del día (00:00)
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        // El offset se resta a la referencia. 
        // En eventos todo el día, si el usuario puso 17:30, el offset será negativo (-1050 min)
        // por lo que 00:00 - (-1050) = 17:30.
        val alarmDateTime = eventDateTime.minusMinutes(offset.toLong())
        val alarmMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (alarmMillis <= System.currentTimeMillis()) {
            LogCollector.addLog("ALARMA: No se programa para '${event.title}': la hora ya ha pasado")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 1. Intent para que el AlarmReceiver capture el evento
        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
        }
        val receiverPendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 2. Intent para que el sistema abra nuestra app si el usuario toca el icono de alarma en la barra de estado
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            event.id.toInt(), // ID único para que el icono se registre bien
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val info = AlarmManager.AlarmClockInfo(alarmMillis, showPendingIntent)
        alarmManager.setAlarmClock(info, receiverPendingIntent)
        
        android.widget.Toast.makeText(context, "Alarma: ${event.title} programada", android.widget.Toast.LENGTH_SHORT).show()
        LogCollector.addLog("ALARMA: Programada con éxito para '${event.title}' a las $alarmDateTime")
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
            Log.d(TAG, "Alarma cancelada para evento $eventId")
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
