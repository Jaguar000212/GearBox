# Toolbox refresh: staged implementation

This branch is built on the exact source of draft launch-screen PR #55. It does not merge or release either change.

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

## Still to complete in later reviewable phases

- Per-tool history for ongoing games/scoreboards and explicit snapshots for device sensors (not continuous sensor logging).
- Broader examples, expandable help and compact selectors across remaining tools.
- Full adaptive input/result split layouts, reachability and result-action consistency.
- Settings controls for recent-tool visibility/collection and complete audit of legacy state retention.
- Broader compatible handoffs and preset entry points, optional active-clock Home shortcut.
- Device-level layout, TalkBack, font scaling, restore/cancel/navigation and migration QA.

## Verification

Source/whitespace checks were run. Local Gradle execution was attempted with the documented aggregate build/test/lint tasks but failed downloading Gradle 8.10.2 because the execution environment could not reach the network. This is not a compilation pass. CI and user device testing remain required. Instrumentation tests are added, not claimed run.
