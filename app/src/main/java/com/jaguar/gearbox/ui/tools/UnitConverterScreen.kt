package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale

private enum class UnitCategory(val label: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    VOLUME("Volume"),
    TEMPERATURE("Temperature"),
}

/**
 * Converts a value to/from the category's base unit. Length/Weight/Volume units are simple
 * multiples of their base (meter, kilogram, liter), but Temperature scales have an offset
 * (0°C = 32°F = 273.15K), so a single `factorToBase` multiplier can't represent them — hence
 * explicit to/from lambdas instead.
 */
private data class MeasurementUnit(
    val label: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

private fun linearUnit(label: String, factorToBase: Double) = MeasurementUnit(
    label = label,
    toBase = { it * factorToBase },
    fromBase = { it / factorToBase },
)

private val unitsByCategory: Map<UnitCategory, List<MeasurementUnit>> = mapOf(
    UnitCategory.LENGTH to listOf(
        linearUnit("Millimeters", 0.001),
        linearUnit("Centimeters", 0.01),
        linearUnit("Meters", 1.0),
        linearUnit("Kilometers", 1000.0),
        linearUnit("Inches", 0.0254),
        linearUnit("Feet", 0.3048),
        linearUnit("Yards", 0.9144),
        linearUnit("Miles", 1609.344),
    ),
    UnitCategory.WEIGHT to listOf(
        linearUnit("Milligrams", 0.000001),
        linearUnit("Grams", 0.001),
        linearUnit("Kilograms", 1.0),
        linearUnit("Ounces", 0.0283495),
        linearUnit("Pounds", 0.453592),
        linearUnit("Tonnes", 1000.0),
    ),
    UnitCategory.VOLUME to listOf(
        linearUnit("Milliliters", 0.001),
        linearUnit("Liters", 1.0),
        linearUnit("Cubic meters", 1000.0),
        linearUnit("Teaspoons", 0.00492892),
        linearUnit("Tablespoons", 0.0147868),
        linearUnit("Cups", 0.24),
        linearUnit("Gallons (US)", 3.78541),
    ),
    // Base unit is Celsius.
    UnitCategory.TEMPERATURE to listOf(
        MeasurementUnit("Celsius", toBase = { it }, fromBase = { it }),
        MeasurementUnit(
            "Fahrenheit",
            toBase = { (it - 32.0) * 5.0 / 9.0 },
            fromBase = { it * 9.0 / 5.0 + 32.0 },
        ),
        MeasurementUnit(
            "Kelvin",
            toBase = { it - 273.15 },
            fromBase = { it + 273.15 },
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var category by rememberSaveable { mutableStateOf(UnitCategory.LENGTH.name) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedCategory = UnitCategory.valueOf(category)
    val units = unitsByCategory.getValue(selectedCategory)

    var fromUnit by rememberSaveable(selectedCategory) { mutableStateOf(units.first().label) }
    var toUnit by rememberSaveable(selectedCategory) { mutableStateOf(units.getOrElse(1) { units.first() }.label) }
    var fromExpanded by rememberSaveable { mutableStateOf(false) }
    var toExpanded by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("1") }

    ToolScaffold(
        title = "Unit Converters",
        icon = Tools.byRoute(Tools.ROUTE_UNIT_CONVERTER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedCategory.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }) {
                UnitCategory.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.label) },
                        onClick = {
                            category = entry.name
                            categoryExpanded = false
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UnitDropdown(
                label = "From",
                units = units,
                selected = fromUnit,
                expanded = fromExpanded,
                onExpandedChange = { fromExpanded = it },
                onSelect = { fromUnit = it },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { val temp = fromUnit; fromUnit = toUnit; toUnit = temp }) {
                Icon(Icons.Filled.SwapVert, contentDescription = "Swap units")
            }
            UnitDropdown(
                label = "To",
                units = units,
                selected = toUnit,
                expanded = toExpanded,
                onExpandedChange = { toExpanded = it },
                onSelect = { toUnit = it },
                modifier = Modifier.weight(1f),
            )
        }

        val value = input.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        Spacer(Modifier.height(20.dp))
        val resultText = if (value != null) {
            val from = units.first { it.label == fromUnit }
            val to = units.first { it.label == toUnit }
            val result = to.fromBase(from.toBase(value))
            String.format(
                Locale.US,
                "%s %s = %s %s",
                trimNumber(value),
                fromUnit,
                trimNumber(result),
                toUnit,
            )
        } else null

        if (resultText != null) {
            ResultCard(
                text = resultText,
                onCopy = { context.copyToClipboard("Unit conversion", resultText) },
                onShare = { context.shareText(resultText) },
            )
        } else if (input.isNotBlank()) {
            Text("Enter a valid number.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    label: String,
    units: List<MeasurementUnit>,
    selected: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label) },
                    onClick = {
                        onSelect(unit.label)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

private fun trimNumber(value: Double): String {
    val rounded = String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    return rounded.ifEmpty { "0" }
}
