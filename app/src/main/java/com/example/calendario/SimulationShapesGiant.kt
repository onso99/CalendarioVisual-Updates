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
fun ShapeSimulationGiant() {
    val bgColor = Color(0xFFF5F5F5)
    val textSystem = Color.Black
    val laneColor = Color(0xFF2196F3) 
    val calendarColor = Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(100.dp)
    ) {
        // --- SECCIÓN 1: DIÁLOGO DETALLADO ---
        Text("1. DIÁLOGO (Círculos y Cuadrados)", fontWeight = FontWeight.Bold, fontSize = 90.sp, lineHeight = 100.sp)
        Spacer(Modifier.height(80.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(60.dp))
                .background(Color.White)
                .padding(80.dp),
            verticalArrangement = Arrangement.spacedBy(60.dp)
        ) {
            // Evento largo con CUADRADO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(80.dp)
                        .background(laneColor, RoundedCornerShape(20.dp))
                )
                Spacer(Modifier.width(60.dp))
                Text("Viaje a Londres (1/4)", fontSize = 80.sp, color = textSystem)
            }
            // Evento normal con CÍRCULO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(80.dp)
                        .background(calendarColor, CircleShape)
                )
                Spacer(Modifier.width(60.dp))
                Text("10:00 Dentista", fontSize = 80.sp, color = textSystem)
            }
        }

        Spacer(Modifier.height(200.dp))

        // --- SECCIÓN 2: LISTA PRINCIPAL ---
        Text("2. LISTA PRINCIPAL (Efecto Escalera)", fontWeight = FontWeight.Bold, fontSize = 90.sp, lineHeight = 100.sp)
        Spacer(Modifier.height(80.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(60.dp))
                .background(Color.White)
                .padding(80.dp),
            verticalArrangement = Arrangement.spacedBy(50.dp)
        ) {
            // Evento Normal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("15", fontSize = 80.sp, color = Color.Gray, modifier = Modifier.width(180.dp))
                Text("Cena con amigos", fontSize = 80.sp, color = textSystem)
            }
            
            // Evento Largo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("16", fontSize = 80.sp, color = Color.Gray, modifier = Modifier.width(180.dp))
                // Línea vertical más gruesa y alta
                Box(
                    Modifier
                        .width(25.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(laneColor)
                )
                Spacer(Modifier.width(40.dp))
                Text("Viaje a Londres (2/4)", fontSize = 80.sp, color = textSystem)
            }

            // Otro Evento Normal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("17", fontSize = 80.sp, color = Color.Gray, modifier = Modifier.width(180.dp))
                Text("Reunión equipo", fontSize = 80.sp, color = textSystem)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 2500, heightDp = 3000)
@Composable
fun PreviewShapesGiant() {
    ShapeSimulationGiant()
}
