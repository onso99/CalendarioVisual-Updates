package com.example.calendario

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark

enum class ThemeType {
    LIGHT,
    DARK
}

@Composable
fun ThemeMixerDialog(
    onDismissRequest: () -> Unit,
    onThemeMixed: (type: ThemeType) -> Unit,
    onUriSelected: (uri: Uri) -> Unit,
    lightThemeName: String?,
    darkThemeName: String?
) {
    val context = LocalContext.current
    var selectedThemeType by remember { mutableStateOf(ThemeType.LIGHT) }
    var isFileLoaded by remember { mutableStateOf(false) }

    var stagedLightThemeName by remember { mutableStateOf(lightThemeName) }
    var stagedDarkThemeName by remember { mutableStateOf(darkThemeName) }

    val onFondoDialogos = if (isColorDark(CalendarioTheme.colors.fondoDialogos)) Color.White else Color.Black
    val colorScheme = MaterialTheme.colorScheme

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                onUriSelected(uri)
                isFileLoaded = true
                val fileName = getFileNameFromUri(context, uri)?.removeSuffix(".json")

                if (selectedThemeType == ThemeType.LIGHT) {
                    stagedLightThemeName = fileName
                } else {
                    stagedDarkThemeName = fileName
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text("Mezclador de Temas", fontWeight = FontWeight.Bold, color = onFondoDialogos, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text("Seleccionar Tema", color = onFondoDialogos, fontWeight = FontWeight.Medium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectedThemeType = ThemeType.LIGHT }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedThemeType == ThemeType.LIGHT, onClick = { selectedThemeType = ThemeType.LIGHT })
                        Text("Claro", color = onFondoDialogos)
                    }
                    stagedLightThemeName?.let { Text(it, color = onFondoDialogos.copy(alpha = 0.7f), fontSize = 14.sp) }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectedThemeType = ThemeType.DARK }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically){
                        RadioButton(selected = selectedThemeType == ThemeType.DARK, onClick = { selectedThemeType = ThemeType.DARK })
                        Text("Oscuro", color = onFondoDialogos)
                    }
                    stagedDarkThemeName?.let { Text(it, color = onFondoDialogos.copy(alpha = 0.7f), fontSize = 14.sp) }
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                        }
                        filePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary.copy(alpha = 0.6f),
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text("Cargar fichero...")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onThemeMixed(selectedThemeType) },
                enabled = isFileLoaded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    disabledContainerColor = colorScheme.primary.copy(alpha = 0.4f),
                    disabledContentColor = onFondoDialogos.copy(alpha = 0.4f)
                )
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar", color = onFondoDialogos) } }
    )
}
