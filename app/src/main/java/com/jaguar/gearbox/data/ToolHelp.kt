package com.jaguar.gearbox.data

/** Local guidance. Supported examples can be explicitly loaded after confirmation. */
data class ToolHelp(val instructions: String, val example: String, val limitation: String)

object ToolHelpCatalog {
    val all: Map<String, ToolHelp> = mapOf(
        Tools.ROUTE_AVERAGE to ToolHelp(
            "Separate numbers with spaces, commas, or new lines. Review the summary statistics below.",
            "2, 4, 6 has an average of 4.",
            "Each entered value has equal weight; this is not a weighted average.",
        ),
        Tools.ROUTE_LOAN to ToolHelp(
            "Enter the loan amount, annual rate, and term in years for an estimated monthly payment.",
            "Try 100000 at 6% for 5 years.",
            "Assumes a fixed rate and equal monthly payments. Fees and lender-specific rounding are excluded.",
        ),
        Tools.ROUTE_COUNTER to ToolHelp(
            "Use the plus and minus controls to track a count; reset starts again. A Counter home-screen widget shares this count. Find setup instructions in Settings → Home-screen widgets.",
            "Count attendees as they arrive.",
            "Check the count before resetting; a tap is not a sensor measurement.",
        ),
        Tools.ROUTE_AGE to ToolHelp(
            "Choose the starting date and the date to compare it with.",
            "Compare 1 January 2000 with 1 January 2025: 25 years.",
            "Calendar months have different lengths; years and months are not fixed day counts.",
        ),
        Tools.ROUTE_SCOREBOARD to ToolHelp(
            "Name the players or teams, then adjust their scores during play.",
            "Track a friendly match between Team A and Team B.",
            "Scores are entered manually; the tool does not enforce sport-specific rules.",
        ),
        Tools.ROUTE_RANDOM to ToolHelp(
            "Set the lower and upper bounds, then generate a number.",
            "Try a range from 1 to 10.",
            "Repeated outcomes are possible. Random results are not predictions.",
        ),
        Tools.ROUTE_BASE_CONVERTER to ToolHelp(
            "Choose the source base and enter an integer using digits valid in that base.",
            "Decimal 10 is binary 1010 and hexadecimal A.",
            "This converter handles integers, not decimal fractions; binary only accepts 0 and 1.",
        ),
        Tools.ROUTE_COLOR_PICKER to ToolHelp(
            "Enter HEX, adjust RGB, or enter HSL and tap Apply HSL. Copy, Share and saved colors use the last applied color.",
            "HEX #FF0000 is RGB 255, 0, 0.",
            "Screen appearance depends on display settings. HSL inputs convert to rounded 8-bit RGB; this is not a measurement of a physical object.",
        ),
        Tools.ROUTE_UNIT_CONVERTER to ToolHelp(
            "Choose a measurement category, source unit, and destination unit before entering a value.",
            "1 kilometre equals 1000 metres.",
            "Only units within the selected category are comparable; displayed values may be rounded.",
        ),
        Tools.ROUTE_NUMBER_TO_WORDS to ToolHelp(
            "Enter a number and read its written form.",
            "42 becomes forty-two.",
            "Use the supported number range shown by the tool; this is not a language translator.",
        ),
        Tools.ROUTE_NUMBER_TO_ROMAN to ToolHelp(
            "Enter a whole number to convert it to Roman numerals.",
            "14 becomes XIV.",
            "Standard Roman numerals do not represent zero or decimal fractions.",
        ),
        Tools.ROUTE_GEOMETRY to ToolHelp(
            "Select a shape and enter the requested dimensions using one consistent unit.",
            "A rectangle of width 3 and height 4 has area 12 and perimeter 14.",
            "Area uses squared units; volume uses cubed units. Enter lengths, not angles, in dimension fields.",
        ),
        Tools.ROUTE_PERCENTAGE to ToolHelp(
            "Choose the calculation mode, then fill in the quantities named by its fields.",
            "20% of 150 is 30.",
            "Percentage change uses the original value as its base; a zero base cannot define that change.",
        ),
        Tools.ROUTE_TAMBOLA to ToolHelp(
            "Draw numbers during a game. Capture a snapshot when you want to keep the current call order.",
            "After several calls, capture the board and inspect it in History.",
            "Restoring a snapshot replaces current progress after confirmation; it does not draw another number.",
        ),
        Tools.ROUTE_FLIP_COIN to ToolHelp(
            "Tap the flip control to generate a coin outcome.",
            "Use a flip to decide who starts a friendly game.",
            "A run of heads or tails is possible; previous flips do not determine the next result.",
        ),
        Tools.ROUTE_SPIN_BOTTLE to ToolHelp(
            "Place the phone where everyone can see it, then spin and wait for the result.",
            "Use the final bottle direction to choose a turn.",
            "The direction is virtual and random, not a compass bearing or physical measurement.",
        ),
        Tools.ROUTE_RPS to ToolHelp(
            "Choose rock, paper, or scissors to play a round against the app.",
            "Rock beats scissors; scissors beat paper; paper beats rock.",
            "Matching choices are a draw. Restoring saved inputs does not play a new round.",
        ),
        Tools.ROUTE_DICE to ToolHelp(
            "Select the available dice settings and roll to generate a result.",
            "Roll a die to choose how many spaces to move in a board game.",
            "Rolls can repeat. Virtual dice do not predict physical dice.",
        ),
        Tools.ROUTE_CHESS_SCOREBOARD to ToolHelp(
            "Use the player controls to manage each side during a chess match.",
            "Set up both players before starting a friendly match.",
            "This tool does not validate chess moves or determine checkmate.",
        ),
        Tools.ROUTE_ONE_OF_TWO to ToolHelp(
            "Enter two alternatives, then ask the tool to choose one.",
            "Tea or coffee? Enter one in each field.",
            "This is a random choice, not a comparison of the alternatives.",
        ),
        Tools.ROUTE_RATIOS to ToolHelp(
            "Choose a ratio operation and enter the values requested by that mode.",
            "The ratio 6:9 simplifies to 2:3.",
            "The order matters: 2:3 and 3:2 describe different proportions.",
        ),
        Tools.ROUTE_DECIMAL_TO_FRACTION to ToolHelp(
            "Enter a decimal to see a fractional representation.",
            "0.75 equals 3/4.",
            "A finite decimal such as 0.333 is not the same as an infinitely repeating third.",
        ),
        Tools.ROUTE_TEXT_TOOLS to ToolHelp(
            "Paste or type text, then choose a case conversion or inspect the counts.",
            "Try Hello world to inspect words and characters.",
            "Whitespace and punctuation can affect counts; review transformed text before replacing an original.",
        ),
        Tools.ROUTE_TIMER_STOPWATCH to ToolHelp(
            "Choose Timer for a countdown or Stopwatch for elapsed time; use the on-screen start and pause controls. A Timer / Stopwatch home-screen widget provides a snapshot and controls. Find setup instructions in Settings → Home-screen widgets.",
            "Set a short countdown to check the behavior before using a longer one.",
            "Background alerts depend on device permissions and power management. Do not rely on it for safety-critical timing.",
        ),
        Tools.ROUTE_BMI to ToolHelp(
            "Choose units and enter height and weight to calculate body mass index.",
            "At 70 kg and 1.75 m, BMI is approximately 22.9.",
            "BMI is a screening estimate, not a diagnosis; standard adult categories may not apply to children or pregnancy.",
        ),
        Tools.ROUTE_TIP to ToolHelp(
            "Enter the bill, tip percentage, and number of people sharing it.",
            "A bill of 100 with a 10% tip split between 2 people is 55 each.",
            "Use one currency throughout. Rounded shares may need a small adjustment to match the total.",
        ),
        Tools.ROUTE_PASSWORD_GENERATOR to ToolHelp(
            "Choose a length and character groups, then generate a password.",
            "Try a length of 20 with the character types accepted by your service.",
            "Use a unique password for each account. Copying exposes it to the clipboard; store it in a trusted password manager.",
        ),
        Tools.ROUTE_DATE_CALCULATOR to ToolHelp(
            "Choose a date, add or subtract, then select the amount and calendar unit.",
            "Adding 1 week to 1 January gives 8 January.",
            "Calendar months and years are not fixed numbers of days; month-end dates may adjust.",
        ),
        Tools.ROUTE_DISCOUNT to ToolHelp(
            "Enter the original price and each discount in the order it applies.",
            "Two discounts of 10% on 100 leave 81, not 80.",
            "Stacked discounts apply to the remaining amount. Taxes and extra charges are not automatically included.",
        ),
        Tools.ROUTE_GST to ToolHelp(
            "Choose whether to add tax to a base amount or remove included tax.",
            "Adding 18% to 100 gives 118; removing included 18% from 118 gives 100.",
            "Enter the applicable rate yourself. The tool does not determine tax rules or eligibility.",
        ),
        Tools.ROUTE_DAYS_UNTIL to ToolHelp(
            "Choose an event date to see the calendar days until or since it.",
            "Pick tomorrow to see a one-day countdown.",
            "This compares calendar dates, not exact hours or time zones for an event.",
        ),
        Tools.ROUTE_TEAM_PICKER to ToolHelp(
            "Enter the names and choose whether to pick a name or divide them into teams.",
            "Enter four names and choose two teams.",
            "Review duplicate names and team sizes; random groups do not balance ability automatically.",
        ),
        Tools.ROUTE_FUEL_COST to ToolHelp(
            "Enter distance, fuel efficiency, and price using the selected units.",
            "At 15 km/L, a 150 km trip needs about 10 litres.",
            "An estimate only: traffic, terrain, vehicle load, tolls, and parking can change the actual cost.",
        ),
        Tools.ROUTE_MORSE_BINARY to ToolHelp(
            "Choose Morse or binary and the translation direction, then enter text or encoded symbols.",
            "SOS in Morse is ... --- ...; A in binary is 01000001.",
            "Morse has a limited character set. Binary text uses UTF-8 bytes; keep byte groups complete when decoding.",
        ),
        Tools.ROUTE_HASH_GENERATOR to ToolHelp(
            "Choose an algorithm and enter the exact text whose digest you need.",
            "Compare hello with Hello: their digests differ.",
            "A hash is not encryption and cannot be decoded. MD5 and SHA-1 are unsuitable for security-sensitive verification.",
        ),
        Tools.ROUTE_TIC_TAC_TOE to ToolHelp(
            "Two players alternate placing X and O. Capture a snapshot to keep a board position.",
            "Three matching symbols in a row, column, or diagonal win.",
            "Restoring a position does not award a win or change lifetime totals; replacing a live board requires confirmation.",
        ),
        Tools.ROUTE_QR_GENERATOR to ToolHelp(
            "Enter text or a complete URL to create a QR code, then check it with a scanner.",
            "Try https://example.com as a sample URL.",
            "Anyone who scans can read the encoded content. A QR code does not make a link trustworthy or keep text secret.",
        ),
        Tools.ROUTE_BUBBLE_LEVEL to ToolHelp(
            "Place the device on a stable surface and use the displayed tilt to check alignment.",
            "Compare the reading on a desk with the phone gently tilted.",
            "Accuracy depends on sensors and the device case. Do not use as a precision construction instrument.",
        ),
        Tools.ROUTE_COMPASS to ToolHelp(
            "Hold the device steady and keep it away from magnets and metal while reading the heading.",
            "Turn slowly and watch the heading change.",
            "Shows magnetic direction, not guaranteed true north. Missing sensors or interference can make readings unavailable or inaccurate.",
        ),
        Tools.ROUTE_FLASHLIGHT to ToolHelp(
            "Use the torch control to turn the camera flash on or off.",
            "Turn it on briefly to illuminate a nearby object.",
            "Requires compatible flash hardware and permissions. Avoid shining into eyes; extended use consumes battery and may warm the phone.",
        ),
        Tools.ROUTE_SCREEN_RULER to ToolHelp(
            "Align a small object with the ruler marks and read the selected units.",
            "Check the displayed scale against a known physical ruler before measuring.",
            "Screen density and calibration affect accuracy; do not rely on this for precision or safety-critical measurements.",
        ),
    )

    fun localizedForRoute(context: android.content.Context, route: String?): ToolHelp? = localizedToolHelp(context, route)

    fun forRoute(route: String?): ToolHelp? = all[route]
}
