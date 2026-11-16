package com.jaguar.gearbox.data

/** Eligibility to display two stored snapshots, not a numerical comparison or validation of results. */
enum class SavedResultComparisonReason { SAME_ITEM, DIFFERENT_TOOLS, UNSUPPORTED_TOOL, INVALID_SNAPSHOT, DIFFERENT_CONFIGURATION }
data class SavedResultComparison(val compatible: Boolean, val reason: SavedResultComparisonReason? = null)

private data class ComparisonSchema(val fields: Set<String>, val configuration: Set<String>)
private val comparisonSchemas = mapOf(
    "tool/unit_converter" to ComparisonSchema(setOf("category", "fromUnit", "toUnit", "input"), setOf("category", "fromUnit", "toUnit")),
    "tool/percentage" to ComparisonSchema(setOf("mode", "field1", "field2"), setOf("mode")),
    "tool/geometry" to ComparisonSchema(setOf("shape", "field1", "field2", "field3"), setOf("shape")),
    "tool/gst" to ComparisonSchema(setOf("amount", "rate", "inclusive"), setOf("inclusive")),
    "tool/tip" to ComparisonSchema(setOf("bill", "tipPercent", "splitCount"), emptySet()),
    "tool/base_converter" to ComparisonSchema(setOf("input", "fromBase"), setOf("fromBase")),
    "tool/number_to_words" to ComparisonSchema(setOf("input", "system"), setOf("system")),
    "tool/number_to_roman" to ComparisonSchema(setOf("input"), emptySet()),
    "tool/decimal_to_fraction" to ComparisonSchema(setOf("input"), emptySet()),
)

// Snapshot keys are stable serialized identifiers, not localized display strings.
// Keep this deliberately closed when a tool adds a new mode or unit.
private val comparisonUnits = mapOf(
    "LENGTH" to setOf("Millimeters", "Centimeters", "Meters", "Kilometers", "Inches", "Feet", "Yards", "Miles"),
    "WEIGHT" to setOf("Milligrams", "Grams", "Kilograms", "Ounces", "Pounds", "Tonnes"),
    "VOLUME" to setOf("Milliliters", "Liters", "Cubic meters", "Teaspoons", "Tablespoons", "Cups", "Gallons (US)"),
    "TEMPERATURE" to setOf("Celsius", "Fahrenheit", "Kelvin"),
)
private fun validComparisonConfiguration(route: String, inputs: Map<String, String>): Boolean = when (route) {
    "tool/unit_converter" -> comparisonUnits[inputs["category"]]?.let {
        inputs["fromUnit"] in it && inputs["toUnit"] in it
    } ?: false
    "tool/percentage" -> inputs["mode"] in setOf("OF_VALUE", "WHAT_PERCENT", "CHANGE")
    "tool/geometry" -> inputs["shape"] in setOf("CIRCLE", "RECTANGLE", "TRIANGLE")
    "tool/gst" -> inputs["inclusive"] in setOf("true", "false")
    "tool/base_converter" -> inputs["fromBase"] in setOf("BINARY", "OCTAL", "DECIMAL", "HEXADECIMAL")
    "tool/number_to_words" -> inputs["system"] in setOf("INTERNATIONAL", "INDIAN")
    else -> route in comparisonSchemas
}

/** Fail closed for unknown/new tools and incomplete legacy snapshots. No default mode is inferred. */
fun savedResultComparison(first: SavedToolItem, second: SavedToolItem): SavedResultComparison {
    fun blocked(reason: SavedResultComparisonReason) = SavedResultComparison(false, reason)
    if (first.id == second.id) return blocked(SavedResultComparisonReason.SAME_ITEM)
    if (first.route != second.route) return blocked(SavedResultComparisonReason.DIFFERENT_TOOLS)
    val schema = comparisonSchemas[first.route] ?: return blocked(SavedResultComparisonReason.UNSUPPORTED_TOOL)
    if (listOf(first, second).any { item ->
        item.id.isBlank() || item.output.isBlank() || !ToolMemoryPolicy.validRecord(item.inputs, item.output) ||
            item.inputs.keys != schema.fields || !validComparisonConfiguration(item.route, item.inputs)
    }) return blocked(SavedResultComparisonReason.INVALID_SNAPSHOT)
    if (schema.configuration.any { first.inputs[it] != second.inputs[it] })
        return blocked(SavedResultComparisonReason.DIFFERENT_CONFIGURATION)
    return SavedResultComparison(true)
}
