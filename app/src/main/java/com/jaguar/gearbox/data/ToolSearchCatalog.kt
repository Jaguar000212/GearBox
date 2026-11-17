package com.jaguar.gearbox.data

import android.content.Context
import com.jaguar.gearbox.R

data class ToolSearchOperation(val id: String, val label: String, val modeInputs: Map<String, String>)
data class ToolSearchResult(val tool: Tool, val operation: ToolSearchOperation?, val score: Int) {
    val label: String get() = operation?.label ?: tool.name
    val detail: String get() = if (operation != null) tool.name else tool.description
}

/** Curated discovery of existing tools/selectors only. No new tool, automatic run, or input replacement. */
object ToolSearchCatalog {
    private data class AliasSpec(val resource: Int, val english: List<String>)
    private data class OperationSpec(val id: String, val route: String, val label: Int, val aliases: Int,
        val englishLabel: String, val englishAliases: List<String>, val inputs: Map<String, String>)
    private val taskAliases = mapOf(
        Tools.ROUTE_BUBBLE_LEVEL to AliasSpec(R.array.search_tasks_bubble_level, listOf("is this straight", "is this level", "level a surface", "hang a picture", "check a slope")),
        Tools.ROUTE_COMPASS to AliasSpec(R.array.search_tasks_compass, listOf("which way is north", "find direction", "find north", "magnetic heading")),
        Tools.ROUTE_FLASHLIGHT to AliasSpec(R.array.search_tasks_flashlight, listOf("light up the dark", "turn on torch", "need some light")),
        Tools.ROUTE_SCREEN_RULER to AliasSpec(R.array.search_tasks_screen_ruler, listOf("measure a small object", "screen ruler length", "how long is this")),
        Tools.ROUTE_AGE to AliasSpec(R.array.search_tasks_age, listOf("how old am i", "age between dates", "birthday age")),
        Tools.ROUTE_AVERAGE to AliasSpec(R.array.search_tasks_average, listOf("average these numbers", "arithmetic geometric harmonic mean", "summary statistics")),
        Tools.ROUTE_LOAN to AliasSpec(R.array.search_tasks_loan, listOf("monthly loan payment", "loan repayment", "emi estimate")),
        Tools.ROUTE_DATE_CALCULATOR to AliasSpec(R.array.search_tasks_date_calculator, listOf("add days to a date", "subtract weeks from a date", "date arithmetic")),
        Tools.ROUTE_DAYS_UNTIL to AliasSpec(R.array.search_tasks_days_until, listOf("days until an event", "countdown to a date", "how many days left")),
        Tools.ROUTE_BASE_CONVERTER to AliasSpec(R.array.search_tasks_base_converter, listOf("convert number base", "binary decimal hexadecimal")),
        Tools.ROUTE_BMI to AliasSpec(R.array.search_tasks_bmi, listOf("body mass index", "bmi from height and weight")),
        Tools.ROUTE_DECIMAL_TO_FRACTION to AliasSpec(R.array.search_tasks_decimal_to_fraction, listOf("decimal as fraction", "turn decimal into fraction")),
        Tools.ROUTE_DISCOUNT to AliasSpec(R.array.search_tasks_discount, listOf("sale price", "price after discount", "stacked discounts")),
        Tools.ROUTE_FUEL_COST to AliasSpec(R.array.search_tasks_fuel_cost, listOf("trip fuel cost", "cost of petrol", "fuel for a journey")),
        Tools.ROUTE_GEOMETRY to AliasSpec(R.array.search_tasks_geometry, listOf("shape area perimeter", "calculate area", "area and perimeter")),
        Tools.ROUTE_GST to AliasSpec(R.array.search_tasks_gst, listOf("add tax to price", "remove tax from price", "tax included price")),
        Tools.ROUTE_NUMBER_TO_ROMAN to AliasSpec(R.array.search_tasks_number_to_roman, listOf("roman numerals", "number in roman numerals")),
        Tools.ROUTE_NUMBER_TO_WORDS to AliasSpec(R.array.search_tasks_number_to_words, listOf("spell out a number", "number in words")),
        Tools.ROUTE_PERCENTAGE to AliasSpec(R.array.search_tasks_percentage, listOf("work out a percentage", "percentage calculation")),
        Tools.ROUTE_RATIOS to AliasSpec(R.array.search_tasks_ratios, listOf("simplify a ratio", "ratio between numbers")),
        Tools.ROUTE_TIP to AliasSpec(R.array.search_tasks_tip, listOf("calculate a tip", "tip per person", "bill with tip per person")),
        Tools.ROUTE_UNIT_CONVERTER to AliasSpec(R.array.search_tasks_unit_converter, listOf("convert measurement units", "unit conversion")),
        Tools.ROUTE_TEXT_TOOLS to AliasSpec(R.array.search_tasks_text_tools, listOf("change letter case", "word and character count", "convert text case")),
        Tools.ROUTE_HASH_GENERATOR to AliasSpec(R.array.search_tasks_hash_generator, listOf("hash some text", "text checksum", "generate a digest")),
        Tools.ROUTE_QR_GENERATOR to AliasSpec(R.array.search_tasks_qr_generator, listOf("make a qr code", "text to qr", "link to qr code")),
        Tools.ROUTE_MORSE_BINARY to AliasSpec(R.array.search_tasks_morse_binary, listOf("translate morse code", "text in binary", "decode binary text")),
        Tools.ROUTE_DECISION_MATRIX to AliasSpec(R.array.decision_catalog_tasks, listOf("decision matrix", "compare options", "weighted decision", "rank options by criteria", "weighted scores")),
        Tools.ROUTE_TIME_ZONE_CONVERTER to AliasSpec(R.array.zone_catalog_tasks, listOf("convert time zones", "time zone converter", "timezone converter", "time in another zone", "UTC to local time", "daylight saving time")),
        Tools.ROUTE_TEXT_DIFF to AliasSpec(R.array.diff_catalog_tasks, listOf("compare two texts", "find text differences", "text diff", "compare lines", "what changed in this text")),
        Tools.ROUTE_SCIENTIFIC_CALCULATOR to AliasSpec(R.array.scientific_catalog_tasks, listOf("scientific calculator", "calculate sine", "calculate cosine", "calculate logarithm", "evaluate expression", "calculate square root")),
        Tools.ROUTE_BUSINESS_DAY_PLANNER to AliasSpec(R.array.business_catalog_tasks, listOf("business day planner", "working days", "skip weekends", "business days between dates", "add business days", "subtract business days", "count business days")),
        Tools.ROUTE_BILL_SPLITTER to AliasSpec(R.array.bill_catalog_tasks, listOf("split the bill", "split bill", "divide restaurant bill", "equal bill shares", "bill per diner", "shared expenses", "who owes whom", "expense balances", "settlement suggestions")),
        Tools.ROUTE_COLOR_BLEND to AliasSpec(R.array.blend_catalog_tasks, listOf("blend two colors", "blend two colours", "mix colors", "mix colours", "interpolate color", "color blend")),
        Tools.ROUTE_COLOR_PICKER to AliasSpec(R.array.search_tasks_color_picker, listOf("convert color values", "color hex rgb hsl", "choose a color")),
        Tools.ROUTE_COUNTER to AliasSpec(R.array.search_tasks_counter, listOf("count things", "count people", "tally counter")),
        Tools.ROUTE_FOCUS_TIMER to AliasSpec(R.array.focus_catalog_tasks, listOf("pomodoro timer", "work rest intervals", "focus and break", "interval timer", "study session")),
        Tools.ROUTE_TIMER_STOPWATCH to AliasSpec(R.array.search_tasks_timer_stopwatch, listOf("time an activity", "start a countdown", "measure elapsed time")),
        Tools.ROUTE_PASSWORD_GENERATOR to AliasSpec(R.array.search_tasks_password_generator, listOf("create a strong password", "generate a password")),
        Tools.ROUTE_DICE to AliasSpec(R.array.search_tasks_dice, listOf("roll dice", "roll a die")),
        Tools.ROUTE_FLIP_COIN to AliasSpec(R.array.search_tasks_flip_coin, listOf("flip a coin", "heads or tails")),
        Tools.ROUTE_SPIN_BOTTLE to AliasSpec(R.array.search_tasks_spin_bottle, listOf("spin a bottle", "choose a direction randomly")),
        Tools.ROUTE_ONE_OF_TWO to AliasSpec(R.array.search_tasks_one_of_two, listOf("choose between two options", "pick one of two", "decide between two things")),
        Tools.ROUTE_RANDOM to AliasSpec(R.array.search_tasks_random, listOf("random number", "pick a number in a range")),
        Tools.ROUTE_RPS to AliasSpec(R.array.search_tasks_rps, listOf("rock paper scissors", "play against the computer")),
        Tools.ROUTE_TAMBOLA to AliasSpec(R.array.search_tasks_tambola, listOf("tambola number caller", "housie caller")),
        Tools.ROUTE_TEAM_PICKER to AliasSpec(R.array.search_tasks_team_picker, listOf("pick a name", "make random teams", "choose from names")),
        Tools.ROUTE_TIC_TAC_TOE to AliasSpec(R.array.search_tasks_tic_tac_toe, listOf("noughts and crosses", "tic tac toe game")),
        Tools.ROUTE_SCOREBOARD to AliasSpec(R.array.search_tasks_scoreboard, listOf("keep score", "two team score")),
        Tools.ROUTE_CHESS_SCOREBOARD to AliasSpec(R.array.search_tasks_chess_scoreboard, listOf("chess score", "track chess match score")),
    )
    private val operations = listOf(
        OperationSpec("geometry.circle", Tools.ROUTE_GEOMETRY, R.string.search_operation_geometry_circle, R.array.search_aliases_geometry_circle, "Circle area and perimeter", listOf("circle area", "circle perimeter", "area of a circle", "perimeter of a circle"), mapOf("shape" to "CIRCLE")),
        OperationSpec("geometry.rectangle", Tools.ROUTE_GEOMETRY, R.string.search_operation_geometry_rectangle, R.array.search_aliases_geometry_rectangle, "Rectangle area and perimeter", listOf("rectangle area", "rectangle perimeter", "area of a rectangle", "perimeter of a rectangle"), mapOf("shape" to "RECTANGLE")),
        OperationSpec("geometry.triangle", Tools.ROUTE_GEOMETRY, R.string.search_operation_geometry_triangle, R.array.search_aliases_geometry_triangle, "Triangle area and perimeter", listOf("triangle area", "triangle perimeter", "area of a triangle", "perimeter of a triangle"), mapOf("shape" to "TRIANGLE")),
        OperationSpec("units.length", Tools.ROUTE_UNIT_CONVERTER, R.string.search_operation_units_length, R.array.search_aliases_units_length, "Length conversion", listOf("length", "convert length", "length units", "meters feet", "inches centimeters", "kilometers miles"), mapOf("category" to "LENGTH")),
        OperationSpec("units.weight", Tools.ROUTE_UNIT_CONVERTER, R.string.search_operation_units_weight, R.array.search_aliases_units_weight, "Weight conversion", listOf("weight", "convert weight", "weight units", "kilograms pounds", "grams ounces"), mapOf("category" to "WEIGHT")),
        OperationSpec("units.volume", Tools.ROUTE_UNIT_CONVERTER, R.string.search_operation_units_volume, R.array.search_aliases_units_volume, "Volume conversion", listOf("volume", "convert volume", "volume units", "liters gallons", "milliliters cups"), mapOf("category" to "VOLUME")),
        OperationSpec("units.temperature", Tools.ROUTE_UNIT_CONVERTER, R.string.search_operation_units_temperature, R.array.search_aliases_units_temperature, "Temperature conversion", listOf("temperature", "convert temperature", "temperature units", "celsius fahrenheit", "fahrenheit celsius", "kelvin", "degrees temperature"), mapOf("category" to "TEMPERATURE")),
        OperationSpec("percentage.of_value", Tools.ROUTE_PERCENTAGE, R.string.search_operation_percentage_of_value, R.array.search_aliases_percentage_of_value, "Percentage of a value", listOf("percent of a number", "percentage of value"), mapOf("mode" to "OF_VALUE")),
        OperationSpec("percentage.what_percent", Tools.ROUTE_PERCENTAGE, R.string.search_operation_percentage_what_percent, R.array.search_aliases_percentage_what_percent, "What percentage?", listOf("what percent", "part as percentage of total"), mapOf("mode" to "WHAT_PERCENT")),
        OperationSpec("percentage.change", Tools.ROUTE_PERCENTAGE, R.string.search_operation_percentage_change, R.array.search_aliases_percentage_change, "Percentage change", listOf("percent increase", "percent decrease", "percentage change"), mapOf("mode" to "CHANGE")),
        OperationSpec("text.upper", Tools.ROUTE_TEXT_TOOLS, R.string.search_operation_text_upper, R.array.search_aliases_text_upper, "Uppercase", listOf("uppercase", "capital letters", "make text uppercase"), mapOf("case" to "UPPER")),
        OperationSpec("text.lower", Tools.ROUTE_TEXT_TOOLS, R.string.search_operation_text_lower, R.array.search_aliases_text_lower, "Lowercase", listOf("lowercase", "small letters", "make text lowercase"), mapOf("case" to "LOWER")),
        OperationSpec("text.title", Tools.ROUTE_TEXT_TOOLS, R.string.search_operation_text_title, R.array.search_aliases_text_title, "Title case", listOf("title case", "capitalize words"), mapOf("case" to "TITLE")),
        OperationSpec("text.camel", Tools.ROUTE_TEXT_TOOLS, R.string.search_operation_text_camel, R.array.search_aliases_text_camel, "Camel case", listOf("camel case", "camelcase"), mapOf("case" to "CAMEL")),
        OperationSpec("morse.text_to_morse", Tools.ROUTE_MORSE_BINARY, R.string.search_operation_morse_text_to_morse, R.array.search_aliases_morse_text_to_morse, "Text to Morse", listOf("encode morse", "text to morse"), mapOf("mode" to "TEXT_TO_MORSE")),
        OperationSpec("morse.morse_to_text", Tools.ROUTE_MORSE_BINARY, R.string.search_operation_morse_morse_to_text, R.array.search_aliases_morse_morse_to_text, "Morse to text", listOf("decode morse", "morse to text"), mapOf("mode" to "MORSE_TO_TEXT")),
        OperationSpec("morse.text_to_binary", Tools.ROUTE_MORSE_BINARY, R.string.search_operation_morse_text_to_binary, R.array.search_aliases_morse_text_to_binary, "Text to binary", listOf("encode binary text", "text to binary"), mapOf("mode" to "TEXT_TO_BINARY")),
        OperationSpec("morse.binary_to_text", Tools.ROUTE_MORSE_BINARY, R.string.search_operation_morse_binary_to_text, R.array.search_aliases_morse_binary_to_text, "Binary to text", listOf("decode binary", "binary to text"), mapOf("mode" to "BINARY_TO_TEXT")),
        OperationSpec("base.binary", Tools.ROUTE_BASE_CONVERTER, R.string.search_operation_base_binary, R.array.search_aliases_base_binary, "Binary input base", listOf("convert binary number", "binary number base", "binary to decimal"), mapOf("fromBase" to "BINARY")),
        OperationSpec("base.octal", Tools.ROUTE_BASE_CONVERTER, R.string.search_operation_base_octal, R.array.search_aliases_base_octal, "Octal input base", listOf("convert octal number", "octal number base", "octal to decimal"), mapOf("fromBase" to "OCTAL")),
        OperationSpec("base.decimal", Tools.ROUTE_BASE_CONVERTER, R.string.search_operation_base_decimal, R.array.search_aliases_base_decimal, "Decimal input base", listOf("convert decimal number", "decimal number base", "decimal to binary"), mapOf("fromBase" to "DECIMAL")),
        OperationSpec("base.hexadecimal", Tools.ROUTE_BASE_CONVERTER, R.string.search_operation_base_hexadecimal, R.array.search_aliases_base_hexadecimal, "Hexadecimal input base", listOf("convert hexadecimal number", "hexadecimal number base", "hexadecimal to decimal"), mapOf("fromBase" to "HEXADECIMAL")),
        OperationSpec("hash.md5", Tools.ROUTE_HASH_GENERATOR, R.string.search_operation_hash_md5, R.array.search_aliases_hash_md5, "MD5 hash", listOf("MD5 hash", "md5", "hash with MD5"), mapOf("algorithm" to "MD5")),
        OperationSpec("hash.sha1", Tools.ROUTE_HASH_GENERATOR, R.string.search_operation_hash_sha1, R.array.search_aliases_hash_sha1, "SHA-1 hash", listOf("SHA-1 hash", "sha1", "hash with SHA-1"), mapOf("algorithm" to "SHA1")),
        OperationSpec("hash.sha256", Tools.ROUTE_HASH_GENERATOR, R.string.search_operation_hash_sha256, R.array.search_aliases_hash_sha256, "SHA-256 hash", listOf("SHA-256 hash", "sha256", "hash with SHA-256"), mapOf("algorithm" to "SHA256")),
        OperationSpec("hash.sha512", Tools.ROUTE_HASH_GENERATOR, R.string.search_operation_hash_sha512, R.array.search_aliases_hash_sha512, "SHA-512 hash", listOf("SHA-512 hash", "sha512", "hash with SHA-512"), mapOf("algorithm" to "SHA512")),
    )

    fun search(context: Context, query: String, tools: List<Tool> = Tools.all, category: ToolCategory? = null): List<ToolSearchResult> {
        val shown = tools.filter { category == null || it.category == category }.distinctBy { it.route }.map { it.localized(context) }
        val documents = shown.map { tool ->
            val source = Tools.byRoute(tool.route) ?: tool
            val aliases = taskAliases[tool.route]
            val terms = listOf(tool.name, tool.description, tool.category.localizedLabel(context), source.name, source.description, source.category.label) +
                tool.keywords + source.keywords + aliases?.english.orEmpty() +
                (aliases?.let { context.resources.getStringArray(it.resource).toList() } ?: emptyList())
            ToolSearchDocument(tool.route, terms, operations.filter { it.route == tool.route }.map {
                SearchOperationDocument(it.id, listOf(context.getString(it.label), it.englishLabel) +
                    context.resources.getStringArray(it.aliases).toList() + it.englishAliases)
            }, genericTerms = listOf(tool.name, source.name, tool.category.localizedLabel(context), source.category.label))
        }
        return ToolSearchEngine.rank(query, documents).map { match ->
            ToolSearchResult(shown.first { it.route == match.route },
                operations.firstOrNull { it.id == match.operationId }?.let { it.present(context) }, match.score)
        }
    }

    /** Resolve only trusted IDs, not caller-supplied input maps. Navigation must import modeOnly. */
    fun resolveOperation(context: Context, id: String): ToolSearchResult? {
        val spec = operations.firstOrNull { it.id == id } ?: return null
        val tool = Tools.byRoute(spec.route)?.localized(context) ?: return null
        return ToolSearchResult(tool, spec.present(context), 0)
    }
    private fun OperationSpec.present(context: Context) = ToolSearchOperation(id, context.getString(label), inputs.toMap())

    /** Pure index for matching/schema tests, without Context or resource loading. */
    fun englishDocuments(): List<ToolSearchDocument> = Tools.all.map { tool ->
        ToolSearchDocument(tool.route, listOf(tool.name, tool.description, tool.category.label) + tool.keywords + taskAliases[tool.route]?.english.orEmpty(),
            operations.filter { it.route == tool.route }.map { SearchOperationDocument(it.id, listOf(it.englishLabel) + it.englishAliases) },
            genericTerms = listOf(tool.name, tool.category.label))
    }
    fun operationInputs(id: String): Pair<String, Map<String, String>>? = operations.firstOrNull { it.id == id }?.let { it.route to it.inputs.toMap() }
}
