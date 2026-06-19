package com.example.calendario

import androidx.compose.foundation.background
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
fun ShapeSimulationLarge() {
    val bgColor = Color(0xFFF5F5F5)
    val textSystem = Color.Black
    val laneColor = Color(0xFF2196F3) 
    val calendarColor = Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(40.dp)
    ) {
        // --- SECCIÓN 1: DIÁLOGO DETALLADO ---
        Text("1. DIÁLOGO (Círculos y Cuadrados)", fontWeight = FontWeight.Bold, fontSize = 36.sp)
        Spacer(Modifier.height(30.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            // Evento largo con CUADRADO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(laneColor, RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(25.dp))
                Text("Viaje a Londres (1/4)", fontSize = 32.sp, color = textSystem)
            }
            // Evento normal con CÍRCULO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(calendarColor, CircleShape)
                )
                Spacer(Modifier.width(25.dp))
                Text("10:00 Dentista", fontSize = 32.sp, color = textSystem)
            }
        }

        Spacer(Modifier.height(80.dp))

        // --- SECCIÓN 2: LISTA PRINCIPAL ---
        Text("2. LISTA PRINCIPAL (Efecto Escalera)", fontWeight = FontWeight.Bold, fontSize = 36.sp)
        Spacer(Modifier.height(30.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Evento Normal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("15", fontSize = 32.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
                Text("Cena con amigos", fontSize = 32.sp, color = textSystem)
            }
            
            // Evento Largo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("16", fontSize = 32.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
                Box(
                    Modifier
                        .width(10.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(laneColor)
                )
                Spacer(Modifier.width(20.dp))
                Text("Viaje a Londres (2/4)", fontSize = 32.sp, color = textSystem)
            }

            // Otro Evento Normal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("17", fontSize = 32.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
                Text("Reunión equipo", fontSize = 32.sp, color = textSystem)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 1200)
@Composable
fun PreviewShapesLarge() {
    ShapeSimulationLarge()
}
