package com.example.calendario

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.json.JSONObject

sealed class ValidationResult {
    data class Success(val lightTheme: JSONObject?, val darkTheme: JSONObject?) : ValidationResult()
    data class Failure(val errorMessage: String) : ValidationResult()
}

object ThemeUtils {

    private val mandatoryKeys = setOf(
        AppThemeSetup.ColorKeys.LIGHT_CABECERA,
        AppThemeSetup.ColorKeys.DARK_CABECERA,
        AppThemeSetup.ColorKeys.LIGHT_BACKGROUND,
        AppThemeSetup.ColorKeys.DARK_BACKGROUND
    )

    fun validateAndParseTheme(jsonString: String): ValidationResult {
        try {
            val themeData = JSONObject(jsonString)

            val lightTheme = themeData.optJSONObject("lightTheme")
            val darkTheme = themeData.optJSONObject("darkTheme")

            if (lightTheme == null && darkTheme == null) {
                return ValidationResult.Failure("El fichero no contiene ni tema claro ni oscuro.")
            }

            lightTheme?.let { 
                val validation = validateThemeContent(it, "claro") 
                if(validation is ValidationResult.Failure) return validation
            }
            darkTheme?.let { 
                val validation = validateThemeContent(it, "oscuro")
                if(validation is ValidationResult.Failure) return validation
             }

            return ValidationResult.Success(lightTheme, darkTheme)

        } catch (e: Exception) {
            return ValidationResult.Failure("El fichero no es un JSON válido.")
        }
    }

    private fun validateThemeContent(theme: JSONObject, themeName: String): ValidationResult {
        for (key in mandatoryKeys) {
            if (key.startsWith("light_") && themeName == "claro" && theme.has(key)) {
                 if (!isValidColor(theme.getString(key))) return ValidationResult.Failure("El tema claro contiene un color no válido en \"$key\".")
            }
             if (key.startsWith("dark_") && themeName == "oscuro" && theme.has(key)) {
                 if (!isValidColor(theme.getString(key))) return ValidationResult.Failure("El tema oscuro contiene un color no válido en \"$key\".")
            }
        }
        return ValidationResult.Success(null, null) // Success means no errors found in this part
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