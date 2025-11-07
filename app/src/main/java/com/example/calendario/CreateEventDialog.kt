package com.example.calendario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CreateEventDialog(
    onDismissRequest: () -> Unit,
    onSaveRequest: (title: String, isAllDay: Boolean) -> Unit,
    eventDate: LocalDate,
    calendarName: String
) {
    var title by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Añadir evento", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del evento") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Todo el día", modifier = Modifier.weight(1f))
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text("Resumen de datos a guardar:", fontWeight = FontWeight.Bold)
                Text("Calendario: $calendarName")
                Text("Título: $title")
                Text("Fecha: ${eventDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                Text("Todo el día: ${if (isAllDay) "Sí" else "No"}")
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveRequest(title, isAllDay)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}