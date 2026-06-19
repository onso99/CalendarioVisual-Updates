package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainEventListSimulationLarge() {
    val bgColor = Color(0xFFF0F9FE) // Fondo tipo Océano claro
    val textSystem = Color.Black
    val oceanColor = Color(0xFF0077C2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp) // Más padding para que respire
    ) {
        Text(
            "Eventos de Junio",
            fontSize = 24.sp, // Título más grande
            fontWeight = FontWeight.Bold,
            color = oceanColor,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Lista de items con mayor separación
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            
            // 1. EVENTO LARGO (Iniciando)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp), // Padding interno mayor
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("15", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = textSystem, modifier = Modifier.width(40.dp))
                
                // BARRA VERTICAL más prominente
                Box(Modifier.width(6.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2196F3)))
                
                Spacer(Modifier.width(12.dp))
                
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Viaje a Londres", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSystem)
                    Spacer(Modifier.width(10.dp))
                    Text("(Día 1/4)", fontSize = 15.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            }

            // 2. EVENTO NORMAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("16", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = textSystem, modifier = Modifier.width(40.dp))
                
                Spacer(Modifier.width(18.dp)) // Espacio equivalente a la barra
                
                Text("10:00 Dentista", fontSize = 18.sp, color = textSystem, modifier = Modifier.weight(1f))
            }

            // 3. EVENTO LARGO (Continuación)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("16", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = textSystem, modifier = Modifier.width(40.dp))
                
                Box(Modifier.width(6.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2196F3)))
                
                Spacer(Modifier.width(12.dp))
                
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Viaje a Londres", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSystem)
                    Spacer(Modifier.width(10.dp))
                    Text("(Día 2/4)", fontSize = 15.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            }

            // 4. EVENTO HOY (Resaltado)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF9C4)) // Hoy
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("17", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textSystem, modifier = Modifier.width(40.dp))
                
                Spacer(Modifier.width(18.dp))
                
                Text("18:00 Clase de Francés", fontSize = 18.sp, color = textSystem, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Refresh, null, tint = textSystem.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun PreviewMainEventListLarge() {
    MainEventListSimulationLarge()
}
