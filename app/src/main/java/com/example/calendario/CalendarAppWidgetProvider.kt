package com.example.calendario

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
// Asegúrate de tener el import correcto para MainActivity
// import com.example.calendario.MainActivity
import com.example.calendario.R

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
            Log.d(TAG, "Acción ACTION_REFRESH_WIDGET recibida")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalendarAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                Log.d(TAG, "Notificando cambio de datos para R.id.widget_event_list en todos los widgets.")
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
        private const val TAG = "WidgetProvider"
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_WIDGET"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "Actualizando vistas para widget ID: $appWidgetId (ListView, EmptyView, PendingIntentTemplate)")
            val views = RemoteViews(context.packageName, R.layout.calendar_widget_layout)

            // Configurar PendingIntent para abrir la app
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
                appWidgetId, // Usar un requestCode único por widget instance
                launchAppIntent,
                pendingIntentFlags
            )

            // Asignar PendingIntents directos
            views.setOnClickPendingIntent(R.id.widget_root_layout, launchAppPendingIntent)
            // El setOnClickPendingIntent para widget_empty_view puede ser redundante si setEmptyView toma precedencia para el clic
            // o si la emptyView nunca se muestra de forma independiente. Decidimos dejarlo no clicable por ahora.
            // views.setOnClickPendingIntent(R.id.widget_empty_view, launchAppPendingIntent)

            // La siguiente línea ha sido comentada/eliminada porque R.id.widget_title ya no existe en el layout
            // views.setOnClickPendingIntent(R.id.widget_title, launchAppPendingIntent)
            Log.d(TAG, "PendingIntent (directo) asignado a widget_root_layout para widget ID: $appWidgetId")

            // Configurar el RemoteAdapter para la ListView
            val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME)).buildUpon()
                    .appendPath(appWidgetId.toString())
                    .build()
            }
            views.setRemoteAdapter(R.id.widget_event_list, serviceIntent)

            // Conectar la ListView con su EmptyView
            views.setEmptyView(R.id.widget_event_list, R.id.widget_empty_view)

            // Configurar el PendingIntent plantilla para la ListView.
            views.setPendingIntentTemplate(R.id.widget_event_list, launchAppPendingIntent)
            Log.d(TAG, "PendingIntentTemplate asignado a R.id.widget_event_list para widget ID: $appWidgetId")

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "updateAppWidget llamado para widget ID: $appWidgetId")

                // Notificar cambio de datos para asegurar que la ListView se refresque
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_event_list)
                Log.d(TAG, "notifyAppWidgetViewDataChanged para R.id.widget_event_list llamado para widget ID: $appWidgetId")

            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando widget ID $appWidgetId", e)
            }
        }
    }
}
