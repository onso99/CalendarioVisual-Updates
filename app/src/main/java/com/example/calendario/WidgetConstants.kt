package com.example.calendario

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object WidgetConstants {
    const val GLOBAL_WIDGET_PREFS_NAME = "global_calendar_widget_prefs" // O el que estés usando consistentemente
    const val KEY_EVENT_COUNT = "widget_event_count"
    const val DEFAULT_EVENT_COUNT = 4
    const val KEY_FONT_SIZE_LARGE = "font_size_large_preference_key"

    const val KEY_WIDGET_EVENT_COLOR = "widget_event_color_key"
    const val KEY_WIDGET_TODAY_EVENT_COLOR = "widget_today_event_color_key"

    // --- COLORES POR DEFECTO SELECCIONADOS DE TUS PALETAS ---
    val DEFAULT_WIDGET_EVENT_COLOR_ARGB = Color(0xFF5F5F5F).toArgb() // Gris oscuro de tu paleta
    val DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB = Color(0xFFFF8000).toArgb() // Naranja de tu paleta
}