package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RealisticDayEventsDialogSimulation() {
    val fondoDialogos = Color.White
    val textSystem = Color.Black
    val cabecera = Color(0xFF0077C2)

    Surface(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        color = fondoDialogos,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Título y Botón Añadir
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mié, 17/06/2026", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textSystem)
                Surface(shape = CircleShape, color = cabecera) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // LISTA DE EVENTOS
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // 1. PROPUESTA EVENTO LARGO
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(textSystem.copy(0.04f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Barra vertical distintiva
                    Box(Modifier.width(4.dp).height(40.dp).background(Color(0xFF2196F3)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Viaje a Londres", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textSystem)
                            Spacer(Modifier.width(8.dp))
                            Text("(Día 2 de 4)", fontSize = 13.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Medium)
                        }
                        Text("Finaliza el viernes a las 20:00", fontSize = 13.sp, color = textSystem.copy(0.6f))
                    }
                }

                // 2. EVENTO NORMAL (Tal cual está hoy en tu código)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(Color.Red, CircleShape).border(0.5.dp, textSystem.copy(0.6f), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("10:00 Dentista", fontSize = 16.sp, color = textSystem)
                }

                // 3. EVENTO RECURRENTE (Actual + Icono)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(Color.Green, CircleShape).border(0.5.dp, textSystem.copy(0.6f), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("18:00 Clase de Francés", fontSize = 16.sp, color = textSystem, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Repeat, null, tint = textSystem.copy(0.3f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("CERRAR", modifier = Modifier.align(Alignment.End), color = cabecera, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRealisticEventList() {
    RealisticDayEventsDialogSimulation()
}
