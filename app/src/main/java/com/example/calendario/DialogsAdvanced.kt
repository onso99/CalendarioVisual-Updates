package com.example.calendario

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.example.calendario.ui.theme.CalendarioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
class AdvancedColorPickerState(
    initialColor: Color,
    private val scope: CoroutineScope,
    private val clipboardManager: ClipboardManager,
    private val context: Context
) {
    var currentColor by mutableStateOf(initialColor)
    var isHexError by mutableStateOf(false)
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

        if (hexCode.length == 9 || hexCode.length == 7) { // Support ARGB and RGB
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

    fun copyHexToClipboard() {
        scope.launch {
            clipboardManager.setText(AnnotatedString(hexCode))
            Toast.makeText(context, "Copiado: $hexCode", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteHexFromClipboard() {
        scope.launch {
            val clipboardText = clipboardManager.getText()?.text
            if (clipboardText != null) {
                val pasted = clipboardText.take(9)
                updateColorFromHex(if (pasted.startsWith("#")) pasted else "#$pasted")
            }
        }
    }
}

@Composable
fun rememberAdvancedColorPickerState(
    initialColor: Color
): AdvancedColorPickerState {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    return remember(initialColor, scope, clipboardManager, context) {
        AdvancedColorPickerState(initialColor, scope, clipboardManager, context)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordColorPickerDialog(
    label: String,
    initialColor: Color,
    initialKeyword: String,
    onDismissRequest: () -> Unit,
    onConfirm: (Color, String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var keyword by remember { mutableStateOf(initialKeyword) }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text(label, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CalendarioTheme.colors.textSystem) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Color del evento:", color = CalendarioTheme.colors.textSystem)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(selectedColor, CircleShape)
                            .border(1.dp, CalendarioTheme.colors.textSystem.copy(alpha = 0.5f), CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Palabra clave") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CalendarioTheme.colors.cabecera,
                        unfocusedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedColor, keyword) }, colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = CalendarioTheme.colors.cabecera)
            }
        }
    )

    if (showColorPicker) {
        AdvancedColorPickerDialog(
            initialColor = selectedColor,
            onDismissRequest = { showColorPicker = false },
            onColorConfirm = { color ->
                selectedColor = color
                showColorPicker = false
            }
        )
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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text("Seleccionar Color", fontWeight = FontWeight.Bold, color = CalendarioTheme.colors.textSystem) },
        text = {
            Column {
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
                            .background(if (state.isHexError) initialColor else state.currentColor)
                    )
                }
                Spacer(Modifier.height(16.dp))

                ColorSlider(label = "A", value = state.currentColor.alpha * 255, onValueChange = { state.onColorPartChanged(alpha = it / 255f) })
                ColorSlider(label = "R", value = state.currentColor.red * 255, onValueChange = { state.onColorPartChanged(red = it / 255f) })
                ColorSlider(label = "G", value = state.currentColor.green * 255, onValueChange = { state.onColorPartChanged(green = it / 255f) })
                ColorSlider(label = "B", value = state.currentColor.blue * 255, onValueChange = { state.onColorPartChanged(blue = it / 255f) })
                ColorSlider(
                    label = "L",
                    value = state.lightness * 100,
                    onValueChange = { state.onLightnessChanged(it) },
                    valueRange = 0f..100f
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.hexCode,
                        onValueChange = { state.updateColorFromHex(it) },
                        label = { Text("Hex (ARGB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!state.isHexError) onColorConfirm(state.currentColor) }),
                        modifier = Modifier.weight(1f),
                        isError = state.isHexError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CalendarioTheme.colors.cabecera,
                            unfocusedBorderColor = CalendarioTheme.colors.textSystem.copy(alpha = 0.5f)
                        )
                    )

                    IconButton(onClick = { state.clearHex() }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = CalendarioTheme.colors.textSystem)
                    }

                    IconButton(onClick = { state.copyHexToClipboard() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar color", tint = CalendarioTheme.colors.textSystem)
                    }

                    IconButton(onClick = { state.pasteHexFromClipboard() }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Pegar color", tint = CalendarioTheme.colors.textSystem)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (!state.isHexError) onColorConfirm(state.currentColor) },
                colors = ButtonDefaults.buttonColors(containerColor = CalendarioTheme.colors.cabecera)
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = CalendarioTheme.colors.cabecera)
            }
        }
    )
}

@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..255f
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), color = CalendarioTheme.colors.textSystem)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = CalendarioTheme.colors.cabecera,
                activeTrackColor = CalendarioTheme.colors.cabecera,
                inactiveTrackColor = CalendarioTheme.colors.cabecera.copy(alpha = 0.24f)
            )
        )
        Text(
            text = value.roundToInt().toString(),
            modifier = Modifier.width(35.dp),
            textAlign = TextAlign.End,
            fontSize = 14.sp,
            color = CalendarioTheme.colors.textSystem
        )
    }
}
