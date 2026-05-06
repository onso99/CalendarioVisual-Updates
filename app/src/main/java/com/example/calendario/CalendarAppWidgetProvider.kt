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
import androidx.work.OutOfQuotaPolicy
import java.util.concurrent.TimeUnit

class CalendarAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate llamado para IDs: ${appWidgetIds.joinToString()}. Forzando refresco de datos.")
        
        // Obligamos al widget a limpiar su caché de datos y re-leer del calendario
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        
        when (action) {
            ACTION_REFRESH_WIDGET,
            ACTION_SCHEDULED_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                LogCollector.addLog("AUTONOMÍA: Actualización por $action")
                
                // Mantenemos al observador vigilante
                CalendarObserverManager.registerObserver(context)
                
                if (action == ACTION_SCHEDULED_UPDATE || action == Intent.ACTION_BOOT_COMPLETED) {
                    scheduleNextAlarm(context)
                }

                // Redibujado directo (el widget lee solo el calendario)
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
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    // La clave: updateAppWidget fuerza la recreación de la Factory vía timestamp
                    // Evitamos notifyAppWidgetViewDataChanged para no causar NPE en Android 14
                    appWidgetIds.forEach { appWidgetId ->
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                LogCollector.addLog("ERROR_REDIBUJADO: ${e.message}")
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

            // --- LA CLAVE DEL REFRESCO (MARCA DE TIEMPO) ---
            // Añadimos System.currentTimeMillis() para que Android crea que es un servicio distinto
            // y fuerce la recreación de la Factory, limpiando datos antiguos.
            val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = "content://widget/refresh/${appWidgetId}/${System.currentTimeMillis()}".toUri()
            }
            views.setRemoteAdapter(R.id.widget_event_list, serviceIntent)
            // -----------------------------------------------

            views.setEmptyView(R.id.widget_event_list, R.id.widget_empty_view)
            views.setPendingIntentTemplate(R.id.widget_event_list, launchAppPendingIntent)

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "updateAppWidget - Forzada recreación del adaptador para ID: $appWidgetId")
            } catch (e: Exception) {
                Log.e(TAG, "updateAppWidget - Error actualizando widget ID $appWidgetId", e)
            }
        }
    }
}