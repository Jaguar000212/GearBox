# GearBox

GearBox is the Kotlin + Jetpack Compose rewrite of the original Java
[Tool-Kit](https://github.com/Jaguar000212/Tool-Kit) Android app — a suite of handy mini-tools.

## Tools

28 tools, grouped into four categories on the home screen.

#### Calculators

| Tool | Description |
| --- | --- |
| Average Calculator | Arithmetic, geometric and harmonic means plus sum, count, largest and smallest. |
| Interest Calculator | Monthly installment, total repayable, total interest and interest percentage. |
| Age Calculator | Age (years, months, days) between two dates. |
| Geometry Calculator | Area/perimeter for circles, rectangles and triangles. |
| Percentage Calculator | X% of Y, what % X is of Y, and percentage change. |
| Ratios | Simplify a ratio to lowest whole-number terms. |
| BMI Calculator | Body mass index and weight category from height and weight. |
| Tip Calculator | Bill + tip % split across any number of people. |
| Date Calculator | Add or subtract days/weeks/months/years from a date. |

#### Converters

| Tool | Description |
| --- | --- |
| Base Converter | Convert between binary, octal, decimal and hexadecimal. |
| Unit Converters | Length, weight and volume conversions. |
| Number to Words | Numeric value → English words. |
| Number to Roman | Numeric value → Roman numerals (1–3999). |
| Decimal to Fraction Converter | Decimal → simplified fraction. |

#### Games & Randomizers

| Tool | Description |
| --- | --- |
| Random Number Generator | Random whole number within a min/max range. |
| Tambola Numbers | Call random, non-repeating numbers 1–90 for Tambola/Housie. |
| Flip Coin | Heads or tails. |
| Spin the Bottle | Animated bottle spin to a random direction. |
| Rock Paper Scissors | Play against the app; running win/loss/draw tally. |
| Dice Roll | Roll 1–6 dice at once, with total. |
| 1 of 2 | Randomly choose between two user-entered options. |

#### Utilities

| Tool | Description |
| --- | --- |
| Counter | A simple increment / decrement / reset counter. |
| Scoreboard | Track scores for any number of players/teams, persisted across app restarts. |
| Chess Scoreboard | Win/draw/loss tracking with standard chess scoring (1 / ½ / 0), persisted across app restarts. |
| Color Picker | Pick a color via RGB sliders or a HEX input; view HEX, RGB and HSL; save recent colors. |
| Text Tools | Word/character counts, case conversion (upper/lower/title/camel), and palindrome checking. |
| Timer / Stopwatch | Stopwatch with laps, or a countdown timer from a set duration. |
| Password Generator | Random password with configurable length and character types, plus a strength meter. |

## Features

- **Jetpack Compose** UI with Material 3 and dynamic colour (Android 12+).
- **Navigation Compose** single-activity architecture (one composable per tool instead of one
  activity per tool).
- **Home screen**: tools grouped by category, with a search field and category filter chips;
  tap a tool to open it, long-press to see its description.
- **Favourites** persisted with `SharedPreferences` — star a tool to pin it to the Favorites tab.
- **Settings / About** screen.

## Architecture

```
com.jaguar.gearbox
├── MainActivity.kt          # Nav host, bottom navigation, snackbar
├── data/
│   ├── Tool.kt              # Tool model + ToolCategory enum
│   ├── Tools.kt             # Tool registry + routes (was ToolList.java)
│   └── FavoritesStore.kt    # SharedPreferences-backed favourites
└── ui/
    ├── components/          # ToolCard, ToolScaffold
    ├── screens/             # Home (search + grouping), Favorites, Settings
    └── tools/               # One composable per tool + clipboard/share helpers
```

## Build

Open in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```
