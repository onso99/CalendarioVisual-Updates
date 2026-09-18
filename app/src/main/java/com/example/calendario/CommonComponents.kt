package com.example.calendario

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

/**
 * Librería de Componentes Comunes (Fase 4 - Optimización v3.1.34)
 * Centralización de piezas visuales repetidas para asegurar consistencia.
 */

object AppLayout {
    val BannerHeight = 32.dp
    val ScreenHorizontalPadding = 16.dp
    val TopToSectionPadding = 0.dp // Espacio reservado para el banner informativo (v3.2.06)
}

/**
 * Componente Maestro para todas las pantallas de la App (v3.2.12)
 * Unifica la cabecera, fondo, navegación y sistema de banners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bannerMessage: String? = null,
    isBannerVisible: Boolean = false,
    bannerIcon: ImageVector? = null,
    bannerIconColor: Color = Color.Unspecified,
    topBarExtension: @Composable (() -> Unit)? = null, // NUEVO: Para alojar la barra de selección (v3.2.12.1)
    scrollable: Boolean = true,
    content: @Composable (ColumnScope) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CalendarioTheme.colors.cabecera,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ZONA RESERVADA (32dp - LÍNEA ROJA)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppLayout.BannerHeight),
                contentAlignment = Alignment.Center
            ) {
                if (topBarExtension != null) {
                    topBarExtension()
                } else {
                    InfoBanner(
                        message = bannerMessage,
                        isVisible = isBannerVisible,
                        icon = bannerIcon,
                        iconColor = bannerIconColor
                    )
                }
            }

            // Contenedor de contenido con margen superior normalizado (v3.2.12.1)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AppLayout.TopToSectionPadding)
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            ) {
                content(this)
            }
        }
    }
}

/**
 * Banner informativo común para la parte superior de las pantallas (v3.2.06)
 */
@Composable
fun InfoBanner(
    message: String?,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = Color.Unspecified
) {
    val messageAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    
    var lastKnownMessage by remember { mutableStateOf("") }
    if (message != null) lastKnownMessage = message

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppLayout.BannerHeight),
        contentAlignment = Alignment.Center
    ) {
        if (messageAlpha > 0.01f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (iconColor == Color.Unspecified) 
                               CalendarioTheme.colors.textSystem.copy(alpha = 0.4f * messageAlpha)
                               else iconColor.copy(alpha = 0.6f * messageAlpha)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = lastKnownMessage,
                    fontSize = 12.sp,
                    color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f * messageAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Componente Maestro para todos los diálogos de la App (v3.2.11)
 * Asegura coherencia visual en títulos, fondos y espaciados.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: @Composable (() -> Unit)? = null,
    titleTrailingContent: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    bannerMessage: String? = null,
    isBannerVisible: Boolean = false,
    bannerIcon: ImageVector? = null,
    bannerIconColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        confirmButton = {
            if (confirmButton != null) confirmButton()
        },
        dismissButton = {
            if (dismissButton != null) dismissButton()
        },
        // ANULAMOS SLOTS DEL SISTEMA PARA CONTROLAR EL PADDING (v3.2.12.8)
        title = null,
        icon = null,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. CABECERA PERSONALIZADA (Sin paddings fantasma)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Box(modifier = Modifier.padding(end = 12.dp)) { icon() }
                    }
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = CalendarioTheme.colors.cabecera,
                        modifier = Modifier.weight(1f),
                        textAlign = if (icon != null && titleTrailingContent == null) TextAlign.Center else TextAlign.Start
                    )
                    if (titleTrailingContent != null) {
                        Box(modifier = Modifier.padding(start = 8.dp)) { titleTrailingContent() }
                    }
                }

                // 2. ZONA RESERVADA (32dp - LÃNEA ROJA)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppLayout.BannerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    InfoBanner(
                        message = bannerMessage,
                        isVisible = isBannerVisible,
                        icon = bannerIcon,
                        iconColor = bannerIconColor
                    )
                }

                // 3. CONTENIDO CON COMPENSACIÃ“N VISUAL
                Box(modifier = Modifier.fillMaxWidth().offset(y = (-8).dp)) {
                    content()
                }
            }
        }
    )
}

@Composable
fun AdaptiveButtonText(
    text: String,
    fontSize: TextUnit,
    onOverflow: () -> Unit,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        color = color,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && fontSize > 10.sp) {
                onOverflow()
            }
        }
    )
}

@Composable
fun DialogConfirmButton(
    text: String,
    fontSize: TextUnit = 14.sp,
    onOverflow: () -> Unit = {},
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = CalendarioTheme.colors.cabecera
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        AdaptiveButtonText(
            text = text,
            fontSize = fontSize,
            onOverflow = onOverflow
        )
    }
}

@Composable
fun DialogDismissButton(
    text: String = stringResource(id = R.string.cancel),
    fontSize: TextUnit = 14.sp,
    onOverflow: () -> Unit = {},
    onDismiss: () -> Unit
) {
    TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = CalendarioTheme.colors.textSystem)
    ) {
        AdaptiveButtonText(
            text = text,
            fontSize = fontSize,
            onOverflow = onOverflow,
            color = CalendarioTheme.colors.textSystem
        )
    }
}

@Composable
fun AdaptiveDialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = stringResource(id = R.string.cancel),
    onDismiss: () -> Unit,
    confirmColor: Color = CalendarioTheme.colors.cabecera,
    confirmEnabled: Boolean = true
) {
    var fontSize by remember { mutableStateOf(14.sp) }
    val decreaseSize = {
        if (fontSize > 10.sp) {
            fontSize = (fontSize.value - 0.5f).sp
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DialogDismissButton(
            text = dismissText,
            fontSize = fontSize,
            onOverflow = decreaseSize,
            onDismiss = onDismiss
        )
        Spacer(modifier = Modifier.width(8.dp))
        DialogConfirmButton(
            text = confirmText,
            fontSize = fontSize,
            onOverflow = decreaseSize,
            onClick = onConfirm,
            color = confirmColor,
            enabled = confirmEnabled
        )
    }
}

@Composable
fun SectionTitle(
    text: String, 
    modifier: Modifier = Modifier, 
    topPadding: androidx.compose.ui.unit.Dp = 32.dp, // AUMENTADO A 32dp PARA UNIFORMIDAD (v3.2.12.4)
    isFirst: Boolean = false 
) {
    val titleColor = lerp(
        start = CalendarioTheme.colors.cabecera,
        stop = CalendarioTheme.colors.textSystem,
        fraction = 0.4f,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(bottom = 2.dp, top = if (isFirst) 0.dp else topPadding)
            .offset(y = (-8).dp), // COMPENSACIÃ“N VISUAL AUMENTADA PARA TODAS LAS SECCIONES
        fontWeight = FontWeight.Bold,
        color = titleColor,
    )
}

@Composable
fun WidgetSectionTitle() {
    SectionTitle(
        text = stringResource(id = R.string.widget),
        topPadding = 32.dp // SINCRONIZADO CON LÃNEA ROJA
    )
}

@Composable
fun ActionRow(
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
            detail?.let {
                Text(
                    text = it,
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
fun AppActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    containerColor: Color = Color.Transparent,
    icon: Any? = null, // Puede ser ImageVector o Painter
    isIconRotating: Boolean = false,
    reverseRotation: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) CalendarioTheme.colors.cabecera.copy(alpha = 0.28f) else containerColor,
        animationSpec = if (isPressed) repeatable(3, tween(60)) else tween(500),
        label = "flash"
    )

    val borderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.1f)

    // Animación de rotación infinita
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "angle"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isIconRotating,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (icon != null) {
                val iconModifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        if (isIconRotating) rotationZ = if (reverseRotation) -rotation else rotation
                    }

                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon, 
                            contentDescription = null, 
                            modifier = iconModifier,
                            tint = CalendarioTheme.colors.textSystem
                        )
                    }
                    is Painter -> {
                        Icon(
                            painter = icon, 
                            contentDescription = null, 
                            modifier = iconModifier,
                            tint = CalendarioTheme.colors.textSystem
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = if (isIconRotating) CalendarioTheme.colors.textSystem.copy(alpha = 0.5f) else CalendarioTheme.colors.textSystem,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
