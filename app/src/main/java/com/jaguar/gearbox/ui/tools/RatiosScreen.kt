package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import kotlin.math.abs

@Composable
fun RatiosScreen(onNavigateBack: () -> Unit) {
    var partA by rememberSaveable { mutableStateOf("") }
    var partB by rememberSaveable { mutableStateOf("") }

    val a = partA.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val b = partB.trim().toDoubleOrNull()?.takeIf { it.isFinite() }

    ToolScaffold(
        title = "Ratios",
        icon = Tools.byRoute(Tools.ROUTE_RATIOS)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = partA,
                onValueChange = { partA = it },
                label = { Text("First value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = partB,
                onValueChange = { partB = it },
                label = { Text("Second value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        if (a != null && b != null && (a != 0.0 || b != 0.0)) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = simplifyRatio(a, b),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        } else if (partA.isNotBlank() || partB.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Enter valid numbers.", color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Simplifies a:b to lowest whole-number terms, scaling up first if either value has decimals. */
private fun simplifyRatio(a: Double, b: Double): String {
    val decimalPlaces = maxOf(decimalPlacesOf(a), decimalPlacesOf(b))

    // Cap the scale so a*scale/b*scale can't overflow Long (Math.round() silently clamps to
    // Long.MAX_VALUE/MIN_VALUE on overflow instead of throwing, which would corrupt the ratio).
    val maxMagnitude = maxOf(abs(a), abs(b), 1.0)
    val maxSafeExponent = kotlin.math.floor(kotlin.math.log10(Long.MAX_VALUE / maxMagnitude / 10.0))
        .toInt()
        .coerceIn(0, decimalPlaces)
    val scale = Math.pow(10.0, maxSafeExponent.toDouble())

    var scaledA = Math.round(a * scale)
    var scaledB = Math.round(b * scale)

    // Safe now: scaledA/scaledB are bounded well within Long range by the exponent cap above, so
    // this can't hit the kotlin.math.abs(Long.MIN_VALUE) overflow that stays negative.
    val divisor = gcd(abs(scaledA), abs(scaledB))
    if (divisor != 0L) {
        scaledA /= divisor
        scaledB /= divisor
    }

    return "$scaledA : $scaledB"
}

private fun decimalPlacesOf(value: Double): Int {
    val text = String.format(Locale.ROOT, "%.6f", value).trimEnd('0')
    val dotIndex = text.indexOf('.')
    return if (dotIndex == -1) 0 else text.length - dotIndex - 1
}

private tailrec fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
