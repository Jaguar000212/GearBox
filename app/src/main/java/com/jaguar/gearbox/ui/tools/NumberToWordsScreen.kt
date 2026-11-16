package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.numberToWords
import com.jaguar.gearbox.logic.numberToWordsIndian
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.ResultCard
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.focus.onFocusChanged

private enum class NumberSystem(val label: Int) {
    INTERNATIONAL(R.string.calc_international),
    INDIAN(R.string.calc_indian_lakh_crore),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NumberToWordsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var numberSystem by rememberSaveable { mutableStateOf(NumberSystem.INTERNATIONAL.name) }
    var focused by rememberSaveable { mutableStateOf(false) }
    var touched by rememberSaveable { mutableStateOf(false) }
    val selectedSystem = NumberSystem.valueOf(numberSystem)

    val parsed = input.trim().toLongOrNull()
    val words = when {
        input.isBlank() -> null
        parsed == null -> null
        selectedSystem == NumberSystem.INDIAN -> numberToWordsIndian(parsed)
        else -> numberToWords(parsed)
    }
    val error = if (input.isNotBlank() && parsed == null && !focused && touched) context.getString(R.string.calc_enter_a_signed_64_bit_whole_number_without_commas_or_de) else ""

    val history = com.jaguar.gearbox.ui.components.rememberToolHistory(
        Tools.ROUTE_NUMBER_TO_WORDS, mapOf("input" to input, "system" to numberSystem), words,
        onRestore = { values ->
            input = values["input"].orEmpty()
            NumberSystem.entries.firstOrNull { it.name == values["system"] }?.let { numberSystem = it.name }
        },
        modeInputs = mapOf("system" to numberSystem),
        onRestoreMode = { values -> NumberSystem.entries.firstOrNull { it.name == values["system"] }?.let { numberSystem = it.name } },
    )
    ToolScaffold(
        title = context.getString(R.string.calc_number_to_words),
        icon = Tools.byRoute(Tools.ROUTE_NUMBER_TO_WORDS)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumberSystem.entries.forEach { entry ->
                FilterChip(
                    selected = entry == selectedSystem,
                    onClick = { numberSystem = entry.name },
                    label = { Text(context.getString(entry.label)) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.calc_whole_number)) },
            placeholder = { Text(stringResource(R.string.calc_e_g_123456)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error.isNotEmpty(),
            supportingText = { Text(error.ifEmpty { if (selectedSystem == NumberSystem.INDIAN) context.getString(R.string.calc_example_100000_is_one_lakh) else context.getString(R.string.calc_example_100000_is_one_hundred_thousand) }) },
            modifier = Modifier.fillMaxWidth().onFocusChanged {
                if (focused && !it.isFocused) touched = true
                focused = it.isFocused
            },
        )
        OutlinedButton(onClick = {
            val normalized = input.trim()
            input = if (normalized.startsWith("-")) normalized.drop(1) else "-" + normalized.removePrefix("+")
        }) {
            Text(stringResource(R.string.calc_change_sign))
        }

        if (words != null) {
            Spacer(Modifier.height(16.dp))
            Text(context.getString(R.string.calc_1_s_wording, context.getString(selectedSystem.label)), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ResultCard(
                text = words,
                textStyle = MaterialTheme.typography.headlineSmall,
                onCopy = { context.copyToClipboard(context.getString(R.string.calc_number_in_words), words) },
                onShare = { context.shareText(words) },
            )
        } else if (input.isBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.calc_enter_a_whole_number_to_write_it_in_english))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = ""; touched = false }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Text(stringResource(R.string.calc_clear))
        }
    }
}
