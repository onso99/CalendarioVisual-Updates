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
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmUtils {

    private fun getUniqueRequestCode(eventId: Long, date: LocalDate): Int {
        return ((eventId.toInt() % 10000) * 1000) + date.dayOfYear
    }

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
        
        val referenceDateTime = if ((event.isAllDay) || (event.startTime == null)) {
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
        val alarmMillis = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        if (alarmMillis <= now) return

        // VENTANA DE 3 DÍAS: Evita el spam de alarmas lejanas
        val threeDaysOut = now + (3L * 24 * 60 * 60 * 1000)
        if (alarmMillis > threeDaysOut) return

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = getUniqueRequestCode(event.id, event.date)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "event_alarms_v3",
                "Alarmas del Calendario",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)

            // 1. Intent de disparo
            val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.calendario.ALARM_DISPARO_${event.id}_${event.date}"
                putExtra("event_id", event.id)
                putExtra("event_title", event.title)
            }
            val receiverPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            // 2. Intent de visualización (Usamos el oficial de apertura de la app)
            val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                0, // Código 0 para el intent principal
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val info = AlarmManager.AlarmClockInfo(alarmMillis, showPendingIntent)
            alarmManager.setAlarmClock(info, receiverPendingIntent)
            
            LogCollector.addLog("ALARMA: PROGRAMADA -> '${event.title}' para el ${event.date} a las ${alarmDateTime.toLocalTime()}")
            
        } catch (e: Exception) {
            LogCollector.addLog("ALARMA: ERROR en ID ${event.id}: ${e.message}")
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnooze(context: Context, eventId: Long, title: String) {
        try {
            val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            val snoozeMinutes = prefs.getInt(AppConstants.KEY_DEFAULT_SNOOZE_INTERVAL, 10)
            
            val snoozeTime = System.currentTimeMillis() + (snoozeMinutes.toLong() * 60 * 1000)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = ((eventId.toInt() % 10000) * 1000) + 999 // Código especial para snooze
            
            val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.calendario.ALARM_DISPARO_${eventId}_SNOOZE"
                putExtra("event_id", eventId)
                putExtra("event_title", title)
            }
            val receiverPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val info = AlarmManager.AlarmClockInfo(snoozeTime, showPendingIntent)
            alarmManager.setAlarmClock(info, receiverPendingIntent)
            
            LogCollector.addLog("ALARMA: Pospuesta 10 min para '$title'")
        } catch (e: Exception) {
            LogCollector.addLog("ALARMA: Error al posponer ID $eventId: ${e.message}")
        }
    }

    fun cancelAlarm(context: Context, eventId: Long, date: LocalDate? = null) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // 1. Cancelar Alarma Normal (del día actual o específico)
            val targetDate = date ?: LocalDate.now()
            val requestCode = getUniqueRequestCode(eventId, targetDate)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.calendario.ALARM_DISPARO_${eventId}_$targetDate"
            }
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }

            // 2. Cancelar Snooze (si existiera)
            val snoozeRequestCode = (eventId.toInt() % 10000) * 1000 + 999
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.calendario.ALARM_DISPARO_${eventId}_SNOOZE"
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(context, snoozeRequestCode, snoozeIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (snoozePendingIntent != null) {
                alarmManager.cancel(snoozePendingIntent)
                snoozePendingIntent.cancel()
            }

        } catch (_: Exception) {}
    }

    fun rescheduleAllAlarms(context: Context) {
        val eventsMap = loadEventsFromPrefs(context)
        val allEvents = eventsMap.values.flatten()
        
        // 1. LIMPIEZA DE HISTORIAL: Quitar registros de eventos que ya no existen o son antiguos
        val prefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        val savedEntries = prefs.all
        val today = LocalDate.now()

        prefs.edit(commit = true) {
            savedEntries.keys.forEach { idStr ->
                val id = idStr.toLongOrNull() ?: -1L
                val event = allEvents.find { it.id == id }
                
                // Borramos registro si el evento ya no existe o pasó hace más de 30 días
                if (event == null || event.date.isBefore(today.minusDays(30))) {
                    remove(idStr)
                    if (event == null) cancelAlarm(context, id)
                }
            }
        }

        // 2. SINCRONIZACIÓN DE VENTANA (7 DÍAS)
        LogCollector.addLog("ALARMA: Sincronizando ventana de 7 días...")
        var count = 0
        allEvents.forEach { event ->
            if (getAlarmOffset(context, event.id) != null) {
                // Solo procesamos eventos futuros dentro de la ventana de 7 días
                val eventDateTime = if (event.isAllDay || event.startTime == null) event.date.atStartOfDay() else LocalDateTime.of(event.date, event.startTime)
                if (eventDateTime.isAfter(LocalDateTime.now())) {
                    cancelAlarm(context, event.id, event.date)
                    scheduleAlarm(context, event)
                    count++
                }
            } else {
                cancelAlarm(context, event.id, event.date)
            }
        }
        LogCollector.addLog("ALARMA: Fin sincronización ($count activas)")
    }
}
