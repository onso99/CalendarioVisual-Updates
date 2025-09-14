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
import androidx.work.ExistingPeriodicWorkPolicy // Importar para política periódica
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder // Importar para worker periódico
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit // Importar para TimeUnit

// Asegúrate de tener el import correcto para MainActivity si lo usas explícitamente.
// import com.example.calendario.MainActivity

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
            } else {
                Log.d(TAG, "Acción ACTION_REFRESH_WIDGET recibida, pero no hay IDs de widget activos.")
            }
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

        // --- Lógica del ContentObserver (se mantiene igual) ---
        if (calendarObserverInstance == null) {
            Log.d(TAG, "onEnabled - calendarObserverInstance es null. Creando NUEVA instancia de CalendarObserver.")
            calendarObserverInstance = CalendarObserver(context.applicationContext)
            calendarObserverInstance?.register()
        } else {
            Log.w(TAG, "onEnabled - calendarObserverInstance NO era null. Llamando a register() en la instancia existente.")
            calendarObserverInstance?.register()
        }

        // --- Encolar trabajo OneTime para actualización inicial (se mantiene igual) ---
        Log.d(TAG, "onEnabled - Encolando trabajo OneTime para actualización inicial del widget.")
        val initialUpdateWorkRequest = OneTimeWorkRequestBuilder<UpdateCalendarDataWorker>()
            .addTag(TAG_INITIAL_UPDATE_WORK) // Opcional: añadir un tag para identificarlo si es necesario
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_INITIAL_WORK_NAME, // Nombre único para el trabajo inicial
            ExistingWorkPolicy.REPLACE,
            initialUpdateWorkRequest
        )
        Log.i(TAG, "onEnabled - Trabajo inicial '$UNIQUE_INITIAL_WORK_NAME' encolado.")

        // --- Encolar el TRABAJO PERIÓDICO para actualizaciones regulares ---
        val periodicUpdateRequest =
            PeriodicWorkRequestBuilder<UpdateCalendarDataWorker>(
                15, TimeUnit.MINUTES // Intervalo mínimo de repetición (Android lo ajustará si es menor)
                // Podrías añadir un flexInterval aquí si quieres más flexibilidad para el sistema:
                // 15, TimeUnit.MINUTES, // repeatInterval
                // 5, TimeUnit.MINUTES  // flexInterval (el trabajo se ejecuta en los últimos 5 min del intervalo de 15)
            )
                // .setConstraints(Constraints.Builder()...build()) // Añade restricciones si es necesario
                .addTag(TAG_PERIODIC_UPDATE_WORK) // Opcional: tag para identificarlo
                .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,             // Nombre único para este trabajo periódico
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene el trabajo existente si ya está encolado con este nombre
            // Usa .REPLACE si quieres que se actualice la definición del trabajo (ej. si cambias el intervalo)
            periodicUpdateRequest
        )
        Log.i(TAG, "onEnabled - Trabajo periódico '$PERIODIC_WORK_NAME' encolado/verificado (política KEEP).")

        Log.i(TAG, "onEnabled - FIN.")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i(TAG, "onDisabled - INICIO - Última instancia de CalendarAppWidgetProvider eliminada.")

        // --- Lógica para desregistrar el ContentObserver (se mantiene igual) ---
        if (calendarObserverInstance != null) {
            Log.d(TAG, "onDisabled - Desregistrando calendarObserverInstance.")
            calendarObserverInstance?.unregister()
            calendarObserverInstance = null
            Log.d(TAG, "onDisabled - calendarObserverInstance puesto a null.")
        } else {
            Log.w(TAG, "onDisabled - calendarObserverInstance ya era null.")
        }

        // --- Cancelar el trabajo periódico ---
        // Esto es importante para detener las actualizaciones periódicas si ya no hay widgets.
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.i(TAG, "onDisabled - Trabajo periódico '$PERIODIC_WORK_NAME' cancelado.")

        // Opcionalmente, también podrías cancelar el trabajo inicial si aún estuviera pendiente, aunque es menos común.
        // WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_INITIAL_WORK_NAME)
        // Log.i(TAG, "onDisabled - Trabajo inicial '$UNIQUE_INITIAL_WORK_NAME' cancelado (si estaba pendiente).")

        Log.i(TAG, "onDisabled - FIN.")
    }

    companion object {
        private const val TAG = "WidgetProvider"
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_WIDGET"
        private var calendarObserverInstance: CalendarObserver? = null

        // Nombres únicos para los trabajos de WorkManager
        private const val UNIQUE_INITIAL_WORK_NAME = "InitialCalendarWidgetUpdate"
        private const val PERIODIC_WORK_NAME = "PeriodicCalendarWidgetUpdate"

        // Tags opcionales para los workers (pueden ser útiles para observarlos o cancelarlos por tag)
        private const val TAG_INITIAL_UPDATE_WORK = "tag_initial_calendar_work"
        private const val TAG_PERIODIC_UPDATE_WORK = "tag_periodic_calendar_work"


        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "updateAppWidget - INICIO para widget ID: $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.calendar_widget_layout)

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
                appWidgetId, // Usar appWidgetId como requestCode para PendingIntents únicos por widget
                launchAppIntent,
                pendingIntentFlags
            )

            views.setOnClickPendingIntent(R.id.widget_root_layout, launchAppPendingIntent)
            Log.d(TAG, "updateAppWidget - PendingIntent (directo) asignado a widget_root_layout para widget ID: $appWidgetId")

            val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME)).buildUpon()
                    .appendPath(appWidgetId.toString())
                    .build()
            }
            views.setRemoteAdapter(R.id.widget_event_list, serviceIntent)
            Log.d(TAG, "updateAppWidget - RemoteAdapter configurado para R.id.widget_event_list, widget ID: $appWidgetId")

            views.setEmptyView(R.id.widget_event_list, R.id.widget_empty_view)
            Log.d(TAG, "updateAppWidget - EmptyView configurado para R.id.widget_event_list, widget ID: $appWidgetId")

            views.setPendingIntentTemplate(R.id.widget_event_list, launchAppPendingIntent) // Usar el mismo PendingIntent que para el root por simplicidad, o crear uno específico.
            Log.d(TAG, "updateAppWidget - PendingIntentTemplate asignado a R.id.widget_event_list, widget ID: $appWidgetId")

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "updateAppWidget - appWidgetManager.updateAppWidget llamado para widget ID: $appWidgetId")

                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_event_list)
                Log.d(TAG, "updateAppWidget - notifyAppWidgetViewDataChanged para R.id.widget_event_list llamado para widget ID: $appWidgetId")

            } catch (e: Exception) {
                Log.e(TAG, "updateAppWidget - Error actualizando widget ID $appWidgetId", e)
            }
            Log.d(TAG, "updateAppWidget - FIN para widget ID: $appWidgetId")
        }
    }
}
