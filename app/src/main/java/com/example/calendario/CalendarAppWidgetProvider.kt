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
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (ACTION_REFRESH_WIDGET == intent.action) {
            Log.d(TAG, "Acción ACTION_REFRESH_WIDGET recibida. Disparando actualización completa del widget.")
            triggerWidgetUpdate(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.i(TAG, "onDeleted - INICIO - llamado para IDs: ${appWidgetIds.joinToString()}")
        super.onDeleted(context, appWidgetIds)
        Log.i(TAG, "onDeleted - FIN.")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.i(TAG, "onEnabled - INICIO - Primera instancia de CalendarAppWidgetProvider añadida.")

        CalendarObserverManager.registerObserver(context)
        Log.d(TAG, "onEnabled - CalendarObserverManager.registerObserver() llamado.")

        Log.d(TAG, "onEnabled - Encolando trabajo OneTime para actualización inicial del widget.")
        val initialUpdateWorkRequest = OneTimeWorkRequestBuilder<UpdateCalendarDataWorker>()
            .addTag(TAG_INITIAL_UPDATE_WORK)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_INITIAL_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            initialUpdateWorkRequest
        )
        Log.i(TAG, "onEnabled - Trabajo inicial '$UNIQUE_INITIAL_WORK_NAME' encolado.")

        val periodicUpdateRequest =
            PeriodicWorkRequestBuilder<UpdateCalendarDataWorker>(15, TimeUnit.MINUTES)
                .addTag(TAG_PERIODIC_UPDATE_WORK)
                .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicUpdateRequest
        )
        Log.i(TAG, "onEnabled - Trabajo periódico '$PERIODIC_WORK_NAME' encolado/verificado (política KEEP).")
        Log.i(TAG, "onEnabled - FIN.")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i(TAG, "onDisabled - INICIO - Última instancia de CalendarAppWidgetProvider eliminada.")

        CalendarObserverManager.unregisterObserver()
        Log.d(TAG, "onDisabled - CalendarObserverManager.unregisterObserver() llamado.")

        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.i(TAG, "onDisabled - Trabajo periódico '$PERIODIC_WORK_NAME' cancelado.")
        Log.i(TAG, "onDisabled - FIN.")
    }

    companion object {
        private const val TAG = "WidgetProvider"
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_WIDGET"
        private const val UNIQUE_INITIAL_WORK_NAME = "InitialCalendarWidgetUpdate"
        private const val PERIODIC_WORK_NAME = "PeriodicCalendarWidgetUpdate"
        private const val TAG_INITIAL_UPDATE_WORK = "tag_initial_calendar_work"
        private const val TAG_PERIODIC_UPDATE_WORK = "tag_periodic_calendar_work"

        fun triggerWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "triggerWidgetUpdate - Forzando actualización completa para los widgets: ${appWidgetIds.joinToString()}")
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