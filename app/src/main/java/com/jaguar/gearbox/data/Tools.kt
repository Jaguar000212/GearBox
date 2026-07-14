package com.jaguar.gearbox.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import com.jaguar.gearbox.ui.tools.AgeCalculatorScreen
import com.jaguar.gearbox.ui.tools.AverageCalculatorScreen
import com.jaguar.gearbox.ui.tools.BaseConverterScreen
import com.jaguar.gearbox.ui.tools.BmiCalculatorScreen
import com.jaguar.gearbox.ui.tools.ChessScoreboardScreen
import com.jaguar.gearbox.ui.tools.ColorPickerScreen
import com.jaguar.gearbox.ui.tools.CounterScreen
import com.jaguar.gearbox.ui.tools.DateCalculatorScreen
import com.jaguar.gearbox.ui.tools.DecimalToFractionScreen
import com.jaguar.gearbox.ui.tools.DiceRollScreen
import com.jaguar.gearbox.ui.tools.FlipCoinScreen
import com.jaguar.gearbox.ui.tools.GeometryCalculatorScreen
import com.jaguar.gearbox.ui.tools.LoanCalculatorScreen
import com.jaguar.gearbox.ui.tools.NumberToRomanScreen
import com.jaguar.gearbox.ui.tools.NumberToWordsScreen
import com.jaguar.gearbox.ui.tools.OneOfTwoScreen
import com.jaguar.gearbox.ui.tools.PasswordGeneratorScreen
import com.jaguar.gearbox.ui.tools.PercentageCalculatorScreen
import com.jaguar.gearbox.ui.tools.RandomNumberScreen
import com.jaguar.gearbox.ui.tools.RatiosScreen
import com.jaguar.gearbox.ui.tools.RockPaperScissorsScreen
import com.jaguar.gearbox.ui.tools.ScoreboardScreen
import com.jaguar.gearbox.ui.tools.SpinBottleScreen
import com.jaguar.gearbox.ui.tools.TambolaScreen
import com.jaguar.gearbox.ui.tools.TextToolsScreen
import com.jaguar.gearbox.ui.tools.TimerStopwatchScreen
import com.jaguar.gearbox.ui.tools.TipCalculatorScreen
import com.jaguar.gearbox.ui.tools.UnitConverterScreen

/**
 * Central registry of the tools shown on the home screen. This is the Compose equivalent of the
 * Java `ToolList` class. Each [Tool] carries its own screen composable, so registering a new
 * destination in [com.jaguar.gearbox.GearBoxApp]'s `NavHost` is a one-line loop over [all]
 * instead of a separate `composable(...)` call per tool.
 */
object Tools {

    const val ROUTE_AVERAGE = "tool/average"
    const val ROUTE_LOAN = "tool/loan"
    const val ROUTE_COUNTER = "tool/counter"
    const val ROUTE_AGE = "tool/age"
    const val ROUTE_SCOREBOARD = "tool/scoreboard"
    const val ROUTE_RANDOM = "tool/random"
    const val ROUTE_BASE_CONVERTER = "tool/base_converter"
    const val ROUTE_COLOR_PICKER = "tool/color_picker"
    const val ROUTE_UNIT_CONVERTER = "tool/unit_converter"
    const val ROUTE_NUMBER_TO_WORDS = "tool/number_to_words"
    const val ROUTE_NUMBER_TO_ROMAN = "tool/number_to_roman"
    const val ROUTE_GEOMETRY = "tool/geometry"
    const val ROUTE_PERCENTAGE = "tool/percentage"
    const val ROUTE_TAMBOLA = "tool/tambola"
    const val ROUTE_FLIP_COIN = "tool/flip_coin"
    const val ROUTE_SPIN_BOTTLE = "tool/spin_bottle"
    const val ROUTE_RPS = "tool/rps"
    const val ROUTE_DICE = "tool/dice"
    const val ROUTE_CHESS_SCOREBOARD = "tool/chess_scoreboard"
    const val ROUTE_ONE_OF_TWO = "tool/one_of_two"
    const val ROUTE_RATIOS = "tool/ratios"
    const val ROUTE_DECIMAL_TO_FRACTION = "tool/decimal_to_fraction"
    const val ROUTE_TEXT_TOOLS = "tool/text_tools"
    const val ROUTE_TIMER_STOPWATCH = "tool/timer_stopwatch"
    const val ROUTE_BMI = "tool/bmi"
    const val ROUTE_TIP = "tool/tip"
    const val ROUTE_PASSWORD_GENERATOR = "tool/password_generator"
    const val ROUTE_DATE_CALCULATOR = "tool/date_calculator"

    val all: List<Tool> = listOf(
        Tool(
            name = "Average Calculator",
            description = "Calculate average with some other interesting results.",
            icon = Icons.Filled.Calculate,
            route = ROUTE_AVERAGE,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> AverageCalculatorScreen(onNavigateBack) },
        ),
        Tool(
            name = "Interest Calculator",
            description = "Calculates both simple and compound interests.",
            icon = Icons.Filled.Percent,
            route = ROUTE_LOAN,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> LoanCalculatorScreen(onNavigateBack) },
            keywords = listOf("loan", "emi", "installment"),
        ),
        Tool(
            name = "Age Calculator",
            description = "Calculates the age between two dates.",
            icon = Icons.Filled.Cake,
            route = ROUTE_AGE,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> AgeCalculatorScreen(onNavigateBack) },
        ),
        Tool(
            name = "Geometry Calculator",
            description = "Perform calculations for shapes like circles, rectangles, and triangles.",
            icon = Icons.Filled.Straighten,
            route = ROUTE_GEOMETRY,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> GeometryCalculatorScreen(onNavigateBack) },
        ),
        Tool(
            name = "Percentage Calculator",
            description = "Perform percentage-based calculations.",
            icon = Icons.Filled.PieChart,
            route = ROUTE_PERCENTAGE,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> PercentageCalculatorScreen(onNavigateBack) },
        ),
        Tool(
            name = "Ratios",
            description = "Calculate and simplify ratios.",
            icon = Icons.Filled.Balance,
            route = ROUTE_RATIOS,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> RatiosScreen(onNavigateBack) },
        ),
        Tool(
            name = "BMI Calculator",
            description = "Calculate body mass index and weight category from height and weight.",
            icon = Icons.Filled.MonitorWeight,
            route = ROUTE_BMI,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> BmiCalculatorScreen(onNavigateBack) },
            keywords = listOf("weight", "obesity", "body fat"),
        ),
        Tool(
            name = "Tip Calculator",
            description = "Split a bill with a tip across any number of people.",
            icon = Icons.Filled.Payments,
            route = ROUTE_TIP,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> TipCalculatorScreen(onNavigateBack) },
            keywords = listOf("gratuity", "bill split"),
        ),
        Tool(
            name = "Date Calculator",
            description = "Add or subtract days, weeks, months, or years from a date.",
            icon = Icons.Filled.EditCalendar,
            route = ROUTE_DATE_CALCULATOR,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> DateCalculatorScreen(onNavigateBack) },
        ),
        Tool(
            name = "Base Converter",
            description = "Convert numbers between binary, octal, decimal, and hexadecimal bases.",
            icon = Icons.Filled.Tag,
            route = ROUTE_BASE_CONVERTER,
            category = ToolCategory.CONVERTERS,
            content = { onNavigateBack -> BaseConverterScreen(onNavigateBack) },
            keywords = listOf("binary", "hex", "octal"),
        ),
        Tool(
            name = "Unit Converters",
            description = "Convert between various units like length, weight, and volume.",
            icon = Icons.Filled.SwapHoriz,
            route = ROUTE_UNIT_CONVERTER,
            category = ToolCategory.CONVERTERS,
            content = { onNavigateBack -> UnitConverterScreen(onNavigateBack) },
            keywords = listOf("temperature", "celsius", "fahrenheit"),
        ),
        Tool(
            name = "Number to Words",
            description = "Convert numerical values into word format.",
            icon = Icons.Filled.TextFields,
            route = ROUTE_NUMBER_TO_WORDS,
            category = ToolCategory.CONVERTERS,
            content = { onNavigateBack -> NumberToWordsScreen(onNavigateBack) },
        ),
        Tool(
            name = "Number to Roman",
            description = "Convert numerical values into Roman numeral format.",
            icon = Icons.Filled.HistoryEdu,
            route = ROUTE_NUMBER_TO_ROMAN,
            category = ToolCategory.CONVERTERS,
            content = { onNavigateBack -> NumberToRomanScreen(onNavigateBack) },
        ),
        Tool(
            name = "Decimal to Fraction Converter",
            description = "Convert decimal numbers into fractions.",
            icon = Icons.Filled.Functions,
            route = ROUTE_DECIMAL_TO_FRACTION,
            category = ToolCategory.CONVERTERS,
            content = { onNavigateBack -> DecimalToFractionScreen(onNavigateBack) },
        ),
        Tool(
            name = "Random Number Generator",
            description = "Generate random numbers within a specified range.",
            icon = Icons.Filled.Shuffle,
            route = ROUTE_RANDOM,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> RandomNumberScreen(onNavigateBack) },
        ),
        Tool(
            name = "Tambola Numbers",
            description = "Generate random numbers for Tambola (Housie) games.",
            icon = Icons.Filled.Grid4x4,
            route = ROUTE_TAMBOLA,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> TambolaScreen(onNavigateBack) },
            keywords = listOf("housie", "bingo"),
        ),
        Tool(
            name = "Flip Coin",
            description = "Simulate a coin flip for heads or tails.",
            icon = Icons.Filled.MonetizationOn,
            route = ROUTE_FLIP_COIN,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> FlipCoinScreen(onNavigateBack) },
        ),
        Tool(
            name = "Spin the Bottle",
            description = "Simulate spinning a bottle for fun games.",
            icon = Icons.AutoMirrored.Filled.RotateRight,
            route = ROUTE_SPIN_BOTTLE,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> SpinBottleScreen(onNavigateBack) },
        ),
        Tool(
            name = "Rock Paper Scissors",
            description = "Play the classic Rock-Paper-Scissors game.",
            icon = Icons.Filled.FrontHand,
            route = ROUTE_RPS,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> RockPaperScissorsScreen(onNavigateBack) },
        ),
        Tool(
            name = "Dice Roll",
            description = "Roll a virtual die.",
            icon = Icons.Filled.Casino,
            route = ROUTE_DICE,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> DiceRollScreen(onNavigateBack) },
        ),
        Tool(
            name = "1 of 2",
            description = "Choose between two options.",
            icon = Icons.AutoMirrored.Filled.CallSplit,
            route = ROUTE_ONE_OF_TWO,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> OneOfTwoScreen(onNavigateBack) },
        ),
        Tool(
            name = "Counter",
            description = "A simple counter to increment, decrement and reset.",
            icon = Icons.Filled.PlusOne,
            route = ROUTE_COUNTER,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> CounterScreen(onNavigateBack) },
        ),
        Tool(
            name = "Scoreboard",
            description = "Manage scores for games and activities.",
            icon = Icons.Filled.Scoreboard,
            route = ROUTE_SCOREBOARD,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> ScoreboardScreen(onNavigateBack) },
        ),
        Tool(
            name = "Chess Scoreboard",
            description = "A specially designed scoreboard for chess matches.",
            icon = Icons.Filled.EmojiEvents,
            route = ROUTE_CHESS_SCOREBOARD,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> ChessScoreboardScreen(onNavigateBack) },
        ),
        Tool(
            name = "Color Picker",
            description = "Identify and select colors using HEX, RGB, or HSL values.",
            icon = Icons.Filled.Colorize,
            route = ROUTE_COLOR_PICKER,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> ColorPickerScreen(onNavigateBack) },
        ),
        Tool(
            name = "Text Tools",
            description = "Word/character counts, case conversion, and palindrome checking.",
            icon = Icons.AutoMirrored.Filled.Article,
            route = ROUTE_TEXT_TOOLS,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> TextToolsScreen(onNavigateBack) },
        ),
        Tool(
            name = "Timer / Stopwatch",
            description = "Count up with lap times or count down from a set duration.",
            icon = Icons.Filled.Timer,
            route = ROUTE_TIMER_STOPWATCH,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> TimerStopwatchScreen(onNavigateBack) },
        ),
        Tool(
            name = "Password Generator",
            description = "Generate a random password with configurable length and character types.",
            icon = Icons.Filled.Password,
            route = ROUTE_PASSWORD_GENERATOR,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> PasswordGeneratorScreen(onNavigateBack) },
            keywords = listOf("pin", "passcode", "security"),
        ),
    )

    fun byRoute(route: String?): Tool? = all.firstOrNull { it.route == route }
}
