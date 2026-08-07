package com.example.calendario

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.math.roundToInt

@Stable
class AdvancedColorPickerState(
    initialColor: Color,
) {
    var currentColor by mutableStateOf(initialColor)
    var isHexError by mutableStateOf(value = false)
        private set

    val hsl: FloatArray
        get() {
            val h = FloatArray(3)
            ColorUtils.colorToHSL(currentColor.toArgb(), h)
            return h
        }

    val lightness: Float
        get() = hsl[2]

    var hexCode by mutableStateOf(String.format("#%08X", initialColor.toArgb()))
        private set

    fun updateColorFromHex(newHex: String) {
        val newHexUncapped = if (newHex.startsWith("#")) newHex else "#$newHex"
        hexCode = newHexUncapped.take(9)

        if ((hexCode.length == 9) || (hexCode.length == 7)) { // Support ARGB and RGB
            try {
                val colorToParse = if (hexCode.length == 7) hexCode.replace("#", "#FF") else hexCode
                currentColor = Color(colorToParse.toColorInt())
                isHexError = false
            } catch (_: IllegalArgumentException) {
                isHexError = true
            }
        } else {
            isHexError = true
        }
    }

    fun onColorPartChanged(alpha: Float? = null, red: Float? = null, green: Float? = null, blue: Float? = null) {
        currentColor = currentColor.copy(
            alpha = alpha ?: currentColor.alpha,
            red = red ?: currentColor.red,
            green = green ?: currentColor.green,
            blue = blue ?: currentColor.blue
        )
        hexCode = String.format("#%08X", currentColor.toArgb())
        isHexError = false
    }

    fun onLightnessChanged(newLightnessValue: Float) {
        val h = hsl
        h[2] = newLightnessValue / 100f
        val newColorInt = ColorUtils.HSLToColor(h)
        currentColor = Color(newColorInt).copy(alpha = currentColor.alpha)
        hexCode = String.format("#%08X", currentColor.toArgb())
        isHexError = false
    }

    fun clearHex() {
        hexCode = "#"
        isHexError = true
    }
}

@Composable
fun rememberAdvancedColorPickerState(
    initialColor: Color
): AdvancedColorPickerState {
    return remember(initialColor) {
        AdvancedColorPickerState(initialColor)
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorConfirm: (Color) -> Unit
) {
    val state = rememberAdvancedColorPickerState(initialColor = initialColor)
    val onConfirmAction = { if (!state.isHexError) onColorConfirm(state.currentColor) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text(stringResource(id = R.string.select_color_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = CalendarioTheme.colors.textSystem, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                ColorPreview(
                    initialColor = initialColor,
                    newColor = state.currentColor,
                    isHexError = state.isHexError
                )
                Spacer(Modifier.height(16.dp))
                ColorSliders(state = state)
                Spacer(Modifier.height(8.dp))
                HexInput(state = state, onConfirm = onConfirmAction)
            }
        },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(id = R.string.accept),
                onClick = onConfirmAction
            )
        },
        dismissButton = { DialogDismissButton(onDismiss = onDismissRequest) }
    )
}

@Composable
private fun ColorPreview(initialColor: Color, newColor: Color, isHexError: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(initialColor)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isHexError) initialColor else newColor)
        )
    }
}

@Composable
private fun ColorSliders(state: AdvancedColorPickerState) {
    Column {
        ColorSlider(label = stringResource(id = R.string.color_alpha_label), value = state.currentColor.alpha * 255, onValueChange = { state.onColorPartChanged(alpha = it / 255f) })
        ColorSlider(label = stringResource(id = R.string.color_red_label), value = state.currentColor.red * 255, onValueChange = { state.onColorPartChanged(red = it / 255f) })
        ColorSlider(label = stringResource(id = R.string.color_green_label), value = state.currentColor.green * 255, onValueChange = { state.onColorPartChanged(green = it / 255f) })
        ColorSlider(label = stringResource(id = R.string.color_blue_label), value = state.currentColor.blue * 255, onValueChange = { state.onColorPartChanged(blue = it / 255f) })
        ColorSlider(
            label = stringResource(id = R.string.color_lightness_label),
            value = state.lightness * 100,
            onValueChange = { state.onLightnessChanged(it) },
            valueRange = 0f..100f
        )
    }
}

@Composable
private fun HexInput(state: AdvancedColorPickerState, onConfirm: () -> Unit) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val fontScale = LocalConfiguration.current.fontScale
    val useVerticalLayout = fontScale > 1.4f

    if (useVerticalLayout) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = state.hexCode,
                onValueChange = { state.updateColorFromHex(it) },
                label = { Text(stringResource(id = R.string.hex_argb), fontSize = 12.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                modifier = Modifier.fillMaxWidth(),
                isError = state.isHexError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CalendarioTheme.colors.cabecera,
                    unfocusedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                )
            )
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { state.clearHex() }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.clear), tint = CalendarioTheme.colors.textSystem)
                }
                val copiedMessage = stringResource(id = R.string.copied_to_clipboard, state.hexCode)
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(state.hexCode))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_color), tint = CalendarioTheme.colors.textSystem)
                }
                IconButton(onClick = {
                    clipboardManager.getText()?.text?.let { pastedText ->
                        state.updateColorFromHex(pastedText)
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = stringResource(id = R.string.paste_color), tint = CalendarioTheme.colors.textSystem)
                }
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.hexCode,
                onValueChange = { state.updateColorFromHex(it) },
                label = { Text(stringResource(id = R.string.hex_argb), fontSize = 12.sp) },
                textStyle = TextStyle(fontSize = 14.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                modifier = Modifier.weight(1f),
                isError = state.isHexError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CalendarioTheme.colors.cabecera,
                    unfocusedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                )
            )

            IconButton(onClick = { state.clearHex() }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.clear), tint = CalendarioTheme.colors.textSystem)
            }

            val copiedMessage = stringResource(id = R.string.copied_to_clipboard, state.hexCode)
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(state.hexCode))
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_color), tint = CalendarioTheme.colors.textSystem)
            }

            IconButton(onClick = {
                clipboardManager.getText()?.text?.let { pastedText ->
                    state.updateColorFromHex(pastedText)
                }
            }) {
                Icon(Icons.Default.ContentPaste, contentDescription = stringResource(id = R.string.paste_color), tint = CalendarioTheme.colors.textSystem)
            }
        }
    }
}

@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..255f
) {
    val fontScale = LocalConfiguration.current.fontScale
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(24.dp), color = CalendarioTheme.colors.textSystem)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.roundToInt().toString(),
            modifier = Modifier.width((40 * fontScale).dp.coerceAtLeast(40.dp)),
            textAlign = TextAlign.End,
            fontSize = 14.sp,
            color = CalendarioTheme.colors.textSystem
        )
    }
}