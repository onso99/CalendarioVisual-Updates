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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainEventListSimulationSuperLarge() {
    val bgColor = Color(0xFFF0F9FE) 
    val textSystem = Color.Black
    val oceanColor = Color(0xFF0077C2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(40.dp) 
    ) {
        Text(
            "Eventos de Junio",
            fontSize = 60.sp, // Gigante
            fontWeight = FontWeight.Bold,
            color = oceanColor,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
            
            // 1. EVENTO LARGO (Iniciando)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White)
                    .padding(horizontal = 30.dp, vertical = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("15", fontSize = 50.sp, fontWeight = FontWeight.Medium, color = textSystem, modifier = Modifier.width(100.dp))
                
                // BARRA VERTICAL SUPER GRANDE
                Box(Modifier.width(15.dp).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2196F3)))
                
                Spacer(Modifier.width(30.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Viaje a Londres", fontSize = 45.sp, fontWeight = FontWeight.Bold, color = textSystem)
                    }
                    Text("(Día 1 de 4)", fontSize = 35.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            }

            // 2. EVENTO NORMAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White)
                    .padding(horizontal = 30.dp, vertical = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("16", fontSize = 50.sp, fontWeight = FontWeight.Medium, color = textSystem, modifier = Modifier.width(100.dp))
                
                Spacer(Modifier.width(45.dp)) 
                
                Text("10:00 Dentista", fontSize = 45.sp, color = textSystem)
            }

            // 3. EVENTO LARGO (Continuación)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White)
                    .padding(horizontal = 30.dp, vertical = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("16", fontSize = 50.sp, fontWeight = FontWeight.Medium, color = textSystem, modifier = Modifier.width(100.dp))
                
                Box(Modifier.width(15.dp).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2196F3)))
                
                Spacer(Modifier.width(30.dp))
                
                Column {
                    Text("Viaje a Londres", fontSize = 45.sp, fontWeight = FontWeight.Bold, color = textSystem)
                    Text("(Día 2 de 4)", fontSize = 35.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1000)
@Composable
fun PreviewMainEventListSuperLarge() {
    MainEventListSimulationSuperLarge()
}
