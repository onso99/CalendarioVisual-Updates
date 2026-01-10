package com.example.calendario

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.example.calendario.ui.theme.CalendarioTheme
import com.example.calendario.ui.theme.isColorDark
import java.lang.IllegalArgumentException

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ColorThemeScreen(
    onBackPress: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppConstants.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val groupedItems = ColorThemeConfig.colorThemeItems.groupBy { it.category }
    val categories = remember {
        listOf(
            "General" to R.string.general,
            "Lista de Eventos" to R.string.event_list,
            "Calendario Mensual" to R.string.monthly_calendar,
            "Calendario Anual" to R.string.yearly_calendar
        )
            .filter { groupedItems.containsKey(it.first) }
    }

    val themeManager = rememberThemeManager()
    val themeSetting by themeManager.themeSetting.collectAsState()
    val isDarkTheme = when (themeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
    }

    val pendingColorChanges = remember { mutableStateMapOf<String, Color>() }
    val pendingKeywordChanges = remember { mutableStateMapOf<String, String>() }
    var monthlyCalendarEffect by remember { mutableStateOf(prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "none")) }

    var showAdvancedColorDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<Triple<String, Color, Int>?>(null) }

    var showKeywordColorDialog by remember { mutableStateOf(false) }
    var keywordColorToEdit by remember { mutableStateOf<KeywordColorEditInfo?>(null) }

    var showDiscardChangesDialog by remember { mutableStateOf(false) }

    val hasPendingChanges by remember {
        derivedStateOf { pendingColorChanges.isNotEmpty() || pendingKeywordChanges.isNotEmpty() || monthlyCalendarEffect != prefs.getString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, "none") }
    }

    val backAction = {
        if (hasPendingChanges) {
            showDiscardChangesDialog = true
        } else {
            onBackPress()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.customize_colors), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = backAction) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onPrimary) } },
                actions = {
                    if (hasPendingChanges) {
                        IconButton(onClick = { 
                            prefs.edit {
                                pendingColorChanges.forEach { (key, color) -> putInt(key, color.toArgb()) }
                                pendingKeywordChanges.forEach { (key, keyword) -> putString(key, keyword) }
                                putString(AppConstants.KEY_MONTHLY_CALENDAR_EFFECT_TYPE, monthlyCalendarEffect)
                                remove(AppConstants.KEY_LIGHT_THEME_NAME)
                                remove(AppConstants.KEY_DARK_THEME_NAME)
                            }
                            onBackPress()
                        }) {
                            Icon(Icons.Default.Check, stringResource(id = R.string.apply_changes), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = CalendarioTheme.colors.settingsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            categories.forEachIndexed { index, (category, categoryRes) ->
                val items = groupedItems[category]!!

                SectionTitle(
                    text = stringResource(id = categoryRes),
                    modifier = Modifier.padding(top = if(index > 0) 24.dp else 0.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones).padding(horizontal = 16.dp)
                ) {
                    items.forEach { item ->
                        if (item.isSeparator) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        } else {
                            val (colorKey, defaultColor) = if (isDarkTheme) {
                                item.darkThemeKey to item.defaultDark
                            } else {
                                item.lightThemeKey to item.defaultLight
                            }

                            if (colorKey.isNotBlank()) {
                                val currentColor = pendingColorChanges[colorKey] ?: getThemeColor(prefs, colorKey, defaultColor)

                                when (item.labelRes) {
                                    R.string.effect -> {
                                        EffectColorThemeRow(
                                            label = stringResource(id = item.labelRes),
                                            color = currentColor,
                                            effectType = monthlyCalendarEffect ?: "none",
                                            onEffectChange = { monthlyCalendarEffect = it },
                                            onColorClick = {
                                                colorToEdit = Triple(colorKey, currentColor, item.labelRes)
                                                showAdvancedColorDialog = true
                                            }
                                        )
                                    }
                                    R.string.event_1, R.string.event_2 -> {
                                        val keywordKey = if (item.labelRes == R.string.event_1) AppConstants.KEY_EVENT_1_KEYWORD else AppConstants.KEY_EVENT_2_KEYWORD
                                        val currentKeyword = pendingKeywordChanges[keywordKey] ?: prefs.getString(keywordKey, "") ?: ""

                                        SingleColorThemeRow(
                                            label = currentKeyword.ifBlank { stringResource(id = item.labelRes) },
                                            color = currentColor,
                                            onClick = {
                                                keywordColorToEdit = KeywordColorEditInfo(colorKey, currentColor, item.labelRes, keywordKey, currentKeyword)
                                                showKeywordColorDialog = true
                                            }
                                        )
                                    }
                                    else -> {
                                        SingleColorThemeRow(
                                            label = stringResource(id = item.labelRes),
                                            color = currentColor,
                                            onClick = {
                                                colorToEdit = Triple(colorKey, currentColor, item.labelRes)
                                                showAdvancedColorDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdvancedColorDialog && colorToEdit != null) {
        AdvancedColorPickerDialog(
            initialColor = colorToEdit!!.second,
            onDismissRequest = { showAdvancedColorDialog = false },
            onColorConfirm = { newColor ->
                val key = colorToEdit!!.first
                pendingColorChanges[key] = newColor
                showAdvancedColorDialog = false
            }
        )
    }

    if (showKeywordColorDialog && keywordColorToEdit != null) {
        KeywordColorPickerDialog(
            label = stringResource(id = keywordColorToEdit!!.labelRes),
            initialColor = keywordColorToEdit!!.color,
            initialKeyword = keywordColorToEdit!!.keyword,
            onDismissRequest = { showKeywordColorDialog = false },
            onConfirm = { newColor, newKeyword ->
                pendingColorChanges[keywordColorToEdit!!.colorKey] = newColor
                pendingKeywordChanges[keywordColorToEdit!!.keywordKey] = newKeyword
                showKeywordColorDialog = false
            }
        )
    }

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            containerColor = CalendarioTheme.colors.fondoDialogos,
            titleContentColor = CalendarioTheme.colors.textSystem,
            textContentColor = CalendarioTheme.colors.textSystem,
            title = { Text(stringResource(id = R.string.discard_changes_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.discard_changes_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardChangesDialog = false
                        onBackPress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text(stringResource(id = R.string.cancel), color = CalendarioTheme.colors.textSystem)
                }
            }
        )
    }
}

data class KeywordColorEditInfo(val colorKey: String, val color: Color, @field:StringRes val labelRes: Int, val keywordKey: String, val keyword: String)

private fun getThemeColor(prefs: SharedPreferences, key: String, defaultColor: Color): Color {
    if (!prefs.contains(key)) return defaultColor
    return when (val value = prefs.all[key]) {
        is Int -> Color(value)
        is String -> {
            try {
                Color(value.toColorInt())
            } catch (_: IllegalArgumentException) {
                defaultColor
            }
        }
        else -> defaultColor
    }
}

@Composable
private fun SingleColorThemeRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = CalendarioTheme.colors.textSystem, 
            fontSize = 16.sp, 
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = CalendarioTheme.colors.textSystem, 
            fontSize = 16.sp, 
            modifier = Modifier.padding(end = 8.dp)
        )

        val options = listOf("none" to "0", "gradient" to "1", "sweep" to "2", "radial" to "3")
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
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text, color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        ColorBox(color = color, onClick = onColorClick)
    }
}


@Composable
private fun ColorBox(color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape).clickable(onClick = onClick))
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val titleColor = lerp(
        start = CalendarioTheme.colors.cabecera,
        stop = CalendarioTheme.colors.textSystem,
        fraction = 0.4f
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = titleColor,
        modifier = modifier
    )
}
