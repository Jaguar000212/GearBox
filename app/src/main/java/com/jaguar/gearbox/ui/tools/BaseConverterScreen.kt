package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.jaguar.gearbox.ui.components.ResultCard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.focus.onFocusChanged

private enum class NumberBase(val label: String, val radix: Int) {
    BINARY("Binary", 2),
    OCTAL("Octal", 8),
    DECIMAL("Decimal", 10),
    HEXADECIMAL("Hexadecimal", 16),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseConverterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var fromBase by rememberSaveable { mutableStateOf(NumberBase.DECIMAL.name) }
    var fromExpanded by rememberSaveable { mutableStateOf(false) }
    var touched by rememberSaveable { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }

    val selectedBase = NumberBase.valueOf(fromBase)
    val parsed = input.trim().toLongOrNull(selectedBase.radix)

    val digits = input.trim().removePrefix("-").removePrefix("+")
    val invalidDigits = digits.isEmpty() || digits.any { it.digitToIntOrNull(selectedBase.radix) == null }
    val error = when {
        input.isBlank() || parsed != null || focused || !touched -> ""
        invalidDigits -> "Use ${digitGuidance(selectedBase)} with an optional leading sign."
        else -> "Outside the signed 64-bit whole-number range."
    }

    ToolScaffold(
        title = "Base Converter",
        icon = Tools.byRoute(Tools.ROUTE_BASE_CONVERTER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        ExposedDropdownMenuBox(
            expanded = fromExpanded,
            onExpandedChange = { fromExpanded = it },
        ) {
            OutlinedTextField(
                value = "${selectedBase.label} (base ${selectedBase.radix})",
                onValueChange = {},
                readOnly = true,
                label = { Text("From base") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                NumberBase.entries.forEach { base ->
                    DropdownMenuItem(
                        text = { Text("${base.label} · base ${base.radix}") },
                        onClick = {
                            fromBase = base.name
                            fromExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Value") },
            placeholder = { Text(placeholderFor(selectedBase)) },
            isError = error.isNotEmpty(),
            supportingText = { Text(error.ifEmpty { "Whole numbers: ${digitGuidance(selectedBase)}. Optional + or − sign." }) },
            modifier = Modifier.fillMaxWidth().onFocusChanged {
                if (focused && !it.isFocused) touched = true
                focused = it.isFocused
            },
        )

        if (parsed != null) {
            Spacer(Modifier.height(16.dp))
            Text("Converted values", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val summary = NumberBase.entries.joinToString("\n\n") { base ->
                val value = parsed.toString(base.radix).let { if (base.radix == 16) it.uppercase() else it }
                "${base.label} (base ${base.radix})\n$value"
            }
            ResultCard(
                text = summary,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                onCopy = { context.copyToClipboard("Base conversion", summary) },
                onShare = { context.shareText(summary) },
            )
        } else if (input.isBlank()) {
            Spacer(Modifier.height(16.dp))
            Text("Enter a whole number to see binary, octal, decimal and hexadecimal results.")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = ""; touched = false }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Text(" Clear")
        }
    }
}

private fun digitGuidance(base: NumberBase): String = when (base) {
    NumberBase.BINARY -> "digits 0–1"
    NumberBase.OCTAL -> "digits 0–7"
    NumberBase.DECIMAL -> "digits 0–9"
    NumberBase.HEXADECIMAL -> "digits 0–9 and A–F"
}

private fun placeholderFor(base: NumberBase): String = when (base) {
    NumberBase.BINARY -> "e.g. 1010"
    NumberBase.OCTAL -> "e.g. 17"
    NumberBase.DECIMAL -> "e.g. 42"
    NumberBase.HEXADECIMAL -> "e.g. 2A"
}
