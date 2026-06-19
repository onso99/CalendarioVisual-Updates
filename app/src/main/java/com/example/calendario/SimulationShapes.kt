package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
fun ShapeSimulation() {
    val bgColor = Color(0xFFF5F5F5)
    val textSystem = Color.Black
    val laneColor = Color(0xFF2196F3) // Azul para periodos
    val calendarColor = Color(0xFF4CAF50) // Verde para calendario de origen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp)
    ) {
        // --- SECCIÓN 1: DIÁLOGO DETALLADO ---
        Text("1. DIÁLOGO (Círculos y Cuadrados)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Evento largo con CUADRADO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(laneColor, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(12.dp))
                Text("Viaje a Londres (1/4)", fontSize = 16.sp, color = textSystem)
            }
            // Evento normal con CÍRCULO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(calendarColor, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text("10:00 Dentista", fontSize = 16.sp, color = textSystem)
            }
        }

        Spacer(Modifier.height(40.dp))

        // --- SECCIÓN 2: LISTA PRINCIPAL ---
        Text("2. LISTA PRINCIPAL (Efecto Escalera)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Evento Normal (Sin nada, pegado al número)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("15", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
                Text("Cena con amigos", fontSize = 16.sp, color = textSystem)
            }
            
            // Evento Largo (Línea + Texto desplazado)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("16", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
                // Línea vertical fina (cuadrado comprimido)
                Box(
                    Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(laneColor)
                )
                Spacer(Modifier.width(8.dp))
                Text("Viaje a Londres (2/4)", fontSize = 16.sp, color = textSystem)
            }

            // Otro Evento Normal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("17", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
                Text("Reunión equipo", fontSize = 16.sp, color = textSystem)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 500, heightDp = 800)
@Composable
fun PreviewShapes() {
    ShapeSimulation()
}
