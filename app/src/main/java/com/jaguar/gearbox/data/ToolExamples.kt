package com.jaguar.gearbox.data

import java.time.LocalDate

/** Explicit input-only imports. Never includes game, clock, password or hardware actions. */
data class ToolExample(val description: String, val inputs: Map<String, String>)

object ToolExampleCatalog {
    private fun example(description: String, vararg inputs: Pair<String, String>) =
        ToolExample(description, mapOf(*inputs)).also { require(ToolMemoryPolicy.validInputs(it.inputs)) }

    private val fixed = mapOf(
        Tools.ROUTE_DECISION_MATRIX to example("Load two generic options with equal Value and Comfort weights, then tap Compare to see their tied scores.", "optionLabels" to "Option A\nOption B", "criterionLabels" to "Value\nComfort", "weights" to "1,1", "scores" to "4,3\n3,4"),
        Tools.ROUTE_TIME_ZONE_CONVERTER to example("Load 15 January 2026 at 12:00 UTC and Asia/Kolkata, then convert to 17:30 on the same day.", "date" to "2026-01-15", "time" to "12:00", "sourceZone" to "UTC", "targetZone" to "Asia/Kolkata", "sourceOffset" to "AUTO"),
        Tools.ROUTE_TEXT_DIFF to example("Load Hello followed by world, and Hello followed by GearBox, then tap Compare to inspect the changed second line.", "firstText" to "Hello\nworld", "secondText" to "Hello\nGearBox"),
        Tools.ROUTE_SCIENTIFIC_CALCULATOR to example("Load sqrt(16)+2^3, then calculate 12. The example does not calculate until you request it.", "expression" to "sqrt(16)+2^3", "angleMode" to "DEG"),
        Tools.ROUTE_BUSINESS_DAY_PLANNER to example("Load five business days after 16 November 2026 with Saturday and Sunday excluded, then calculate 23 November.", "mode" to "ADD", "start" to "2026-11-16", "end" to "2026-11-23", "offset" to "5", "weekendMask" to "96", "excludedDates" to ""),
        Tools.ROUTE_BILL_SPLITTER to example("Load a bill of 100 with a tip of 10 percent and three diners, then calculate shares of 36.67, 36.67 and 36.66.", "bill" to "100", "tax" to "0", "tipPercent" to "10", "diners" to "3"),
        Tools.ROUTE_COLOR_BLEND to example("Load black and white with an equal blend, then tap Blend to calculate #808080.", "firstHex" to "#000000", "secondHex" to "#FFFFFF", "weight" to "50"),
        Tools.ROUTE_AVERAGE to example("2, 4, 6 has an average of 4.", "input" to "2, 4, 6"),
        Tools.ROUTE_AGE to example("Compare 1 January 2000 with 1 January 2025: 25 years.", "earlierDate" to "2000-01-01", "laterDate" to "2025-01-01"),
        Tools.ROUTE_BASE_CONVERTER to example("Decimal 10 is binary 1010 and hexadecimal A.", "input" to "10", "fromBase" to "DECIMAL"),
        Tools.ROUTE_BMI to example("Metric example: 170 cm and 65 kg.", "unitSystem" to "METRIC", "heightCm" to "170", "weightKg" to "65", "heightFt" to "", "heightIn" to "", "weightLbs" to ""),
        Tools.ROUTE_COLOR_PICKER to example("HEX #FF0000 is RGB 255, 0, 0.", "hex" to "#FF0000"),
        Tools.ROUTE_DATE_CALCULATOR to example("Add 14 days to 1 January 2025.", "baseDate" to "2025-01-01", "amountInput" to "14", "unit" to "DAYS", "direction" to "ADD"),
        Tools.ROUTE_DECIMAL_TO_FRACTION to example("0.75 equals 3/4.", "input" to "0.75"),
        Tools.ROUTE_DISCOUNT to example("Apply 20% then 10% off 100: final price 72.", "price" to "100", "discounts" to "20\n10"),
        Tools.ROUTE_FUEL_COST to example("Distance 120, efficiency 15 distance units per fuel unit, and price 2 per fuel unit: cost 16.", "distance" to "120", "mileage" to "15", "price" to "2"),
        Tools.ROUTE_GST to example("Add an illustrative 18% tax to 100: total 118. This is not tax advice.", "amount" to "100", "rate" to "18", "inclusive" to "false"),
        Tools.ROUTE_GEOMETRY to example("Rectangle width 3 and height 4: area 12, perimeter 14.", "shape" to "RECTANGLE", "field1" to "3", "field2" to "4", "field3" to ""),
        Tools.ROUTE_LOAN to example("Loan amount 100000, annual rate 6%, term 5 years.", "principal" to "100000", "rate" to "6", "time" to "5"),
        Tools.ROUTE_MORSE_BINARY to example("Encode SOS as Morse code.", "mode" to "TEXT_TO_MORSE", "input" to "SOS"),
        Tools.ROUTE_NUMBER_TO_ROMAN to example("14 becomes XIV.", "input" to "14"),
        Tools.ROUTE_NUMBER_TO_WORDS to example("42 becomes forty-two in the international system.", "input" to "42", "system" to "INTERNATIONAL"),
        Tools.ROUTE_PERCENTAGE to example("20% of 150 is 30.", "mode" to "OF_VALUE", "field1" to "20", "field2" to "150"),
        Tools.ROUTE_RATIOS to example("Simplify 12:18 to 2:3.", "partA" to "12", "partB" to "18"),
        Tools.ROUTE_TEXT_TOOLS to example("Convert Hello GearBox to uppercase.", "text" to "Hello GearBox", "case" to "UPPER"),
        Tools.ROUTE_TIP to example("Bill 100, tip 15%, split between 2 people: 57.50 each.", "bill" to "100", "tipPercent" to "15", "splitCount" to "2"),
        Tools.ROUTE_UNIT_CONVERTER to example("1 kilometer equals 1000 meters.", "category" to "LENGTH", "fromUnit" to "Kilometers", "toUnit" to "Meters", "input" to "1"),
        Tools.ROUTE_HASH_GENERATOR to example("Load Hello GearBox with SHA-256, then tap Generate.", "text" to "Hello GearBox", "algorithm" to "SHA256"),
        Tools.ROUTE_QR_GENERATOR to example("Load Hello GearBox, then tap Generate to make its QR code.", "text" to "Hello GearBox"),
    )

    val routes: Set<String> = fixed.keys + Tools.ROUTE_DAYS_UNTIL

    fun localizedForRoute(context: android.content.Context, route: String?, today: LocalDate = LocalDate.now()): ToolExample? =
        localizedToolExample(context, route, today)

    fun forRoute(route: String?, today: LocalDate = LocalDate.now()): ToolExample? =
        if (route == Tools.ROUTE_DAYS_UNTIL) example(
            "An example event seven days from today (${today.plusDays(7)}).",
            "label" to "Example event", "targetDate" to today.plusDays(7).toString(),
        ) else fixed[route]
}
