package com.example.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ShareIconWithFillet(
    iconColor: Color = Color.White,
    filletColor: Color = Color.Black
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
        val offset = 1.dp
        Icon(Icons.Default.Share, contentDescription = null, tint = filletColor, modifier = Modifier.offset(x = offset, y = offset).size(24.dp))
        Icon(Icons.Default.Share, contentDescription = null, tint = filletColor, modifier = Modifier.offset(x = -offset, y = -offset).size(24.dp))
        Icon(Icons.Default.Share, contentDescription = null, tint = filletColor, modifier = Modifier.offset(x = offset, y = -offset).size(24.dp))
        Icon(Icons.Default.Share, contentDescription = null, tint = filletColor, modifier = Modifier.offset(x = -offset, y = offset).size(24.dp))
        Icon(Icons.Default.Share, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF001F29) // Fondo azul muy oscuro
@Composable
fun PreviewFilletIcon() {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        ShareIconWithFillet(
            iconColor = Color.Black,
            filletColor = Color.White
        )
    }
}
