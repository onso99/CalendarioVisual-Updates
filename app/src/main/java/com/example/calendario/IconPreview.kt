package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, backgroundColor = 0xFFF0F9FE)
@Composable
fun ProposedAppIconPreviewV17() {
    Box(
        modifier = Modifier
            .size(108.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. FONDO DEGRADADO
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0077C2), Color(0xFF38A391), Color(0xFF0077C2))
                    )
                )
        )

        // 2. CUERPO DEL CALENDARIO
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        ) {
            Column {
                // Cabecera Azul marino
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color(0xFF003366))
                )
                
                // Cuadrícula 2x2 de bloques rectangulares
                Box(modifier = Modifier.fillMaxSize().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(width = 16.dp, height = 10.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(2.dp)))
                            Box(Modifier.size(width = 16.dp, height = 10.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(2.dp)))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(width = 16.dp, height = 10.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(2.dp)))
                            Box(Modifier.size(width = 16.dp, height = 10.dp).background(Color(0xFFD32F2F), RoundedCornerShape(2.dp))) // Domingo
                        }
                    }
                }
            }
        }

        // 3. LA ALARMA FLOTANTE
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 21.dp, end = 21.dp)
                .size(18.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(2.dp)
                .background(Color(0xFF0077C2), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
