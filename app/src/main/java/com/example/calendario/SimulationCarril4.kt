package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CellCarril4(
    day: String,
    hasOverflow: Boolean = false,
    lane4Color: Color = Color(0xFFFFA500) // Naranja por defecto
) {
    val laneColors = listOf(Color.Red, Color.Green, Color.Blue)
    val overflowColor = Color.Gray

    Box(
        modifier = Modifier
            .size(65.dp)
            .border(0.5.dp, Color.LightGray)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Carriles 1, 2 y 3 (Siempre normales)
            laneColors.forEach { color ->
                Box(modifier = Modifier.fillMaxWidth().height(2.5.dp)) {
                    Box(modifier = Modifier.align(Alignment.CenterStart).width(8.dp).fillMaxHeight().background(color, RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp)))
                    Box(modifier = Modifier.align(Alignment.CenterEnd).width(8.dp).fillMaxHeight().background(color, RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp)))
                }
            }

            // Carril 4 (Dinámico)
            val height = if (hasOverflow) 5.dp else 2.5.dp
            val color = if (hasOverflow) overflowColor else lane4Color
            
            Box(modifier = Modifier.fillMaxWidth().height(height)) {
                Box(modifier = Modifier.align(Alignment.CenterStart).width(8.dp).fillMaxHeight().background(color, RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp)))
                Box(modifier = Modifier.align(Alignment.CenterEnd).width(8.dp).fillMaxHeight().background(color, RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp)))
            }
        }

        Text(text = day, fontSize = 20.sp, fontWeight = FontWeight.Normal)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSimulationCarril4() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Text("Simulación: El 4º evento (Naranja) coincide con un 5º el Miércoles", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Row {
            // Lunes y Martes: 4 eventos (Carril 4 es Naranja)
            CellCarril4(day = "1", hasOverflow = false)
            CellCarril4(day = "2", hasOverflow = false)
            
            // Miércoles, Jueves y Viernes: Aparece un 5º evento (Carril 4 se vuelve Gris Grueso)
            CellCarril4(day = "3", hasOverflow = true)
            CellCarril4(day = "4", hasOverflow = true)
            CellCarril4(day = "5", hasOverflow = true)
            
            // Sábado y Domingo: Vuelve a haber 4 eventos (Carril 4 vuelve a Naranja)
            CellCarril4(day = "6", hasOverflow = false)
            CellCarril4(day = "7", hasOverflow = false)
        }
    }
}
