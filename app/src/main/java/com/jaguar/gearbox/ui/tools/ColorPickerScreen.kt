package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import com.jaguar.gearbox.R
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.parseHex
import com.jaguar.gearbox.logic.rgbToHsl
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.ValueRow
import kotlin.math.roundToInt

private const val MAX_RECENT_COLORS = 8
private const val KEY_RECENT_COLORS = "color_picker.recent"

@Composable
fun ColorPickerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context) }
    var red by rememberSaveable { mutableFloatStateOf(103f) }
    var green by rememberSaveable { mutableFloatStateOf(80f) }
    var blue by rememberSaveable { mutableFloatStateOf(164f) }
    var hexInput by rememberSaveable { mutableStateOf("") }
    var hexError by rememberSaveable { mutableStateOf("") }
    var recentColors by rememberSaveable(stateSaver = StringListSaver) {
        mutableStateOf(store.getString(KEY_RECENT_COLORS, "").split(",").filter { it.isNotBlank() })
    }

    val color = Color(red / 255f, green / 255f, blue / 255f)
    val hex =
        String.format("#%02X%02X%02X", red.roundToInt(), green.roundToInt(), blue.roundToInt())
    val previewDescription = stringResource(R.string.color_preview, hex)
    val hsl = rgbToHsl(red.roundToInt(), green.roundToInt(), blue.roundToInt())

    ToolScaffold(
        title = stringResource(R.string.color_picker_title),
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
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = previewDescription },
            )
            IconButton(
                onClick = {
                    recentColors =
                        (listOf(hex) + recentColors.filterNot { it == hex }).take(MAX_RECENT_COLORS)
                    store.putString(KEY_RECENT_COLORS, recentColors.joinToString(","))
                },
            ) {
                Icon(Icons.Filled.BookmarkAdd, contentDescription = stringResource(R.string.color_save, hex))
            }
        }

        if (recentColors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.color_saved_palette), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentColors.forEach { savedHex ->
                    val parsed = parseHex(savedHex)
                    if (parsed != null) {
                        val swatchDescription = stringResource(R.string.color_saved_swatch, savedHex)
                        val selectLabel = stringResource(R.string.color_select_swatch, savedHex)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClickLabel = selectLabel) {
                                    red = parsed.first.toFloat()
                                    green = parsed.second.toFloat()
                                    blue = parsed.third.toFloat()
                                    hexInput = ""
                                    hexError = ""
                                }
                                .padding(4.dp)
                                .semantics {
                                    contentDescription = swatchDescription
                                    selected = savedHex.equals(hex, ignoreCase = true)
                                },
                        ) {
                            Box(
                                Modifier.size(48.dp)
                                    .background(Color(parsed.first / 255f, parsed.second / 255f, parsed.third / 255f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            )
                            Text(savedHex, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clearAndSetSemantics { })
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.color_rgb), style = MaterialTheme.typography.titleMedium)
        // Clearing hexInput lets the field fall back to displaying the live `hex` computed from
        // the sliders - otherwise it kept showing whatever was last typed even after a slider
        // moved the color elsewhere, showing a swatch and a hex value that no longer matched.
        ColorSlider(stringResource(R.string.color_red), red) { red = it; hexInput = "" }
        ColorSlider(stringResource(R.string.color_green), green) { green = it; hexInput = "" }
        ColorSlider(stringResource(R.string.color_blue), blue) { blue = it; hexInput = "" }

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
                }
                // Don't flag an error while the user is still mid-keystroke (e.g. "#FF0" on the
                // way to "#FF0000") - only validate once the field loses focus, below.
                hexError = ""
            },
            label = { Text(stringResource(R.string.color_hex)) },
            isError = hexError.isNotEmpty(),
            supportingText = { if (hexError.isNotEmpty()) Text(hexError) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && hexInput.isNotBlank() && parseHex(hexInput) == null) {
                        hexError = context.getString(R.string.color_hex_error)
                    }
                },
        )

        Spacer(Modifier.height(16.dp))
        val rgbText = "rgb(${red.roundToInt()}, ${green.roundToInt()}, ${blue.roundToInt()})"
        val hslText = "hsl(${hsl.first}, ${hsl.second}%, ${hsl.third}%)"
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ValueRow(stringResource(R.string.color_hex), hex)
                Spacer(Modifier.height(8.dp))
                ValueRow(stringResource(R.string.color_rgb), rgbText)
                Spacer(Modifier.height(8.dp))
                ValueRow(stringResource(R.string.color_hsl), hslText)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { context.copyToClipboard(context.getString(R.string.color_copy_label), "$hex\n$rgbText\n$hslText") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
            Text(stringResource(R.string.color_copy_all))
        }
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
    Slider(value = value, onValueChange = onValueChange, valueRange = 0f..255f,
        modifier = Modifier.semantics { contentDescription = label })
}
