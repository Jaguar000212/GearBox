# Toolbox refresh: staged implementation

Phase 1 (PR #56) and launch-screen PR #55 were merged after exact-head CI/review checks. Phase 2 builds on that same verified main tree. No release or tag has been created.

## Implemented foundation

- Seven task-oriented categories, compact Home search and category chooser.
- Favorites normal/Edit modes, searchable multi-select, accessible reorder, and removal Undo.
- Tool switcher, clearer local typography, password customization dialog, focused color/team/dice/counter layouts.
- Shared Clock options in Settings and the timer header; Timer and Counter focus displays.
- Local opt-in history (default off, 20 entries, configurable 1–100), separate remembered-mode and last-result controls.
- History copy/share/restore on supported calculators, converters, text/hash/QR, color/counter/team/dice and random games.
- Explicit named configuration presets and saved result Workbench (100 each), text-result handoff to QR/hash.

## Privacy and behavior

All new history, result, mode, preset and workbench files live in Android `noBackupFilesDir`, not cloud backup or device transfer. Password Generator is excluded from this storage. Explicitly sharing or copying is a user action. Each field/result is capped at 4096 characters and each record at 8192 total. Oversized records are rejected, not truncated. Lowering the history count removes oldest rolling entries; explicit saves never auto-evict.

History is opt-in. Turning a setting off pauses its use without deleting stored data; clear controls are explicit. Existing active clocks, game scores, favorites and manually saved palettes are not reset by these new retention toggles. Live calculations record after a one-second editing pause; button-generated games record completed outcomes. Restoring history restores inputs; QR/hash require explicit regeneration. Counter snapshots are restored only explicitly, never over newer widget state automatically.

## Phase 2: history coverage and retention

- History entry points now cover all 40 non-password tools. Clocks, ongoing games, scoreboards and device tools use deliberate snapshot capture rather than continuous logging.
- Restoring game snapshots validates state and confirms replacement. Hardware readings cannot be replayed; clock snapshots restore setup only, never start or replace active clocks.
- Timer presets respect notification/shortcut provenance and authoritative active/paused/finished clocks.
- Separate Show recent tools and Record recent tools choices, confirmed clear, and bounded route-only local migration.
- Separate game-session/participant retention, enabled by default to preserve existing data, with explicit clearing of dormant saved games. Legacy game storage still follows older Android backup behavior; new memory/recents/settings are excluded from backup.
- Living [testing guide](testing-guide.md), including deferred device checks and interruption/restore cases.

## Still to complete in later reviewable phases

- Broader examples, expandable help and compact selectors across remaining tools.
- Full adaptive input/result split layouts, reachability and result-action consistency.
- Broader compatible handoffs and preset entry points, optional active-clock Home shortcut.
- Device-level layout, TalkBack, font scaling, restore/cancel/navigation and migration QA.

## Verification

Local whitespace and Python packaging tests passed. Local Gradle execution was blocked downloading Gradle 8.10.2 by network restrictions. CI run 144 passed on commit `1309ed4cc1cd7f52d576ef049ea2d30fde60f210`: debug/release app builds, instrumentation APK compilation, JVM unit tests, debug/release lint, and version/packaging guardrails. Instrumentation tests were compiled, not executed on a device.

CI now uploads debug app and instrumentation APKs plus checksums for hands-on testing (seven-day retention). These are development artifacts, not a signed release. The debug app uses `com.jaguar.gearbox.beta`; an existing installation with a different signing key may reject an update. Do not uninstall or clear existing data to bypass that error. Use a separate test device/profile or report the conflict.
