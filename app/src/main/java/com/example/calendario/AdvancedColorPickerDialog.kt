package com.example.calendario

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.calendario.ui.theme.CalendarioTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorConfirm: (Color) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var currentColor by remember(initialColor) { mutableStateOf(initialColor) }

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(currentColor.toArgb(), hsl)
    val lightness = hsl[2]

    var hexCode by remember(currentColor) {
        mutableStateOf(String.format("#%02X%02X%02X%02X", (currentColor.alpha * 255).toInt(), (currentColor.red * 255).toInt(), (currentColor.green * 255).toInt(), (currentColor.blue * 255).toInt()))
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CalendarioTheme.colors.fondoDialogos,
        title = { Text("Seleccionar Color", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth().height(60.dp).border(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(initialColor))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(currentColor))
                }
                Spacer(Modifier.height(16.dp))

                ColorSlider(label = "A", value = currentColor.alpha * 255, onValueChange = { currentColor = currentColor.copy(alpha = it / 255f) })
                ColorSlider(label = "R", value = currentColor.red * 255, onValueChange = { currentColor = currentColor.copy(red = it / 255f) })
                ColorSlider(label = "G", value = currentColor.green * 255, onValueChange = { currentColor = currentColor.copy(green = it / 255f) })
                ColorSlider(label = "B", value = currentColor.blue * 255, onValueChange = { currentColor = currentColor.copy(blue = it / 255f) })
                ColorSlider(label = "L", value = lightness * 100, onValueChange = { newLightnessValue ->
                    ColorUtils.colorToHSL(currentColor.toArgb(), hsl)
                    hsl[2] = newLightnessValue / 100f
                    val newColorInt = ColorUtils.HSLToColor(hsl)
                    currentColor = Color(newColorInt).copy(alpha = currentColor.alpha)
                }, valueRange = 0f..100f)

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hexCode,
                        onValueChange = { 
                            val newHex = if (it.startsWith("#")) it else "#$it"
                            hexCode = newHex
                            if (newHex.length == 9) {
                                try {
                                    val parsedColor = Color(android.graphics.Color.parseColor(newHex))
                                    currentColor = parsedColor
                                } catch (e: Exception) { /* No-op, invalid color */ }
                            }
                        },
                        label = { Text("Hex (ARGB)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onColorConfirm(currentColor) }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(hexCode))
                        Toast.makeText(context, "Copiado: $hexCode", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar color")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onColorConfirm(currentColor) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float> = 0f..255f) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.weight(1f))
        Text(value.roundToInt().toString(), modifier = Modifier.width(30.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}