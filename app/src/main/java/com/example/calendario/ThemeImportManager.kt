package com.example.calendario

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.json.JSONObject

sealed class ImportResult {
    data class Success(val parsedTheme: ParsedTheme) : ImportResult()
    data class LegacyThemeDetected(val parsedTheme: ParsedTheme) : ImportResult()
    data class Failure(val errorMessage: String) : ImportResult()
}

data class ParsedTheme(
    val manifest: JSONObject?,
    val lightTheme: JSONObject?,
    val darkTheme: JSONObject?
)

object ThemeImportManager {

    private val allKnownKeys = ColorThemeConfig.colorThemeItems.flatMap { listOf(it.lightThemeKey, it.darkThemeKey) }.toSet()

    fun processThemeImport(context: Context, uri: Uri): ImportResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (jsonString == null) {
                return ImportResult.Failure(context.getString(R.string.could_not_read_file))
            }
            
            when(val validationResult = validateAndParseTheme(context, jsonString)) {
                is ValidationResult.Success -> {
                    val manifest = validationResult.parsedTheme.manifest
                    val themeVersion = manifest?.optInt("version", 1) ?: 1
                    if (themeVersion < AppConstants.CURRENT_THEME_VERSION) {
                        ImportResult.LegacyThemeDetected(validationResult.parsedTheme)
                    } else {
                        ImportResult.Success(validationResult.parsedTheme)
                    }
                }
                is ValidationResult.Failure -> ImportResult.Failure(validationResult.errorMessage)
            }

        } catch (e: Exception) {
            ImportResult.Failure(context.getString(R.string.error_processing_theme, e.message))
        }
    }

    private fun validateAndParseTheme(context: Context, jsonString: String): ValidationResult {
        try {
            val themeData = JSONObject(jsonString)
            val manifest = themeData.optJSONObject("themeManifest")

            if (manifest != null) {
                if (manifest.optString("appName") != AppConstants.APP_SIGNATURE) {
                    return ValidationResult.Failure(context.getString(R.string.incompatible_theme_file))
                }
            }

            val lightTheme = themeData.optJSONObject("lightTheme")
            val darkTheme = themeData.optJSONObject("darkTheme")

            if (lightTheme == null && darkTheme == null) {
                return ValidationResult.Failure(context.getString(R.string.file_contains_no_theme))
            }

            lightTheme?.let { 
                val validation = validateThemeContent(context, it, "light") 
                if(validation is ValidationResult.Failure) return validation
            }
            darkTheme?.let { 
                val validation = validateThemeContent(context, it, "dark")
                if(validation is ValidationResult.Failure) return validation
             }

            return ValidationResult.Success(ParsedTheme(manifest, lightTheme, darkTheme))

        } catch (_: Exception) {
            return ValidationResult.Failure(context.getString(R.string.invalid_json_file))
        }
    }

    private fun validateThemeContent(context: Context, theme: JSONObject, themeType: String): ValidationResult {
        val keysToCheck = if (themeType == "light") allKnownKeys.filter { it.startsWith("light_") } else allKnownKeys.filter { it.startsWith("dark_") }
        for (key in keysToCheck) {
            if (theme.has(key)) {
                 if (!isValidColor(theme.getString(key))) {
                     return ValidationResult.Failure(context.getString(R.string.invalid_color_in_theme, key))
                 }
            }
        }
        return ValidationResult.Success(ParsedTheme(null, null, null)) // Success means no errors found
    }

    private fun isValidColor(colorString: String): Boolean {
        return try {
            Color(colorString.toColorInt())
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}

internal sealed class ValidationResult {
    data class Success(val parsedTheme: ParsedTheme) : ValidationResult()
    data class Failure(val errorMessage: String) : ValidationResult()
}
