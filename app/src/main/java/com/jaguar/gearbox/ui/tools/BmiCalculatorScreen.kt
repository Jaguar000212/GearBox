package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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

private enum class BmiUnitSystem(val label: String) {
    METRIC("Metric (cm / kg)"),
    IMPERIAL("Imperial (ft, in / lbs)"),
}

@Composable
fun BmiCalculatorScreen(onNavigateBack: () -> Unit) {
    var unitSystem by rememberSaveable { mutableStateOf(BmiUnitSystem.METRIC.name) }
    val selectedSystem = BmiUnitSystem.valueOf(unitSystem)

    var heightCm by rememberSaveable { mutableStateOf("") }
    var weightKg by rememberSaveable { mutableStateOf("") }
    var heightFt by rememberSaveable { mutableStateOf("") }
    var heightIn by rememberSaveable { mutableStateOf("") }
    var weightLbs by rememberSaveable { mutableStateOf("") }

    val heightM: Double?
    val weightKgValue: Double?
    val requiredFieldsFilled: Boolean

    if (selectedSystem == BmiUnitSystem.METRIC) {
        val height = heightCm.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val weight = weightKg.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        heightM = height?.let { it / 100.0 }
        weightKgValue = weight
        requiredFieldsFilled = heightCm.isNotBlank() && weightKg.isNotBlank()
    } else {
        val ft = heightFt.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        // Inches is optional (defaults to 0) - a whole-feet height like "6 ft" is a valid input.
        val inch = if (heightIn.isBlank()) 0.0 else heightIn.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val lbs = weightLbs.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        heightM = if (ft != null && inch != null) (ft * 12.0 + inch) * 0.0254 else null
        weightKgValue = lbs?.let { it * 0.453592 }
        requiredFieldsFilled = heightFt.isNotBlank() && weightLbs.isNotBlank()
    }

    val result = if (heightM != null && weightKgValue != null && heightM > 0 && weightKgValue > 0) {
        computeBmi(heightM = heightM, weightKg = weightKgValue)
    } else {
        null
    }
    // Only flag an error once every required field for the active unit system has something in
    // it - otherwise this fires the moment the user types the first character of the first field.
    val error = if (requiredFieldsFilled && result == null) {
        "Enter valid, positive height and weight."
    } else {
        ""
    }

    ToolScaffold(
        title = "BMI Calculator",
        icon = Tools.byRoute(Tools.ROUTE_BMI)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BmiUnitSystem.entries.forEach { entry ->
                FilterChip(
                    selected = entry == selectedSystem,
                    onClick = { unitSystem = entry.name },
                    label = { Text(entry.label) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        if (selectedSystem == BmiUnitSystem.METRIC) {
            OutlinedTextField(
                value = heightCm,
                onValueChange = { heightCm = it },
                label = { Text("Height (cm)") },
                placeholder = { Text("e.g. 175") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = weightKg,
                onValueChange = { weightKg = it },
                label = { Text("Weight (kg)") },
                placeholder = { Text("e.g. 70") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = heightFt,
                    onValueChange = { heightFt = it },
                    label = { Text("Height (ft)") },
                    placeholder = { Text("e.g. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = heightIn,
                    onValueChange = { heightIn = it },
                    label = { Text("Height (in)") },
                    placeholder = { Text("e.g. 9") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = weightLbs,
                onValueChange = { weightLbs = it },
                label = { Text("Weight (lbs)") },
                placeholder = { Text("e.g. 154") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (result != null) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "BMI: ${result.first}\n${result.second}",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        } else if (error.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Returns the BMI value (formatted) and its WHO weight-status category. */
private fun computeBmi(heightM: Double, weightKg: Double): Pair<String, String> {
    val bmi = weightKg / (heightM * heightM)
    val category = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal weight"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }
    val formatted = String.format(Locale.US, "%.1f", bmi)
    return formatted to category
}
