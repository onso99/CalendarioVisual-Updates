package com.example.calendario

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
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

        showNotification(context, eventId, eventTitle)
    }

    private fun showNotification(context: Context, eventId: Long, title: String) {
        val channelId = "event_alarms"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Alarmas de Eventos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones para alarmas de eventos"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_icon) // Usamos el que ya tenemos
            .setContentTitle(title)
            .setContentText("¡Es hora de tu evento!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.toInt(), notification)
    }
}
