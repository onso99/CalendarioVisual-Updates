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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorThemeScreen(
    onBackPress: () -> Unit,
    onThemeModified: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val isAppDark = ColorUtils.calculateLuminance(CalendarioTheme.colors.settingsBackground.toArgb()) < 0.5
    
    var showColorPicker by remember { mutableStateOf(value = false) }
    var pendingItem by remember { mutableStateOf<ColorThemeItem?>(null) }
    var updateTrigger by remember { mutableIntStateOf(0) }

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
                val themeItems = ColorThemeConfig.colorThemeItems.filter { it.category == "Tema" }
                themeItems.forEachIndexed { index, item ->
                    val currentColor = getThemeColor(prefs, if (isAppDark) item.darkThemeKey else item.lightThemeKey, if (isAppDark) item.defaultDark else item.defaultLight)
                    
                    if (item.labelRes == R.string.effect) {
                        EffectColorThemeRow(
                            label = stringResource(id = item.labelRes),
                            color = currentColor,
                            effectType = effectType,
                            onEffectChange = { newType ->
                                prefs.edit { putString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, newType) }
                                ThemePersistence.markThemeAsModified(prefs)
                                onThemeModified()
                                updateTrigger++
                            }
                        ) {
                            pendingItem = item
                            showColorPicker = true
                        }
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
                    
                    SingleColorThemeRow(
                        label = stringResource(id = item.labelRes),
                        color = currentColor,
                        onReset = {
                            prefs.edit { remove(key) }
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
                    onThemeModified()
                }
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
    onReset: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var labelFontSize by remember { mutableStateOf(16.sp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
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
    color: Color,
    effectType: String,
    onEffectChange: (String) -> Unit,
    onColorClick: () -> Unit
) {
    var labelFontSize by remember { mutableStateOf(16.sp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CalendarioTheme.colors.textSystem,
            fontSize = labelFontSize,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.hasVisualOverflow && labelFontSize > 11.sp) {
                    labelFontSize = (labelFontSize.value - 1f).sp
                }
            }
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            val options = listOf("none" to "0", "gradient" to "1", "sweep" to "2")
            val baseColor = CalendarioTheme.colors.fondoSecciones
            val activeColor = CalendarioTheme.colors.cabecera

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(baseColor.copy(alpha = 0.5f))
                    .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                options.forEach { (type, text) ->
                    val isSelected = effectType == type

                    val containerColor = if (isSelected) activeColor else Color.Transparent
                    val textColor = if (isSelected) {
                        if (isColorDark(activeColor, baseColor)) Color.White else Color.Black
                    } else {
                        val hsl = FloatArray(3)
                        ColorUtils.colorToHSL(baseColor.toArgb(), hsl)
                        val isDark = hsl[2] < 0.5f
                        hsl[2] = if (isDark) (hsl[2] + 0.2f).coerceIn(0f, 1f) else (hsl[2] - 0.2f).coerceIn(0f, 1f)
                        Color(ColorUtils.HSLToColor(hsl))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .clickable { onEffectChange(type) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        ColorBox(color = color, onClick = onColorClick)
    }
}

@Composable
private fun ColorBox(color: Color, onClick: () -> Unit) {
    val borderColor = if (isColorDark(CalendarioTheme.colors.fondoSecciones, Color.White)) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
    )
}
