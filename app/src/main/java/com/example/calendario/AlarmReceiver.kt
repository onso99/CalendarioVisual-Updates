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
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmUtils.rescheduleAllAlarms(context)
            return
        }

        val eventId = intent.getLongExtra("event_id", -1L)
        val eventTitle = intent.getStringExtra("event_title") ?: "Evento"
        LogCollector.addLog("ALARMA: ¡Disparada! para '$eventTitle'")

        showNotification(context, eventId, eventTitle)
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = NotificationChannel(
            channelId,
            "Alarmas del Calendario",
            NotificationManager.IMPORTANCE_HIGH
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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText("¡Es hora de tu evento!")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Prioridad Máxima
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true) // Hace que sea más difícil de ignorar
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.toInt(), notification)
    }
}
