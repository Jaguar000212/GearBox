package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.RandomResultSnapshots
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.randomLongInclusive
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun RandomNumberScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var min by rememberSaveable { mutableStateOf("") }
    var max by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    var runId by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val low = min.trim().toLongOrNull()
    val high = max.trim().toLongOrNull()
    val minError = if (submitted && low == null) "Enter a whole number within the 64-bit range." else null
    val maxError = when {
        !submitted -> null
        high == null -> "Enter a whole number within the 64-bit range."
        low != null && low > high -> "Maximum must be at least the minimum."
        else -> null
    }

    val history = rememberToolHistory(
        route = Tools.ROUTE_RANDOM,
        inputs = mapOf("min" to min, "max" to max, "_run" to runId, "_number" to result),
        output = result.takeIf { it.isNotEmpty() && low != null && high != null && low <= high },
        recordingDelayMillis = 0L,
        modeInputs = mapOf("min" to min, "max" to max),
        onRestoreMode = { saved ->
            min = saved["min"]?.takeIf { it.trim().toLongOrNull() != null } ?: ""
            max = saved["max"]?.takeIf { it.trim().toLongOrNull() != null } ?: ""
        },
        onRestore = { saved ->
            min = saved["min"].orEmpty(); max = saved["max"].orEmpty()
            result = RandomResultSnapshots.number(saved)?.toString().orEmpty()
            runId = saved["_run"].orEmpty(); submitted = false
        },
    )

    ToolScaffold("Random Number Generator", Tools.byRoute(Tools.ROUTE_RANDOM)!!.icon, onNavigateBack, history = history) {
        Text("Choose your range", style = MaterialTheme.typography.titleLarge)
        Text("Both endpoints are included. Negative numbers are welcome.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        SignedNumberField("Minimum", "e.g. 1", min, minError) { min = it; result = ""; submitted = false }
        Spacer(Modifier.height(12.dp))
        SignedNumberField("Maximum", "e.g. 100", max, maxError) { max = it; result = ""; submitted = false }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                submitted = true
                runId = java.util.UUID.randomUUID().toString()
                result = if (low != null && high != null && low <= high) randomLongInclusive(low, high).toString() else ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (result.isEmpty()) "Generate" else "Generate again")
        }
        Spacer(Modifier.height(20.dp))
        if (result.isNotEmpty()) {
            Text("Your number", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ResultCard(
                text = result,
                textStyle = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text("Range: ${low} to ${high} (inclusive)", Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Generate a number to see your result here.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        ToolActions(
            onCopy = { context.copyToClipboard("Random Number", result) },
            onShare = { context.shareText("Random number ($low–$high): $result") },
            onClear = { min = ""; max = ""; result = ""; submitted = false },
            resultEnabled = result.isNotEmpty(),
            clearEnabled = min.isNotEmpty() || max.isNotEmpty() || result.isNotEmpty(),
        )
    }
}

/** The explicit sign control supports numeric keyboards that omit a minus key. */
@Composable
private fun SignedNumberField(
    label: String,
    placeholder: String,
    value: String,
    fieldError: String?,
    onValueChange: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            label = { Text(label) }, placeholder = { Text(placeholder) },
            isError = fieldError != null,
            supportingText = fieldError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = {
                val normalized = value.trim()
                onValueChange(
                    if (normalized.startsWith("-")) normalized.removePrefix("-")
                    else "-" + normalized.removePrefix("+"),
                )
            },
            modifier = Modifier.semantics { contentDescription = "Toggle $label sign" },
        ) { Text("±") }
    }
}
