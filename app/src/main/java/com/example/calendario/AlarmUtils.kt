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

        // VENTANA DE 7 DÍAS: Registramos la alarma en el sistema para que Android gestione el icono
        val sevenDaysOut = now + (7L * 24 * 60 * 60 * 1000)
        if (alarmMillis > sevenDaysOut) return

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Verificación de permiso de sistema (Android 12+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    LogCollector.addLog("ALARMA: Sin permiso EXACT_ALARM para '${event.title}'")
                    return
                }
            }

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

            // 2. Intent de visualización (DNI absoluto para el sistema)
            val showIntent = Intent("android.intent.action.SHOW_ALARMS").apply {
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val info = AlarmManager.AlarmClockInfo(alarmMillis, showPendingIntent)
            alarmManager.setAlarmClock(info, receiverPendingIntent)
            
            val timeStr = alarmDateTime.toLocalTime().toString().take(5)
            LogCollector.addLog("ALARMA: SISTEMA -> '${event.title}' ($timeStr) [Icono solicitado]")
            
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
            
            // 1. Cancelar Alarma Normal (Iteramos por seguridad si no hay fecha)
            // En Android, los PendingIntent se identifican por el par (RequestCode + Intent).
            // Si no tenemos la fecha exacta, el sistema puede dejar alarmas vivas.
            
            if (date != null) {
                cancelAlarmInternal(context, alarmManager, eventId, date)
            } else {
                // Si borramos el evento raíz (desde el listado o historial), 
                // barremos una ventana razonable de 31 días para asegurar limpieza total.
                val today = LocalDate.now()
                for (i in -1..30) {
                    cancelAlarmInternal(context, alarmManager, eventId, today.plusDays(i.toLong()))
                }
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

    private fun cancelAlarmInternal(context: Context, alarmManager: AlarmManager, eventId: Long, date: LocalDate) {
        val requestCode = getUniqueRequestCode(eventId, date)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.calendario.ALARM_DISPARO_${eventId}_$date"
        }
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllAlarms(context: Context): Int {
        val eventsMap = loadEventsFromPrefs(context)
        val allEvents = eventsMap.values.flatten()
        val allEventIds = allEvents.map { it.id.toString() }.toSet()
        
        val prefs = context.getSharedPreferences(AppConstants.ALARM_PREFS_NAME, Context.MODE_PRIVATE)
        val now = LocalDateTime.now()
        var purgedCount = 0

        // 1. LIMPIEZA DE HUÉRFANOS Y HISTORIAL
        LogCollector.addLog("ALARMA: Limpiando alarmas huérfanas...")
        prefs.edit(commit = true) {
            // Buscamos IDs en las preferencias que ya no existan en el calendario real
            val keysInPrefs = prefs.all.keys.toList()
            keysInPrefs.forEach { eventIdStr ->
                val eventId = eventIdStr.toLongOrNull() ?: return@forEach
                
                if (!allEventIds.contains(eventIdStr)) {
                    // El evento ya no existe: Cancelamos en el sistema y borramos rastro
                    cancelAlarm(context, eventId) 
                    remove(eventIdStr)
                    purgedCount++
                    LogCollector.addLog("ALARMA: Purgado ID huérfano $eventId (Probablemente 'Prueba')")
                } else {
                    // El evento existe: Verificamos si ya pasó para auto-apagar el switch
                    val event = allEvents.find { it.id == eventId }
                    if (event != null && event.rrule == null) {
                        val offset = getAlarmOffset(context, eventId) ?: 20
                        val referenceDateTime = if (event.isAllDay || event.startTime == null) {
                            event.date.atStartOfDay()
                        } else {
                            LocalDateTime.of(event.date, event.startTime)
                        }
                        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
                        if (alarmDateTime.isBefore(now)) {
                            remove(eventIdStr)
                        }
                    }
                }
            }
        }

        // 2. SINCRONIZACIÓN DE VENTANA (7 DÍAS para lógica, 2h para Icono)
        LogCollector.addLog("ALARMA: Sincronizando ventana...")
        var count = 0
        allEvents.forEach { event ->
            if (getAlarmOffset(context, event.id) != null) {
                val referenceDateTime = if (event.isAllDay || event.startTime == null) {
                    event.date.atStartOfDay()
                } else {
                    LocalDateTime.of(event.date, event.startTime)
                }
                
                // Solo programamos si es futuro (la función scheduleAlarm ya filtrará las 2h para el icono)
                if (referenceDateTime.isAfter(now)) {
                    cancelAlarm(context, event.id, event.date)
                    scheduleAlarm(context, event)
                    count++
                }
            } else {
                cancelAlarm(context, event.id, event.date)
            }
        }
        LogCollector.addLog("ALARMA: Fin sincronización ($count activas)")
        return purgedCount
    }

    /**
     * Determina si se debe mostrar el icono de campana.
     * Criterio: Switch ON Y (Es futuro O es serie recurrente que aún no ha pasado en el día)
     */
    fun shouldShowAlarmIcon(context: Context, event: Festivo): Boolean {
        val offset = getAlarmOffset(context, event.id) ?: return false
        
        val referenceDateTime = if (event.isAllDay || event.startTime == null) {
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
        
        // Solo mostramos si la alarma está en el futuro
        return alarmDateTime.isAfter(LocalDateTime.now())
    }

    fun getAlarmTimeString(context: Context, event: Festivo): String? {
        val offset = getAlarmOffset(context, event.id) ?: return null
        
        val referenceDateTime = if (event.isAllDay || event.startTime == null) {
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
        return alarmDateTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
}
