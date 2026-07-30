package com.example.calendario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.calendario.ui.theme.isColorDark


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoToYearDialog(
    initialYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
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
                IconButton(onClick = {
                    val currentYear = year.toIntOrNull() ?: initialYear
                    val newYear = (currentYear - 1).coerceIn(minYear, maxYear)
                    year = newYear.toString()
                }) {
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
            DialogConfirmButton(
                text = stringResource(id = R.string.accept),
                onClick = {
                    val selectedYear = year.toIntOrNull()?.coerceIn(minYear, maxYear) ?: initialYear
                    onYearSelected(selectedYear)
                    onDismissRequest()
                }
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeSetting,
    onThemeSelected: (ThemeSetting) -> Unit,
    onDismiss: () -> Unit
) {
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
                        Modifier.fillMaxWidth().clickable { onThemeSelected(theme); onDismiss() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = theme.displayNameRes),
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        if (theme == currentTheme) {
                            val checkColor = if (isColorDark(CalendarioTheme.colors.fondoDialogos, MaterialTheme.colorScheme.background)) {
                                CalendarioTheme.colors.textSystem
                            } else {
                                CalendarioTheme.colors.cabecera
                            }
                            Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.custom_selected), tint = checkColor)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { DialogDismissButton(onDismiss = onDismiss) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.confirm_deletion_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { Text(stringResource(id = R.string.confirm_deletion_message, title)) },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(id = R.string.delete),
                onClick = onConfirm,
                color = Color.Red
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
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
                    timeSelectorSelectedContentColor = if (isColorDark(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.fondoSecciones)) Color.White else Color.Black,
                    periodSelectorSelectedContainerColor = CalendarioTheme.colors.cabecera
                )
            )
        },
        confirmButton = { 
            DialogConfirmButton(
                text = stringResource(id = R.string.accept),
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}
