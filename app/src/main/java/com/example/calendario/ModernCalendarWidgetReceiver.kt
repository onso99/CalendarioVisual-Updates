package com.example.calendario

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ModernCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ModernCalendarWidget()

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.calendario.ACTION_REFRESH_MODERN_WIDGET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            MainScope().launch {
                ModernCalendarWidget().updateAll(context)
                LogCollector.addLog("RECEPTOR MODERNO: Forzando redibujado instantÃ¡neo")
            }
        }
    }
}
