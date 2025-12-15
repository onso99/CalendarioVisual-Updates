package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark

@Composable
fun ThemeMixerDialog(
    onDismissRequest: () -> Unit,
    onThemeMixed: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    var availableThemes by remember { mutableStateOf<List<ParsedTheme>>(emptyList()) }
    var selectedLightTheme by remember { mutableStateOf<ParsedTheme?>(null) }
    var selectedDarkTheme by remember { mutableStateOf<ParsedTheme?>(null) }

    var lightThemeMenuExpanded by remember { mutableStateOf(false) }
    var darkThemeMenuExpanded by remember { mutableStateOf(false) }

    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        availableThemes = ThemeImportManager.discoverThemes(context)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text("Mezclador de Temas", fontWeight = FontWeight.Bold, color = onFondoDialogos, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                ThemeSelector(label = "Tema Claro:", selectedTheme = selectedLightTheme, themes = availableThemes, expanded = lightThemeMenuExpanded, onExpandedChange = { lightThemeMenuExpanded = it }, onThemeSelected = { selectedLightTheme = it })
                ThemeSelector(label = "Tema Oscuro:", selectedTheme = selectedDarkTheme, themes = availableThemes, expanded = darkThemeMenuExpanded, onExpandedChange = { darkThemeMenuExpanded = it }, onThemeSelected = { selectedDarkTheme = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    prefs.edit {
                        selectedLightTheme?.lightTheme?.let { theme ->
                            theme.keys().forEach { key ->
                                putString(key, theme.getString(key))
                            }
                        }
                        selectedDarkTheme?.darkTheme?.let { theme ->
                            theme.keys().forEach { key ->
                                putString(key, theme.getString(key))
                            }
                        }
                        putString(AppConstants.KEY_LIGHT_THEME_NAME, selectedLightTheme?.manifest?.optString("name") ?: "Mixto")
                        putString(AppConstants.KEY_DARK_THEME_NAME, selectedDarkTheme?.manifest?.optString("name") ?: "Mixto")
                    }
                    onThemeMixed()
                },
                enabled = selectedLightTheme != null && selectedDarkTheme != null,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) { Text("Mezclar") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", color = onFondoDialogos) } }
    )
}

@Composable
private fun ThemeSelector(label: String, selectedTheme: ParsedTheme?, themes: List<ParsedTheme>, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onThemeSelected: (ParsedTheme) -> Unit) {
    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, fontWeight = FontWeight.Medium, color = onFondoDialogos, fontSize = 16.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(onFondoDialogos.copy(alpha = 0.1f))
                .clickable { onExpandedChange(true) }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedTheme?.manifest?.optString("name") ?: "Seleccionar un tema", modifier = Modifier.weight(1f), color = onFondoDialogos)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar", tint = onFondoDialogos)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }, modifier = Modifier.background(CalendarioTheme.colors.dropdownMenuBackground)) {
                LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                    items(themes) { theme ->
                        DropdownMenuItem(text = { Text(theme.manifest?.optString("name") ?: "Tema sin nombre") }, onClick = { onThemeSelected(theme); onExpandedChange(false) })
                    }
                }
            }
        }
    }
}
