package com.example.calendario

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import org.json.JSONObject
import java.util.Locale

internal fun exportThemeToJson(context: Context, uri: Uri) {
    try {
        val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val allPrefs = prefs.all

        val themeData = JSONObject()
        val manifest = JSONObject()
        manifest.put("version", AppConstants.CURRENT_THEME_VERSION)
        manifest.put("appName", AppConstants.APP_SIGNATURE)
        themeData.put("themeManifest", manifest)

        val lightTheme = JSONObject()
        val darkTheme = JSONObject()

        ColorThemeConfig.colorThemeItems.forEach { item ->
            // Light Theme
            if (item.lightThemeKey.isNotBlank()) {
                val colorString = when (val value = allPrefs[item.lightThemeKey]) {
                    is Int -> String.format("#%08X", value)
                    is String -> value
                    else -> String.format("#%08X", item.defaultLight.toArgb()) // Fallback to default
                }
                lightTheme.put(item.lightThemeKey, colorString)
            }
            
            // Dark Theme
            if (item.darkThemeKey.isNotBlank()) {
                 val colorString = when (val value = allPrefs[item.darkThemeKey]) {
                    is Int -> String.format("#%08X", value)
                    is String -> value
                    else -> String.format("#%08X", item.defaultDark.toArgb()) // Fallback to default
                }
                darkTheme.put(item.darkThemeKey, colorString)
            }
        }

        if(lightTheme.length() > 0) themeData.put("lightTheme", lightTheme)
        if(darkTheme.length() > 0) themeData.put("darkTheme", darkTheme)

        context.contentResolver.openOutputStream(uri)?.use { 
            it.write(themeData.toString(4).toByteArray())
        }
        Toast.makeText(context, "Tema exportado con éxito", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar el tema: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

internal fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (colIndex > -1) {
                    result = cursor.getString(colIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

internal fun importThemeFromJson(
    context: Context,
    uri: Uri,
    onThemeImported: () -> Unit,
    showDialog: (CompatibilityDialogInfo) -> Unit
) {
    when (val importResult = ThemeImportManager.processThemeImport(context, uri)) {
        is ImportResult.Success -> {
            val parsedTheme = importResult.parsedTheme
            val manifest = parsedTheme.manifest
            val fileVersion = manifest?.optInt("version", 1) ?: 1

            val applyChanges = { themeToApply: ParsedTheme ->
                val prefs = context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                val themeName = getFileNameFromUri(context, uri)?.removeSuffix(".json")?.replace('_', ' ')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Tema importado"

                prefs.edit {
                    val allKnownKeys = ColorThemeConfig.colorThemeItems.flatMap { listOf(it.lightThemeKey, it.darkThemeKey) }.toSet()

                    themeToApply.lightTheme?.let {
                        for (key in it.keys()) {
                            if(allKnownKeys.contains(key)) putString(key, it.getString(key))
                        }
                    }
                    themeToApply.darkTheme?.let {
                        for (key in it.keys()) {
                            if(allKnownKeys.contains(key)) putString(key, it.getString(key))
                        }
                    }
                    
                    putString(AppConstants.KEY_LIGHT_THEME_NAME, themeName)
                    putString(AppConstants.KEY_DARK_THEME_NAME, themeName)
                }
                onThemeImported()
                Toast.makeText(context, "Tema '${themeName}' importado con éxito.", Toast.LENGTH_SHORT).show()
            }

            when {
                fileVersion == AppConstants.CURRENT_THEME_VERSION -> applyChanges(parsedTheme)
                fileVersion < AppConstants.CURRENT_THEME_VERSION -> {
                    showDialog(CompatibilityDialogInfo(
                        title = "Tema Antiguo Detectado",
                        message = "Este tema es de una versión anterior y no contiene todas las opciones de color. Los colores que falten se rellenarán con los valores por defecto.",
                        onConfirm = { applyChanges(parsedTheme) }
                    ))
                }
                else -> {
                     showDialog(CompatibilityDialogInfo(
                        title = "Tema Incompatible Detectado",
                        message = "Este tema es de una versión más nueva. Se importarán solo los colores compatibles con tu versión actual.",
                        onConfirm = { applyChanges(parsedTheme) }
                    ))
                }
            }
        }
        is ImportResult.Failure -> {
            Toast.makeText(context, importResult.errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
