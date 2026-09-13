package com.example.calendario

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.isColorDark

/**
 * Extensiones de utilidad para simplificar la gestión de la Interfaz de Usuario.
 * (Fase 4 - Optimización v3.1.34)
 */

/**
 * Muestra un Toast corto de forma simplificada.
 */
fun Context.showToast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resId, duration).show()
}

/**
 * Muestra un Toast corto con un texto personalizado.
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (message.isNotBlank()) {
        Toast.makeText(this, message, duration).show()
    }
}

/**
 * Devuelve Color.White o Color.Black según el contraste con el fondo proporcionado.
 */
fun Color.getContrastColor(background: Color): Color {
    return if (isColorDark(this, background)) Color.White else Color.Black
}

/**
 * REGLA DE COHERENCIA (v3.1.34):
 * Intenta usar el color actual, pero si no tiene suficiente contraste contra el fondo,
 * devuelve el color de contraste (Blanco o Negro).
 */
fun Color.getCoherentColor(background: Color, threshold: Double = 1.5): Color {
    val contrast = ColorUtils.calculateContrast(this.toArgb(), background.toArgb())
    return if (contrast >= threshold) this else this.getContrastColor(background)
}
