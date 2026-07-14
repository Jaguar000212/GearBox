<img src="docs/icon.svg" width="96" height="96" alt="GearBox icon" align="left" />

# GearBox

GearBox is the Kotlin + Jetpack Compose rewrite of the original Java
[Tool-Kit](https://github.com/Jaguar000212/Tool-Kit) Android app — a suite of handy mini-tools.

<br clear="left" />

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
| BMI Calculator | Body mass index and weight category, in metric (cm/kg) or imperial (ft+in/lbs). |
| Tip Calculator | Bill + tip % split across any number of people. |
| Date Calculator | Add or subtract days/weeks/months/years from a date. |

#### Converters

| Tool | Description |
| --- | --- |
| Base Converter | Convert between binary, octal, decimal and hexadecimal. |
| Unit Converters | Length, weight, volume and temperature (°C/°F/K) conversions. |
| Number to Words | Numeric value → English words, in International or Indian (Lakh/Crore) numbering. |
| Number to Roman | Numeric value → Roman numerals (1–3999). |
| Decimal to Fraction Converter | Decimal → simplified fraction, with a mixed-number form and repeating-decimal recognition. |

#### Games & Randomizers

| Tool | Description |
| --- | --- |
| Random Number Generator | Random whole number within a min/max range, including negative bounds. |
| Tambola Numbers | Call random, non-repeating numbers 1–90 for Tambola/Housie; game state persists across restarts. |
| Flip Coin | Heads or tails. |
| Spin the Bottle | Animated bottle spin to a random direction. |
| Rock Paper Scissors | Play against the app; win/loss/draw tally persists across restarts. |
| Dice Roll | Roll 1–6 dice at once, with total. |
| 1 of 2 | Randomly choose between two user-entered options. |

#### Utilities

| Tool | Description |
| --- | --- |
| Counter | Increment/decrement/reset counter with a configurable step (+1/+5/+10) and haptic feedback; persists across restarts. |
| Scoreboard | Track scores for any number of players/teams, persisted across app restarts. |
| Chess Scoreboard | Win/draw/loss tracking with standard chess scoring (1 / ½ / 0) and an undo for the last result; persisted across restarts. |
| Color Picker | Pick a color via RGB sliders or a HEX input; view HEX, RGB and HSL; save recent colors (persisted across restarts). |
| Text Tools | Word/character counts, case conversion (upper/lower/title/camel), and palindrome checking. |
| Timer / Stopwatch | Stopwatch with laps, or a countdown timer with a sound + vibration alert on completion; keeps ticking (wall-clock correct) even across restarts. |
| Password Generator | Cryptographically random password (`SecureRandom`) guaranteeing at least one character from each enabled class, with a strength meter and a sensitive-marked clipboard copy. |

## Features

- **Jetpack Compose** UI with Material 3 and dynamic colour (Android 12+).
- **Navigation Compose** single-activity architecture. Each tool is a `Tool` entry that owns its
  own screen composable, so the nav graph is a one-line loop over the tool registry rather than
  a `composable(...)` call per tool.
- **Home screen**: tools grouped by category, with a search field and category filter chips;
  tap a tool to open it, long-press to see its description.
- **Favourites** persisted with `SharedPreferences` — star a tool to pin it to the Favorites tab.
- **State that survives more than rotation**: `rememberSaveable` alone only survives a
  configuration change, not back-navigation or the process being killed. Tools where losing
  progress would actually hurt (Tambola's called numbers, Counter, RPS's tally, the Timer/
  Stopwatch, Color Picker's saved palette, both scoreboards) persist through a small generic
  `SimplePrefsStore` key-value layer backed by `SharedPreferences`.
- **Consistent result UX**: every tool with a result offers Copy (and Share, where sending it
  elsewhere makes sense) via a shared `ResultCard`; numeric inputs validate on blur instead of
  mid-keystroke via a shared `DecimalField`.
- **Settings / About** screen.

## Architecture

```
com.jaguar.gearbox
├── MainActivity.kt          # Nav host (one loop over Tools.all), bottom navigation, snackbar
├── data/
│   ├── Tool.kt              # Tool model (name/icon/route/category + its own @Composable content)
│   ├── Tools.kt             # Tool registry + routes (was ToolList.java)
│   ├── FavoritesStore.kt    # SharedPreferences-backed favourites
│   ├── ScoreboardStore.kt / ChessMatchStore.kt   # Per-feature persistence
│   └── SimplePrefsStore.kt  # Generic key-value persistence for simpler per-tool state
├── logic/                   # Pure, unit-tested functions extracted out of UI files
│                             # (number-to-words, BMI, ratios, decimal↔fraction, RGB↔HSL, ...)
└── ui/
    ├── components/           # ToolCard, ToolScaffold, ResultCard, DecimalField, ValueRow, ...
    ├── screens/              # Home (search + grouping), Favorites, Settings
    └── tools/                # One composable per tool + clipboard/share helpers
```

## Testing

Pure logic (number-to-words, BMI, ratio simplification, decimal↔fraction, RGB↔HSL, random-range
generation) lives in `logic/` and is covered by JVM unit tests under `app/src/test`:

```bash
./gradlew testDebugUnitTest
```

## Build

Open in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```

A release build is minified and resource-shrunk (`isMinifyEnabled` / `isShrinkResources`):

```bash
./gradlew assembleRelease
```

Locally this produces an **unsigned** release APK. Signing only kicks in when `KEYSTORE_PATH`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` are set in the environment — see
[Releasing](#releasing) below.

## Releasing

Pushing a tag matching `v*` triggers [`.github/workflows/release.yml`](.github/workflows/release.yml),
which builds a signed, minified release APK and publishes it to a new GitHub Release with the
APK attached:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow reads the signing keystore and passwords from repository secrets
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) — nothing is committed to
the repo itself.
