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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZoomedListSimulation() {
    val textSystem = Color.Black
    val laneColor = Color(0xFF2196F3) 

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Ejemplo 1: Evento Normal
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("15", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
            Text("Cena con amigos", fontSize = 18.sp, color = textSystem)
        }

        // Ejemplo 2: Evento Largo con CUADRADO MINÚSCULO
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("16", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
            
            // EL CUADRADO MINÚSCULO
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(laneColor)
            )
            
            Spacer(Modifier.width(6.dp))
            
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Viaje a Londres con mucha maleta...", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Normal, 
                    color = textSystem,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " (2/4)", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Normal,
                    color = textSystem,
                    maxLines = 1
                )
            }
        }

        // Ejemplo 3: Otro Evento Normal
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("17", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
            Text("Reunión de equipo", fontSize = 18.sp, color = textSystem)
        }
    }
}

// Preview con ancho muy pequeño para forzar que el contenido se vea "grande" en la imagen resultante
@Preview(showBackground = true, widthDp = 220)
@Composable
fun PreviewZoomed() {
    ZoomedListSimulation()
}
