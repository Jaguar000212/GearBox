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
import java.util.Locale
import kotlin.math.pow

@Composable
fun DecimalToFractionScreen(onNavigateBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }

    val parsed = input.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val fraction = parsed?.let { decimalToFraction(it) }
    val error = when {
        input.isBlank() -> ""
        parsed == null -> "Enter a valid decimal number."
        fraction == null -> "Number is too large to convert."
        else -> ""
    }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        }
    }
}

/** Converts a finite decimal to a simplified fraction, e.g. 0.75 -> "3/4". Returns null on overflow. */
private fun decimalToFraction(value: Double): String? {
    // abs() is safe here: any Double surviving isFinite() is well within Long range for its
    // magnitude to be negated without the two's-complement overflow that bites kotlin.math.abs(Long).
    val isNegative = value < 0
    val absValue = kotlin.math.abs(value)
    if (absValue > 1e15) return null // too large to represent exactly as a Long-based fraction

    val whole = absValue.toLong()
    val fractionalPart = absValue - whole

    if (fractionalPart == 0.0) return (if (isNegative) -whole else whole).toString()

    // Fixed-point formatting avoids Double.toString()'s scientific notation for small magnitudes
    // (e.g. 0.0001 -> "1.0E-4"), which would otherwise fail to parse as a fraction digit string.
    val text = String.format(Locale.ROOT, "%.9f", fractionalPart).substringAfter('.').take(9)
    var numerator = text.toLong()
    var denominator = 10.0.pow(text.length.toDouble()).toLong()

    val divisor = gcd(numerator, denominator)
    if (divisor != 0L) {
        numerator /= divisor
        denominator /= divisor
    }

    return try {
        val totalNumerator = Math.addExact(Math.multiplyExact(whole, denominator), numerator)
        val sign = if (isNegative) "-" else ""
        "$sign$totalNumerator/$denominator"
    } catch (_: ArithmeticException) {
        null
    }
}

private tailrec fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
