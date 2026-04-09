package com.example.calendario

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object WidgetConstants {
    const val GLOBAL_WIDGET_PREFS_NAME = "global_calendar_widget_prefs" 
    const val KEY_EVENT_COUNT = "widget_event_count"
    const val DEFAULT_EVENT_COUNT = 4
    const val KEY_WIDGET_TEXT_BOOST = "widget_text_boost_key"
    const val KEY_WIDGET_FONT_FAMILY = "widget_font_family_key"

    const val KEY_WIDGET_EVENT_COLOR = "widget_event_color_key"
    const val KEY_WIDGET_TODAY_EVENT_COLOR = "widget_today_event_color_key"
    const val KEY_WIDGET_BACKGROUND_COLOR = "widget_background_color_key"

    val DEFAULT_WIDGET_BACKGROUND_COLOR_ARGB = Color(0x230000DB).toArgb()
    val DEFAULT_WIDGET_EVENT_COLOR_ARGB = Color(0xFFECECEC.toInt()).toArgb()
    val DEFAULT_WIDGET_TODAY_EVENT_COLOR_ARGB = Color(0xFFFFEC94.toInt()).toArgb()
    
    const val FONT_FAMILY_SYSTEM = ""
    const val FONT_FAMILY_SANS_SERIF = "sans-serif"
    const val FONT_FAMILY_SERIF = "serif"
    const val FONT_FAMILY_MONOSPACE = "monospace"
    const val FONT_FAMILY_CONDENSED = "sans-serif-condensed"
    const val DEFAULT_WIDGET_FONT_FAMILY = FONT_FAMILY_SYSTEM
}