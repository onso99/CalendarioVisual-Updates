package com.example.calendario

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ModernCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ModernCalendarWidget()

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_MODERN_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        LogCollector.addLog("RECEPTOR MODERNO: onUpdate (Widget aÃ±adido o reiniciado)")
        // Pedimos al motor que inyecte datos en los nuevos IDs
        WidgetStateManager.refreshWithCurrentEvents(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            MainScope().launch {
                delay(100) // Breve respiro para el DataStore
                ModernCalendarWidget().updateAll(context)
                LogCollector.addLog("RECEPTOR MODERNO: Redibujado forzado ejecutado")
            }
        }
    }
}
