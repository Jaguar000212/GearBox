package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.jaguar.gearbox.ui.components.ResultCard
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
import com.jaguar.gearbox.logic.computeBmi
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

private enum class BmiUnitSystem(val label: Int) {
    METRIC(R.string.calc_metric_cm_kg),
    IMPERIAL(R.string.calc_imperial_ft_in_lbs),
}

@Composable
fun BmiCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
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
        val inch = if (heightIn.isBlank()) 0.0 else heightIn.trim().toDoubleOrNull()
            ?.takeIf { it.isFinite() }
        val lbs = weightLbs.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        heightM = if (ft != null && inch != null) (ft * 12.0 + inch) * 0.0254 else null
        weightKgValue = lbs?.let { it * 0.453592 }
        requiredFieldsFilled = heightFt.isNotBlank() && weightLbs.isNotBlank()
    }

    val result = if (heightM != null && weightKgValue != null && heightM > 0 && weightKgValue > 0) {
        computeBmi(heightM = heightM, weightKg = weightKgValue)?.let { it.first to localizedBmiCategory(context, it.second) }
    } else {
        null
    }
    // Only flag an error once every required field for the active unit system has something in
    // it - otherwise this fires the moment the user types the first character of the first field.
    val error = if (requiredFieldsFilled && result == null) {
        context.getString(R.string.calc_enter_valid_positive_height_and_weight)
    } else {
        ""
    }


    val history = rememberToolHistory(
        route = Tools.ROUTE_BMI,
        inputs = mapOf("unitSystem" to unitSystem, "heightCm" to heightCm, "weightKg" to weightKg, "heightFt" to heightFt, "heightIn" to heightIn, "weightLbs" to weightLbs),
        output = result?.let { context.getString(R.string.calc_bmi_1_s_nadult_category_2_s, it.first, it.second) },
        modeInputs = mapOf("unitSystem" to unitSystem),
        onRestoreMode = { saved ->
            unitSystem = BmiUnitSystem.entries.firstOrNull { it.name == saved["unitSystem"] }?.name ?: BmiUnitSystem.METRIC.name
        },
        onRestore = { saved ->
            unitSystem = BmiUnitSystem.entries.firstOrNull { it.name == saved["unitSystem"] }?.name ?: BmiUnitSystem.METRIC.name
            heightCm = saved["heightCm"] ?: ""
            weightKg = saved["weightKg"] ?: ""
            heightFt = saved["heightFt"] ?: ""
            heightIn = saved["heightIn"] ?: ""
            weightLbs = saved["weightLbs"] ?: ""
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_bmi_calculator),
        icon = Tools.byRoute(Tools.ROUTE_BMI)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_units), style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.fillMaxWidth()) {
                BmiUnitSystem.entries.forEach { entry ->
                    FilterChip(
                        selected = entry == selectedSystem,
                        onClick = { unitSystem = entry.name },
                        label = { Text(context.getString(entry.label)) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (selectedSystem == BmiUnitSystem.METRIC) {
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    label = { Text(stringResource(R.string.calc_height_cm)) },
                    placeholder = { Text(stringResource(R.string.calc_e_g_175)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    label = { Text(stringResource(R.string.calc_weight_kg)) },
                    placeholder = { Text(stringResource(R.string.calc_e_g_70)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = heightFt,
                        onValueChange = { heightFt = it },
                        label = { Text(stringResource(R.string.calc_height_ft)) },
                        placeholder = { Text(stringResource(R.string.calc_e_g_5)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = heightIn,
                        onValueChange = { heightIn = it },
                        label = { Text(stringResource(R.string.calc_additional_inches_optional)) },
                        placeholder = { Text(stringResource(R.string.calc_e_g_9)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = weightLbs,
                    onValueChange = { weightLbs = it },
                    label = { Text(stringResource(R.string.calc_weight_lbs)) },
                    placeholder = { Text(stringResource(R.string.calc_e_g_154)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }, result = {
            if (result != null) {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.calc_body_mass_index), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val summary = context.getString(R.string.calc_bmi_1_s_nadult_category_2_s, result.first, result.second)
                ResultCard(text = summary, textStyle = MaterialTheme.typography.headlineSmall,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_bmi), summary) },
                    onShare = { context.shareText(summary) })
            } else if (error.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.calc_these_categories_are_for_adults_aged_20_and_older_bmi_i),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToolActions(onClear = { heightCm = ""; weightKg = ""; heightFt = ""; heightIn = ""; weightLbs = "" },
                clearEnabled = listOf(heightCm, weightKg, heightFt, heightIn, weightLbs).any { it.isNotEmpty() })
        })
    }
}

internal fun localizedBmiCategory(context: android.content.Context, category: String): String = when (category) {
    "Underweight" -> context.getString(R.string.calc_underweight)
    "Normal weight" -> context.getString(R.string.calc_normal_weight)
    "Overweight" -> context.getString(R.string.calc_overweight)
    "Obese" -> context.getString(R.string.calc_obese)
    else -> category
}
