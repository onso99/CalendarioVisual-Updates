package com.example.calendario

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.calendario.ui.theme.CalendarioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorThemeScreen(
    onBackPress: () -> Unit,
    onThemeUpdated: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AppThemeSetup.APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE) }
    val groupedItems = ColorThemeConfig.colorThemeItems.groupBy { it.category }
    val categories = groupedItems.keys.toList()

    val pendingChanges = remember { mutableStateMapOf<String, Color>() }
    var showAdvancedColorDialog by remember { mutableStateOf(false) }
    var colorToEdit by remember { mutableStateOf<Triple<String, Color, String>?>(null) } // key, color, label

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalizar Colores", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBackPress) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onPrimary) } },
                actions = {
                    FilledIconButton(
                        onClick = { 
                            if (pendingChanges.isNotEmpty()) {
                                prefs.edit { pendingChanges.forEach { (key, color) -> putInt(key, color.toArgb()) } }
                                pendingChanges.clear()
                                onThemeUpdated()
                            }
                            onBackPress()
                        },
                        modifier = Modifier.padding(end = 8.dp).size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, "Aplicar")
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
            categories.forEachIndexed { index, category ->
                val items = groupedItems[category]!!

                SectionTitle(
                    text = category,
                    modifier = Modifier.padding(top = if(index > 0) 24.dp else 0.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)
                ) {
                    items.forEach { item ->
                        val (colorKey, defaultColor) = if (isDarkTheme) {
                            item.darkThemeKey to item.defaultDark
                        } else {
                            item.lightThemeKey to item.defaultLight
                        }

                        if (colorKey.isNotBlank()) {
                            val currentColor = pendingChanges[colorKey] ?: Color(prefs.getInt(colorKey, defaultColor.toArgb()))

                            SingleColorThemeRow(
                                label = item.label,
                                color = currentColor,
                                onClick = {
                                    colorToEdit = Triple(colorKey, currentColor, item.label)
                                    showAdvancedColorDialog = true
                                }
                            )
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
                pendingChanges[key] = newColor
                showAdvancedColorDialog = false
            }
        )
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
        Text(text = label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        ColorBox(color = color, onClick = onClick)
    }
}

@Composable
private fun ColorBox(color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape).clickable(onClick = onClick))
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
