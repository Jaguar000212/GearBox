package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.logic.RgbColor
import com.jaguar.gearbox.logic.contrastRatio
import com.jaguar.gearbox.logic.meetsAaLargeText
import com.jaguar.gearbox.logic.meetsAaNormalText
import com.jaguar.gearbox.logic.relativeLuminance
import com.jaguar.gearbox.logic.rgbToHsl
import java.util.Locale

/** Inspector inside Color Picker. Pair selection is explicit; changing the picker never changes it. */
@Composable
fun ColorInspection(red: Int, green: Int, blue: Int) {
    val current = RgbColor(red, green, blue)
    var foregroundPacked by rememberSaveable { mutableIntStateOf(0x000000) }
    var backgroundPacked by rememberSaveable { mutableIntStateOf(0xFFFFFF) }
    fun unpack(value: Int) = RgbColor(value shr 16 and 255, value shr 8 and 255, value and 255)
    val foreground = unpack(foregroundPacked)
    val background = unpack(backgroundPacked)
    val packedCurrent = (red shl 16) or (green shl 8) or blue
    fun hex(value: RgbColor) = String.format(Locale.ROOT, "#%02X%02X%02X", value.red, value.green, value.blue)
    val foregroundHex = hex(foreground)
    val backgroundHex = hex(background)
    val ratio = contrastRatio(foreground, background)
    val sampleDescription = stringResource(R.string.color_inspection_sample_description, foregroundHex, backgroundHex)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.color_inspection_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.color_inspection_selected, hex(current)))
            Text(stringResource(R.string.color_inspection_lightness, rgbToHsl(red, green, blue).third))
            Text(stringResource(R.string.color_inspection_luminance, String.format(Locale.ROOT, "%.4f", relativeLuminance(current))))
            Text(stringResource(R.string.color_inspection_brightness_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.color_inspection_pair), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.color_inspection_foreground, foregroundHex))
            Text(stringResource(R.string.color_inspection_background, backgroundHex))
            Text(stringResource(R.string.color_inspection_sample), color = Color(foreground.red, foreground.green, foreground.blue),
                modifier = Modifier.fillMaxWidth().background(Color(background.red, background.green, background.blue)).padding(16.dp)
                    .clearAndSetSemantics { contentDescription = sampleDescription })
            OutlinedButton(onClick = { foregroundPacked = packedCurrent }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.color_inspection_set_foreground))
            }
            OutlinedButton(onClick = { backgroundPacked = packedCurrent }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.color_inspection_set_background))
            }
            OutlinedButton(onClick = { val old = foregroundPacked; foregroundPacked = backgroundPacked; backgroundPacked = old }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.color_inspection_swap))
            }
            Text(stringResource(R.string.color_inspection_ratio, String.format(Locale.ROOT, "%.2f", ratio)), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (meetsAaNormalText(ratio)) R.string.color_inspection_normal_pass else R.string.color_inspection_normal_fail))
            Text(stringResource(if (meetsAaLargeText(ratio)) R.string.color_inspection_large_pass else R.string.color_inspection_large_fail))
            Text(stringResource(R.string.color_inspection_caveat), style = MaterialTheme.typography.bodySmall)
        }
    }
}
