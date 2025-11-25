package com.example.calendario

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import org.json.JSONObject
import java.util.Locale

@Composable
fun ThemeMixerDialog(
    onDismissRequest: () -> Unit,
    onThemeMixed: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }

    var lightThemeName by remember { mutableStateOf("Actual") }
    var darkThemeName by remember { mutableStateOf("Actual") }

    var pendingLightTheme by remember { mutableStateOf<JSONObject?>(null) }
    var pendingDarkTheme by remember { mutableStateOf<JSONObject?>(null) }

    val lightThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: ""
                        when (val validationResult = ThemeUtils.validateAndParseTheme(jsonString)) {
                            is ValidationResult.Success -> {
                                if (validationResult.lightTheme != null) {
                                    pendingLightTheme = validationResult.lightTheme
                                    lightThemeName = getFileNameFromUri(context, uri)?.removeSuffix(".json")?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "Importado"
                                    Toast.makeText(context, "Tema claro '${lightThemeName}' cargado.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "El fichero no contiene un tema claro válido.", Toast.LENGTH_LONG).show()
                                }
                            }
                            is ValidationResult.Failure -> {
                                Toast.makeText(context, validationResult.errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al leer el fichero del tema.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val darkThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: ""
                         when (val validationResult = ThemeUtils.validateAndParseTheme(jsonString)) {
                            is ValidationResult.Success -> {
                                if (validationResult.darkTheme != null) {
                                    pendingDarkTheme = validationResult.darkTheme
                                    darkThemeName = getFileNameFromUri(context, uri)?.removeSuffix(".json")?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "Importado"
                                    Toast.makeText(context, "Tema oscuro '${darkThemeName}' cargado.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "El fichero no contiene un tema oscuro válido.", Toast.LENGTH_LONG).show()
                                }
                            }
                            is ValidationResult.Failure -> {
                                Toast.makeText(context, validationResult.errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al leer el fichero del tema.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Mezclador de Temas", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ThemeMixerRow(
                    label = "Tema Claro:",
                    themeName = lightThemeName,
                    onLoadClick = { 
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
                        lightThemeLauncher.launch(intent)
                    }
                )
                ThemeMixerRow(
                    label = "Tema Oscuro:",
                    themeName = darkThemeName,
                    onLoadClick = { 
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
                        darkThemeLauncher.launch(intent)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    prefs.edit {
                        if (pendingLightTheme != null) {
                            for (key in pendingLightTheme!!.keys()) {
                                putString(key, pendingLightTheme!!.getString(key))
                            }
                        }
                        if (pendingDarkTheme != null) {
                            for (key in pendingDarkTheme!!.keys()) {
                                putString(key, pendingDarkTheme!!.getString(key))
                            }
                        }
                        if (pendingLightTheme != null || pendingDarkTheme != null) {
                           remove(AppThemeSetup.KEY_CURRENT_THEME_NAME) // Es una mezcla, no un tema "puro"
                           Toast.makeText(context, "Temas mezclados aplicados.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    onThemeMixed()
                    onDismissRequest()
                }
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}

@Composable
private fun ThemeMixerRow(label: String, themeName: String, onLoadClick: () -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = themeName,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onLoadClick) {
                Text("Cargar...")
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
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