package com.example.calendario

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import org.json.JSONObject

object ThemePersistence {

    fun applyTheme(context: Context, parsedTheme: ParsedTheme) {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            // Limpiar nombres de temas anteriores para evitar inconsistencias
            remove(AppConstants.KEY_LIGHT_THEME_NAME)
            remove(AppConstants.KEY_DARK_THEME_NAME)

            parsedTheme.manifest?.optString("name")?.let {
                putString(AppConstants.KEY_LIGHT_THEME_NAME, it)
                putString(AppConstants.KEY_DARK_THEME_NAME, it)
            }

            // Aplicar los temas claro y oscuro si existen
            parsedTheme.lightTheme?.let { applyThemeColors(it, "light") }
            parsedTheme.darkTheme?.let { applyThemeColors(it, "dark") }
        }
    }

    private fun android.content.SharedPreferences.Editor.applyThemeColors(theme: JSONObject, themeType: String) {
        val allKeys = ColorThemeConfig.colorThemeItems.map { if (themeType == "light") it.lightThemeKey else it.darkThemeKey }
        
        // Primero, limpiar todas las claves de color existentes para este tipo de tema
        allKeys.forEach { key ->
            if (key.isNotBlank()) {
                remove(key)
            }
        }

        // Ahora, aplicar los nuevos colores desde el JSON
        for (item in ColorThemeConfig.colorThemeItems) {
            val key = if (themeType == "light") item.lightThemeKey else item.darkThemeKey
            if (key.isNotBlank() && theme.has(key)) {
                val colorString = theme.getString(key)
                // La validación del color ya se hizo en ThemeImportManager
                putInt(key, android.graphics.Color.parseColor(colorString))
            }
        }
    }

    fun exportThemeToJson(context: Context, uri: Uri) {
        try {
            val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            val themeJson = JSONObject()

            // Manifest
            val manifest = JSONObject()
            manifest.put("version", AppConstants.CURRENT_THEME_VERSION)
            manifest.put("appName", AppConstants.APP_SIGNATURE)
            prefs.getString(AppConstants.KEY_LIGHT_THEME_NAME, null)?.let { manifest.put("name", it) }
            themeJson.put("themeManifest", manifest)

            // Light & Dark Themes
            val lightTheme = JSONObject()
            val darkTheme = JSONObject()

            ColorThemeConfig.colorThemeItems.forEach { item ->
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
            Toast.makeText(context, "Tema exportado con éxito", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al exportar el tema: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
