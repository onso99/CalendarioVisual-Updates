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
fun MainEventListSimulation() {
    val bgColor = Color(0xFFF0F9FE) // Fondo tipo Océano claro
    val textSystem = Color.Black
    val todayColor = Color(0xFFE3F2FD)
    val oceanColor = Color(0xFF0077C2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp)
    ) {
        Text(
            "Eventos de Junio",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = oceanColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Lista de items
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            
            // 1. EVENTO LARGO (Iniciando hoy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("15", fontSize = 16.sp, color = textSystem, modifier = Modifier.width(24.dp))
                
                // BARRA VERTICAL de periodo largo
                Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2196F3)))
                
                Spacer(Modifier.width(8.dp))
                
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Viaje a Londres", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textSystem, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Text("(Día 1/4)", fontSize = 13.sp, color = Color(0xFF2196F3))
                }
            }

            // 2. EVENTO NORMAL (Día siguiente)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("16", fontSize = 16.sp, color = textSystem, modifier = Modifier.width(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("10:00 Dentista", fontSize = 16.sp, color = textSystem, modifier = Modifier.weight(1f))
            }

            // 3. EVENTO LARGO (Continuando)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("16", fontSize = 16.sp, color = textSystem, modifier = Modifier.width(24.dp))
                
                Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2196F3)))
                
                Spacer(Modifier.width(8.dp))
                
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Viaje a Londres", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textSystem)
                    Spacer(Modifier.width(6.dp))
                    Text("(Día 2/4)", fontSize = 13.sp, color = Color(0xFF2196F3))
                }
            }

            // 4. EVENTO HOY (Resaltado)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF9C4)) // Fondo hoy (amarillo sutil)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("17", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textSystem, modifier = Modifier.width(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("18:00 Clase de Francés", fontSize = 16.sp, color = textSystem, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Refresh, null, tint = textSystem.copy(0.4f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainEventList() {
    MainEventListSimulation()
}
