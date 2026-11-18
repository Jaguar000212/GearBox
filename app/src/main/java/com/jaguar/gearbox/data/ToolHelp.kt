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
        Tools.ROUTE_IMAGE_STUDIO to ToolHelp(
            "Choose a local image, adjust crop, dimensions or compression, then explicitly export a new PNG, JPEG or WebP copy. Processing stays on the device; the source image is not overwritten.",
            "Choose a non-private test image, try a smaller size and a crop, then export a new copy and inspect its dimensions and appearance.",
            "Decoded images and outputs are limited to a 2048-pixel edge and 4 megapixels, so large originals are reduced. Re-encoding does not copy source metadata, but visible identifying content remains. This phase does not include markup, blur, mosaic, collage or enhancement.",
        ),
        Tools.ROUTE_JSON_FORMATTER to ToolHelp(
            "Enter JSON, choose validation, pretty-printing or minification, then explicitly run the operation. Processing stays local; automatic history and result saving are off by default.",
            "Try a small object with a name string and an enabled boolean, then explicitly format or validate it.",
            "JSON only, not CSS, XML or HTML. Duplicate object keys are rejected rather than silently overwritten. Input and nesting limits apply. Valid syntax does not prove data is safe or matches a schema; review before copying or sharing private content.",
        ),
        Tools.ROUTE_DEADLINE_PLANNER to ToolHelp(
            "Enter a target date, an as-of date, and ordered phase names with calendar-day durations. Tap Plan to schedule backward: the last phase ends on the target date and each earlier phase ends the day before the next begins. Both start and end dates count toward duration.",
            "Load a 10 January 2027 target with Prepare for 2 days and Review for 3 days, then tap Plan. Prepare runs 6 to 7 January and Review runs 8 to 10 January.",
            "Sequential calendar-day planning only, including weekends. No holiday calendar, parallel tasks, reminders or calendar events. The as-of date is a reference, not automatic progress tracking or a guarantee that the plan is feasible.",
        ),
        Tools.ROUTE_ASPECT_RATIO to ToolHelp(
            "Enter source width and height to reduce their ratio. Choose a target width, height, percentage, or a box to fit or fill, then calculate dimensions.",
            "A 1920 by 1080 source has ratio 16:9. Resizing to width 1280 gives height 720.",
            "Dimension calculations only: no image is uploaded, resized or edited. Integer rounding can slightly change the ratio. Fit rounds down to stay inside the box; Fill rounds up to cover it. Padding and cropping assume centered placement.",
        ),
        Tools.ROUTE_DECISION_MATRIX to ToolHelp(
            "Enter 2 to 6 options and 1 to 6 criteria. Set whole-number weights from 0 to 100, with at least one positive weight, and ratings from 0 to 5. Higher ratings always mean better. Tap Compare to rank weighted scores and show ties.",
            "Compare two generic options using Value and Comfort ratings. Equal weighted totals remain tied rather than choosing a winner.",
            "An advisory calculation based only on your inputs, not objective evidence or an AI judgment. For costs or risks, assign higher ratings to more desirable outcomes. Review labels before copying or sharing private information.",
        ),
        Tools.ROUTE_TIME_ZONE_CONVERTER to ToolHelp(
            "Enter a date and local time, choose source and destination IANA zones, then convert. A nonexistent daylight-saving time is rejected. For a repeated time, explicitly select the intended source offset.",
            "Load 15 January 2026 at 12:00 UTC and Asia/Kolkata, then convert to 17:30 on the same day.",
            "Uses time-zone rules installed on your device, which may become outdated. No network lookup or location permission is used. Check important future travel or meeting times against current official information.",
        ),
        Tools.ROUTE_TEXT_DIFF to ToolHelp(
            "Enter the original and revised text, then tap Compare to inspect added, removed and unchanged lines. Comparison runs locally; automatic history and result saving are off by default.",
            "Load Hello followed by world, and Hello followed by GearBox, then tap Compare to inspect the changed second line.",
            "Each text is limited to 16000 characters and 200 lines. Case and whitespace matter; CRLF line endings are normalized and a trailing newline produces an empty line. This is not a semantic comparison or proof of equivalence. Review before copying or sharing private text.",
        ),
        Tools.ROUTE_SCIENTIFIC_CALCULATOR to ToolHelp(
            "Enter an expression with explicit multiplication, choose degrees or radians for trigonometry, then calculate. Use parentheses for grouping and functions such as sqrt(16), sin(30), ln(e) or log(100).",
            "Load sqrt(16)+2^3, then calculate 12. The example does not calculate until you request it.",
            "Finite-precision real-number arithmetic only, not symbolic algebra or complex numbers. Multiplication must be explicit, such as 2*pi. Powers associate right to left; -2^2 means -(2^2).",
        ),
        Tools.ROUTE_BUSINESS_DAY_PLANNER to ToolHelp(
            "Choose Add/subtract or Count between dates. Select your weekend days and enter excluded dates, then calculate. The start date is excluded; counting includes an eligible end date and reverses sign for earlier end dates.",
            "Five business days after Monday 16 November 2026 is Monday 23 November, excluding Saturday and Sunday with no other excluded dates.",
            "Excluded dates are entered manually, not an official holiday calendar. This is a planning estimate, not a legal or contractual deadline calculation. Dates use no time zone.",
        ),
        Tools.ROUTE_BILL_SPLITTER to ToolHelp(
            "For a simple bill, enter bill and tax amounts, tip percentage and diners; tip uses the original bill before tax. For shared expenses, enter participants, each amount, who paid and who benefited, then calculate balances and suggested settlements. For example, if A pays 30 for A and B equally, B owes A 15.",
            "Simple bill: 100 plus a 10 percent tip split three ways gives 36.67, 36.67 and 36.66. Shared expenses: if A pays 30 for A and B equally, B owes A 15.",
            "Draft calculations use two decimal currency places. Settlement suggestions do not send money, verify payments or guarantee the fewest transfers. Use one currency and review participant labels and amounts before sharing.",
        ),
        Tools.ROUTE_COLOR_BLEND to ToolHelp(
            "Enter two opaque HEX colors, choose the second color percentage from 0 to 100, then tap Blend.",
            "Equal parts red #FF0000 and blue #0000FF give #800080 with encoded sRGB interpolation.",
            "This interpolates encoded sRGB channels, not linear-light values or physical paint. Alpha transparency is not supported.",
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
        Tools.ROUTE_FOCUS_TIMER to ToolHelp(
            "Choose Pomodoro or intervals, configure work and rest durations, then explicitly start the session.",
            "Try a short work and rest session before relying on a longer schedule.",
            "Background alerts depend on device permissions and power management. Live sessions are not imported from History or Workbench. Do not use for safety-critical timing.",
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
        Tools.ROUTE_BARCODE_SCANNER to ToolHelp("Start explicitly, grant camera permission then Start again. Review untrusted decoded text before any action.", "Scan a non-private QR code without opening it.", "Local bounded decoding; no automatic navigation, saved results or network. Oversized payloads are rejected."),
        Tools.ROUTE_MAGNIFIER to ToolHelp("Explicitly start the back camera; grant permission then Start again. Freeze and optionally export a reviewed PNG copy.", "Magnify non-private printed text.", "Preview quality depends on hardware. Backgrounding clears the camera and frozen frame, except a reviewed export frame while choosing its destination."),
        Tools.ROUTE_SENSOR_CHECKER to ToolHelp("Choose one sensor and explicitly Start. Changing selection or backgrounding stops readings.", "Tilt the device while observing accelerometer axes.", "Fixed permission-free inventory; readings are not calibration or hardware certification. No stored samples."),
        Tools.ROUTE_BAROMETER to ToolHelp(
            "Read pressure and enter an explicit sea-level reference between 800 and 1100 hPa.",
            "Equal measured and reference pressures give zero estimated altitude.",
            "Standard-atmosphere approximation, not GPS or true elevation. No weather lookup or safety-critical use.",
        ),
        Tools.ROUTE_BATTERY_INFO to ToolHelp(
            "Open to view supported Android battery properties while this screen is active.",
            "Connect a charger and wait for the reported power source to update.",
            "System health is not a capacity percentage. Device-reported values only, no remaining-time estimates or saved measurements.",
        ),
        Tools.ROUTE_PITCH_TUNER to ToolHelp(
            "Tap Start, grant microphone permission, then tap Start again. Play one steady note.",
            "A steady 440 Hz tone corresponds to A4 near zero cents.",
            "Single sustained tones approximately 65–1000 Hz, A4=440. Not calibrated or polyphonic; no audio saved.",
        ),
        Tools.ROUTE_SOUND_METER to ToolHelp(
            "Tap Start, grant microphone permission, then tap Start again. Stop or leave to end capture.",
            "Digital silence shows negative infinity dBFS.",
            "Relative digital levels only, not calibrated sound pressure. No saved audio or health assessment.",
        ),
        Tools.ROUTE_LIGHT_METER to ToolHelp(
            "Open Light Meter and leave the device light sensor unobstructed. Read ambient illuminance in lux while this screen is active.",
            "Compare a shaded room with a spot nearer a window, without covering the sensor.",
            "Requires an actual ambient light sensor. Not a calibrated instrument; no camera fallback or saved measurement history.",
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
