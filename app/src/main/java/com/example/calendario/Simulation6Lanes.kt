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
fun Cell6Lanes(
    day: String,
    leftColors: List<Color>,
    rightColors: List<Color>,
    dots: Int = 0
) {
    Box(
        modifier = Modifier
            .size(70.dp) // Un poco más grande para el móvil medio
            .border(0.5.dp, Color.LightGray)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Área de Carriles (6 posibles)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp), // Margen superior mínimo
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(6) { index ->
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    // Guion Izquierdo
                    if (index < leftColors.size && leftColors[index] != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(7.dp)
                                .fillMaxHeight()
                                .background(leftColors[index], RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
                        )
                    }
                    // Guion Derecho
                    if (index < rightColors.size && rightColors[index] != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(7.dp)
                                .fillMaxHeight()
                                .background(rightColors[index], RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp))
                        )
                    }
                }
            }
        }

        // Número del día (Ligeramente desplazado hacia abajo para no chocar con el bloque de 6 líneas)
        Text(
            text = day,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 10.dp) 
        )

        // Puntos inferiores
        if (dots > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(dots) {
                    Box(Modifier.size(5.dp).background(Color.Gray, CircleShape))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview6Lanes() {
    val c1 = Color(0xFFE91E63) // Rosa
    val c2 = Color(0xFF2196F3) // Azul
    val c3 = Color(0xFF4CAF50) // Verde
    val c4 = Color(0xFFFF9800) // Naranja
    val c5 = Color(0xFF9C27B0) // Morado
    val c6 = Color(0xFF795548) // Marrón
    val empty = Color.Transparent

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Máxima Saturación: 6 Periodos Largos + 2 Puntos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row {
            // Día 1: Empiezan todos
            Cell6Lanes(
                day = "15",
                leftColors = emptyList(),
                rightColors = listOf(c1, c2, c3, c4, c5, c6),
                dots = 1
            )
            // Día 2: Máxima saturación (Continuidad total)
            Cell6Lanes(
                day = "16",
                leftColors = listOf(c1, c2, c3, c4, c5, c6),
                rightColors = listOf(c1, c2, c3, c4, c5, c6),
                dots = 2
            )
            // Día 3: Algunos terminan, otros siguen
            Cell6Lanes(
                day = "17",
                leftColors = listOf(c1, c2, c3, c4, c5, c6),
                rightColors = listOf(c1, empty, c3, empty, c5, empty),
                dots = 1
            )
        }
    }
}
