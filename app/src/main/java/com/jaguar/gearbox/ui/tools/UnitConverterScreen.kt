package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale

private enum class UnitCategory(val label: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    VOLUME("Volume"),
}

/** Stores a factor to convert 1 unit into the category's base unit (meter, kilogram, liter). */
private data class MeasurementUnit(val label: String, val factorToBase: Double)

private val unitsByCategory: Map<UnitCategory, List<MeasurementUnit>> = mapOf(
    UnitCategory.LENGTH to listOf(
        MeasurementUnit("Millimeters", 0.001),
        MeasurementUnit("Centimeters", 0.01),
        MeasurementUnit("Meters", 1.0),
        MeasurementUnit("Kilometers", 1000.0),
        MeasurementUnit("Inches", 0.0254),
        MeasurementUnit("Feet", 0.3048),
        MeasurementUnit("Yards", 0.9144),
        MeasurementUnit("Miles", 1609.344),
    ),
    UnitCategory.WEIGHT to listOf(
        MeasurementUnit("Milligrams", 0.000001),
        MeasurementUnit("Grams", 0.001),
        MeasurementUnit("Kilograms", 1.0),
        MeasurementUnit("Ounces", 0.0283495),
        MeasurementUnit("Pounds", 0.453592),
        MeasurementUnit("Tonnes", 1000.0),
    ),
    UnitCategory.VOLUME to listOf(
        MeasurementUnit("Milliliters", 0.001),
        MeasurementUnit("Liters", 1.0),
        MeasurementUnit("Cubic meters", 1000.0),
        MeasurementUnit("Teaspoons", 0.00492892),
        MeasurementUnit("Tablespoons", 0.0147868),
        MeasurementUnit("Cups", 0.24),
        MeasurementUnit("Gallons (US)", 3.78541),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(onNavigateBack: () -> Unit) {
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
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
        if (value != null) {
            val fromFactor = units.first { it.label == fromUnit }.factorToBase
            val toFactor = units.first { it.label == toUnit }.factorToBase
            val result = value * fromFactor / toFactor
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = String.format(Locale.getDefault(), "%s %s = %s %s", trimNumber(value), fromUnit, trimNumber(result), toUnit),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
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
            modifier = Modifier.fillMaxWidth().menuAnchor(),
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
    val rounded = String.format(Locale.getDefault(), "%.6f", value).trimEnd('0').trimEnd('.')
    return rounded.ifEmpty { "0" }
}
