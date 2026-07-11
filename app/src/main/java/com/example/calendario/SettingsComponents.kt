package com.example.calendario

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val titleColor = lerp(
        start = CalendarioTheme.colors.cabecera,
        stop = CalendarioTheme.colors.textSystem,
        fraction = 0.4f
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(bottom = 8.dp, top = 24.dp),
        fontWeight = FontWeight.Bold,
        color = titleColor
    )
}

@Composable
internal fun WidgetSectionTitle() {
    val titleColor = lerp(
        start = CalendarioTheme.colors.cabecera,
        stop = CalendarioTheme.colors.textSystem,
        fraction = 0.4f
    )
    Text(
        text = stringResource(id = R.string.widget),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp, top = 24.dp),
        fontWeight = FontWeight.Bold,
        color = titleColor
    )
}

@Composable
internal fun ActionRow(
    text: String, 
    detail: String? = null, 
    isLoading: Boolean = false, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = text, 
                color = if (isLoading) CalendarioTheme.colors.textSystem.copy(alpha = 0.4f) else CalendarioTheme.colors.textSystem, 
                fontSize = 16.sp
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CalendarioTheme.colors.cabecera,
                strokeWidth = 2.5.dp,
                strokeCap = StrokeCap.Round
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun ColorPickerRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label, 
            fontSize = 16.sp, 
            modifier = Modifier.weight(1f), 
            color = CalendarioTheme.colors.textSystem
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), CircleShape)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
internal fun SettingsActionChip(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Flash dinÃ¡mico 'parpadeante':
    // Al presionar, parpadea rÃ¡pidamente. Al soltar, se desvanece.
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) CalendarioTheme.colors.cabecera.copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = if (isPressed) {
            repeatable(
                iterations = 3,
                animation = tween(durationMillis = 60),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(durationMillis = 500)
        },
        label = "parpadeo"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CalendarioTheme.colors.textSystem,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
