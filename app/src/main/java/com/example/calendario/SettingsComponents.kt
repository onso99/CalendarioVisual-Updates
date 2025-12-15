package com.example.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionTitle(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(
        text = text,
        style = typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
        fontWeight = FontWeight.Bold,
        color = colorScheme.primary
    )
}

@Composable
internal fun WidgetSectionTitle() {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(
        text = "Widget",
        style = typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
        fontWeight = FontWeight.Bold,
        color = colorScheme.primary
    )
}

@Composable
internal fun ActionRow(text: String, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ColorPickerRow(label: String, currentColor: Color, onColorBoxClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f), color = colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(currentColor, CircleShape)
                .border(1.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onColorBoxClick)
        )
    }
}
