package com.example.calendario

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.provider.CalendarContract
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import com.example.calendario.database.AppDatabase
import com.example.calendario.database.toFestivo

object AlarmUtils {

    private fun getUniqueRequestCode(eventId: Long, date: LocalDate): Int {
        return ((eventId.toInt() % 10000) * 1000) + date.dayOfYear
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, event: Festivo) {
        val offset = SettingsManager.getEventAlarmOffset(context, event.id, event.adn) ?: return
        
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
                putExtra("event_date", event.date.toString())
                putExtra("event_adn", event.adn)
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
            val snoozeMinutes = SettingsManager.getDefaultSnoozeInterval(context)
            
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
            // Eliminamos FLAG_IMMUTABLE aquí para asegurar que el sistema pueda encontrarlo para cancelar
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, 
                snoozeRequestCode, 
                snoozeIntent, 
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
            )
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

    private fun isEventInSystemCalendar(context: Context, eventId: Long): Boolean {
        if (eventId <= 0) return false
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.query(uri, arrayOf(CalendarContract.Events._ID), null, null, null)?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun rescheduleAllAlarms(context: Context, providedEvents: List<Festivo>? = null): Int {
        val allEvents = providedEvents ?: run {
            val database = AppDatabase.getDatabase(context)
            database.calendarDao().getAllEventsSync().map { it.toFestivo() }
        }
        
        // SALVAGUARDA (v3.1.34): Si la lista está vacía, no limpiamos nada para evitar borrados accidentales 
        // durante cargas intermedias o fallos de permisos.
        if (allEvents.isEmpty()) {
            LogCollector.addLog("ALARMA: Lista vacía. Abortando limpieza por seguridad.")
            return 0
        }
        
        val prefs = SettingsManager.getPrefs(context, AppConstants.ALARM_PREFS_NAME)
        val now = LocalDateTime.now()
        var purgedCount = 0

        // 1. LIMPIEZA DE HISTORIAL Y AUTO-APAGADO
        LogCollector.addLog("ALARMA: Sincronizando ventana...")
        prefs.edit(commit = true) {
            val keysInPrefs = prefs.all.keys.toList()
            keysInPrefs.forEach { eventIdStr ->
                val eventId = eventIdStr.toLongOrNull() ?: return@forEach
                val eventInstances = allEvents.filter { it.id == eventId }
                
                if (eventInstances.isEmpty()) {
                    // Verificar si el evento aún existe en el calendario del sistema antes de purgar la alarma
                    val existsInSystem = isEventInSystemCalendar(context, eventId)
                    if (!existsInSystem) {
                        cancelAlarm(context, eventId)
                        remove(eventIdStr)
                        purgedCount++
                    }
                } else {
                    // El evento existe: Verificamos si el bloque completo ya ha pasado
                    val lastInstance = eventInstances.maxByOrNull { it.date }
                    if (lastInstance != null && lastInstance.rrule == null) {
                        val offset = SettingsManager.getEventAlarmOffset(context, eventId) ?: 20
                        val referenceDateTime = if (lastInstance.isAllDay || lastInstance.startTime == null) {
                            lastInstance.date.atStartOfDay()
                        } else {
                            LocalDateTime.of(lastInstance.date, lastInstance.startTime)
                        }
                        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
                        
                        // REGLA DE ORO (v3.1.34): Solo auto-apagamos si TODO el bloque ha quedado en el pasado
                        if (alarmDateTime.isBefore(now)) {
                            remove(eventIdStr)
                            LogCollector.addLog("ALARMA: Auto-apagado tras fin de evento $eventId")
                        }
                    }
                }
            }
        }

        // 2. SINCRONIZACIÓN DE VENTANA (7 DÍAS para lógica, 2h para Icono)
        LogCollector.addLog("ALARMA: Sincronizando ventana...")
        var count = 0
        allEvents.forEach { event ->
            if (SettingsManager.getEventAlarmOffset(context, event.id, event.adn) != null) {
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
        val offset = SettingsManager.getEventAlarmOffset(context, event.id, event.adn) ?: return false
        
        // SINCRO INTELIGENTE (v3.1.34): En periodos largos, si el ajuste existe en Prefs, 
        // mostramos la campana en todos los días del bloque para dar confianza al usuario.
        if (event.isLongPeriod) return true

        val referenceDateTime = if ((event.isAllDay) || (event.startTime == null)) {
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
        
        // Solo mostramos si la alarma está en el futuro
        return alarmDateTime.isAfter(LocalDateTime.now())
    }

    fun getAlarmTimeString(context: Context, event: Festivo): String? {
        val offset = SettingsManager.getEventAlarmOffset(context, event.id, event.adn) ?: return null
        
        val referenceDateTime = if (event.isAllDay || event.startTime == null) {
            event.date.atStartOfDay()
        } else {
            LocalDateTime.of(event.date, event.startTime)
        }.withSecond(0).withNano(0)

        val alarmDateTime = referenceDateTime.minusMinutes(offset.toLong())
        return alarmDateTime.toLocalTime().format(AppFormats.TimeShort)
    }
}
