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
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun NumberToRomanScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    val parsed = input.trim().toIntOrNull()
    val roman = if (parsed != null && parsed in 1..3999) numberToRoman(parsed) else null
    val error = when {
        input.isBlank() -> ""
        parsed == null -> "Enter a valid whole number."
        parsed !in 1..3999 -> "Enter a number between 1 and 3999."
        else -> ""
    }

    val history = com.jaguar.gearbox.ui.components.rememberToolHistory(
        Tools.ROUTE_NUMBER_TO_ROMAN, mapOf("input" to input), roman,
        onRestore = { values -> input = values["input"].orEmpty() },
    )
    ToolScaffold(
        title = "Number to Roman",
        icon = Tools.byRoute(Tools.ROUTE_NUMBER_TO_ROMAN)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Number (1-3999)") },
            placeholder = { Text("e.g. 1994") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error.isNotEmpty(),
            supportingText = { if (error.isNotEmpty()) Text(error) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        if (roman != null) {
            Text("Roman numeral", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ResultCard(
                text = roman,
                textStyle = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                onCopy = { context.copyToClipboard("Roman numeral", roman) },
                onShare = { context.shareText("$parsed → $roman") },
            )
            Text("$parsed → $roman", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp))
        } else if (input.isBlank()) {
            Text("Enter a whole number to see its Roman numeral. For example, 1994 → MCMXCIV.",
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { input = "" }, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Text(" Clear")
        }
    }
}

private val romanValues = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
private val romanSymbols =
    arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")

private fun numberToRoman(number: Int): String {
    var remaining = number
    val builder = StringBuilder()
    for (i in romanValues.indices) {
        while (remaining >= romanValues[i]) {
            remaining -= romanValues[i]
            builder.append(romanSymbols[i])
        }
    }
    return builder.toString()
}
