package com.jaguar.gearbox.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Image
import com.jaguar.gearbox.ui.tools.ImageStudioScreen
import androidx.compose.material.icons.filled.DataObject
import com.jaguar.gearbox.ui.tools.JsonFormatterScreen
import androidx.compose.material.icons.filled.AccountTree
import com.jaguar.gearbox.ui.tools.DeadlinePlannerScreen
import androidx.compose.material.icons.filled.AspectRatio
import com.jaguar.gearbox.ui.tools.AspectRatioScreen
import androidx.compose.material.icons.filled.TableChart
import com.jaguar.gearbox.ui.tools.DecisionMatrixScreen
import androidx.compose.material.icons.filled.Public
import com.jaguar.gearbox.ui.tools.TimeZoneConverterScreen
import androidx.compose.material.icons.filled.Difference
import com.jaguar.gearbox.ui.tools.TextDiffScreen
import androidx.compose.material.icons.filled.Science
import com.jaguar.gearbox.ui.tools.ScientificCalculatorScreen
import androidx.compose.material.icons.filled.EventAvailable
import com.jaguar.gearbox.ui.tools.BusinessDaysScreen
import androidx.compose.material.icons.filled.RequestQuote
import com.jaguar.gearbox.ui.tools.BillSplitterScreen
import androidx.compose.material.icons.filled.Palette
import com.jaguar.gearbox.ui.tools.ColorBlendScreen
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timelapse
import com.jaguar.gearbox.ui.tools.FocusTimerScreen
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import com.jaguar.gearbox.ui.tools.AgeCalculatorScreen
import com.jaguar.gearbox.ui.tools.AverageCalculatorScreen
import com.jaguar.gearbox.ui.tools.BaseConverterScreen
import com.jaguar.gearbox.ui.tools.BmiCalculatorScreen
import com.jaguar.gearbox.ui.tools.BubbleLevelScreen
import com.jaguar.gearbox.ui.tools.ChessScoreboardScreen
import com.jaguar.gearbox.ui.tools.ColorPickerScreen
import com.jaguar.gearbox.ui.tools.BarcodeScannerScreen
import androidx.compose.material.icons.filled.QrCodeScanner
import com.jaguar.gearbox.ui.tools.MagnifierScreen
import androidx.compose.material.icons.filled.ZoomIn
import com.jaguar.gearbox.ui.tools.SensorCheckerScreen
import androidx.compose.material.icons.filled.Sensors
import com.jaguar.gearbox.ui.tools.ProximityScreen
import com.jaguar.gearbox.ui.tools.BarometerScreen
import androidx.compose.material.icons.filled.Speed
import com.jaguar.gearbox.ui.tools.BatteryInfoScreen
import androidx.compose.material.icons.filled.BatteryFull
import com.jaguar.gearbox.ui.tools.PitchTunerScreen
import com.jaguar.gearbox.ui.tools.SoundMeterScreen
import androidx.compose.material.icons.filled.Mic
import com.jaguar.gearbox.ui.tools.LightMeterScreen
import androidx.compose.material.icons.filled.LightMode
import com.jaguar.gearbox.ui.tools.CompassScreen
import com.jaguar.gearbox.ui.tools.CounterScreen
import com.jaguar.gearbox.ui.tools.DateCalculatorScreen
import com.jaguar.gearbox.ui.tools.DaysUntilScreen
import com.jaguar.gearbox.ui.tools.DecimalToFractionScreen
import com.jaguar.gearbox.ui.tools.DiceRollScreen
import com.jaguar.gearbox.ui.tools.DiscountCalculatorScreen
import com.jaguar.gearbox.ui.tools.FlashlightScreen
import com.jaguar.gearbox.ui.tools.FlipCoinScreen
import com.jaguar.gearbox.ui.tools.FuelCostScreen
import com.jaguar.gearbox.ui.tools.GeometryCalculatorScreen
import com.jaguar.gearbox.ui.tools.GstCalculatorScreen
import com.jaguar.gearbox.ui.tools.HashGeneratorScreen
import com.jaguar.gearbox.ui.tools.LoanCalculatorScreen
import com.jaguar.gearbox.ui.tools.MorseBinaryScreen
import com.jaguar.gearbox.ui.tools.NumberToRomanScreen
import com.jaguar.gearbox.ui.tools.NumberToWordsScreen
import com.jaguar.gearbox.ui.tools.OneOfTwoScreen
import com.jaguar.gearbox.ui.tools.PasswordGeneratorScreen
import com.jaguar.gearbox.ui.tools.PercentageCalculatorScreen
import com.jaguar.gearbox.ui.tools.QrGeneratorScreen
import com.jaguar.gearbox.ui.tools.RandomNumberScreen
import com.jaguar.gearbox.ui.tools.RatiosScreen
import com.jaguar.gearbox.ui.tools.RockPaperScissorsScreen
import com.jaguar.gearbox.ui.tools.ScoreboardScreen
import com.jaguar.gearbox.ui.tools.ScreenRulerScreen
import com.jaguar.gearbox.ui.tools.SpinBottleScreen
import com.jaguar.gearbox.ui.tools.TambolaScreen
import com.jaguar.gearbox.ui.tools.TeamPickerScreen
import com.jaguar.gearbox.ui.tools.TextToolsScreen
import com.jaguar.gearbox.ui.tools.TicTacToeScreen
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
    const val ROUTE_IMAGE_STUDIO = "tool/image_studio"
    const val ROUTE_JSON_FORMATTER = "tool/json_formatter"
    const val ROUTE_DEADLINE_PLANNER = "tool/deadline_planner"
    const val ROUTE_ASPECT_RATIO = "tool/aspect_ratio"
    const val ROUTE_DECISION_MATRIX = "tool/decision_matrix"
    const val ROUTE_TIME_ZONE_CONVERTER = "tool/time_zone_converter"
    const val ROUTE_TEXT_DIFF = "tool/text_diff"
    const val ROUTE_SCIENTIFIC_CALCULATOR = "tool/scientific_calculator"
    const val ROUTE_BUSINESS_DAY_PLANNER = "tool/business_day_planner"
    const val ROUTE_BILL_SPLITTER = "tool/bill_splitter"
    const val ROUTE_COLOR_BLEND = "tool/color_blend"
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
    const val ROUTE_FOCUS_TIMER = "tool/focus_timer"
    const val ROUTE_TIMER_STOPWATCH = "tool/timer_stopwatch"
    const val ROUTE_BMI = "tool/bmi"
    const val ROUTE_TIP = "tool/tip"
    const val ROUTE_PASSWORD_GENERATOR = "tool/password_generator"
    const val ROUTE_DATE_CALCULATOR = "tool/date_calculator"
    const val ROUTE_DISCOUNT = "tool/discount"
    const val ROUTE_GST = "tool/gst"
    const val ROUTE_DAYS_UNTIL = "tool/days_until"
    const val ROUTE_TEAM_PICKER = "tool/team_picker"
    const val ROUTE_FUEL_COST = "tool/fuel_cost"
    const val ROUTE_MORSE_BINARY = "tool/morse_binary"
    const val ROUTE_HASH_GENERATOR = "tool/hash_generator"
    const val ROUTE_TIC_TAC_TOE = "tool/tic_tac_toe"
    const val ROUTE_QR_GENERATOR = "tool/qr_generator"
    const val ROUTE_BUBBLE_LEVEL = "tool/bubble_level"
    const val ROUTE_BARCODE_SCANNER = "tool/barcode_scanner"
    const val ROUTE_MAGNIFIER = "tool/magnifier"
    const val ROUTE_SENSOR_CHECKER = "tool/sensor_checker"
    const val ROUTE_PROXIMITY = "tool/proximity_checker"
    const val ROUTE_BAROMETER = "tool/barometer"
    const val ROUTE_BATTERY_INFO = "tool/battery_information"
    const val ROUTE_PITCH_TUNER = "tool/pitch_tuner"
    const val ROUTE_SOUND_METER = "tool/sound_level_meter"
    const val ROUTE_LIGHT_METER = "tool/light_meter"
    const val ROUTE_COMPASS = "tool/compass"
    const val ROUTE_FLASHLIGHT = "tool/flashlight"
    const val ROUTE_SCREEN_RULER = "tool/screen_ruler"

    fun localized(context: android.content.Context): List<Tool> = all.map { it.localized(context) }

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
            name = "Loan Calculator",
            description = "Estimates fixed-rate monthly loan repayments.",
            icon = Icons.Filled.Percent,
            route = ROUTE_LOAN,
            category = ToolCategory.MONEY,
            content = { onNavigateBack -> LoanCalculatorScreen(onNavigateBack) },
            keywords = listOf("loan", "emi", "installment", "interest", "repayment"),
        ),
        Tool(
            name = "Age Calculator",
            description = "Calculates the age between two dates.",
            icon = Icons.Filled.Cake,
            route = ROUTE_AGE,
            category = ToolCategory.TIME,
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
            keywords = listOf("weight", "health", "body mass index"),
        ),
        Tool(
            name = "Tip Calculator",
            description = "Split a bill with a tip across any number of people.",
            icon = Icons.Filled.Payments,
            route = ROUTE_TIP,
            category = ToolCategory.MONEY,
            content = { onNavigateBack -> TipCalculatorScreen(onNavigateBack) },
            keywords = listOf("gratuity", "bill split", "tip and split"),
        ),
        Tool(
            name = "Date Calculator",
            description = "Add or subtract days, weeks, months, or years from a date.",
            icon = Icons.Filled.EditCalendar,
            route = ROUTE_DATE_CALCULATOR,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> DateCalculatorScreen(onNavigateBack) },
        ),
        Tool(
            name = "Discount Calculator",
            description = "Apply one or more stacked discounts to a price.",
            icon = Icons.Filled.LocalOffer,
            route = ROUTE_DISCOUNT,
            category = ToolCategory.MONEY,
            content = { onNavigateBack -> DiscountCalculatorScreen(onNavigateBack) },
            keywords = listOf("sale", "off", "savings"),
        ),
        Tool(
            name = "GST / Tax Calculator",
            description = "Add or remove tax from an amount.",
            icon = Icons.Filled.Receipt,
            route = ROUTE_GST,
            category = ToolCategory.MONEY,
            content = { onNavigateBack -> GstCalculatorScreen(onNavigateBack) },
            keywords = listOf("vat", "sales tax", "gst"),
        ),
        Tool(
            name = "Days Until",
            description = "Count down the days until (or since) a date you pick.",
            icon = Icons.Filled.Event,
            route = ROUTE_DAYS_UNTIL,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> DaysUntilScreen(onNavigateBack) },
            keywords = listOf("countdown", "event", "how many days"),
        ),
        Tool(
            name = "Fuel Cost Calculator",
            description = "Estimate fuel needed and trip cost from distance and mileage.",
            icon = Icons.Filled.LocalGasStation,
            route = ROUTE_FUEL_COST,
            category = ToolCategory.MONEY,
            content = { onNavigateBack -> FuelCostScreen(onNavigateBack) },
            keywords = listOf("mileage", "petrol", "gas", "trip cost"),
        ),
        Tool(
            name = "Base Converter",
            description = "Convert numbers between binary, octal, decimal, and hexadecimal bases.",
            icon = Icons.Filled.Tag,
            route = ROUTE_BASE_CONVERTER,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> BaseConverterScreen(onNavigateBack) },
            keywords = listOf("binary", "hex", "octal"),
        ),
        Tool(
            name = "Unit Converters",
            description = "Convert between various units like length, weight, and volume.",
            icon = Icons.Filled.SwapHoriz,
            route = ROUTE_UNIT_CONVERTER,
            category = ToolCategory.DEVICE,
            content = { onNavigateBack -> UnitConverterScreen(onNavigateBack) },
            keywords = listOf("temperature", "celsius", "fahrenheit"),
        ),
        Tool(
            name = "Number to Words",
            description = "Convert numerical values into word format.",
            icon = Icons.Filled.TextFields,
            route = ROUTE_NUMBER_TO_WORDS,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> NumberToWordsScreen(onNavigateBack) },
        ),
        Tool(
            name = "Number to Roman",
            description = "Convert numerical values into Roman numeral format.",
            icon = Icons.Filled.HistoryEdu,
            route = ROUTE_NUMBER_TO_ROMAN,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> NumberToRomanScreen(onNavigateBack) },
        ),
        Tool(
            name = "Decimal to Fraction Converter",
            description = "Convert decimal numbers into fractions.",
            icon = Icons.Filled.Functions,
            route = ROUTE_DECIMAL_TO_FRACTION,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> DecimalToFractionScreen(onNavigateBack) },
        ),
        Tool(
            name = "Morse / Binary Translator",
            description = "Translate text to and from Morse code or binary.",
            icon = Icons.Filled.Translate,
            route = ROUTE_MORSE_BINARY,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> MorseBinaryScreen(onNavigateBack) },
            keywords = listOf("dots", "dashes", "encode", "decode"),
        ),
        Tool(
            name = "Random Number Generator",
            description = "Generate random numbers within a specified range.",
            icon = Icons.Filled.Shuffle,
            route = ROUTE_RANDOM,
            category = ToolCategory.RANDOM,
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
            category = ToolCategory.RANDOM,
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
            keywords = listOf("random", "dice"),
        ),
        Tool(
            name = "1 of 2",
            description = "Choose between two options.",
            icon = Icons.AutoMirrored.Filled.CallSplit,
            route = ROUTE_ONE_OF_TWO,
            category = ToolCategory.RANDOM,
            content = { onNavigateBack -> OneOfTwoScreen(onNavigateBack) },
        ),
        Tool(
            name = "Team / Name Picker",
            description = "Pick a random name, or split a list of names into teams.",
            icon = Icons.Filled.Groups,
            route = ROUTE_TEAM_PICKER,
            category = ToolCategory.RANDOM,
            content = { onNavigateBack -> TeamPickerScreen(onNavigateBack) },
            keywords = listOf("names", "groups", "draw", "pick someone", "choose teams"),
        ),
        Tool(
            name = "Tic-tac-toe",
            description = "Play a two-player game of tic-tac-toe.",
            icon = Icons.Filled.Grid3x3,
            route = ROUTE_TIC_TAC_TOE,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> TicTacToeScreen(onNavigateBack) },
            keywords = listOf("noughts and crosses", "xo"),
        ),
        Tool(
            name = "Counter",
            description = "A simple counter to increment, decrement and reset.",
            icon = Icons.Filled.PlusOne,
            route = ROUTE_COUNTER,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> CounterScreen(onNavigateBack) },
        ),
        Tool(
            name = "Scoreboard",
            description = "Manage scores for games and activities.",
            icon = Icons.Filled.Scoreboard,
            route = ROUTE_SCOREBOARD,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> ScoreboardScreen(onNavigateBack) },
        ),
        Tool(
            name = "Chess Scoreboard",
            description = "A specially designed scoreboard for chess matches.",
            icon = Icons.Filled.EmojiEvents,
            route = ROUTE_CHESS_SCOREBOARD,
            category = ToolCategory.GAMES_AND_RANDOM,
            content = { onNavigateBack -> ChessScoreboardScreen(onNavigateBack) },
        ),
        Tool(
            name = "Image Studio",
            description = "Resize, crop and compress a local image, then export a new copy.",
            icon = Icons.Filled.Image,
            route = ROUTE_IMAGE_STUDIO,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> ImageStudioScreen(onNavigateBack) },
            keywords = listOf("image editor", "resize image", "crop image", "compress image", "PNG", "JPEG", "WebP"),
        ),
        Tool(
            name = "Code Formatter",
            description = "Validate, pretty-print or minify JSON locally.",
            icon = Icons.Filled.DataObject,
            route = ROUTE_JSON_FORMATTER,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> JsonFormatterScreen(onNavigateBack) },
            keywords = listOf("JSON", "formatter", "validator", "pretty print", "minify"),
        ),
        Tool(
            name = "Deadline & Milestone Planner",
            description = "Plan sequential calendar-day phases backward from a target date.",
            icon = Icons.Filled.AccountTree,
            route = ROUTE_DEADLINE_PLANNER,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> DeadlinePlannerScreen(onNavigateBack) },
            keywords = listOf("deadline planner", "milestones", "backward planning", "phase durations", "project dates"),
        ),
        Tool(
            name = "Aspect Ratio & Image Size",
            description = "Calculate reduced ratios and resized dimensions, fit or fill a box.",
            icon = Icons.Filled.AspectRatio,
            route = ROUTE_ASPECT_RATIO,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> AspectRatioScreen(onNavigateBack) },
            keywords = listOf("aspect ratio", "image size", "resize dimensions", "fit", "fill", "crop", "padding"),
        ),
        Tool(
            name = "Decision Matrix",
            description = "Compare options using your own criteria, weights and ratings.",
            icon = Icons.Filled.TableChart,
            route = ROUTE_DECISION_MATRIX,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> DecisionMatrixScreen(onNavigateBack) },
            keywords = listOf("decision matrix", "weighted scores", "compare options", "criteria", "ratings"),
        ),
        Tool(
            name = "Time Zone Converter",
            description = "Convert a date and time between named time zones using device rules.",
            icon = Icons.Filled.Public,
            route = ROUTE_TIME_ZONE_CONVERTER,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> TimeZoneConverterScreen(onNavigateBack) },
            keywords = listOf("time zone", "timezone", "IANA", "UTC", "daylight saving", "world time"),
        ),
        Tool(
            name = "Text Diff",
            description = "Compare two texts locally and inspect line-by-line changes.",
            icon = Icons.Filled.Difference,
            route = ROUTE_TEXT_DIFF,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> TextDiffScreen(onNavigateBack) },
            keywords = listOf("text diff", "compare text", "text differences", "line changes"),
        ),
        Tool(
            name = "Scientific Calculator",
            description = "Evaluate expressions with powers, roots, logarithms and trigonometry.",
            icon = Icons.Filled.Science,
            route = ROUTE_SCIENTIFIC_CALCULATOR,
            category = ToolCategory.CALCULATORS,
            content = { onNavigateBack -> ScientificCalculatorScreen(onNavigateBack) },
            keywords = listOf("scientific calculator", "trigonometry", "sine", "cosine", "logarithm", "square root", "expression"),
        ),
        Tool(
            name = "Business-day Planner",
            description = "Add or count working days using your weekends and excluded dates.",
            icon = Icons.Filled.EventAvailable,
            route = ROUTE_BUSINESS_DAY_PLANNER,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> BusinessDaysScreen(onNavigateBack) },
            keywords = listOf("business days", "working days", "weekdays", "excluded dates"),
        ),
        Tool(
            name = "Bill Splitter",
            description = "Draft a simple bill split or shared expenses with balances and settlement suggestions.",
            icon = Icons.Filled.RequestQuote,
            route = ROUTE_BILL_SPLITTER,
            category = ToolCategory.MONEY,
            content = { onNavigateBack -> BillSplitterScreen(onNavigateBack) },
            keywords = listOf("split the bill", "split bill", "equal shares", "diners", "tax", "tip", "shared expenses", "payer", "beneficiaries", "settlement"),
        ),
        Tool(
            name = "Color Blend",
            description = "Blend two opaque colors by interpolating encoded sRGB channels.",
            icon = Icons.Filled.Palette,
            route = ROUTE_COLOR_BLEND,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> ColorBlendScreen(onNavigateBack) },
            keywords = listOf("color blend", "colour blend", "mix colors", "mix colours", "interpolate", "sRGB"),
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
            name = "Focus Timer",
            description = "Plan Pomodoro focus sessions or repeating work and rest intervals.",
            icon = Icons.Filled.Timelapse,
            route = ROUTE_FOCUS_TIMER,
            category = ToolCategory.TIME,
            content = { onNavigateBack -> FocusTimerScreen(onNavigateBack) },
            keywords = listOf("pomodoro", "interval", "focus", "work", "rest", "break"),
        ),
        Tool(
            name = "Timer / Stopwatch",
            description = "Count up with lap times or count down from a set duration.",
            icon = Icons.Filled.Timer,
            route = ROUTE_TIMER_STOPWATCH,
            category = ToolCategory.TIME,
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
        Tool(
            name = "Hash Generator",
            description = "Generate an MD5, SHA-1, SHA-256, or SHA-512 hash of some text.",
            icon = Icons.Filled.Fingerprint,
            route = ROUTE_HASH_GENERATOR,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> HashGeneratorScreen(onNavigateBack) },
            keywords = listOf("md5", "sha", "checksum", "digest"),
        ),
        Tool(
            name = "QR Code Generator",
            description = "Turn text or a URL into a scannable QR code.",
            icon = Icons.Filled.QrCode,
            route = ROUTE_QR_GENERATOR,
            category = ToolCategory.UTILITIES,
            content = { onNavigateBack -> QrGeneratorScreen(onNavigateBack) },
            keywords = listOf("barcode", "scan"),
        ),
        Tool(
            name = "Bubble Level",
            description = "Use the device's sensors as a spirit level.",
            icon = Icons.Filled.Adjust,
            route = ROUTE_BUBBLE_LEVEL,
            category = ToolCategory.DEVICE,
            content = { onNavigateBack -> BubbleLevelScreen(onNavigateBack) },
            keywords = listOf("spirit level", "tilt", "check if straight"),
        ),
        Tool(name = "Barcode / QR Scanner", description = "Read supported codes locally with explicit camera access.",
            icon = Icons.Filled.QrCodeScanner, route = ROUTE_BARCODE_SCANNER, category = ToolCategory.DEVICE,
            content = { BarcodeScannerScreen(it) }, keywords = listOf("scanner", "QR", "barcode")),
        Tool(name = "Magnifier", description = "Magnify a live camera preview and optionally export a frozen frame.",
            icon = Icons.Filled.ZoomIn, route = ROUTE_MAGNIFIER, category = ToolCategory.DEVICE,
            content = { MagnifierScreen(it) }, keywords = listOf("magnifier", "camera zoom")),
        Tool(name = "Sensor Checker", description = "Inspect selected foreground sensor readings and availability.",
            icon = Icons.Filled.Sensors, route = ROUTE_SENSOR_CHECKER, category = ToolCategory.DEVICE,
            content = { SensorCheckerScreen(it) }, keywords = listOf("sensor", "hardware", "accelerometer")),
        Tool(name = "Proximity Checker", description = "Read the device proximity sensor and near/far indication.",
            icon = Icons.Filled.Sensors, route = ROUTE_PROXIMITY, category = ToolCategory.DEVICE,
            content = { ProximityScreen(it) }, keywords = listOf("proximity", "near", "far", "distance sensor")),
        Tool(name = "Barometer / Altitude Estimate", description = "Read air pressure and estimate altitude with an explicit reference.",
            icon = Icons.Filled.Speed, route = ROUTE_BAROMETER, category = ToolCategory.DEVICE,
            content = { BarometerScreen(it) }, keywords = listOf("pressure", "barometer", "altitude estimate", "hPa")),
        Tool(name = "Battery Information", description = "View battery readings reported by Android.",
            icon = Icons.Filled.BatteryFull, route = ROUTE_BATTERY_INFO, category = ToolCategory.DEVICE,
            content = { BatteryInfoScreen(it) }, keywords = listOf("battery", "charging", "voltage", "temperature", "current")),
        Tool(name = "Pitch Tuner", description = "Estimate a sustained single note from microphone input.",
            icon = Icons.Filled.Mic, route = ROUTE_PITCH_TUNER, category = ToolCategory.DEVICE,
            content = { PitchTunerScreen(it) }, keywords = listOf("tuner", "pitch", "frequency", "note", "cents")),
        Tool(name = "Sound-level Meter", description = "Read relative microphone levels in dBFS.",
            icon = Icons.Filled.Mic, route = ROUTE_SOUND_METER, category = ToolCategory.DEVICE,
            content = { SoundMeterScreen(it) }, keywords = listOf("microphone", "audio", "dBFS", "RMS")),
        Tool(
            name = "Light Meter",
            description = "Read ambient illuminance from the device light sensor.",
            icon = Icons.Filled.LightMode,
            route = ROUTE_LIGHT_METER,
            category = ToolCategory.DEVICE,
            content = { onNavigateBack -> LightMeterScreen(onNavigateBack) },
            keywords = listOf("lux", "ambient light", "illuminance", "brightness sensor"),
        ),
        Tool(
            name = "Compass",
            description = "Find magnetic north using the device's sensors.",
            icon = Icons.Filled.Explore,
            route = ROUTE_COMPASS,
            category = ToolCategory.DEVICE,
            content = { onNavigateBack -> CompassScreen(onNavigateBack) },
            keywords = listOf("heading", "direction", "north"),
        ),
        Tool(
            name = "Flashlight",
            description = "Toggle the device's camera flash as a torch.",
            icon = Icons.Filled.FlashlightOn,
            route = ROUTE_FLASHLIGHT,
            category = ToolCategory.DEVICE,
            content = { onNavigateBack -> FlashlightScreen(onNavigateBack) },
            keywords = listOf("torch", "light"),
        ),
        Tool(
            name = "Screen Ruler",
            description = "Measure small objects against the screen in cm or inches.",
            icon = Icons.Filled.SquareFoot,
            route = ROUTE_SCREEN_RULER,
            category = ToolCategory.DEVICE,
            content = { onNavigateBack -> ScreenRulerScreen(onNavigateBack) },
            keywords = listOf("measure", "length"),
        ),
    )

    fun byRoute(route: String?): Tool? = all.firstOrNull { it.route == route }
}
