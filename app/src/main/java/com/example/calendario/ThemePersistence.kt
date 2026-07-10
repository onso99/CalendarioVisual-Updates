package com.example.calendario

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import org.json.JSONObject

object ThemePersistence {

    fun applyTheme(context: Context, parsedTheme: ParsedTheme, fileName: String) {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            // Limpiar nombres de temas anteriores para evitar inconsistencias
            remove(AppConstants.KEY_LIGHT_THEME_NAME)
            remove(AppConstants.KEY_DARK_THEME_NAME)

            val themeVersion = parsedTheme.manifest?.optInt("version", 1) ?: 1
            // Priorizamos el ID para temas predefinidos (solo si no es nulo ni vacío)
            val themeId = parsedTheme.manifest?.optString("id", null)?.takeIf { it.isNotBlank() }
            val themeName = parsedTheme.manifest?.optString("name", null)?.takeIf { it.isNotBlank() } ?: fileName

            val baseName = themeId ?: themeName
            val finalName = if (themeVersion < AppConstants.CURRENT_THEME_VERSION) {
                "$baseName (v$themeVersion)"
            } else {
                baseName
            }
            putString(AppConstants.KEY_LIGHT_THEME_NAME, finalName)
            putString(AppConstants.KEY_DARK_THEME_NAME, finalName)

            // Aplicar los temas claro y oscuro si existen
            parsedTheme.lightTheme?.let { applyThemeColors(it, "light") }
            parsedTheme.darkTheme?.let { applyThemeColors(it, "dark") }

            // Handle monthly calendar effect type
            if (parsedTheme.manifest?.has(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE) == true) {
                // For v5+ themes, read the effect type from the manifest
                val effectType = parsedTheme.manifest.optString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient")
                putString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, effectType)
            } else {
                // For older themes that don't have this key, force gradient for compatibility
                putString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient")
            }
        }
    }

    private fun android.content.SharedPreferences.Editor.applyThemeColors(theme: JSONObject, themeType: String) {
        // Solo operamos con los colores que NO son independientes (ajustes globales)
        val themeItems = ColorThemeConfig.colorThemeItems.filter { !it.isIndependent }
        val allKeys = themeItems.map { if (themeType == "light") it.lightThemeKey else it.darkThemeKey }
        
        // 1. Limpiar colores estéticos anteriores
        allKeys.forEach { key ->
            if (key.isNotBlank()) remove(key)
        }

        // 2. Aplicar nuevos colores del tema (solo los estéticos)
        for (item in themeItems) {
            val key = if (themeType == "light") item.lightThemeKey else item.darkThemeKey
            if (key.isNotBlank() && theme.has(key)) {
                val colorString = theme.getString(key)
                putInt(key, colorString.toColorInt())
            }
        }
    }

    fun exportThemeToJson(context: Context, uri: Uri, newName: String): Boolean {
        try {
            val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            val themeJson = JSONObject()

            // Manifest
            val manifest = JSONObject()
            manifest.put("version", AppConstants.CURRENT_THEME_VERSION)
            manifest.put("appName", AppConstants.APP_SIGNATURE)
            manifest.put("name", newName)
            prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient")?.let {
                manifest.put(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, it)
            }
            themeJson.put("themeManifest", manifest)

            // Light & Dark Themes
            val lightTheme = JSONObject()
            val darkTheme = JSONObject()

            // Filtramos la lista para que SOLO exporte los colores vinculados al tema
            // (Ignoramos Cumpleaños, Evento-1 y Evento-2 ya que son ajustes globales)
            val themeItems = ColorThemeConfig.colorThemeItems.filter { !it.isIndependent }

            themeItems.forEach { item ->
                // Light
                if (item.lightThemeKey.isNotBlank()) {
                    val lightColor = prefs.getInt(item.lightThemeKey, item.defaultLight.toArgb())
                    lightTheme.put(item.lightThemeKey, String.format("#%08X", lightColor))
                }
                // Dark
                if (item.darkThemeKey.isNotBlank()) {
                    val darkColor = prefs.getInt(item.darkThemeKey, item.defaultDark.toArgb())
                    darkTheme.put(item.darkThemeKey, String.format("#%08X", darkColor))
                }
            }

            themeJson.put("lightTheme", lightTheme)
            themeJson.put("darkTheme", darkTheme)

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(themeJson.toString(4).toByteArray())
            }
            Toast.makeText(context, R.string.theme_exported_successfully, Toast.LENGTH_SHORT).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_exporting_theme, e.message), Toast.LENGTH_LONG).show()
            return false
        }
    }
}
