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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.jaguar.gearbox.ui.components.ToolActions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.hslToRgb
import com.jaguar.gearbox.logic.parseHex
import com.jaguar.gearbox.logic.rgbToHsl
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
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
    var view by rememberSaveable { mutableStateOf("HEX") }
    var hexInput by rememberSaveable { mutableStateOf("#6750A4") }
    var hexError by rememberSaveable { mutableStateOf("") }
    var savedNotice by remember { mutableStateOf("") }
    var recentColors by rememberSaveable(stateSaver = StringListSaver) {
        mutableStateOf(store.getString(KEY_RECENT_COLORS, "").split(",").filter { it.isNotBlank() })
    }

    val color = Color(red / 255f, green / 255f, blue / 255f)
    val hex =
        String.format("#%02X%02X%02X", red.roundToInt(), green.roundToInt(), blue.roundToInt())
    val previewDescription = stringResource(R.string.color_preview, hex)
    val hsl = rgbToHsl(red.roundToInt(), green.roundToInt(), blue.roundToInt())

    var hueInput by rememberSaveable { mutableStateOf(hsl.first.toString()) }
    var saturationInput by rememberSaveable { mutableStateOf(hsl.second.toString()) }
    var lightnessInput by rememberSaveable { mutableStateOf(hsl.third.toString()) }
    var hslSourceHex by rememberSaveable { mutableStateOf(hex) }
    var hslError by rememberSaveable { mutableStateOf(false) }
    fun resetHslDraft(r: Int, g: Int, b: Int) {
        val restored = rgbToHsl(r, g, b)
        hueInput = restored.first.toString()
        saturationInput = restored.second.toString()
        lightnessInput = restored.third.toString()
        hslSourceHex = String.format("#%02X%02X%02X", r, g, b)
        hslError = false
    }
    // Changed applied colors refresh HSL drafts. Explicit restore/palette actions also
    // reset them when the color is unchanged. Mere view switches preserve editing drafts.
    LaunchedEffect(hex) {
        if (hslSourceHex != hex) {
            hueInput = hsl.first.toString()
            saturationInput = hsl.second.toString()
            lightnessInput = hsl.third.toString()
            hslSourceHex = hex
            hslError = false
        }
    }

    val rgbText = "rgb(${red.roundToInt()}, ${green.roundToInt()}, ${blue.roundToInt()})"
    val hslText = "hsl(${hsl.first}, ${hsl.second}%, ${hsl.third}%)"
    val history = rememberToolHistory(Tools.ROUTE_COLOR_PICKER, mapOf("hex" to hex),
        if (parseHex(hexInput) != null) "$hex\n$rgbText\n$hslText" else null,
        onRestore = { saved -> parseHex(saved["hex"].orEmpty())?.let { (r, g, b) ->
            red = r.toFloat(); green = g.toFloat(); blue = b.toFloat()
            hexInput = saved.getValue("hex"); hexError = ""
            resetHslDraft(r, g, b)
        } }, modeInputs = mapOf("view" to view),
        onRestoreMode = { saved -> view = saved["view"]?.takeIf { it in listOf("HEX", "RGB", "HSL") } ?: "HEX" })
    ToolScaffold(
        title = stringResource(R.string.color_picker_title),
        icon = Tools.byRoute(Tools.ROUTE_COLOR_PICKER)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .background(color, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = previewDescription },
            )
            IconButton(
                onClick = {
                    recentColors =
                        (listOf(hex) + recentColors.filterNot { it == hex }).take(MAX_RECENT_COLORS)
                    store.putString(KEY_RECENT_COLORS, recentColors.joinToString(","))
                    savedNotice = context.getString(R.string.color_saved_notice, hex)
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
                                    hexInput = savedHex
                                    hexError = ""
                                    resetHslDraft(parsed.first, parsed.second, parsed.third)
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("HEX", "RGB", "HSL").forEach { mode ->
                FilterChip(view == mode, { view = mode }, label = { Text(mode) })
            }
        }
        fun updateDraft() {
            hexInput = String.format("#%02X%02X%02X", red.roundToInt(), green.roundToInt(), blue.roundToInt())
            hexError = ""
        }
        if (view == "RGB") {
        ColorSlider(stringResource(R.string.color_red), red) { red = it; updateDraft() }
        ColorSlider(stringResource(R.string.color_green), green) { green = it; updateDraft() }
        ColorSlider(stringResource(R.string.color_blue), blue) { blue = it; updateDraft() }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        }
        if (view == "HEX") {
        OutlinedTextField(
            value = hexInput,
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
            supportingText = { Text(hexError.ifEmpty { stringResource(R.string.color_draft_help) }) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("colorHexInput")
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && hexInput.isNotBlank() && parseHex(hexInput) == null) {
                        hexError = context.getString(R.string.color_hex_error)
                    }
                },
        )

        Spacer(Modifier.height(16.dp))
        }
        if (view == "HSL") {
            Text(context.getString(R.string.special_edit_hsl_then_apply_preview_history_palette_copy_and_share_use_th), style = MaterialTheme.typography.bodySmall)
            HslField(context.getString(R.string.special_hue_0_360), "colorHueInput", hueInput, hslError) { hueInput = it; hslError = false }
            HslField(context.getString(R.string.special_saturation_0_100), "colorSaturationInput", saturationInput, hslError) { saturationInput = it; hslError = false }
            HslField(context.getString(R.string.special_lightness_0_100), "colorLightnessInput", lightnessInput, hslError) { lightnessInput = it; hslError = false }
            OutlinedButton(onClick = {
                val parsed = hueInput.trim().toDoubleOrNull()?.let { h ->
                    saturationInput.trim().toDoubleOrNull()?.let { s ->
                        lightnessInput.trim().toDoubleOrNull()?.let { l -> hslToRgb(h, s, l) }
                    }
                }
                if (parsed == null) hslError = true
                else {
                    red = parsed.first.toFloat(); green = parsed.second.toFloat(); blue = parsed.third.toFloat()
                    updateDraft()
                    hslError = false
                }
            }) { Text(context.getString(R.string.special_apply_hsl)) }
            if (hslError) Text(context.getString(R.string.special_enter_finite_numbers_hue_0_360_saturation_and_lightness_0_100_use),
                color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(context.getString(R.string.special_360_equals_0_colors_are_rounded_to_8_bit_rgb_displayed_hsl_is_rou), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (view) {
                    "RGB" -> ColorValue(stringResource(R.string.color_rgb), rgbText)
                    "HSL" -> {
                        ColorValue(stringResource(R.string.color_hsl), hslText)
                        Text(context.getString(R.string.special_equivalent_hsl_of_the_applied_color), style = MaterialTheme.typography.bodySmall)
                    }
                    else -> ColorValue(stringResource(R.string.color_hex), hex)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { context.copyToClipboard(context.getString(R.string.color_copy_label), "$hex\n$rgbText\n$hslText") },
            onShare = { context.shareText("$hex\n$rgbText\n$hslText") },
        )
        if (savedNotice.isNotEmpty()) {
            Text(savedNotice, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(20.dp))
        com.jaguar.gearbox.ui.components.ColorInspection(red.roundToInt(), green.roundToInt(), blue.roundToInt())
        Spacer(Modifier.height(20.dp))
        com.jaguar.gearbox.ui.components.LocalImageColorSampler { r, g, b ->
            red = r.toFloat(); green = g.toFloat(); blue = b.toFloat()
            hexInput = String.format("#%02X%02X%02X", r, g, b)
            hexError = ""
            resetHslDraft(r, g, b)
        }
    }
}

@Composable
private fun ColorValue(label: String, value: String) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        ValueRow(label, value)
        TextButton(onClick = { context.copyToClipboard(label, value) }) {
            Text(stringResource(R.string.color_copy_format, label))
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

@Composable
private fun HslField(label: String, tag: String, value: String, isError: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, isError = isError,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag(tag))
}
