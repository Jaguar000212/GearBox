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

@Composable
fun BmiCalculatorScreen(onNavigateBack: () -> Unit) {
    var heightCm by rememberSaveable { mutableStateOf("") }
    var weightKg by rememberSaveable { mutableStateOf("") }

    val height = heightCm.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val weight = weightKg.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val result = if (height != null && weight != null && height > 0 && weight > 0) {
        computeBmi(heightCm = height, weightKg = weight)
    } else {
        null
    }
    val error = if ((heightCm.isNotBlank() || weightKg.isNotBlank()) && result == null) {
        "Enter valid, positive height and weight."
    } else {
        ""
    }

    ToolScaffold(
        title = "BMI Calculator",
        icon = Tools.byRoute(Tools.ROUTE_BMI)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
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
private fun computeBmi(heightCm: Double, weightKg: Double): Pair<String, String> {
    val heightM = heightCm / 100.0
    val bmi = weightKg / (heightM * heightM)
    val category = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal weight"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }
    val formatted = String.format(Locale.getDefault(), "%.1f", bmi)
    return formatted to category
}
