package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

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
    val minError = if (submitted && low == null) context.getString(R.string.game_enter_a_whole_number_within_the_64_bit_range) else null
    val maxError = when {
        !submitted -> null
        high == null -> context.getString(R.string.game_enter_a_whole_number_within_the_64_bit_range)
        low != null && low > high -> context.getString(R.string.game_maximum_must_be_at_least_the_minimum)
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

    ToolScaffold(context.getString(R.string.game_random_number_generator), Tools.byRoute(Tools.ROUTE_RANDOM)!!.icon, onNavigateBack, history = history) {
        Text(context.getString(R.string.game_choose_your_range), style = MaterialTheme.typography.titleLarge)
        Text(context.getString(R.string.game_both_endpoints_are_included_negative_numbers_are_welcome),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        SignedNumberField(context.getString(R.string.game_minimum), context.getString(R.string.game_e_g_1), min, minError) { min = it; result = ""; submitted = false }
        Spacer(Modifier.height(12.dp))
        SignedNumberField(context.getString(R.string.game_maximum), context.getString(R.string.game_e_g_100), max, maxError) { max = it; result = ""; submitted = false }
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
            Text(if (result.isEmpty()) context.getString(R.string.game_generate) else context.getString(R.string.game_generate_again))
        }
        Spacer(Modifier.height(20.dp))
        if (result.isNotEmpty()) {
            Text(context.getString(R.string.game_your_number), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ResultCard(
                text = result,
                textStyle = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(context.getString(R.string.game_range_1_s_to_2_s_inclusive, low, high), Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(context.getString(R.string.game_generate_a_number_to_see_your_result_here), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        ToolActions(
            onCopy = { context.copyToClipboard(context.getString(R.string.game_random_number), result) },
            onShare = { context.shareText(context.getString(R.string.game_random_number_1_s_2_s_3_s, low, high, result)) },
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
    val context = LocalContext.current
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
            modifier = Modifier.semantics { contentDescription = context.getString(R.string.game_toggle_1_s_sign, label) },
        ) { Text("±") }
    }
}
