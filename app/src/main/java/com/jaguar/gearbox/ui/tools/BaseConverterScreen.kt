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
    var error by rememberSaveable { mutableStateOf("") }

    val selectedBase = NumberBase.valueOf(fromBase)
    val parsed = input.trim().toLongOrNull(selectedBase.radix)

    error = if (input.isNotBlank() && parsed == null) {
        "Not a valid ${selectedBase.label.lowercase()} number."
    } else {
        ""
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
                value = selectedBase.label,
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
                        text = { Text(base.label) },
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
            supportingText = { if (error.isNotEmpty()) Text(error) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (parsed != null) {
            Spacer(Modifier.height(20.dp))
            NumberBase.entries.forEach { base ->
                Spacer(Modifier.height(8.dp))
                val converted =
                    parsed.toString(base.radix).let { if (base.radix == 16) it.uppercase() else it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = base.label, style = MaterialTheme.typography.labelLarge)
                    Text(text = converted, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { input = "" },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Text(" Clear")
            }
            OutlinedButton(
                onClick = {
                    if (parsed != null) {
                        val summary = NumberBase.entries.joinToString("\n") { base ->
                            "${base.label}: ${
                                parsed.toString(base.radix)
                                    .let { s -> if (base.radix == 16) s.uppercase() else s }
                            }"
                        }
                        context.copyToClipboard("Base Conversion", summary)
                    }
                },
                enabled = parsed != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(" Copy all")
            }
        }
    }
}

private fun placeholderFor(base: NumberBase): String = when (base) {
    NumberBase.BINARY -> "e.g. 1010"
    NumberBase.OCTAL -> "e.g. 17"
    NumberBase.DECIMAL -> "e.g. 42"
    NumberBase.HEXADECIMAL -> "e.g. 2A"
}
