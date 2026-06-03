package com.example.calendario

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmUtils {

    fun saveAlarmSetting(context: Context, eventId: Long, offsetMinutes: Int?) {
        val prefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)
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
        
        val referenceDateTime = if ((event.isAllDay || event.startTime == null)) {
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
        val alarmMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        if (alarmMillis <= now) {
            LogCollector.addLog("ALARMA: No se programa para '${event.title}': la hora ya pasó ($alarmDateTime)")
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // DIAGNÓSTICO DE PERMISOS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val canSchedule = alarmManager.canScheduleExactAlarms()
                LogCollector.addLog("ALARMA: Comprobando permiso S+ -> canSchedule: $canSchedule")
            }

            // Pre-creamos el canal (paso necesario para que el sistema valide la importancia)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "event_alarms_v3",
                "Alarmas del Calendario",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)

            // 1. Intent para el disparo
            val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.calendario.ALARM_DISPARO_${event.id}"
                addCategory("android.intent.category.ALARM")
                putExtra("event_id", event.id)
                putExtra("event_title", event.title)
            }
            val receiverPendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.toInt(),
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2. Intent para la barra de estado (Abrir app)
            val showIntent = Intent(context, MainActivity::class.java).apply {
                // Usamos la acción estándar de Android para mostrar alarmas
                action = android.provider.AlarmClock.ACTION_SHOW_ALARMS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                event.id.toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val info = AlarmManager.AlarmClockInfo(alarmMillis, showPendingIntent)
            alarmManager.setAlarmClock(info, receiverPendingIntent)
            
            LogCollector.addLog("ALARMA: OK -> '${event.title}' en ${(alarmMillis - now)/1000} seg. (ID: ${event.id})")
            
        } catch (e: SecurityException) {
            LogCollector.addLog("ALARMA: ERROR -> Permiso denegado por el sistema: ${e.message}")
        } catch (e: Exception) {
            LogCollector.addLog("ALARMA: ERROR -> Excepción inesperada: ${e.message}")
        }
    }

    fun cancelAlarm(context: Context, eventId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.calendario.ALARM_DISPARO_$eventId"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                LogCollector.addLog("ALARMA: Cancelada ID $eventId")
            }
        } catch (e: Exception) {
            LogCollector.addLog("ALARMA: Error al cancelar ID $eventId: ${e.message}")
        }
    }

    fun rescheduleAllAlarms(context: Context) {
        val eventsMap = loadEventsFromPrefs(context)
        val allEvents = eventsMap.values.flatten()
        LogCollector.addLog("ALARMA: Reprogramando ${allEvents.size} posibles alarmas...")
        allEvents.forEach { event ->
            if (getAlarmOffset(context, event.id) != null) {
                scheduleAlarm(context, event)
            }
        }
    }
}
