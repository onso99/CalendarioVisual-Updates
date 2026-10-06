package com.example.calendario

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme
import java.time.LocalDate

enum class SearchScope { MONTH, YEAR }

object AppLayout {
    val BannerHeight = 32.dp
    val ScreenHorizontalPadding = 16.dp
    val TopToSectionPadding = 0.dp 
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    selectionCount: Int = 0, 
    onClearSelection: () -> Unit = {}, 
    bannerMessage: String? = null,
    isBannerVisible: Boolean = false,
    bannerIcon: ImageVector? = null,
    bannerIconColor: Color = Color.Unspecified,
    topBarExtension: @Composable (() -> Unit)? = null, 
    scrollable: Boolean = true,
    content: @Composable (ColumnScope) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    if (selectionCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = stringResource(id = R.string.selected_count_short, selectionCount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = onClearSelection, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close, 
                                    contentDescription = null, 
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = title, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp
                        ) 
                    }
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

@Composable
fun InfoBanner(
    message: String?,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Center,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp 
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
                horizontalArrangement = if (textAlign == TextAlign.Start) Arrangement.Start else Arrangement.Center,
                modifier = Modifier.padding(horizontal = horizontalPadding).fillMaxWidth()
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
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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
    bannerTextAlign: TextAlign = TextAlign.Center, 
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
        title = null,
        icon = null,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
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

                // 2. ZONA RESERVADA (32dp - LÍNEA ROJA) (v3.3.04.4)
                val hasBanner = isBannerVisible || bannerMessage != null
                if (hasBanner) {
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
                            iconColor = bannerIconColor,
                            textAlign = bannerTextAlign,
                            horizontalPadding = 0.dp 
                        )
                    }
                } else {
                    Spacer(Modifier.height(48.dp)) // AUMENTADO (v3.3.04.5) para dar el máximo aire respecto al título
                }

                // 3. CONTENIDO (v3.3.04.5)
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
    topPadding: androidx.compose.ui.unit.Dp = 32.dp, 
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
            .offset(y = (-8).dp), 
        fontWeight = FontWeight.Bold,
        color = titleColor,
    )
}

@Composable
fun SyncDriveDialog(
    isSyncing: Boolean,
    statusMessage: String?,
    onSync: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = "Sincronizar",
        confirmButton = {
            DialogDismissButton(
                text = stringResource(id = R.string.close),
                onDismiss = onDismiss
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BOTÓN SINCRONIZAR CENTRADO (v3.3.07)
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalendarioTheme.colors.cabecera,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing)
                        ),
                        label = "angle"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Sync, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).graphicsLayer {
                            if (isSyncing) rotationZ = -rotation // Sentido anti-horario (v3.3.07)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.sincronizar_label), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 2. FILA DE MENSAJES (Altura fija 28dp para estabilidad)
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                statusMessage?.let {
                    Text(text = it, fontSize = 12.sp, color = CalendarioTheme.colors.cabecera, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CleaningAssistantDialog(
    uiState: CalendarioUiState,
    isScanning: Boolean,
    statusMessage: String?,
    onScan: () -> Unit,
    onDelete: (SearchItem) -> Unit,
    onDeleteAll: () -> Unit,
    onNavigateToDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeight = with(density) { containerSize.height.toDp() }

    AppDialog(
        onDismissRequest = onDismiss,
        title = "Optimizar", 
        confirmButton = {
            // Botón Cerrar en modo texto (v3.3.06.6)
            DialogDismissButton(
                text = stringResource(id = R.string.close),
                onDismiss = onDismiss
            )
        },
        dismissButton = {
            if (uiState.cleaningCandidates.isNotEmpty()) {
                DialogConfirmButton(
                    text = stringResource(id = R.string.delete_all_events_option).substringBefore(" "),
                    color = Color.Red,
                    onClick = onDeleteAll
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize() 
        ) {
            // 1. ZONA SUPERIOR ESTÁTICA (v3.3.06.4)
            // Esta zona mantiene siempre la misma altura para evitar que el diálogo "baile" durante el escaneo
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BOTÓN ESCANEAR
                Button(
                    onClick = {
                        isExpanded = false 
                        onScan()
                    },
                    enabled = !isScanning,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalendarioTheme.colors.cabecera,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(id = R.string.scan_label), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // FILA DE MENSAJES (Altura fija 28dp para estabilidad)
                Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                    statusMessage?.let {
                        Text(text = it, fontSize = 12.sp, color = CalendarioTheme.colors.cabecera, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. RANURA DE RESULTADOS / RESUMEN (Altura fija 56dp para acomodar 2 líneas v3.3.06.5)
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    // Vacío durante el escaneo para no mover nada
                } else if (uiState.cleaningCandidates.isEmpty()) {
                    Text(
                        stringResource(id = R.string.no_cleaning_results), 
                        color = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Fila de expansión manual
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.cleaningCandidates.size} elementos encontrados",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalendarioTheme.colors.textSystem.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = CalendarioTheme.colors.cabecera,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3. LISTA DESPLEGABLE (Solo añade altura si se solicita)
            if (isExpanded && uiState.cleaningCandidates.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight * 0.4f)
                ) {
                    items(uiState.cleaningCandidates) { item ->
                        InternalCleaningCandidateRow(
                            item = item,
                            onClick = { onNavigateToDate(item.date) },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InternalCleaningCandidateRow(
    item: SearchItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember { AppFormats.dayDateAbbr(locale) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = if (item is SearchItem.Event) R.drawable.ic_ghost_24 else R.drawable.ic_all_day_24),
            contentDescription = null,
            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val title = when (item) {
                is SearchItem.Event -> item.festivo.title.ifBlank { stringResource(id = R.string.no_title) }
                is SearchItem.Note -> stringResource(id = R.string.note_label)
            }
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CalendarioTheme.colors.textSystem, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.date.format(fmt), fontSize = 11.sp, color = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun WidgetSectionTitle() {
    SectionTitle(
        text = stringResource(id = R.string.widget),
        topPadding = 32.dp 
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
    icon: Any? = null, 
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(id = R.string.close),
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CalendarioTheme.colors.cabecera,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
