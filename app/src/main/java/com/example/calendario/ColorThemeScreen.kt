package com.example.calendario

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@Composable
fun ColorThemeScreen(
    onBackPress: () -> Unit,
    onThemeModified: () -> Unit,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { SettingsManager.getPrefs(context, AppConstants.APP_SETTINGS_PREFS_NAME) }
    
    var showColorPicker by remember { mutableStateOf(value = false) }
    var showRenameDialog by remember { mutableStateOf(value = false) }
    var pendingItem by remember { mutableStateOf<ColorThemeItem?>(null) }
    var updateTrigger by remember { mutableIntStateOf(0) }

    val effectType = remember(updateTrigger) { prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "gradient") ?: "gradient" }
    val dividerColor = CalendarioTheme.colors.settingsBackground
    val dividerThickness = 1.dp

    AppScreen(
        title = stringResource(id = R.string.customize_colors),
        onBackClick = onBackPress
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // --- BLOQUE 1: FONDOS ---
            SectionTitle(stringResource(id = R.string.backgrounds_section_title), isFirst = true)
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val themeItems = ColorThemeConfig.colorThemeItems.filter { it.category == "Fondos" && it.labelRes != R.string.calendar_background }
                themeItems.forEachIndexed { index, item ->
                    val currentColor = getThemeColor(prefs, if (darkTheme) item.darkThemeKey else item.lightThemeKey, if (darkTheme) item.defaultDark else item.defaultLight)
                    
                    if (item.labelRes == R.string.effect) {
                        val backgroundItem = ColorThemeConfig.colorThemeItems.find { it.labelRes == R.string.calendar_background }!!
                        val color1 = getThemeColor(prefs, if (darkTheme) backgroundItem.darkThemeKey else backgroundItem.lightThemeKey, if (darkTheme) backgroundItem.defaultDark else backgroundItem.defaultLight)

                        UnifiedGradientEffectRow(
                            label = stringResource(id = R.string.gradient_label),
                            color1 = color1,
                            color2 = currentColor,
                            effectType = effectType,
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
                                val key1 = if (darkTheme) backgroundItem.darkThemeKey else backgroundItem.lightThemeKey
                                val key2 = if (darkTheme) item.darkThemeKey else item.lightThemeKey
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
                        if (index < (themeItems.size - 1)) {
                            HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                        }
                    }
                }
            }

            // --- BLOQUE 2: CELDAS ---
            SectionTitle(stringResource(id = R.string.cells_section_title))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                val monthCellItem = ColorThemeConfig.colorThemeItems.find { it.labelRes == R.string.current_month_cell }
                val todayCellItem = ColorThemeConfig.colorThemeItems.find { it.labelRes == R.string.today_cell_background }
                val todayBorderItem = ColorThemeConfig.colorThemeItems.find { it.labelRes == R.string.today_border_color }
                val noteIconItem = ColorThemeConfig.colorThemeItems.find { it.labelRes == R.string.note_icon_color }

                val renderCellItem: @Composable (ColorThemeItem) -> Unit = { item ->
                    val key = if (darkTheme) item.darkThemeKey else item.lightThemeKey
                    val defaultColor = if (darkTheme) item.defaultDark else item.defaultLight
                    val currentColor = getThemeColor(prefs, key, defaultColor)

                    SingleColorThemeRow(
                        label = stringResource(id = item.labelRes),
                        color = currentColor,
                        isPulsing = false,
                        onPulseClick = null,
                        onLabelClick = null,
                        onReset = if (item.isIndependent) {
                            {
                                prefs.edit { remove(key) }
                                ThemePersistence.markThemeAsModified(prefs)
                                onThemeModified()
                                updateTrigger++
                            }
                        } else null,
                        onClick = {
                            pendingItem = item
                            showColorPicker = true
                        }
                    )
                }

                // 1. Mes
                if (monthCellItem != null) {
                    renderCellItem(monthCellItem)
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                }

                // 2. Hoy
                if (todayCellItem != null) {
                    renderCellItem(todayCellItem)
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                }

                // 3. Transparencia de Otros (Slider row, SIN divisor interno)
                var showOthersExpand by remember { mutableStateOf(false) }
                var pendingOthersStep by remember { mutableFloatStateOf(SettingsManager.getOutOfMonthOpacityStep(context).toFloat()) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showOthersExpand = !showOthersExpand }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.others_cell_label),
                        color = CalendarioTheme.colors.textSystem,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showOthersExpand) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = CalendarioTheme.colors.textSystem.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (showOthersExpand) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = pendingOthersStep,
                                onValueChange = { 
                                    pendingOthersStep = it
                                    SettingsManager.saveOutOfMonthOpacityStep(context, it.roundToInt())
                                    ThemePersistence.markThemeAsModified(prefs)
                                    onThemeModified()
                                    updateTrigger++
                                },
                                valueRange = 0f..5f,
                                steps = 4,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = CalendarioTheme.colors.cabecera,
                                    activeTrackColor = CalendarioTheme.colors.cabecera,
                                    inactiveTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.24f)
                                )
                            )
                            Text(
                                text = pendingOthersStep.roundToInt().toString(),
                                modifier = Modifier.width(40.dp).padding(start = 8.dp),
                                color = CalendarioTheme.colors.textSystem,
                                textAlign = TextAlign.End,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = dividerColor, thickness = dividerThickness)

                // 4. Borde de Hoy
                if (todayBorderItem != null) {
                    renderCellItem(todayBorderItem)
                    HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                }

                // 5. Icono Nota
                if (noteIconItem != null) {
                    renderCellItem(noteIconItem)
                }
            }
        }
    }

    if (showRenameDialog && pendingItem != null) {
        val item = pendingItem!!
        val keywordKey = when (item.labelRes) {
            R.string.event_1 -> AppConstants.KEY_EVENT_1_KEYWORD
            R.string.event_2 -> AppConstants.KEY_EVENT_2_KEYWORD
            R.string.event_3 -> AppConstants.KEY_EVENT_3_KEYWORD
            R.string.event_4 -> AppConstants.KEY_EVENT_4_KEYWORD
            R.string.event_5 -> AppConstants.KEY_EVENT_5_KEYWORD
            else -> ""
        }
        val currentVal = prefs.getString(keywordKey, "") ?: ""

        RenameEventDialog(
            initialName = currentVal,
            onDismissRequest = { showRenameDialog = false },
            onConfirm = { newName ->
                when (keywordKey) {
                    AppConstants.KEY_EVENT_1_KEYWORD -> SettingsManager.saveEvent1Keyword(context, newName)
                    AppConstants.KEY_EVENT_2_KEYWORD -> SettingsManager.saveEvent2Keyword(context, newName)
                    AppConstants.KEY_EVENT_3_KEYWORD -> SettingsManager.saveEvent3Keyword(context, newName)
                    AppConstants.KEY_EVENT_4_KEYWORD -> SettingsManager.saveEvent4Keyword(context, newName)
                    AppConstants.KEY_EVENT_5_KEYWORD -> SettingsManager.saveEvent5Keyword(context, newName)
                }
                ThemePersistence.markThemeAsModified(prefs)
                onThemeModified()
                updateTrigger++
                showRenameDialog = false
            }
        )
    }

    if (showColorPicker && pendingItem != null) {
        val item = pendingItem!!
        val key = if (darkTheme) item.darkThemeKey else item.lightThemeKey
        val currentColor = getThemeColor(prefs, key, if (darkTheme) item.defaultDark else item.defaultLight)

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
private fun UnifiedGradientEffectRow(
    label: String,
    color1: Color,
    color2: Color,
    effectType: String,
    onEffectChange: (String) -> Unit,
    onColor1Click: () -> Unit,
    onColor2Click: () -> Unit,
    onExchange: () -> Unit
) {
    val innerHeight = 32.dp
    val options = listOf("none" to "0", "gradient" to "1", "sweep" to "2")
    val baseColor = CalendarioTheme.colors.fondoSecciones
    val activeColor = CalendarioTheme.colors.cabecera
    var labelFontSize by remember { mutableStateOf(16.sp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
    ) {
        // 1. Fila 1 (52.dp) arriba: Label "Degradado" a la izquierda, Círculo 1 a la derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = CalendarioTheme.colors.textSystem,
                fontSize = labelFontSize,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.hasVisualOverflow && labelFontSize > 12.sp) {
                        labelFontSize = (labelFontSize.value - 1f).sp
                    }
                },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            ColorBox(color = color1, onClick = onColor1Click)
        }

        // 2. Fila 2 (52.dp) abajo: Círculo 2 a la derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            ColorBox(color = color2, onClick = onColor2Click)
        }

        // 3. Selector [0 1 2] a la izquierda + Previsualización con SwapVert integrado a la derecha (centrados verticalmente)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 74.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A) Selector [0 1 2]
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

            // B) Previsualización con icono SwapVert fusionado en el centro
            val previewBrush = when (effectType) {
                "gradient" -> Brush.verticalGradient(listOf(color1, color2))
                "sweep" -> Brush.verticalGradient(listOf(color1, color2, color1))
                else -> androidx.compose.ui.graphics.SolidColor(color1)
            }

            val avgLum = (ColorUtils.calculateLuminance(color1.toArgb()) + ColorUtils.calculateLuminance(color2.toArgb())) / 2.0
            val arrowTint = if (avgLum > 0.45) Color.Black.copy(alpha = 0.8f) else Color.White

            Box(
                modifier = Modifier
                    .size(innerHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewBrush)
                    .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onExchange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Intercambiar colores",
                    tint = arrowTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RenameEventDialog(initialName: String, onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(id = R.string.customize_colors),
        confirmButton = { DialogConfirmButton(text = stringResource(id = R.string.accept), onClick = { onConfirm(text) }) },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    ) {
        OutlinedTextField(
            value = text, 
            onValueChange = { text = it }, 
            label = { Text(stringResource(id = R.string.title)) }, 
            singleLine = true, 
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        ) 
    }
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
