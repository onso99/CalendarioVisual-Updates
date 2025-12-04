package com.example.calendario

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    var lightThemeName by remember { mutableStateOf<String?>(null) }
    var darkThemeName by remember { mutableStateOf<String?>(null) }
    var lightThemeVersion by remember { mutableIntStateOf(AppThemeSetup.CURRENT_THEME_VERSION) }
    var darkThemeVersion by remember { mutableIntStateOf(AppThemeSetup.CURRENT_THEME_VERSION) }

    var pendingLightTheme by remember { mutableStateOf<JSONObject?>(null) }
    var pendingDarkTheme by remember { mutableStateOf<JSONObject?>(null) }
    var showCompatibilityDialog by remember { mutableStateOf(false) }
    var isLoadingDark by remember { mutableStateOf(false) }

    val themeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    when (val importResult = ThemeImportManager.processThemeImport(context, uri)) {
                        is ImportResult.Success -> {
                            val themeToLoad = if (isLoadingDark) importResult.parsedTheme.darkTheme else importResult.parsedTheme.lightTheme
                            if (themeToLoad != null) {
                                val fileName = getFileNameFromUri(context, uri)?.removeSuffix(".json")?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "Importado"
                                val fileVersion = importResult.parsedTheme.manifest?.optInt("version", 1) ?: 1
                                if(isLoadingDark) {
                                    pendingDarkTheme = themeToLoad
                                    darkThemeName = fileName
                                    darkThemeVersion = fileVersion
                                } else {
                                    pendingLightTheme = themeToLoad
                                    lightThemeName = fileName
                                    lightThemeVersion = fileVersion
                                }
                                Toast.makeText(context, "Tema '${fileName}' cargado para modo ${if(isLoadingDark) "oscuro" else "claro"}.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "El fichero no contiene un tema ${if(isLoadingDark) "oscuro" else "claro"} válido.", Toast.LENGTH_LONG).show()
                            }
                        }
                        is ImportResult.Failure -> {
                            Toast.makeText(context, importResult.errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    val applyChanges = {
        var applied = false
        prefs.edit {
            pendingLightTheme?.let {
                applyThemeKeys(this, it)
                putString(AppThemeSetup.KEY_LIGHT_THEME_NAME, lightThemeName ?: "Mezclado")
                applied = true
            }
            pendingDarkTheme?.let {
                applyThemeKeys(this, it)
                putString(AppThemeSetup.KEY_DARK_THEME_NAME, darkThemeName ?: "Mezclado")
                applied = true
            }
            if (pendingLightTheme != null && pendingDarkTheme == null) {
                putString(AppThemeSetup.KEY_DARK_THEME_NAME, lightThemeName ?: "Mezclado")
            } else if (pendingLightTheme == null && pendingDarkTheme != null) {
                putString(AppThemeSetup.KEY_LIGHT_THEME_NAME, darkThemeName ?: "Mezclado")
            }
        }
        if(applied) {
            Toast.makeText(context, "Temas mezclados aplicados.", Toast.LENGTH_SHORT).show()
            onThemeMixed()
        }
        onDismissRequest()
    }

    if (showCompatibilityDialog) {
        AlertDialog(
            onDismissRequest = { showCompatibilityDialog = false },
            title = { Text("Temas Antiguos Detectados", fontWeight = FontWeight.Bold) },
            text = { Text("Al menos uno de los temas que estás mezclando es de una versión anterior. Los colores que falten se rellenarán con los valores por defecto.") },
            confirmButton = { TextButton(onClick = { showCompatibilityDialog = false; applyChanges() }) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = { showCompatibilityDialog = false }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Mezclador de Temas", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cargar para modo Oscuro", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isLoadingDark, onCheckedChange = { isLoadingDark = it })
                }
                Spacer(Modifier.height(8.dp))
                ThemeInfoRow("Tema Claro:", lightThemeName ?: "Actual")
                ThemeInfoRow("Tema Oscuro:", darkThemeName ?: "Actual")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
                        themeLauncher.launch(intent) 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cargar Fichero...")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val isLegacy = lightThemeVersion < AppThemeSetup.CURRENT_THEME_VERSION || darkThemeVersion < AppThemeSetup.CURRENT_THEME_VERSION
                    if (isLegacy && (pendingLightTheme != null || pendingDarkTheme != null) ) {
                        showCompatibilityDialog = true
                    } else {
                        applyChanges()
                    }
                }
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}

@Composable
private fun ThemeInfoRow(label: String, themeName: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.4f))
        Text(themeName, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

private fun applyThemeKeys(editor: android.content.SharedPreferences.Editor, theme: JSONObject) {
    val allKnownKeys = ColorThemeConfig.colorThemeItems.flatMap { listOf(it.lightThemeKey, it.darkThemeKey) }.toSet()
    for (key in theme.keys()) {
        if (allKnownKeys.contains(key)) {
            editor.putString(key, theme.getString(key))
        }
    }
}
