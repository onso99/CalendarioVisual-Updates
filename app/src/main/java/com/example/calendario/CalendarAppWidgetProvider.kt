package com.example.calendario // Asegúrate que este es tu nombre de paquete correcto

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import android.util.Log

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
        super.onReceive(context, intent) // Es importante llamar a super.onReceive

        // Manejar acciones personalizadas como la actualización forzada desde la app
        if (ACTION_REFRESH_WIDGET == intent.action) {
            Log.d(TAG, "Acción ACTION_REFRESH_WIDGET recibida")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "Notificando cambio de datos para todos los widgets de este proveedor.")
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted llamado para IDs: ${appWidgetIds.joinToString()}")
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "onEnabled - Primer widget añadido")
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled - Último widget eliminado")
        super.onDisabled(context)
    }

    companion object {
        private const val TAG = "WidgetProvider" // TAG para logging
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_WIDGET" // Constante definida

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "Actualizando vistas para widget ID: $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.calendar_widget_layout)

            // --- INICIO: PENDINGINTENT GLOBAL PARA ABRIR LA APP ---
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
            // --- FIN: PENDINGINTENT GLOBAL PARA ABRIR LA APP ---

            // El setOnClickPendingIntent para R.id.widget_title es ahora redundante.
            // Lo dejo comentado por si lo quieres explícitamente o para futuras modificaciones.
            // views.setOnClickPendingIntent(R.id.widget_title, launchAppPendingIntent)

            val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("widgetservice://${context.packageName}/${appWidgetId}").buildUpon().build()
            }
            views.setRemoteAdapter(R.id.widget_event_list, serviceIntent)
            views.setEmptyView(R.id.widget_event_list, R.id.widget_empty_view)

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_event_list)
                Log.d(TAG, "updateAppWidget llamado y datos notificados para widget ID: $appWidgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando widget ID $appWidgetId", e)
            }
        }
    }
}
