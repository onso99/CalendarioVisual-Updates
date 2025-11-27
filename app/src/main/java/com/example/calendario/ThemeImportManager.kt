package com.example.calendario

import android.content.Context
import android.net.Uri

sealed class ImportResult {
    data class Success(val parsedTheme: ParsedTheme) : ImportResult()
    data class Failure(val errorMessage: String) : ImportResult()
}

object ThemeImportManager {

    fun processThemeImport(context: Context, uri: Uri): ImportResult {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (jsonString.isNullOrBlank()) {
                return ImportResult.Failure("El fichero está vacío o no se ha podido leer.")
            }

            return when (val validationResult = ThemeUtils.validateAndParseTheme(jsonString)) {
                is ValidationResult.Success -> ImportResult.Success(validationResult.parsedTheme)
                is ValidationResult.Failure -> ImportResult.Failure(validationResult.errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult.Failure("Error al leer el fichero del tema.")
        }
    }
}