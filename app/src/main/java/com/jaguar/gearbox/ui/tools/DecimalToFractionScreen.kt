package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.decimalToFraction
import com.jaguar.gearbox.logic.formatFraction
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun DecimalToFractionScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    val parsed = input.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val fraction = parsed?.let { decimalToFraction(it) }
    val fractionText = fraction?.let { formatFraction(it) }
    val error = when {
        input.isBlank() -> ""
        parsed == null -> context.getString(R.string.calc_enter_a_valid_decimal_number)
        fraction == null -> context.getString(R.string.calc_value_is_outside_the_supported_fraction_range)
        else -> ""
    }

    val history = com.jaguar.gearbox.ui.components.rememberToolHistory(
        Tools.ROUTE_DECIMAL_TO_FRACTION, mapOf("input" to input), fractionText,
        onRestore = { values -> input = values["input"].orEmpty() },
    )
    ToolScaffold(
        title = context.getString(R.string.calc_decimal_to_fraction),
        icon = Tools.byRoute(Tools.ROUTE_DECIMAL_TO_FRACTION)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.calc_decimal_number)) },
            placeholder = { Text(stringResource(R.string.calc_e_g_0_75)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error.isNotEmpty(),
            supportingText = { if (error.isNotEmpty()) Text(error) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.calc_fraction_approximation), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.calc_uses_denominators_up_to_1_000_000_some_decimal_inputs_a),
            style = MaterialTheme.typography.bodyMedium)
        if (fractionText != null) {
            Spacer(Modifier.height(12.dp))
            val forms = fractionText.split("\n=  ", limit = 2)
            ResultCard(
                text = forms.first(),
                textStyle = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                onCopy = { context.copyToClipboard(context.getString(R.string.calc_fraction), fractionText) },
                onShare = { context.shareText(context.getString(R.string.calc_1_s_2_s_2, input, fractionText)) },
            )
            if (forms.size > 1) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.calc_mixed_number), style = MaterialTheme.typography.titleSmall)
                Text(forms[1], style = MaterialTheme.typography.headlineSmall)
            }
        } else if (input.isBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.calc_for_example_0_75_becomes_3_4), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = "" }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Text(stringResource(R.string.calc_clear))
        }
    }
}
