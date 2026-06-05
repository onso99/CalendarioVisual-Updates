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

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP = "com.example.calendario.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE = "com.example.calendario.ACTION_SNOOZE_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmUtils.rescheduleAllAlarms(context)
            return
        }

        val eventId = intent.getLongExtra("event_id", -1L)
        val eventTitle = intent.getStringExtra("event_title") ?: "Evento"

        when (intent.action) {
            ACTION_STOP -> {
                LogCollector.addLog("ALARMA: Detenida desde notificación ('$eventTitle')")
                stopAlarmGlobally(context, eventId)
            }
            ACTION_SNOOZE -> {
                LogCollector.addLog("ALARMA: Pospuesta desde notificación ('$eventTitle')")
                snoozeAlarmGlobally(context, eventId, eventTitle)
            }
            else -> {
                LogCollector.addLog("ALARMA: ¡Disparada! para '$eventTitle'")
                showNotification(context, eventId, eventTitle)
            }
        }
    }

    private fun stopAlarmGlobally(context: Context, eventId: Long) {
        // 1. Cancelamos la notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.toInt())

        // 2. Avisamos a la AlarmActivity (si está abierta) para que se cierre y pare el sonido
        val stopIntent = Intent("com.example.calendario.ALARM_STOP_SIGNAL").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(stopIntent)
    }

    private fun snoozeAlarmGlobally(context: Context, eventId: Long, title: String) {
        stopAlarmGlobally(context, eventId)
        AlarmUtils.scheduleSnooze(context, eventId, title)
    }

    private fun showNotification(context: Context, eventId: Long, title: String) {
        val channelId = "event_alarms_v3" // Cambiamos ID para forzar el reset del sistema
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
            .setColor(android.graphics.Color.RED) // Color de acento para la notificación
            .addAction(0, context.getString(R.string.snooze_alarm), snoozeBtnPendingIntent) // Izquierda
            .addAction(0, context.getString(R.string.stop_alarm), stopBtnPendingIntent) // Derecha
            .build()

        notificationManager.notify(eventId.toInt(), notification)
    }
}
