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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalendarCellSimulation(
    day: String,
    leftSeams: List<Color> = emptyList(),
    rightSeams: List<Color> = emptyList(),
    dots: List<Color> = emptyList()
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .border(0.5.dp, Color.LightGray)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Carriles de "Costuras" (Seams)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(4) { index ->
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    // Guion Izquierdo (Viene de ayer)
                    if (index < leftSeams.size) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(6.dp)
                                .height(2.dp)
                                .background(leftSeams[index], RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
                        )
                    }
                    // Guion Derecho (Sigue mañana)
                    if (index < rightSeams.size) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(6.dp)
                                .height(2.dp)
                                .background(rightSeams[index], RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp))
                        )
                    }
                }
            }
        }

        // Número del día
        Text(
            text = day,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )

        // Puntos de eventos puntuales
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            dots.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(color, CircleShape)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalendarSeams() {
    val ocean = Color(0xFF0077C2)
    val forest = Color(0xFF5B9741)
    val volcano = Color(0xFFE64A19)
    val lavender = Color(0xFF7E57C2)

    Row {
        // Día 1: Empiezan 2 eventos prolongados, tiene 1 evento puntual
        CalendarCellSimulation(
            day = "1",
            rightSeams = listOf(ocean, forest),
            dots = listOf(Color.Gray)
        )
        // Día 2: Continúan 2 eventos, empieza un 3º, tiene 2 puntuales
        CalendarCellSimulation(
            day = "2",
            leftSeams = listOf(ocean, forest),
            rightSeams = listOf(ocean, forest, volcano),
            dots = listOf(Color.Gray, lavender)
        )
        // Día 3: Termina el 1º (Océano), continúa el 2º y 3º, tiene 1 puntual
        CalendarCellSimulation(
            day = "3",
            leftSeams = listOf(ocean, forest, volcano),
            rightSeams = listOf(Color.Transparent, forest, volcano),
            dots = listOf(volcano)
        )
    }
}
