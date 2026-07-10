package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun DecimalToFractionScreen(onNavigateBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }

    val parsed = input.trim().toDoubleOrNull()
    val fraction = parsed?.let { decimalToFraction(it) }
    val error = if (input.isNotBlank() && parsed == null) "Enter a valid decimal number." else ""

    ToolScaffold(
        title = "Decimal to Fraction",
        icon = Tools.byRoute(Tools.ROUTE_DECIMAL_TO_FRACTION)!!.icon,
        onNavigateBack = onNavigateBack,
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

        if (fraction != null) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fraction,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
    }
}

/** Converts a finite decimal to a simplified fraction, e.g. 0.75 -> "3/4". */
private fun decimalToFraction(value: Double): String {
    val isNegative = value < 0
    val absValue = kotlin.math.abs(value)
    val whole = absValue.toLong()
    val fractionalPart = absValue - whole

    if (fractionalPart == 0.0) return (if (isNegative) -whole else whole).toString()

    val text = fractionalPart.toString().substringAfter('.').take(9)
    var numerator = text.toLong()
    var denominator = Math.pow(10.0, text.length.toDouble()).toLong()

    val divisor = gcd(numerator, denominator)
    if (divisor != 0L) {
        numerator /= divisor
        denominator /= divisor
    }

    val totalNumerator = whole * denominator + numerator
    val sign = if (isNegative) "-" else ""
    return "$sign$totalNumerator/$denominator"
}

private tailrec fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
