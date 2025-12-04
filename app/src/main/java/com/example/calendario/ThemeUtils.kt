package com.example.calendario

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.json.JSONObject

data class ParsedTheme(
    val manifest: JSONObject?,
    val lightTheme: JSONObject?,
    val darkTheme: JSONObject?
)

sealed class ValidationResult {
    data class Success(val parsedTheme: ParsedTheme) : ValidationResult()
    data class Failure(val errorMessage: String) : ValidationResult()
}

object ThemeUtils {

    private val allKnownKeys = ColorThemeConfig.colorThemeItems.flatMap { listOf(it.lightThemeKey, it.darkThemeKey) }.toSet()

    fun validateAndParseTheme(jsonString: String): ValidationResult {
        try {
            val themeData = JSONObject(jsonString)
            val manifest = themeData.optJSONObject("themeManifest")

            if (manifest != null) {
                if (manifest.optString("appName") != AppThemeSetup.APP_SIGNATURE) {
                    return ValidationResult.Failure("Fichero de tema no compatible.")
                }
            }

            val lightTheme = themeData.optJSONObject("lightTheme")
            val darkTheme = themeData.optJSONObject("darkTheme")

            if (lightTheme == null && darkTheme == null) {
                return ValidationResult.Failure("El fichero no contiene ni tema claro ni oscuro.")
            }

            lightTheme?.let { 
                val validation = validateThemeContent(it, "light") 
                if(validation is ValidationResult.Failure) return validation
            }
            darkTheme?.let { 
                val validation = validateThemeContent(it, "dark")
                if(validation is ValidationResult.Failure) return validation
             }

            return ValidationResult.Success(ParsedTheme(manifest, lightTheme, darkTheme))

        } catch (_: Exception) {
            return ValidationResult.Failure("El fichero no es un JSON válido.")
        }
    }

    private fun validateThemeContent(theme: JSONObject, themeType: String): ValidationResult {
        val keysToCheck = if (themeType == "light") allKnownKeys.filter { it.startsWith("light_") } else allKnownKeys.filter { it.startsWith("dark_") }
        for (key in keysToCheck) {
            if (theme.has(key)) {
                 if (!isValidColor(theme.getString(key))) {
                     return ValidationResult.Failure("El tema contiene un color no válido en \"$key\".")
                 }
            }
        }
        return ValidationResult.Success(ParsedTheme(null, null, null)) // Success means no errors found
    }

    fun isValidColor(colorString: String): Boolean {
        return try {
            Color(colorString.toColorInt())
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
