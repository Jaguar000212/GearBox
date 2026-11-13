<div align='center'>
  <img src="docs/icon.svg" width="96" height="96" alt="GearBox icon" />
  <h1>GearBox</h1>
  <img src="https://img.shields.io/github/v/release/Jaguar000212/GearBox" alt="release badge" />
</div>

GearBox is the Kotlin + Jetpack Compose rewrite of the original Java
[Tool-Kit](https://github.com/Jaguar000212/Tool-Kit) Android app — a suite of handy mini-tools.

<br clear="left" />

## Tools

41 tools, grouped into five categories on the home screen. This inventory describes `main`;
a published release may contain fewer tools.

#### Calculators (13)

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
| Discount Calculator | Apply stacked percentage discounts to a price. |
| GST / Tax Calculator | Add tax to an amount or extract included tax. |
| Days Until | Count days until or since a selected date. |
| Fuel Cost Calculator | Estimate fuel consumption and trip cost. |

#### Converters (6)

| Tool | Description |
| --- | --- |
| Base Converter | Convert between binary, octal, decimal and hexadecimal. |
| Unit Converters | Length, weight, volume and temperature (°C/°F/K) conversions. |
| Number to Words | Numeric value → English words, in International or Indian (Lakh/Crore) numbering. |
| Number to Roman | Numeric value → Roman numerals (1–3999). |
| Decimal to Fraction Converter | Decimal → simplified fraction, with a mixed-number form and repeating-decimal recognition. |
| Morse / Binary Translator | Translate text to and from Morse code or binary. |

#### Games & Randomizers (9)

| Tool | Description |
| --- | --- |
| Random Number Generator | Random whole number within a min/max range, including negative bounds. |
| Tambola Numbers | Call random, non-repeating numbers 1–90 for Tambola/Housie; game state persists across restarts. |
| Flip Coin | Heads or tails. |
| Spin the Bottle | Animated bottle spin to a random direction. |
| Rock Paper Scissors | Play against the app; win/loss/draw tally persists across restarts. |
| Dice Roll | Roll 1–6 dice at once, with total. |
| 1 of 2 | Randomly choose between two user-entered options. |
| Team / Name Picker | Pick a name or split a list into teams. |
| Tic-tac-toe | Play a local two-player game. |

#### Utilities (9)

| Tool | Description |
| --- | --- |
| Counter | Increment/decrement/reset counter with a configurable step (+1/+5/+10) and haptic feedback; persists across restarts. |
| Scoreboard | Track scores for any number of players/teams, persisted across app restarts. |
| Chess Scoreboard | Win/draw/loss tracking with standard chess scoring (1 / ½ / 0) and an undo for the last result; persisted across restarts. |
| Color Picker | Pick a color via RGB sliders or a HEX input; view HEX, RGB and HSL; save recent colors (persisted across restarts). |
| Text Tools | Word/character counts, case conversion (upper/lower/title/camel), and palindrome checking. |
| Timer / Stopwatch | Stopwatch with laps or a countdown timer. OS-scheduled completion notification with explicit permission guidance; timing remains subject to Android restrictions. |
| Password Generator | Cryptographically random password (`SecureRandom`) guaranteeing at least one character from each enabled class, with a strength meter and a sensitive-marked clipboard copy. |
| Hash Generator | Generate MD5, SHA-1, SHA-256 or SHA-512 hashes of text. |
| QR Code Generator | Generate and share a QR image from text or a URL. |

#### Device (4)

| Tool | Description |
| --- | --- |
| Bubble Level | Use device sensors as a spirit level. |
| Compass | Show magnetic heading using available sensors. |
| Flashlight | Toggle the camera flash as a torch on supported devices. |
| Screen Ruler | Measure against an on-screen cm/inch scale; accuracy depends on device calibration. |

## Features

- **Jetpack Compose** UI with Material 3 and dynamic colour (Android 12+).
- **Navigation Compose** single-activity architecture. Each tool is a `Tool` entry that owns its
  own screen composable, so the nav graph is a one-line loop over the tool registry rather than
  a `composable(...)` call per tool.
- **Home screen**: tools grouped by category, with a search field and category filter chips;
  tap a tool to open it, long-press to see its description.
- **Favourites** persisted with `SharedPreferences`, with ordering and dynamic launcher shortcuts.
- **Recent tools** provide quick access to the five most recently used tools.
- **Home-screen widgets** for Counter and Timer / Stopwatch.
- **Persistent tool state**: selected tools store state beyond the lifetime of their navigation
  destination. `rememberSaveable` supports saved-state restoration, but it is not durable storage.
  Tools where losing progress would actually hurt (Tambola's called numbers, Counter, RPS's tally, the Timer/
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

The [CI workflow](.github/workflows/ci.yml) runs for pull requests and pushes to `main`.
It builds debug, debug-instrumentation and unsigned release APKs, runs JVM unit tests, and
runs debug Android lint. Reports are uploaded even when a check fails. It does not run an
emulator, sign a release, or publish an APK.

Run the same checks locally:

```bash
bash ./gradlew --continue :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

Pure logic (number-to-words, BMI, ratio simplification, decimal↔fraction, RGB↔HSL, random-range
generation) lives in `logic/` and is covered by JVM unit tests under `app/src/test`:

```bash
./gradlew testDebugUnitTest
```

On a connected Android 11+ device or emulator, run instrumentation separately:

```bash
bash ./gradlew :app:connectedDebugAndroidTest
```

Instrumentation includes package, saved-date/password state, and shared counter-store regressions.
It is not comprehensive device/UI coverage. See [Phase B correctness](docs/phase-b-correctness.md)
and [timer behavior and permissions](docs/timer-behavior.md) for the acceptance checklist.

## Build

Prerequisites: JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. Point Android Studio,
`ANDROID_HOME`, or an untracked `local.properties` at your SDK. The checked-in wrapper uses
Gradle 8.10.2; the first build needs network access to download Gradle and dependencies.
The app supports Android 11 (API 30) and newer.

Open in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```

A release build is minified and resource-shrunk (`isMinifyEnabled` / `isShrinkResources`):

```bash
./gradlew assembleRelease
```

Debug builds install as `com.jaguar.gearbox.beta` with a `-b` version suffix, alongside
production `com.jaguar.gearbox`.

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
