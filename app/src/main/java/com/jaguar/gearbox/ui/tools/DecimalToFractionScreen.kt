package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
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
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.math.abs
import kotlin.math.floor

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

        if (fractionText != null) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fractionText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { context.copyToClipboard("Fraction", fractionText) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(" Copy")
            }
        }
    }
}

private data class Fraction(val numerator: Long, val denominator: Long)

/**
 * Finds the fraction with the smallest denominator (up to [MAX_DENOMINATOR]) that approximates
 * [value] within a tight tolerance, via the standard continued-fraction expansion. Unlike the
 * previous fixed-9-decimal-digit truncation, this both handles arbitrarily long inputs and
 * naturally recovers repeating decimals - e.g. 0.3333333333 resolves to 1/3, 0.142857142857 to
 * 1/7 - since those are exactly what a short continued fraction converges to. Returns null only
 * if [value] is too large to represent (its integer part alone would overflow a Long numerator).
 */
private fun decimalToFraction(value: Double): Fraction? {
    if (value == 0.0) return Fraction(0, 1)
    val absValue = abs(value)
    if (absValue > 1e15) return null

    val maxDenominator = 1_000_000L
    var h0 = 0L
    var h1 = 1L
    var k0 = 1L
    var k1 = 0L
    var b = absValue
    var bestNumerator = 0L
    var bestDenominator = 0L

    return try {
        for (i in 0 until 40) {
            val a = floor(b).toLong()
            val h2 = Math.addExact(Math.multiplyExact(a, h1), h0)
            val k2 = Math.addExact(Math.multiplyExact(a, k1), k0)
            if (k2 > maxDenominator) break
            h0 = h1; h1 = h2
            k0 = k1; k1 = k2
            bestNumerator = h1
            bestDenominator = k1
            val fractional = b - a
            if (fractional < 1e-12) break
            b = 1.0 / fractional
        }
        if (bestDenominator == 0L) null
        else Fraction(if (value < 0) -bestNumerator else bestNumerator, bestDenominator)
    } catch (_: ArithmeticException) {
        null
    }
}

/** Shows the improper fraction, plus a mixed-number form (e.g. "7/4  =  1 3/4") when it applies. */
private fun formatFraction(fraction: Fraction): String {
    val isNegative = fraction.numerator < 0
    val sign = if (isNegative) "-" else ""
    val absNumerator = abs(fraction.numerator)
    val whole = absNumerator / fraction.denominator
    val remainder = absNumerator % fraction.denominator

    if (remainder == 0L) return "$sign$whole"

    val improper = "$sign$absNumerator/${fraction.denominator}"
    if (whole == 0L) return improper

    val mixed = "$sign$whole $remainder/${fraction.denominator}"
    return "$improper\n=  $mixed"
}
