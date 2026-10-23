package com.example.calendario

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.example.calendario.database.AppDatabase

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP = "com.example.calendario.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE = "com.example.calendario.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_UPCOMING = "com.example.calendario.ACTION_DISMISS_UPCOMING"
        const val ACTION_PRE_ALARM_NOTICE = "com.example.calendario.ACTION_PRE_ALARM_NOTICE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmUtils.rescheduleAllAlarms(context)
            return
        }

        val eventId = intent.getLongExtra("event_id", -1L)
        val eventTitle = intent.getStringExtra("event_title") ?: "Evento"

        when (intent.action) {
            ACTION_PRE_ALARM_NOTICE -> {
                val dateStr = intent.getStringExtra("event_date") ?: ""
                val timeStr = intent.getStringExtra("event_time_str") ?: ""
                if (eventId != -1L && dateStr.isNotBlank() && SettingsManager.isAlarmDismissed(context, eventId, dateStr)) {
                    LogCollector.addLog("AVISO PREVIO: Omitido -> '$eventTitle' (Alarma apagada previamente)")
                    return
                }
                showPreAlarmNotification(context, eventId, eventTitle, dateStr, timeStr)
            }
            ACTION_DISMISS_UPCOMING -> {
                val dateStr = intent.getStringExtra("event_date") ?: ""
                LogCollector.addLog("ALARMA: Apagada por adelantado desde notificación ('$eventTitle' $dateStr)")
                if (eventId != -1L) {
                    AlarmUtils.cancelAlarm(context, eventId)
                    if (dateStr.isNotBlank()) {
                        SettingsManager.saveDismissedAlarm(context, eventId, dateStr)
                    }
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(eventId.toInt())
                notificationManager.cancel(((eventId.toInt() % 10000) * 1000) + 777)

                WidgetStateManager.refreshWithCurrentEvents(context)

                val updateIntent = Intent("com.example.calendario.ACTION_ALARM_STATE_CHANGED").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
            }
            ACTION_STOP -> {
                val dateStr = intent.getStringExtra("event_date")
                LogCollector.addLog("ALARMA: Detenida desde notificación ('$eventTitle')")
                stopAlarmGlobally(context, eventId, dateStr)
            }
            ACTION_SNOOZE -> {
                LogCollector.addLog("ALARMA: Pospuesta desde notificación ('$eventTitle')")
                snoozeAlarmGlobally(context, eventId, eventTitle)
            }
            else -> {
                val eventDateStr = intent.getStringExtra("event_date")
                val eventAdn = intent.getStringExtra("event_adn")

                // ESCUDO DE SEGURIDAD PREVIO AL DISPARO:
                // 1. Verificar si el ID de evento está marcado como borrado en SharedPreferences
                val isDeletedInPrefs = SettingsManager.getDeletedEventIds(context).contains(eventId)
                if (isDeletedInPrefs) {
                    LogCollector.addLog("ALARMA: Omitida -> '$eventTitle' (Evento borrado en preferencias)")
                    return
                }

                // 2. Verificar si la entidad del evento en Room está marcada con isDeleted = true
                if (!eventAdn.isNullOrBlank()) {
                    val database = AppDatabase.getDatabase(context)
                    val eventEntity = database.calendarDao().getEventByAdn(eventAdn)
                    if (eventEntity != null && eventEntity.isDeleted) {
                        LogCollector.addLog("ALARMA: Omitida -> '$eventTitle' (Instancia borrada en DB)")
                        return
                    }
                }

                // 3. Verificar si la fecha de esta instancia específica existe en los eventos activos del día
                if (!eventDateStr.isNullOrBlank()) {
                    try {
                        val database = AppDatabase.getDatabase(context)
                        val dayEvents = database.calendarDao().getAllEventsSync().filter { it.date == eventDateStr && !it.isDeleted }
                        val matchingEvent = dayEvents.find { it.googleId == eventId || (!eventAdn.isNullOrBlank() && it.adn == eventAdn) }
                        if (matchingEvent == null) {
                            LogCollector.addLog("ALARMA: Omitida -> '$eventTitle' ($eventDateStr) (Instancia cancelada/eliminada)")
                            return
                        }
                    } catch (_: Exception) {}
                }

                LogCollector.addLog("ALARMA: ¡Disparada! para '$eventTitle'")
                showNotification(context, eventId, eventTitle)
            }
        }
    }

    private fun stopAlarmGlobally(context: Context, eventId: Long, dateStr: String? = null) {
        // 1. Cancelamos las notificaciones
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.toInt())
        notificationManager.cancel(((eventId.toInt() % 10000) * 1000) + 777)

        // 2. Registramos el apagado para la fecha si existe ID
        if (eventId != -1L) {
            val effectiveDateStr = dateStr?.takeIf { it.isNotBlank() } ?: java.time.LocalDate.now().toString()
            SettingsManager.saveDismissedAlarm(context, eventId, effectiveDateStr)
        }

        // 3. Avisamos a la AlarmActivity (si está abierta) para que se cierre y pare el sonido
        val stopIntent = Intent("com.example.calendario.ALARM_STOP_SIGNAL").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(stopIntent)

        // 4. Refrescar el Widget y la interfaz en tiempo real
        WidgetStateManager.refreshWithCurrentEvents(context)
        val updateIntent = Intent("com.example.calendario.ACTION_ALARM_STATE_CHANGED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(updateIntent)
    }

    private fun snoozeAlarmGlobally(context: Context, eventId: Long, title: String) {
        stopAlarmGlobally(context, eventId)
        AlarmUtils.scheduleSnooze(context, eventId, title)
    }

    private fun showNotification(context: Context, eventId: Long, title: String) {
        val channelId = "event_alarms_v3" // Cambiamos ID para forzar el reset del sistema
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(((eventId.toInt() % 10000) * 1000) + 777)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("event_title", title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 
            eventId.toInt(), 
            fullScreenIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val channel = NotificationChannel(
            channelId,
            "Alarmas del Calendario",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Canal crítico para alarmas de eventos"
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 500)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        // Intents para los botones de la notificación
        val stopBtnIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP
            putExtra("event_id", eventId)
            putExtra("event_title", title)
        }
        val stopBtnPendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt() + 1,
            stopBtnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeBtnIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("event_id", eventId)
            putExtra("event_title", title)
        }
        val snoozeBtnPendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt() + 2,
            snoozeBtnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setColor(android.graphics.Color.RED) // Color de acento para la notificación
            .addAction(0, context.getString(R.string.snooze_alarm), snoozeBtnPendingIntent) // Izquierda
            .addAction(0, context.getString(R.string.stop_alarm), stopBtnPendingIntent) // Derecha
            .build()

        notificationManager.notify(eventId.toInt(), notification)
    }

    private fun showPreAlarmNotification(context: Context, eventId: Long, title: String, dateStr: String, timeStr: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "upcoming_alarm_channel_v7"
        val channel = NotificationChannel(
            channelId,
            "Avisos de Alarma Próxima",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_UPCOMING
            putExtra("event_id", eventId)
            putExtra("event_title", title)
            putExtra("event_date", dateStr)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            ((eventId.toInt() % 10000) * 1000) + 777,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentTitle = if (timeStr.isNotBlank()) {
            "${context.getString(R.string.upcoming_alarm_title)}: $timeStr"
        } else {
            context.getString(R.string.upcoming_alarm_title)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("event_id", eventId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            ((eventId.toInt() % 10000) * 1000) + 888,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_chameleon_notification)
            .setColor("#0077C2".toColorInt())
            .setContentTitle(contentTitle)
            .setContentText(title)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.stop_upcoming_alarm), dismissPendingIntent)
            .build()

        notificationManager.notify(((eventId.toInt() % 10000) * 1000) + 777, notification)
    }
}
