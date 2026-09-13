package com.example.calendario

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorThemeScreen(
    onBackPress: () -> Unit,
    onThemeModified: () -> Unit,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val isAppDark = darkTheme
    
    var showColorPicker by remember { mutableStateOf(value = false) }
    var showRenameDialog by remember { mutableStateOf(value = false) }
    var pendingItem by remember { mutableStateOf<ColorThemeItem?>(null) }
    var updateTrigger by remember { mutableIntStateOf(0) }
    var showEffectExpand by remember { mutableStateOf(false) }

    val effectType = remember(updateTrigger) { prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient") ?: "gradient" }
    val dividerColor = CalendarioTheme.colors.settingsBackground
    val dividerThickness = 1.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.customize_colors), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- BLOQUE 1: TEMA ---
            SectionTitle(stringResource(id = R.string.theme_section_title))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val themeItems = ColorThemeConfig.colorThemeItems.filter { it.category == "Tema" && it.labelRes != R.string.calendar_background }
                themeItems.forEachIndexed { index, item ->
                    val currentColor = getThemeColor(prefs, if (isAppDark) item.darkThemeKey else item.lightThemeKey, if (isAppDark) item.defaultDark else item.defaultLight)
                    
                    if (item.labelRes == R.string.effect) {
                        val backgroundItem = ColorThemeConfig.colorThemeItems.find { it.labelRes == R.string.calendar_background }!!
                        val color1 = getThemeColor(prefs, if (isAppDark) backgroundItem.darkThemeKey else backgroundItem.lightThemeKey, if (isAppDark) backgroundItem.defaultDark else backgroundItem.defaultLight)

                        EffectColorThemeRow(
                            label = stringResource(id = item.labelRes),
                            color1 = color1,
                            color2 = currentColor,
                            effectType = effectType,
                            isExpanded = showEffectExpand,
                            onExpandClick = { showEffectExpand = !showEffectExpand },
                            onEffectChange = { newType ->
                                prefs.edit { putString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, newType) }
                                ThemePersistence.markThemeAsModified(prefs)
                                onThemeModified()
                                updateTrigger++
                            },
                            onColor1Click = {
                                pendingItem = backgroundItem
                                showColorPicker = true
                            },
                            onColor2Click = {
                                pendingItem = item
                                showColorPicker = true
                            },
                            onExchange = {
                                val key1 = if (isAppDark) backgroundItem.darkThemeKey else backgroundItem.lightThemeKey
                                val key2 = if (isAppDark) item.darkThemeKey else item.lightThemeKey
                                val c1 = color1.toArgb()
                                val c2 = currentColor.toArgb()
                                prefs.edit {
                                    putInt(key1, c2)
                                    putInt(key2, c1)
                                }
                                ThemePersistence.markThemeAsModified(prefs)
                                onThemeModified()
                                updateTrigger++
                            }
                        )
                    } else {
                        SingleColorThemeRow(
                            label = stringResource(id = item.labelRes),
                            color = currentColor,
                            onClick = {
                                pendingItem = item
                                showColorPicker = true
                            }
                        )
                    }
                    if (index < (themeItems.size - 1)) {
                        HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BLOQUE 2: PROPIOS ---
            SectionTitle(stringResource(id = R.string.propios_section_title))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val propiosItems = ColorThemeConfig.colorThemeItems.filter { it.category == "Propios" }
                propiosItems.forEachIndexed { index, item ->
                    val key = if (isAppDark) item.darkThemeKey else item.lightThemeKey
                    val defaultColor = if (isAppDark) item.defaultDark else item.defaultLight
                    val currentColor = getThemeColor(prefs, key, defaultColor)
                    
                    val keywordKey = when (item.labelRes) {
                        R.string.event_1 -> AppConstants.KEY_EVENT_1_KEYWORD
                        R.string.event_2 -> AppConstants.KEY_EVENT_2_KEYWORD
                        else -> null
                    }
                    
                    val pulseKey = when (item.labelRes) {
                        R.string.event_1 -> AppConstants.KEY_EVENT_1_PULSE
                        R.string.event_2 -> AppConstants.KEY_EVENT_2_PULSE
                        else -> null
                    }
                    
                    val isPulsing = pulseKey?.let { prefs.getBoolean(it, false) } ?: false

                    val currentLabel = keywordKey?.let { prefs.getString(it, "") }?.takeIf { it.isNotBlank() } 
                                       ?: stringResource(id = item.labelRes)

                    SingleColorThemeRow(
                        label = currentLabel,
                        color = currentColor,
                        isPulsing = isPulsing,
                        onPulseClick = if (pulseKey != null) { {
                            prefs.edit { putBoolean(pulseKey, !isPulsing) }
                            updateTrigger++
                        } } else null,
                        onLabelClick = if (keywordKey != null) { { 
                            pendingItem = item
                            showRenameDialog = true 
                        } } else null,
                        onReset = {
                            prefs.edit { 
                                remove(key)
                                keywordKey?.let { remove(it) }
                                pulseKey?.let { remove(it) }
                            }
                            ThemePersistence.markThemeAsModified(prefs)
                            onThemeModified()
                            updateTrigger++
                        },
                        onClick = {
                            pendingItem = item
                            showColorPicker = true
                        }
                    )
                    if (index < propiosItems.size - 1) {
                        HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    }
                }
            }
        }
    }

    if (showRenameDialog && pendingItem != null) {
        val item = pendingItem!!
        val keywordKey = if (item.labelRes == R.string.event_1) AppConstants.KEY_EVENT_1_KEYWORD else AppConstants.KEY_EVENT_2_KEYWORD
        val currentVal = prefs.getString(keywordKey, "") ?: ""

        RenameEventDialog(
            initialName = currentVal,
            onDismissRequest = { showRenameDialog = false },
            onConfirm = { newName ->
                prefs.edit { putString(keywordKey, newName.trim()) }
                ThemePersistence.markThemeAsModified(prefs)
                onThemeModified()
                updateTrigger++
                showRenameDialog = false
            }
        )
    }

    if (showColorPicker && pendingItem != null) {
        val item = pendingItem!!
        val key = if (isAppDark) item.darkThemeKey else item.lightThemeKey
        val currentColor = getThemeColor(prefs, key, if (isAppDark) item.defaultDark else item.defaultLight)

        AdvancedColorPickerDialog(
            initialColor = currentColor,
            onDismissRequest = { showColorPicker = false },
            onColorConfirm = { newColor ->
                prefs.edit { putInt(key, newColor.toArgb()) }
                if (!item.isIndependent) {
                    ThemePersistence.markThemeAsModified(prefs)
                }
                // SIEMPRE notificamos la modificación para refrescar la App (v3.1.34)
                onThemeModified()
                updateTrigger++
                showColorPicker = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val titleColor = lerp(CalendarioTheme.colors.cabecera, CalendarioTheme.colors.textSystem, 0.4f)
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
        fontWeight = FontWeight.Bold,
        color = titleColor
    )
}

private fun getThemeColor(prefs: SharedPreferences, key: String, default: Color): Color {
    val colorInt = try {
        prefs.getInt(key, default.toArgb())
    } catch (_: ClassCastException) {
        (prefs.all[key] as? Number)?.toInt() ?: default.toArgb()
    }
    return Color(colorInt)
}

@Composable
private fun SingleColorThemeRow(
    label: String,
    color: Color,
    isPulsing: Boolean = false,
    onPulseClick: (() -> Unit)? = null,
    onLabelClick: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var labelFontSize by remember { mutableStateOf(16.sp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onLabelClick ?: onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CalendarioTheme.colors.textSystem, 
            fontSize = labelFontSize, 
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.hasVisualOverflow && labelFontSize > 12.sp) {
                    labelFontSize = (labelFontSize.value - 1f).sp
                }
            }
        )
        
        if (onPulseClick != null) {
            IconButton(onClick = onPulseClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.WbSunny, 
                    contentDescription = "Activar parpadeo",
                    tint = if (isPulsing) CalendarioTheme.colors.cabecera else Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (onReset != null) {
            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Refresh, 
                    stringResource(id = R.string.reset_color), 
                    tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        ColorBox(color = color, onClick = onClick)
    }
}

@Composable
private fun EffectColorThemeRow(
    label: String,
    color1: Color,
    color2: Color,
    effectType: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onEffectChange: (String) -> Unit,
    onColor1Click: () -> Unit,
    onColor2Click: () -> Unit,
    onExchange: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onExpandClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = CalendarioTheme.colors.textSystem,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Selector de efecto [0 1 2]
                val options = listOf("none" to "0", "gradient" to "1", "sweep" to "2")
                val baseColor = CalendarioTheme.colors.fondoSecciones
                val activeColor = CalendarioTheme.colors.cabecera
                val innerHeight = 32.dp

                Row(
                    modifier = Modifier
                        .height(innerHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(baseColor.copy(alpha = 0.5f))
                        .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    options.forEach { (type, text) ->
                        val isSelected = effectType == type
                        val containerColor = if (isSelected) activeColor else Color.Transparent
                        val textColor = if (isSelected) {
                            activeColor.getContrastColor(baseColor)
                        } else {
                            CalendarioTheme.colors.textSystem.copy(alpha = 0.6f)
                        }

                        Box(
                            modifier = Modifier
                                .height(innerHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(containerColor)
                                .clickable { onEffectChange(type) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // 2. Simulación
                val previewBrush = when (effectType) {
                    "gradient" -> Brush.verticalGradient(listOf(color1, color2))
                    "sweep" -> Brush.verticalGradient(listOf(color1, color2, color1))
                    else -> androidx.compose.ui.graphics.SolidColor(color1)
                }
                
                Box(
                    modifier = Modifier
                        .size(innerHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(previewBrush)
                        .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )

                // 3. Colores + Intercambio
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorBox(color = color1, onClick = onColor1Click)
                    IconButton(onClick = onExchange, modifier = Modifier.padding(horizontal = 2.dp).size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    ColorBox(color = color2, onClick = onColor2Click)
                }
            }
        }
    }
}

@Composable
private fun RenameEventDialog(initialName: String, onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        titleContentColor = CalendarioTheme.colors.textSystem,
        textContentColor = CalendarioTheme.colors.textSystem,
        title = { Text(stringResource(id = R.string.customize_colors), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
        text = { 
            OutlinedTextField(
                value = text, 
                onValueChange = { text = it }, 
                label = { Text(stringResource(id = R.string.title)) }, 
                singleLine = true, 
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            ) 
        },
        confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.accept), onClick = { onConfirm(text) }) },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@Composable
private fun ColorBox(color: Color, onClick: () -> Unit) {
    val borderColor = CalendarioTheme.colors.fondoSecciones.getContrastColor(Color.White).copy(alpha = 0.2f)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
    )
}
