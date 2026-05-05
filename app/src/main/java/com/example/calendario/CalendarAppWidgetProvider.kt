package com.example.calendario

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.net.toUri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class CalendarAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate llamado para IDs: ${appWidgetIds.joinToString()}")
        
        // Simplemente actualizamos la vista con los datos que ya tenemos en caché
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d(TAG, "onReceive - Acción recibida: $action")

        when (action) {
            ACTION_REFRESH_WIDGET,
            ACTION_SCHEDULED_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Disparando actualización completa por evento: $action")
                
                // Aseguramos que los observadores estén activos
                CalendarObserverManager.registerObserver(context)
                
                // Forzamos un trabajo de actualización inmediata de datos
                val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateCalendarDataWorker>()
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    "ManualUpdate_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    updateWorkRequest
                )
                
                // Si ha sido una alarma programada, agendamos la siguiente
                if (action == ACTION_SCHEDULED_UPDATE || action == Intent.ACTION_BOOT_COMPLETED) {
                    scheduleNextAlarm(context)
                }

                triggerWidgetUpdate(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.i(TAG, "onEnabled - Activando sistema de alarmas y observadores.")
        CalendarObserverManager.registerObserver(context)
        scheduleNextAlarm(context)
        
        // Mantenemos WorkManager como red de seguridad secundaria
        val periodicUpdateRequest = PeriodicWorkRequestBuilder<UpdateCalendarDataWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodicUpdateRequest)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i(TAG, "onDisabled - Cancelando alarmas y limpieza.")
        cancelAlarm(context)
        CalendarObserverManager.unregisterObserver()
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    companion object {
        private const val TAG = "WidgetProvider"
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_WIDGET"
        const val ACTION_SCHEDULED_UPDATE = "com.example.calendario.ACTION_SCHEDULED_UPDATE"
        private const val PERIODIC_WORK_NAME = "PeriodicCalendarWidgetUpdate"

        private fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, CalendarAppWidgetProvider::class.java).apply {
                action = ACTION_SCHEDULED_UPDATE
            }
            
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Programamos la próxima actualización en 15 minutos. 
            // Usamos setAndAllowWhileIdle para que Android 14 no lo ignore en modo ahorro.
            val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)
            
            try {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Próxima alarma de actualización programada en 15 minutos.")
            } catch (e: Exception) {
                Log.e(TAG, "Error programando alarma", e)
            }
        }

        private fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, CalendarAppWidgetProvider::class.java).apply {
                action = ACTION_SCHEDULED_UPDATE
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, intent,
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "Alarma de actualización cancelada.")
            }
        }

        fun triggerWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "triggerWidgetUpdate - Notificando cambio de datos y forzando actualización para: ${appWidgetIds.joinToString()}")
                
                // 1. Notificar cambio en la colección (lista de eventos) para limpiar la caché de la Factory
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
                
                // 2. Actualizar la vista general del widget (layout, colores, etc.)
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } else {
                 Log.d(TAG, "triggerWidgetUpdate - No hay IDs de widget activos para actualizar.")
            }
        }

        @Suppress("DEPRECATION")
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "updateAppWidget - INICIO para widget ID: $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.calendar_widget_layout)

            // Leer preferencias y aplicar color de fondo
            val prefs = context.getSharedPreferences(WidgetConstants.GLOBAL_WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val backgroundColor = prefs.getInt(WidgetConstants.KEY_WIDGET_BACKGROUND_COLOR, WidgetConstants.DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB)
            views.setInt(R.id.widget_root_layout, "setBackgroundColor", backgroundColor)

            val launchAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val launchAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchAppIntent,
                pendingIntentFlags
            )

            views.setOnClickPendingIntent(R.id.widget_root_layout, launchAppPendingIntent)
            Log.d(TAG, "updateAppWidget - PendingIntent (directo) asignado a widget_root_layout para widget ID: $appWidgetId")

            val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = this.toUri(Intent.URI_INTENT_SCHEME).toUri().buildUpon()
                    .appendPath(appWidgetId.toString())
                    .build()
            }
            views.setRemoteAdapter(R.id.widget_event_list, serviceIntent)

            Log.d(TAG, "updateAppWidget - RemoteAdapter configurado para R.id.widget_event_list, widget ID: $appWidgetId")

            views.setEmptyView(R.id.widget_event_list, R.id.widget_empty_view)
            Log.d(TAG, "updateAppWidget - EmptyView configurado para R.id.widget_event_list, widget ID: $appWidgetId")

            views.setPendingIntentTemplate(R.id.widget_event_list, launchAppPendingIntent)
            Log.d(TAG, "updateAppWidget - PendingIntentTemplate asignado a R.id.widget_event_list, widget ID: $appWidgetId")

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "updateAppWidget - appWidgetManager.updateAppWidget llamado para widget ID: $appWidgetId")

            } catch (e: Exception) {
                Log.e(TAG, "updateAppWidget - Error actualizando widget ID $appWidgetId", e)
            }
            Log.d(TAG, "updateAppWidget - FIN para widget ID: $appWidgetId")
        }
    }
}