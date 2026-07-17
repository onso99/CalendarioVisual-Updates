package com.example.calendario

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ModernCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ModernCalendarWidget()
    
    // No sobreescribimos onUpdate para evitar llamadas recursivas a la App.
    // Glance se encargarÃ¡ de llamar a provideGlance cuando sea necesario.
}
