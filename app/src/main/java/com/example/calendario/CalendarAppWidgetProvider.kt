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
        // onUpdate debe ser TOTALMENTE PASIVO para evitar parpadeos.
        // Solo pintamos la información que ya existe en el caché.
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
                LogCollector.addLog("AUTONOMÍA: Despertando por $action")
                
                CalendarObserverManager.registerObserver(context)
                
                if (action == ACTION_SCHEDULED_UPDATE || action == Intent.ACTION_BOOT_COMPLETED) {
                    scheduleNextAlarm(context)
                }

                // Lanzamos el Worker de alta prioridad. ÉL será el único que mande redibujar.
                val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateCalendarDataWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    "AutonomousUpdate", ExistingWorkPolicy.REPLACE, updateWorkRequest
                )
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

            // Programamos la próxima actualización en 15 minutos (mínimo recomendado para estabilidad)
            val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)
            
            try {
                // Usamos la versión EXACTA para saltarnos las restricciones de Android 14
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                LogCollector.addLog("ALARMA: Próxima cita en 15 min (Modo Exacto)")
            } catch (e: Exception) {
                Log.e(TAG, "Error programando alarma", e)
                // Fallback si no hay permiso de alarma exacta
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
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
                    // Usamos notifyAppWidgetViewDataChanged para refrescar la lista de forma nativa
                    // Esto NO dispara onUpdate y por tanto no genera parpadeos.
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
                    
                    appWidgetIds.forEach { appWidgetId ->
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                LogCollector.addLog("ERROR_REFRESCO: ${e.message}")
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