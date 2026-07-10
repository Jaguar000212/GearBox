# GearBox

GearBox is the Kotlin + Jetpack Compose rewrite of the original Java
[Tool-Kit](https://github.com/Jaguar000212/Tool-Kit) Android app — a suite of handy mini-tools.

## Tools

| Tool | Description |
| --- | --- |
| Average Calculator | Arithmetic, geometric and harmonic means plus sum, count, largest and smallest. |
| Interest Calculator | Monthly installment, total repayable, total interest and interest percentage. |
| Counter | A simple increment / decrement / reset counter. |
| Age Calculator | Age (years, months, days) between two dates. |

## Features

- **Jetpack Compose** UI with Material 3 and dynamic colour (Android 12+).
- **Navigation Compose** single-activity architecture (one composable per tool instead of one
  activity per tool).
- **Home grid** of tools; tap to open, long-press to see the description.
- **Favourites** persisted with `SharedPreferences` — star a tool to pin it to the Favorites tab.
- **Settings / About** screen.

## Architecture

```
com.jaguar.gearbox
├── MainActivity.kt          # Nav host, bottom navigation, snackbar
├── data/
│   ├── Tool.kt              # Tool model
│   ├── Tools.kt             # Tool registry + routes (was ToolList.java)
│   └── FavoritesStore.kt    # SharedPreferences-backed favourites
└── ui/
    ├── components/          # ToolCard, ToolScaffold
    ├── screens/             # Home, Favorites, Settings
    └── tools/               # One composable per tool + clipboard/share helpers
```

## Build

Open in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```
