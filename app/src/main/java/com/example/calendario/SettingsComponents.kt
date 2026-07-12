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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.calendario.ui.theme.CalendarioTheme

// FORMA: BotÃ³n Izquierda (Mordisco en la derecha)
val LeftConcaveShape = GenericShape { size, _ ->
    val corner = size.height * 0.25f
    val depth = size.height * 0.25f // Profundidad del mordisco
    
    moveTo(corner, 0f)
    lineTo(size.width, 0f)
    // Mordisco hacia adentro (CÃ³ncavo)
    quadraticBezierTo(size.width - depth, size.height / 2f, size.width, size.height)
    lineTo(corner, size.height)
    quadraticBezierTo(0f, size.height, 0f, size.height - corner)
    lineTo(0f, corner)
    quadraticBezierTo(0f, 0f, corner, 0f)
    close()
}

// FORMA: BotÃ³n Derecha (Mordisco en la izquierda)
val RightConcaveShape = GenericShape { size, _ ->
    val corner = size.height * 0.25f
    val depth = size.height * 0.25f
    
    moveTo(0f, 0f)
    lineTo(size.width - corner, 0f)
    quadraticBezierTo(size.width, 0f, size.width, corner)
    lineTo(size.width, size.height - corner)
    quadraticBezierTo(size.width, size.height, size.width - corner, size.height)
    lineTo(0f, size.height)
    // Mordisco hacia adentro (CÃ³ncavo) en la izquierda
    quadraticBezierTo(depth, size.height / 2f, 0f, 0f)
    close()
}

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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
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

    val borderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CalendarioTheme.colors.textSystem,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F2F5)
@Composable
fun CorrectedPuzzlePreview() {
    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // BotÃ³n Izquierda con mordisco suave
            SettingsActionChip(
                text = "Cargar",
                onClick = {},
                modifier = Modifier.weight(1f),
                shape = LeftConcaveShape
            )

            // Icono Central (Engranaje)
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.padding(horizontal = 4.dp).size(26.dp)
            )

            // BotÃ³n Derecha con mordisco suave
            SettingsActionChip(
                text = "Guardar",
                onClick = {},
                modifier = Modifier.weight(1f),
                shape = RightConcaveShape
            )
        }
    }
}
