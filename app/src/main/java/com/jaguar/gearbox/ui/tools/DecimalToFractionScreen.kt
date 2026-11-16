package com.jaguar.gearbox.ui.tools

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
        parsed == null -> "Enter a valid decimal number."
        fraction == null -> "Value is outside the supported fraction range."
        else -> ""
    }

    val history = com.jaguar.gearbox.ui.components.rememberToolHistory(
        Tools.ROUTE_DECIMAL_TO_FRACTION, mapOf("input" to input), fractionText,
        onRestore = { values -> input = values["input"].orEmpty() },
    )
    ToolScaffold(
        title = "Decimal to Fraction",
        icon = Tools.byRoute(Tools.ROUTE_DECIMAL_TO_FRACTION)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Decimal number") },
            placeholder = { Text("e.g. 0.75") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error.isNotEmpty(),
            supportingText = { if (error.isNotEmpty()) Text(error) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text("Fraction approximation", style = MaterialTheme.typography.titleMedium)
        Text("Uses denominators up to 1,000,000. Some decimal inputs are approximated; the result is not always exact.",
            style = MaterialTheme.typography.bodyMedium)
        if (fractionText != null) {
            Spacer(Modifier.height(12.dp))
            val forms = fractionText.split("\n=  ", limit = 2)
            ResultCard(
                text = forms.first(),
                textStyle = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                onCopy = { context.copyToClipboard("Fraction", fractionText) },
                onShare = { context.shareText("$input ≈ $fractionText") },
            )
            if (forms.size > 1) {
                Spacer(Modifier.height(12.dp))
                Text("Mixed number", style = MaterialTheme.typography.titleSmall)
                Text(forms[1], style = MaterialTheme.typography.headlineSmall)
            }
        } else if (input.isBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("For example, 0.75 becomes 3/4.", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = "" }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Text(" Clear")
        }
    }
}
