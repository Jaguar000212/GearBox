package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory
import com.jaguar.gearbox.ui.components.ResultCard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.focus.onFocusChanged

private enum class NumberBase(val label: Int, val radix: Int) {
    BINARY(R.string.calc_binary, 2),
    OCTAL(R.string.calc_octal, 8),
    DECIMAL(R.string.calc_decimal, 10),
    HEXADECIMAL(R.string.calc_hexadecimal, 16),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseConverterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var fromBase by rememberSaveable { mutableStateOf(NumberBase.DECIMAL.name) }
    var touched by rememberSaveable { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }

    val selectedBase = NumberBase.valueOf(fromBase)
    val parsed = input.trim().toLongOrNull(selectedBase.radix)

    val normalized = input.trim()
    val digits = if (normalized.startsWith("-") || normalized.startsWith("+")) normalized.drop(1) else normalized
    val invalidDigits = digits.isEmpty() || digits.any { it.digitToIntOrNull(selectedBase.radix) == null }
    val error = when {
        input.isBlank() || parsed != null || focused || !touched -> ""
        invalidDigits -> context.getString(R.string.calc_use_1_s_with_an_optional_leading_sign, digitGuidance(context, selectedBase))
        else -> context.getString(R.string.calc_outside_the_signed_64_bit_whole_number_range)
    }

    val history = rememberToolHistory(
        route = Tools.ROUTE_BASE_CONVERTER,
        inputs = mapOf("input" to input, "fromBase" to fromBase),
        output = parsed?.let { value -> NumberBase.entries.joinToString("\n") { context.getString(R.string.calc_1_s_2_s, context.getString(it.label), value.toString(it.radix).uppercase()) } },
        modeInputs = mapOf("fromBase" to fromBase),
        onRestoreMode = { saved ->
            fromBase = NumberBase.entries.firstOrNull { it.name == saved["fromBase"] }?.name ?: NumberBase.DECIMAL.name
        },
        onRestore = { saved ->
            input = saved["input"] ?: ""
            fromBase = NumberBase.entries.firstOrNull { it.name == saved["fromBase"] }?.name ?: NumberBase.DECIMAL.name
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_base_converter),
        icon = Tools.byRoute(Tools.ROUTE_BASE_CONVERTER)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        CompactChoiceSelector(
            label = context.getString(R.string.calc_from_base), selected = selectedBase, choices = NumberBase.entries,
            choiceLabel = { context.getString(R.string.calc_1_s_base_2_s, context.getString(it.label), it.radix) }, onSelect = { fromBase = it.name },
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.calc_value)) },
            placeholder = { Text(placeholderFor(context, selectedBase)) },
            isError = error.isNotEmpty(),
            supportingText = { Text(error.ifEmpty { context.getString(R.string.calc_whole_numbers_1_s_optional_or_sign, digitGuidance(context, selectedBase)) }) },
            modifier = Modifier.fillMaxWidth().onFocusChanged {
                if (focused && !it.isFocused) touched = true
                focused = it.isFocused
            },
        )

        if (parsed != null) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.calc_converted_values), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val summary = NumberBase.entries.joinToString("\n\n") { base ->
                val value = parsed.toString(base.radix).let { if (base.radix == 16) it.uppercase() else it }
                context.getString(R.string.calc_1_s_base_2_s_n_3_s, context.getString(base.label), base.radix, value)
            }
            ResultCard(
                text = summary,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                onCopy = { context.copyToClipboard(context.getString(R.string.calc_base_conversion), summary) },
                onShare = { context.shareText(summary) },
            )
        } else if (input.isBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.calc_enter_a_whole_number_to_see_binary_octal_decimal_and_he))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = ""; touched = false }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Text(stringResource(R.string.calc_clear))
        }
    }
}

private fun digitGuidance(context: android.content.Context, base: NumberBase): String = when (base) {
    NumberBase.BINARY -> context.getString(R.string.calc_digits_0_1)
    NumberBase.OCTAL -> context.getString(R.string.calc_digits_0_7)
    NumberBase.DECIMAL -> context.getString(R.string.calc_digits_0_9)
    NumberBase.HEXADECIMAL -> context.getString(R.string.calc_digits_0_9_and_a_f)
}

private fun placeholderFor(context: android.content.Context, base: NumberBase): String = when (base) {
    NumberBase.BINARY -> context.getString(R.string.calc_e_g_1010)
    NumberBase.OCTAL -> context.getString(R.string.calc_e_g_17)
    NumberBase.DECIMAL -> context.getString(R.string.calc_e_g_42)
    NumberBase.HEXADECIMAL -> context.getString(R.string.calc_e_g_2a)
}
