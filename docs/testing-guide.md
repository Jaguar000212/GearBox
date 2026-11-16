# GearBox refresh testing guide

This guide grows with each implementation phase. Automated CI builds app/test APKs, runs JVM tests and lint; it does **not** execute Android instrumentation or hands-on device tests. Those remain with the owner.

## Safe installation

For same-phone side-by-side testing, prefer the [isolated GearBox Preview build](preview-testing.md). It does not replace the earlier beta app; fresh preview data does not test upgrade migration.

- Use the debug APK from the successful CI run for the commit being tested. The artifact also includes the instrumentation APK and SHA-256 checksums.
- Regular debug uses `com.jaguar.gearbox.beta`; isolated preview uses `com.jaguar.gearbox.preview` and the GearBox Preview label. Neither is a public release.
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
- Clear while a live calculation is waiting to save: that pending result must not reappear afterward, including after rotation. Clear a completed result/mode, rotate without editing, and verify it stays cleared; a genuinely new input/mode may save again.
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
- Opening a notification/shortcut routes to the right tool and records the canonical tool, not an internal reset route. Rapidly open two tools and verify recents follows tap order; manual navigation/Back must cancel stale pending shortcut navigation.

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

## Phase 3: guidance, compact choices and adaptive layouts

- Every tool: More tool actions → Help & examples shows a relevant example, usage steps and limitations. Closing help changes no inputs or results; examples are explanatory, not silently executed.
- Top bar: History stays directly available; Help and Switch tool share the secondary menu. Check a narrow phone with long titles and large text.
- Base, Geometry, Morse/Binary, Date Unit, Hash, Text case, Percentage and Unit category: current mode is visible in the compact selector. Open, cancel/Back, reopen and choose; only choosing a different mode should trigger the tool's existing reset behavior.
- Date Add/Subtract remain directly visible. Two-choice tools should not require an extra menu.
- Percentage, Tip, Unit Converter and Text Tools: use one column on a phone; wide layouts split inputs and results at 720dp usable content width. Font scale above 1.3 returns to a single column.
- Rotate with partial inputs and an open selector; verify no input/result or selected mode changes. With the keyboard open, scroll to all fields and actions.
- Test long text results: Copy/Share stay accessible, output remains selectable, and selection does not trigger unintended actions.
- At 2× text scale and 240dp content width, Copy and Share stack without losing their labels. Clear remains secondary and independently enabled.
- TalkBack follows input controls before the result pane. Check all action labels and dismissal paths.

## Phase 4: workflow and accessibility

### Drafts and explicit reuse
- Enter non-sensitive input in tool A, switch to B, then switch to A: return to A's existing draft rather than a fresh duplicate. Back should return through a sensible stack without circular duplicates.
- Explicit History/Workbench/preset imports must take precedence over a retained draft and be applied exactly once.
- Password values must not survive switching away or saved-instance recreation; customization options may remain. Passwords must never enter history or handoffs.
- Presets affect configuration only; compatible result handoffs populate documented inputs only. Canceling any chooser leaves the source and destination unchanged.

### Favorites and Home
- Drag a favorite to an edge and hold the pointer still: scrolling continues until the edge/end or release. Verify order and visible item stay aligned.
- Rotate with Choose tools open: uncommitted selections persist; Cancel still discards them.
- With keyboard/TalkBack, return from chooser/Undo to an appropriate control. Accessible move actions and launcher shortcut ordering remain correct.
- Optional active-clock shortcut shows only meaningful running/paused clock state. Hiding it does not stop clocks. Opening it does not start/reset a clock.
- Device capability hints describe unavailable hardware, not false live accuracy. The tool remains accessible for explanation/retry; missing hardware must not crash Home.

### Motion and focus
- Reduce motion changes Coin/Bottle presentation without changing result generation or automatic recording. System animation scale must still be honored when the app preference is off.
- Full-screen focus views have obvious Exit and system Back dismissal. Closing them must preserve count, game scores, running/paused timers and sensor subscription lifecycle.
- Counter and clock essential controls must remain reachable at 2× font size, in landscape and with TalkBack. Repeated taps must not duplicate clock actions.
- Sensor focus must retain accuracy/unavailable warnings; a large number is not a claim of calibrated precision.

## Results log

| Phase | Automated result | Owner device result |
| --- | --- | --- |
| 1 | CI145 passed for `dcce25a213f65b76c80fa9a68d98fa44a35bf41c`; APK/checksums verified | Initial visual feedback: clean. Full checklist not yet run. |
| 2 | CI43904137433 passed for `d3f0c574db2dd058527b8518792fbec8aca320aa` (builds, JVM tests, lint; instrumentation compilation only) | Deferred for combined testing |
| 3 | CI43904474797 passed for `0e9c73e99b492cbeb51dad8eaa11359acbbb4610` | Deferred for combined testing |
| 4 | See exact-head [PR59 checks](https://github.com/Jaguar000212/GearBox/pull/59/checks); no device execution | Deferred for combined testing |

Phase 3 CI43904474797 passed at `0e9c73e99b492cbeb51dad8eaa11359acbbb4610`; no device checks have been claimed. For Phase 4 automated results, see the exact-head checks on [PR #59](https://github.com/Jaguar000212/GearBox/pull/59/checks).

Additional details: [focus views](PHASE4_FOCUS_TESTING.md), [switching/presets/handoffs](PHASE4_SWITCHING_TESTING.md).

## Corrective phase: result replay and actionable examples

- Enable Remember last result, complete Coin, Bottle, Dice, Random Number, One of Two and Rock Paper Scissors, leave and reopen each tool. The exact outcome must return without rerolling, replaying animation or increasing a tally. Bottle must point to the same angle; Dice must retain every die.
- Repeat using History Restore and Workbench Restore. New records carry validated typed outcomes. Older records without those fields restore inputs only, not a guessed result parsed from display text. Invalid outcomes must be rejected safely.
- Clear history or remembered modes while retaining a saved last result, then reopen/rotate the tool. Hydration must not repopulate the cleared storage. A genuinely new roll/edit must still record normally.
- QR and Hash restore text/setup only; Generate remains required. Verify the first Generate after hydration can be recorded. Counter, Clock, Team Picker, Tambola, Tic Tac Toe, Scoreboard, Chess Clock, Compass, Level, Flashlight and Ruler still deliberately do not auto-replay saved results. Password remains excluded.
- Help offers **Use example** in 23 deterministic calculator/converter/text tools. Opening Help, Cancel, Back and dismissal must not change inputs. Confirm replaces inputs exactly once, including when the destination already has a retained draft. QR/Hash examples must not auto-generate. No examples start games, clocks, random actions, hardware or password generation.
- Home, Favorites and search must call the loan repayment tool **Loan Calculator**, not Interest Calculator. Searching “interest” still finds it.
- Updated instrumentation selectors cover Password Customize/transient secret state, Geometry, Percentage, Unit Converter and Base Converter. Calculator-memory tests use isolated storage. New tests cover example confirmation and typed result restoration.

**Verification boundary:** Android instrumentation tests are authored and compiled in CI, not executed here. JVM tests/lint/build status must be checked for this exact corrective commit. Owner device execution, TalkBack, hardware, landscape and in-place migration remain unverified. A fresh Preview installation does not exercise upgrade migration.

**Known privacy limitation pending the next phase:** automatic history/result saving can retain private text entered into otherwise ordinary tools, including Text, QR and Hash. New memory files are app-private and excluded from Android backup by storage location, but are not an encrypted vault or automatic secret detector. Existing legacy game/settings backup behavior and Android saved-instance drafts are separate.

## Privacy follow-up checklist

- On both fresh and existing installations, Text, QR and Hash automatic history/result saving must start OFF independently of global history/results settings. Enabling one tool must not enable the others. Password remains blocked.
- Existing saved entries must remain available for review and explicit deletion when recording is disabled. Disabling a toggle must not silently erase history, remembered results or manual Workbench items.
- Disable a tool during a pending recording, then re-enable: the old queued write must not appear. Repeat with rotation/navigation and separate store instances. New deliberate input/results after enabling should record normally.
- Clear a tool's history/remembered result with confirmation. Cancel must leave storage unchanged; successful clear must not delete another tool or manual saved items. Check counts and explanatory text.
- Manual save of potentially private Text/QR/Hash data must disclose what is retained and that automatic-saving controls do not prevent deliberate Workbench saves. Review before confirming; Cancel creates nothing.
- Copy from these tools/history/Workbench must set Android's sensitive clipboard hint. This suppresses supported system previews, not clipboard access or persistence on every Android version.
- Share must first present a cancellable review of the exact payload. Cancel/Back sends nothing. Confirm opens the chooser for that reviewed payload only; editing/generating a new result must not substitute an unreviewed payload. QR file generation/sharing must wait for confirmation and still reject stale render state.
- No privacy text should call app-private local files an encrypted vault, promise secret detection, or imply auto-saving settings disable Android task-state drafts. Legacy preferences/backups remain separate.

Status: implementation and regression tests are included in the privacy follow-up. Do not treat this checklist as a passed result; exact-head CI and owner device checks are required. Editable input selection still uses Android clipboard behavior; the sensitive hint is applied to explicit app Copy actions, not a promise about every OS clipboard path.
