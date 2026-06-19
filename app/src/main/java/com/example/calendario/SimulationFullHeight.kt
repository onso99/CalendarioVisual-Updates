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
fun CellFullHeight(
    day: String,
    leftColors: List<Color>,
    rightColors: List<Color>,
    dots: Int = 0
) {
    Box(
        modifier = Modifier
            .size(75.dp) // Un poco más de altura para la simulación
            .border(0.5.dp, Color.LightGray)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Carriles distribuidos en TODA la altura de la celda
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly // Distribución equitativa
        ) {
            repeat(6) { index ->
                Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                    // Guion Izquierdo
                    if (index < leftColors.size && leftColors[index] != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(8.dp)
                                .fillMaxHeight()
                                .background(leftColors[index], RoundedCornerShape(topEnd = 1.5.dp, bottomEnd = 1.5.dp))
                        )
                    }
                    // Guion Derecho
                    if (index < rightColors.size && rightColors[index] != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(8.dp)
                                .fillMaxHeight()
                                .background(rightColors[index], RoundedCornerShape(topStart = 1.5.dp, bottomStart = 1.5.dp))
                        )
                    }
                }
            }
        }

        // Número del día (En el centro, sobre las líneas)
        // Usamos un fondo semi-transparente para asegurar legibilidad si las líneas pasaran por detrás,
        // aunque aquí las líneas están en los bordes.
        Text(
            text = day,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // Puntos inferiores (reducidos para no pisar el último carril)
        if (dots > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(dots) {
                    Box(Modifier.size(4.dp).background(Color.Gray, CircleShape))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFullHeightLanes() {
    val c1 = Color(0xFFE91E63) // Rosa
    val c2 = Color(0xFF2196F3) // Azul
    val c3 = Color(0xFF4CAF50) // Verde
    val c4 = Color(0xFFFF9800) // Naranja
    val c5 = Color(0xFF9C27B0) // Morado
    val c6 = Color(0xFF795548) // Marrón
    val empty = Color.Transparent

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Simulación: 6 Carriles en TODA la altura", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row {
            CellFullHeight(
                day = "20",
                leftColors = emptyList(),
                rightColors = listOf(c1, c2, c3, c4, c5, c6),
                dots = 1
            )
            CellFullHeight(
                day = "21",
                leftColors = listOf(c1, c2, c3, c4, c5, c6),
                rightColors = listOf(c1, c2, c3, c4, c5, c6),
                dots = 2
            )
            CellFullHeight(
                day = "22",
                leftColors = listOf(c1, c2, c3, c4, c5, c6),
                rightColors = listOf(empty, c2, empty, c4, empty, c6),
                dots = 1
            )
        }
    }
}
