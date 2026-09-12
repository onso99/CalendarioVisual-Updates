package com.example.calendario

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoToYearDialog(
    initialYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var year by remember { mutableStateOf(initialYear.toString()) }
    val minYear = 1924
    val maxYear = 2124

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.year_selection_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val currentYear = year.toIntOrNull() ?: initialYear
                        val newYear = (currentYear - 1).coerceIn(minYear, maxYear)
                        year = newYear.toString()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = stringResource(id = R.string.previous_year), modifier = Modifier.size(36.dp), tint = CalendarioTheme.colors.textSystem)
                }
                OutlinedTextField(
                    value = year,
                    onValueChange = {
                        val newText = it.filter { char -> char.isDigit() }.take(4)
                        year = newText
                        if (newText.length == 4) {
                            val newYear = newText.toInt().coerceIn(minYear, maxYear)
                            year = newYear.toString()
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textStyle = TextStyle(textAlign = TextAlign.Center, color = CalendarioTheme.colors.textSystem),
                    singleLine = true
                )
                IconButton(onClick = {
                    val currentYear = year.toIntOrNull() ?: initialYear
                    val newYear = (currentYear + 1).coerceIn(minYear, maxYear)
                    year = newYear.toString()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = stringResource(id = R.string.next_year), modifier = Modifier.size(36.dp), tint = CalendarioTheme.colors.textSystem)
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.accept),
                onConfirm = {
                    val selectedYear = year.toIntOrNull()?.coerceIn(minYear, maxYear) ?: initialYear
                    onYearSelected(selectedYear)
                    onDismissRequest()
                },
                onDismiss = onDismissRequest
            )
        },
        dismissButton = null
    )
}

/**
 * Representa las opciones de idioma soportadas por la App.
 */
enum class AppLanguageSetting(val code: String?, @field:StringRes val displayNameRes: Int) {
    SYSTEM(null, R.string.lang_system),
    ES("es", R.string.lang_es),
    CA("ca", R.string.lang_ca),
    GL("gl", R.string.lang_gl),
    EU("eu", R.string.lang_eu),
    EN("en", R.string.lang_en),
    FR("fr", R.string.lang_fr),
    DE("de", R.string.lang_de),
    IT("it", R.string.lang_it),
    PT("pt", R.string.lang_pt),
    RU("ru", R.string.lang_ru),
    ZH("zh", R.string.lang_zh),
    JA("ja", R.string.lang_ja);

    companion object {
        fun fromCode(code: String?): AppLanguageSetting {
            if (code == null) return SYSTEM
            // Buscamos coincidencia exacta o por prefijo (ej: "es" coincide con "es-ES")
            return entries.find { it.code != null && code.startsWith(it.code) } ?: SYSTEM
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String?,
    onLanguageSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSetting = remember(currentLanguageCode) { AppLanguageSetting.fromCode(currentLanguageCode) }
    var tempSelection by remember { mutableStateOf(initialSetting) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.language), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth()) {
                items(AppLanguageSetting.entries) { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { tempSelection = lang }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSelected = lang == tempSelection
                        Text(
                            text = stringResource(id = lang.displayNameRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            color = CalendarioTheme.colors.textSystem,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                        if (isSelected) {
                            val checkColor = CalendarioTheme.colors.fondoDialogos.getContrastColor(MaterialTheme.colorScheme.background)
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.custom_selected), tint = checkColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.accept),
                onConfirm = { 
                    onLanguageSelected(tempSelection.code)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeSetting,
    onThemeSelected: (ThemeSetting) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelection by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.select_mode_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = {
            Column {
                ThemeSetting.entries.forEach { theme ->
                    Row(
                        Modifier.fillMaxWidth().clickable { tempSelection = theme }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSelected = theme == tempSelection
                        Text(
                            text = stringResource(id = theme.displayNameRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                        if (isSelected) {
                            val checkColor = CalendarioTheme.colors.fondoDialogos.getContrastColor(MaterialTheme.colorScheme.background)
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.custom_selected), tint = checkColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.accept),
                onConfirm = {
                    onThemeSelected(tempSelection)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    icon: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        icon = icon,
        title = { Text(stringResource(id = R.string.confirm_deletion_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = if (icon != null) TextAlign.Center else TextAlign.Start) },
        text = { Text(stringResource(id = R.string.confirm_deletion_message, title), textAlign = if (icon != null) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.delete),
                onConfirm = onConfirm,
                onDismiss = onDismissRequest,
                confirmColor = Color.Red
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timePickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.select_time_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth(),
                colors = TimePickerDefaults.colors(
                    clockDialColor = CalendarioTheme.colors.fondoSecciones,
                    timeSelectorSelectedContainerColor = CalendarioTheme.colors.cabecera,
                    timeSelectorUnselectedContainerColor = CalendarioTheme.colors.fondoSecciones,
                    timeSelectorSelectedContentColor = CalendarioTheme.colors.cabecera.getContrastColor(CalendarioTheme.colors.fondoSecciones),
                    periodSelectorSelectedContainerColor = CalendarioTheme.colors.cabecera
                )
            )
        },
        confirmButton = {
            AdaptiveDialogButtons(
                confirmText = stringResource(id = R.string.accept),
                onConfirm = { onConfirm(timePickerState.hour, timePickerState.minute) },
                onDismiss = onDismissRequest
            )
        }
    )
}
