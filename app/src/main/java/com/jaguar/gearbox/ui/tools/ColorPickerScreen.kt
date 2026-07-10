package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.math.roundToInt

private val recentColorsSaver: Saver<MutableState<List<String>>, Array<String>> = Saver(
    save = { it.value.toTypedArray() },
    restore = { mutableStateOf(it.toList()) },
)

private const val MAX_RECENT_COLORS = 8

@Composable
fun ColorPickerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var red by rememberSaveable { mutableFloatStateOf(103f) }
    var green by rememberSaveable { mutableFloatStateOf(80f) }
    var blue by rememberSaveable { mutableFloatStateOf(164f) }
    var hexInput by rememberSaveable { mutableStateOf("") }
    var hexError by rememberSaveable { mutableStateOf("") }
    var recentColors by rememberSaveable(saver = recentColorsSaver) { mutableStateOf(emptyList()) }

    val color = Color(red / 255f, green / 255f, blue / 255f)
    val hex = String.format("#%02X%02X%02X", red.roundToInt(), green.roundToInt(), blue.roundToInt())
    val hsl = rgbToHsl(red.roundToInt(), green.roundToInt(), blue.roundToInt())

    ToolScaffold(
        title = "Color Picker",
        icon = Tools.byRoute(Tools.ROUTE_COLOR_PICKER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .background(color, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            )
            IconButton(
                onClick = {
                    recentColors = (listOf(hex) + recentColors.filterNot { it == hex }).take(MAX_RECENT_COLORS)
                },
            ) {
                Icon(Icons.Filled.BookmarkAdd, contentDescription = "Save this color")
            }
        }

        if (recentColors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentColors.forEach { savedHex ->
                    val parsed = parseHex(savedHex)
                    if (parsed != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(parsed.first / 255f, parsed.second / 255f, parsed.third / 255f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    red = parsed.first.toFloat()
                                    green = parsed.second.toFloat()
                                    blue = parsed.third.toFloat()
                                    hexInput = ""
                                    hexError = ""
                                },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("RGB", style = MaterialTheme.typography.titleMedium)
        ColorSlider("Red", red) { red = it }
        ColorSlider("Green", green) { green = it }
        ColorSlider("Blue", blue) { blue = it }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = hexInput.ifEmpty { hex },
            onValueChange = { value ->
                hexInput = value
                val parsed = parseHex(value)
                if (parsed != null) {
                    red = parsed.first.toFloat()
                    green = parsed.second.toFloat()
                    blue = parsed.third.toFloat()
                    hexError = ""
                } else if (value.isNotBlank()) {
                    hexError = "Enter a valid hex color, e.g. #6750A4"
                }
            },
            label = { Text("HEX") },
            isError = hexError.isNotEmpty(),
            supportingText = { if (hexError.isNotEmpty()) Text(hexError) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        val rgbText = "rgb(${red.roundToInt()}, ${green.roundToInt()}, ${blue.roundToInt()})"
        val hslText = "hsl(${hsl.first}, ${hsl.second}%, ${hsl.third}%)"
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                ValueRow("HEX", hex)
                Spacer(Modifier.height(8.dp))
                ValueRow("RGB", rgbText)
                Spacer(Modifier.height(8.dp))
                ValueRow("HSL", hslText)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { context.copyToClipboard("Color", "$hex\n$rgbText\n$hslText") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
            Text(" Copy all")
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.roundToInt().toString(), style = MaterialTheme.typography.bodyMedium)
    }
    Slider(value = value, onValueChange = onValueChange, valueRange = 0f..255f)
}

private fun parseHex(input: String): Triple<Int, Int, Int>? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6 || cleaned.any { it !in "0123456789abcdefABCDEF" }) return null
    return try {
        val r = cleaned.substring(0, 2).toInt(16)
        val g = cleaned.substring(2, 4).toInt(16)
        val b = cleaned.substring(4, 6).toInt(16)
        Triple(r, g, b)
    } catch (_: NumberFormatException) {
        null
    }
}

private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val lightness = (max + min) / 2f

    if (max == min) return Triple(0, 0, (lightness * 100).roundToInt())

    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue = when (max) {
        rf -> ((gf - bf) / delta + (if (gf < bf) 6f else 0f))
        gf -> ((bf - rf) / delta + 2f)
        else -> ((rf - gf) / delta + 4f)
    } * 60f

    return Triple(hue.roundToInt(), (saturation * 100).roundToInt(), (lightness * 100).roundToInt())
}
