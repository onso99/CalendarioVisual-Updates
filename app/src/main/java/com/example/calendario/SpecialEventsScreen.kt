package com.example.calendario

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendario.ui.theme.CalendarioTheme

@Composable
fun SpecialEventsScreen(
    onBackPress: () -> Unit,
    onThemeModified: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { SettingsManager.getPrefs(context, AppConstants.APP_SETTINGS_PREFS_NAME) }

    var showColorPicker by remember { mutableStateOf(value = false) }
    var showRenameDialog by remember { mutableStateOf(value = false) }
    var pendingItem by remember { mutableStateOf<ColorThemeItem?>(null) }
    var updateTrigger by remember { mutableIntStateOf(0) }

    val dividerColor = CalendarioTheme.colors.settingsBackground
    val dividerThickness = 1.dp

    val festivitiesItems = listOf(
        ColorThemeItem(R.string.sundays_and_holidays, AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.ColorKeys.LIGHT_TEXT_SUNDAY_HOLIDAY, AppConstants.LightColors.textSundayHoliday, AppConstants.LightColors.textSundayHoliday, "Festividades", isIndependent = true),
        ColorThemeItem(R.string.birthdays, AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.ColorKeys.LIGHT_TEXT_BIRTHDAY, AppConstants.LightColors.textBirthday, AppConstants.LightColors.textBirthday, "Festividades", isIndependent = true)
    )

    val highlightsItems = listOf(
        ColorThemeItem(R.string.event_1, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_1, AppConstants.LightColors.textEvent1, AppConstants.LightColors.textEvent1, "Destacados", isIndependent = true),
        ColorThemeItem(R.string.event_2, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_2, AppConstants.LightColors.textEvent2, AppConstants.LightColors.textEvent2, "Destacados", isIndependent = true),
        ColorThemeItem(R.string.event_3, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_3, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_3, AppConstants.LightColors.textEvent3, AppConstants.LightColors.textEvent3, "Destacados", isIndependent = true),
        ColorThemeItem(R.string.event_4, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_4, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_4, AppConstants.LightColors.textEvent4, AppConstants.LightColors.textEvent4, "Destacados", isIndependent = true),
        ColorThemeItem(R.string.event_5, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_5, AppConstants.ColorKeys.LIGHT_TEXT_EVENT_5, AppConstants.LightColors.textEvent5, AppConstants.LightColors.textEvent5, "Destacados", isIndependent = true)
    )

    AppScreen(
        title = stringResource(id = R.string.special_events),
        onBackClick = onBackPress
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // --- SECCIÓN 1: FESTIVIDADES ---
            SectionTitle(stringResource(id = R.string.festivities_section_title), isFirst = true)
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                festivitiesItems.forEachIndexed { index, item ->
                    val key = item.lightThemeKey
                    val defaultColor = item.defaultLight
                    val currentColor = getSpecialEventColor(prefs, key, defaultColor)

                    SpecialEventThemeRow(
                        label = stringResource(id = item.labelRes),
                        color = currentColor,
                        isPulsing = false,
                        onPulseClick = null,
                        onLabelClick = null,
                        onReset = {
                            prefs.edit { remove(key) }
                            ThemePersistence.markThemeAsModified(prefs)
                            onThemeModified()
                            updateTrigger++
                        },
                        onClick = {
                            pendingItem = item
                            showColorPicker = true
                        }
                    )
                    if (index < festivitiesItems.size - 1) {
                        HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    }
                }
            }

            // --- SECCIÓN 2: DESTACADOS ---
            SectionTitle(stringResource(id = R.string.highlights_section_title))
            Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CalendarioTheme.colors.fondoSecciones)) {
                highlightsItems.forEachIndexed { index, item ->
                    val key = item.lightThemeKey
                    val defaultColor = item.defaultLight
                    val currentColor = getSpecialEventColor(prefs, key, defaultColor)

                    val keywordKey = when (item.labelRes) {
                        R.string.event_1 -> AppConstants.KEY_EVENT_1_KEYWORD
                        R.string.event_2 -> AppConstants.KEY_EVENT_2_KEYWORD
                        R.string.event_3 -> AppConstants.KEY_EVENT_3_KEYWORD
                        R.string.event_4 -> AppConstants.KEY_EVENT_4_KEYWORD
                        R.string.event_5 -> AppConstants.KEY_EVENT_5_KEYWORD
                        else -> null
                    }

                    val pulseKey = when (item.labelRes) {
                        R.string.event_1 -> AppConstants.KEY_EVENT_1_PULSE
                        R.string.event_2 -> AppConstants.KEY_EVENT_2_PULSE
                        R.string.event_3 -> AppConstants.KEY_EVENT_3_PULSE
                        R.string.event_4 -> AppConstants.KEY_EVENT_4_PULSE
                        R.string.event_5 -> AppConstants.KEY_EVENT_5_PULSE
                        else -> null
                    }

                    val isPulsing = pulseKey?.let { prefs.getBoolean(it, false) } ?: false
                    val currentLabel = keywordKey?.let { prefs.getString(it, "") }?.takeIf { it.isNotBlank() }
                                       ?: stringResource(id = item.labelRes)

                    SpecialEventThemeRow(
                        label = currentLabel,
                        color = currentColor,
                        isPulsing = isPulsing,
                        onPulseClick = if (pulseKey != null) { {
                            when (pulseKey) {
                                AppConstants.KEY_EVENT_1_PULSE -> SettingsManager.saveEvent1Pulse(context, !isPulsing)
                                AppConstants.KEY_EVENT_2_PULSE -> SettingsManager.saveEvent2Pulse(context, !isPulsing)
                                AppConstants.KEY_EVENT_3_PULSE -> SettingsManager.saveEvent3Pulse(context, !isPulsing)
                                AppConstants.KEY_EVENT_4_PULSE -> SettingsManager.saveEvent4Pulse(context, !isPulsing)
                                AppConstants.KEY_EVENT_5_PULSE -> SettingsManager.saveEvent5Pulse(context, !isPulsing)
                            }
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
                    if (index < highlightsItems.size - 1) {
                        HorizontalDivider(color = dividerColor, thickness = dividerThickness)
                    }
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

        SpecialEventRenameDialog(
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
        val key = item.lightThemeKey
        val currentColor = getSpecialEventColor(prefs, key, item.defaultLight)

        AdvancedColorPickerDialog(
            initialColor = currentColor,
            onDismissRequest = { showColorPicker = false },
            onColorConfirm = { newColor ->
                prefs.edit { putInt(key, newColor.toArgb()) }
                ThemePersistence.markThemeAsModified(prefs)
                onThemeModified()
                updateTrigger++
                showColorPicker = false
            }
        )
    }
}

private fun getSpecialEventColor(prefs: SharedPreferences, key: String, default: Color): Color {
    val colorInt = try {
        prefs.getInt(key, default.toArgb())
    } catch (_: ClassCastException) {
        (prefs.all[key] as? Number)?.toInt() ?: default.toArgb()
    }
    return Color(colorInt)
}

@Composable
private fun SpecialEventThemeRow(
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

        SpecialEventColorBox(color = color, onClick = onClick)
    }
}

@Composable
private fun SpecialEventRenameDialog(initialName: String, onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(id = R.string.special_events),
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
private fun SpecialEventColorBox(color: Color, onClick: () -> Unit) {
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
