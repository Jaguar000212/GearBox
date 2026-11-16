# GearBox refresh testing guide

This guide grows with each implementation phase. Automated CI builds app/test APKs, runs JVM tests and lint; it does **not** execute Android instrumentation or hands-on device tests. Those remain with the owner.

## Safe installation

- Use the debug APK from the successful CI run for the commit being tested. The artifact also includes the instrumentation APK and SHA-256 checksums.
- The app is a development build (`com.jaguar.gearbox.beta`), not a public release.
- If an existing installation rejects an update because of signing or version differences, **do not uninstall it or clear its data**. Use another test device/profile or report the error.
- Record phone model, Android version, build/commit, font/display scaling and steps when reporting an issue. Avoid sharing private inputs in screenshots.

## Phase 1: navigation, presentation and local memory

### Home and Favorites
- Confirm all 41 tools appear once across seven categories. Search by tool name and task phrases.
- Select Money, search Counter, then Search all tools: query remains, category scope clears.
- Add several favorites, reorder using drag and accessible arrows, remove then Undo. Original position returns without duplicates.
- Back exits Favorites Edit before navigating away. Dismiss Choose tools without applying and check nothing changed.
- Switch tools; Back returns along the expected navigation path. Check shortcuts and fresh Home/Favorites launch preference separately.

### Memory and privacy
- Fresh history is off. Enable it with a limit of 2, complete three distinct calculations and verify only the newest two remain.
- Set limit 1 and 100; reject 0, 101 and nonnumbers. Lowering the limit trims oldest history, not explicit Workbench saves.
- Pause history: no new entries, existing entries remain readable. Clear one tool and clear all, cancel each confirmation once, then confirm.
- Clear while a live calculation is waiting to save: that pending result must not reappear afterward.
- Toggle Remember modes and Remember last result independently. Restart and compare behavior. Rotate while editing: older disk data must not overwrite saved-instance edits.
- Copy/share a record, restore inputs, and verify mode/units match. Hash/QR imports must clear stale output and require Generate before sharing.
- Passwords never appear in history, Workbench or presets. Password Copy keeps Android sensitive-clipboard treatment.
- Try input beyond documented history size bounds: calculation may work, but oversized history must not silently truncate inputs.

### Presets, Workbench and focus views
- Save a configuration preset, change current inputs, then apply it: config changes without replacing unrelated text/names.
- Keep a named result, restart, copy/share it and restore its inputs. Cancel deletion once, then delete.
- A saved Color view preset changes the view, not the selected color.
- Text → QR/Hash handoff uses the chosen result, does not read the clipboard or automatically share.
- Timer/Counter focus views show current values and have an obvious exit. Closing focus must not reset or stop the tool.
- Clock Options in Settings and Timer must show the same preference. Test denied notification permission, dismissed notifications and running/paused clock recovery.

## Phase 2: remaining history, retention and recents

### Recent tools
- Show recent tools and Record recent tools are independent. Hiding the strip must not stop recording; pausing recording must not hide existing routes.
- Clear recent history removes only recent navigation routes, not tool history, favorites, presets or saved results.
- Restart after each change. Existing recent routes migrate once, stay ordered, and never exceed five.
- Opening a notification/shortcut routes to the right tool and records the canonical tool, not an internal reset route.

### Game-session retention
- Remember game sessions and participants defaults on to preserve existing progress. Turn it off, make changes, reopen tools: automatic restoration/saving is paused, while the currently open screen is not reset.
- Turn it back on: the prior saved session can return. Use Clear saved game sessions to remove dormant progress, including while retention is off; cancel once before confirming.
- This control excludes active clocks, counters/widgets, favorites and manually saved palettes/history/workbench items.
- Legacy game sessions still use the app’s older preference storage and Android backup rules. New history, recents and their settings are excluded from cloud backup/device transfer.

### Clock and game snapshots
- Capture snapshots explicitly. Wait without tapping Capture: clocks, sensors and ongoing games must not flood history.
- Capture a clock snapshot and save a duration/mode preset. Import never starts a clock or replaces a running/paused clock silently.
- Check cold/warm countdown, stopwatch and reset notification targets with Remember modes both on and off.
- Capture Scoreboard, Chess, Tambola and Tic-tac-toe snapshots; change current progress; restore and cancel the replacement prompt first.
- Confirm restore reproduces valid inputs/progress without drawing a number, making a move, increasing lifetime game statistics or retaining invalid Undo state.
- Malformed/unreachable game states must be rejected without touching current progress. Test read-only recovery for damaged scoreboard storage.
- Focus views for scoreboards must remain readable with long names, large scores and large text.

### Device snapshots
- Compass/Level Capture is unavailable until a reliable reading is available. History includes accuracy qualifiers; movement alone adds nothing.
- Flashlight history captures confirmed status only. Restoring history or Workbench must never switch hardware on.
- Ruler captures scale settings, not a claimed physical measurement. Unit presets may restore units, not a physical sensor reading.
- Device snapshots can be copied/shared, but cannot replay physical readings through Restore.

### Cross-cutting checks after every phase
- Small phone, landscape, tablet, large text and TalkBack: no hidden primary action, clipped settings or inaccessible dismissal.
- Repeat taps, Back, dialog Cancel, rotation, background/foreground and process recreation.
- Check upgrade preserves favorites and existing authoritative clock/counter/game data unless a deliberate clear action was confirmed.
- Report untested checks as untested, not passed.

## Results log

| Phase | Automated result | Owner device result |
| --- | --- | --- |
| 1 | CI145 passed for `dcce25a213f65b76c80fa9a68d98fa44a35bf41c`; APK/checksums verified | Initial visual feedback: clean. Full checklist not yet run. |
| 2 | Pending current phase CI | Deferred for combined testing |
