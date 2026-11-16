package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.formatNonzeroTrimmed
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory

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
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

private fun linearUnit(label: String, symbol: String, factorToBase: Double) = MeasurementUnit(
    label = label,
    symbol = symbol,
    toBase = { it * factorToBase },
    fromBase = { it / factorToBase },
)

private val unitsByCategory: Map<UnitCategory, List<MeasurementUnit>> = mapOf(
    UnitCategory.LENGTH to listOf(
        linearUnit("Millimeters", "mm", 0.001),
        linearUnit("Centimeters", "cm", 0.01),
        linearUnit("Meters", "m", 1.0),
        linearUnit("Kilometers", "km", 1000.0),
        linearUnit("Inches", "in", 0.0254),
        linearUnit("Feet", "ft", 0.3048),
        linearUnit("Yards", "yd", 0.9144),
        linearUnit("Miles", "mi", 1609.344),
    ),
    UnitCategory.WEIGHT to listOf(
        linearUnit("Milligrams", "mg", 0.000001),
        linearUnit("Grams", "g", 0.001),
        linearUnit("Kilograms", "kg", 1.0),
        linearUnit("Ounces", "oz", 0.0283495),
        linearUnit("Pounds", "lb", 0.453592),
        linearUnit("Tonnes", "t", 1000.0),
    ),
    UnitCategory.VOLUME to listOf(
        linearUnit("Milliliters", "ml", 0.001),
        linearUnit("Liters", "L", 1.0),
        linearUnit("Cubic meters", "m³", 1000.0),
        linearUnit("Teaspoons", "tsp (US)", 0.00492892),
        linearUnit("Tablespoons", "tbsp (US)", 0.0147868),
        linearUnit("Cups", "cup (240 ml)", 0.24),
        linearUnit("Gallons (US)", "gal (US)", 3.78541),
    ),
    // Base unit is Celsius.
    UnitCategory.TEMPERATURE to listOf(
        MeasurementUnit("Celsius", "°C", toBase = { it }, fromBase = { it }),
        MeasurementUnit(
            "Fahrenheit", "°F",
            toBase = { (it - 32.0) * 5.0 / 9.0 },
            fromBase = { it * 9.0 / 5.0 + 32.0 },
        ),
        MeasurementUnit(
            "Kelvin", "K",
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
    val selectedCategory = UnitCategory.valueOf(category)
    val units = unitsByCategory.getValue(selectedCategory)

    var fromUnit by rememberSaveable { mutableStateOf(units.first().label) }
    var toUnit by rememberSaveable { mutableStateOf(units.getOrElse(1) { units.first() }.label) }
    var fromExpanded by rememberSaveable { mutableStateOf(false) }
    var toExpanded by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("1") }

    val historyValue = input.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val historyFrom = units.firstOrNull { it.label == fromUnit } ?: units.first()
    val historyTo = units.firstOrNull { it.label == toUnit } ?: units.first()
    val historyConverted = historyValue?.let { historyTo.fromBase(historyFrom.toBase(it)) }?.takeIf { it.isFinite() }
    val historyEquation = if (historyValue != null && historyConverted != null)
        "${formatNonzeroTrimmed(historyValue)} ${historyFrom.symbol} = ${formatNonzeroTrimmed(historyConverted)} ${historyTo.symbol}" else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_UNIT_CONVERTER,
        inputs = mapOf("category" to category, "fromUnit" to fromUnit, "toUnit" to toUnit, "input" to input),
        output = historyEquation,
        modeInputs = mapOf("category" to category, "fromUnit" to fromUnit, "toUnit" to toUnit),
        onRestoreMode = { saved ->
            val restoredCategory = UnitCategory.entries.firstOrNull { it.name == saved["category"] } ?: UnitCategory.LENGTH
            val restoredUnits = unitsByCategory.getValue(restoredCategory)
            category = restoredCategory.name
            fromUnit = restoredUnits.firstOrNull { it.label == saved["fromUnit"] }?.label ?: restoredUnits.first().label
            toUnit = restoredUnits.firstOrNull { it.label == saved["toUnit"] }?.label ?: restoredUnits.getOrElse(1) { restoredUnits.first() }.label
        },
        onRestore = { saved ->
            val restoredCategory = UnitCategory.entries.firstOrNull { it.name == saved["category"] } ?: UnitCategory.LENGTH
            val restoredUnits = unitsByCategory.getValue(restoredCategory)
            category = restoredCategory.name
            fromUnit = restoredUnits.firstOrNull { it.label == saved["fromUnit"] }?.label ?: restoredUnits.first().label
            toUnit = restoredUnits.firstOrNull { it.label == saved["toUnit"] }?.label ?: restoredUnits.getOrElse(1) { restoredUnits.first() }.label
            input = saved["input"] ?: ""
        },
    )

    ToolScaffold(
        title = "Unit Converter",
        icon = Tools.byRoute(Tools.ROUTE_UNIT_CONVERTER)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        CompactChoiceSelector(
            label = "Category", selected = selectedCategory, choices = UnitCategory.entries,
            choiceLabel = { it.label }, onSelect = { entry ->
                if (category != entry.name) {
                    val nextUnits = unitsByCategory.getValue(entry)
                    fromUnit = nextUnits.first().label
                    toUnit = nextUnits.getOrElse(1) { nextUnits.first() }.label
                }
                category = entry.name
            },
        )

        val value = input.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        val from = units.first { it.label == fromUnit }
        val to = units.first { it.label == toUnit }
        val result = value?.let { to.fromBase(from.toBase(it)) }?.takeIf { it.isFinite() }
        val equation = if (value != null && result != null) {
            "${formatNonzeroTrimmed(value)} ${from.symbol} = ${formatNonzeroTrimmed(result)} ${to.symbol}"
        } else null

        Spacer(Modifier.height(20.dp))
        AdaptiveToolLayout(input = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Convert from", style = MaterialTheme.typography.titleMedium)
                    UnitDropdown(
                        label = "From unit",
                        units = units,
                        selected = fromUnit,
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = it },
                        onSelect = { fromUnit = it },
                    )
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Value") },
                        suffix = { Text(from.symbol) },
                        trailingIcon = {
                            // Decimal keyboards do not consistently expose a minus key.
                            TextButton(
                                onClick = {
                                    input = if (input.startsWith("-")) input.removePrefix("-")
                                    else "-${input.removePrefix("+")}"
                                },
                                modifier = Modifier.semantics { contentDescription = "Change sign" },
                            ) { Text("±") }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { val previous = fromUnit; fromUnit = toUnit; toUnit = previous }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null)
                    Text(" Swap units")
                }
            }

        }, result = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Convert to", style = MaterialTheme.typography.titleMedium)
                    UnitDropdown(
                        label = "To unit",
                        units = units,
                        selected = toUnit,
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = it },
                        onSelect = { toUnit = it },
                    )
                    if (result != null && equation != null) {
                        // Separate the value from the unit so long values and large fonts can wrap.
                        Text(formatNonzeroTrimmed(result), style = MaterialTheme.typography.displaySmall)
                        Text(to.symbol, style = MaterialTheme.typography.titleMedium)
                        Text(equation, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            if (input.isBlank()) "Enter a value to see the conversion."
                            else if (value == null) "Enter a valid finite number."
                            else "This conversion is too large. Try a smaller value.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            ToolActions(
                onCopy = { equation?.let { context.copyToClipboard("Unit conversion", it) } },
                onShare = { equation?.let { context.shareText(it) } },
                resultEnabled = equation != null,
            )
        })
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
            value = units.first { it.label == selected }.let { "${it.label} (${it.symbol})" },
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
                    text = { Text("${unit.label} (${unit.symbol})") },
                    onClick = {
                        onSelect(unit.label)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

