# GearBox refresh testing guide

This guide grows with each implementation phase. Automated CI builds app/test APKs, runs JVM tests and lint; it does **not** execute Android instrumentation or hands-on device tests. Those remain with the owner.

## Safe installation

For current hands-on testing, download the **current APK linked in the public [Preview release notes](https://github.com/Jaguar000212/GearBox/releases/tag/preview)**. Follow the [rolling developer Preview channel guide](preview-testing.md) and verify the linked checksum/build metadata.

- The current channel is **GearBox Dev Preview**, application ID `com.jaguar.gearbox.devpreview`. Its fixed package, persistent signing certificate and increasing version code let later channel APKs update this same installation. Install only the main APK for ordinary manual testing.
- The first channel installation is separate from stable, beta and historical test previews; it does not migrate their data. Fresh-install tests do not establish upgrade correctness. Test a later same-channel update with non-sensitive sample data to check preservation.
- Historical CI debug APKs (`com.jaguar.gearbox.beta`) and isolated Preview/Preview 2 APKs (`com.jaguar.gearbox.preview` / `com.jaguar.gearbox.preview2`) are **not** this public rolling channel. Their debug signing certificates can differ across runs; do not expect them to update into GearBox Dev Preview. Keep existing installations/data untouched.
- CI debug artifacts remain available on demand for developer diagnostics: manually run CI with **Upload debug and instrumentation APKs** enabled. They include a matching instrumentation APK/checksums. Run instrumentation only in a disposable test device/profile, never against data or active clocks you need to preserve.
- If any installation rejects an update because of signing or version differences, **do not uninstall it or clear its data**. Report the error or use another test device/profile; do not bypass the signature/version checks.
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

## UI completion follow-up

- Favorites → Saved presets lists explicit local setups and handles empty/loading states. Close/Back must not import; selecting a preset imports configuration once, preserves unrelated draft inputs, and never presses Generate/Start. Live calculators may recalculate. Existing Favorites edit, edge autoscroll, accessible moves and shortcuts remain intact.
- Quick switch searches category labels as well as names/descriptions/keywords. Blank search shows recents only when Show recent tools is enabled; those tools must not be duplicated below. Search/clear/close must preserve draft behavior.
- HSL accepts finite decimal values (H 0–360, S/L 0–100) only after Apply. Invalid/blank input leaves applied color, Copy/Share, palette and history unchanged. Check red/green/blue, gray, black/white, 360=0 and fractional values; 8-bit RGB and displayed integer HSL rounding are expected. Changing HEX/RGB, palette or imported color refreshes HSL drafts without loops.
- Percentage Clear empties both values while retaining mode; Unit Converter Clear empties input/result while retaining category/from/to units. Check invalid-input clear and subsequent calculation as well as valid results.
- Adaptive calculator panes must preserve input-before-result traversal, large-font single-column fallback and existing scroll behavior. Test phone portrait/landscape, usable width above/below720dp and fonts above1.3×. See the implementation scope in the phase report rather than assuming every tool is two-column.

All new instrumentation checks still require execution on a device/emulator; CI compilation alone does not establish visual or accessibility success.

Adaptive additions in this follow-up: Age, Average, BMI, Date Calculator, Days Until, Discount, Fuel Cost, Geometry, GST, Loan and Ratios. Together with earlier Percentage, Tip, Unit Converter and Text layouts, 15 tools now use adaptive input/result panes. Compact single-input Roman, Number to Words and Decimal to Fraction remain stacked deliberately.

Clear contracts: Age removes both dates; Days Until removes label/date; Date Calculator resets its start date to today and blanks amount, preserving unit/direction. BMI preserves measurement system and Geometry preserves selected shape. Clear does not delete saved history/preferences. End-to-end Favorites preset navigation, retained drafts, running/paused clock protection and Back restoration still require owner device checks beyond callback tests.

Corrective PR61 verification: exact head `ee0ce3f07527993e620cdd8fcaf228d24ae4d6ad`, CI43925956976 passed all normal/preview builds, JVM tests, lint and identity guards; instrumentation APK compiled only. Merged as `6474d937c64bca3f83e0897734a632a9c149dd7c`.

## Final checkpoint

- Settings → Home-screen widgets explains Counter and Timer/Stopwatch placement without changing launcher state. Verify instructions and choose the correct Preview app group.
- Verify flashlight capability text does not promise a camera-permission request flow. Camera/torch contention must show safe errors.
- Review [the combined scope audit](implementation-scope-audit.md) before declaring completion. In particular, current-input Clear does not delete remembered results; old results can return on reopen if retention is enabled.
- Test Focus Exit and system Back with keyboard and TalkBack. Shared tool state must remain intact; explicit accessibility-focus return to the launch control is not guaranteed by this implementation.

## String-resource migration

- Verify English labels and actions across all 41 tools, Home/Favorites/Settings/Workbench, Help/Examples and History. Resource extraction must not change saved route IDs, input keys, modes, presets or restore behavior.
- Check count0/1/2 and maximum values for plurals (dice, password length, history/preset counts and date differences), formatted percentages, apostrophes, leading spaces, line breaks and long text. Compare Copy/Share exports with visible results.
- Exercise callbacks after dialogs and navigation: errors, snackbars, clipboard confirmations, snapshot captures, example imports, and private-text share review must resolve resources without Compose-context crashes.
- Check timer-completion channel/title/body, running/paused stopwatch notification actions, Counter/Clock widget labels/actions and launcher shortcut names. Active clocks, notification actions and widget package isolation must remain unchanged.
- Change device/app locale and recreate screens; catalog names, category/search labels, help, theme choices and live status presentation should resolve current resources. This phase supplies the default English resources, not translated language packs. Translation coverage needs separate language files.
- Verify Morse partial results cannot be handed off as complete translations after localization; structured `_partial` metadata and legacy English-marker fallback preserve that guard.
- Legacy history outputs and user-entered names remain stored text, not automatically translated. Stable unit identifiers, route/action/storage keys, algorithm identifiers, number encodings and the English Number-to-Words algorithm deliberately remain technical/domain data.

Static resource guardrails check XML names/references, indexed formats/plural fallback and direct hardcoded Compose labels. Android CI must also compile resources/app/test APKs and run JVM tests/lint. Instrumentation resource-parity tests are compiled only until executed on a device/emulator.

## Task search and category identity

This phase keeps the same tool routes and seven categories. Search is curated and on-device, not an AI service.

- On Home and Quick switch, search task phrases (including punctuation/case variants), then verify the matching tool and its actual capability. Unsupported tasks must not invent tools or actions.
- Search `circle area` and `temperature`: the result identifies an existing tool operation rather than registering a duplicate tool. Choose it, cancel the mode-change confirmation once, then confirm. Only the selector changes; existing inputs stay available, and no Generate, Start, roll or share action runs automatically. Live calculators may recalculate when their mode changes.
- Try an operation for the current tool and for a tool with an existing navigation draft. Check repeated selection, Cancel, Back, reopening the switcher and rotation. Confirm a dismissed request cannot apply later.
- With a Home category selected, operation/task search remains scoped. Search all clears the category but keeps the query. Clear search returns ordinary tool browsing and recents, without duplicate operation cards.
- Check blank search, mixed case, punctuation, multiple words and no matches. Exact tool names remain discoverable; category search and localized aliases still work.
- All seven category headings and chooser rows retain their original localized names and correct counts. Icons/colors supplement text, not replace it. Check light/dark/dynamic color, large fonts and TalkBack; decorative icons should not produce duplicate announcements.
- Favorites, existing saved modes/history, shortcuts and tool route IDs must remain unchanged.

Automated tests for these flows are separate from hands-on verification. Android instrumentation execution and visual/device accessibility checks remain unperformed here.

## Calculation clarity and starting preferences

- Text Tools separates its four existing transformations from information about the original input. Choose a case before typing, inspect camel-case output versus original word/UTF-16 counts, Clear and reopen, and verify mode memory and sensitive Copy/Share review still behave as configured. No additional text operation is introduced.
- Settings now offers a default unit system for fresh BMI, Unit Converter and Ruler forms. Metric preserves previous defaults; Imperial starts BMI in feet/inches/pounds, converter in Inches→Feet, and ruler in inches. It does not convert existing inputs, change unit-agnostic Fuel Cost, or override remembered modes/results/imports/restored drafts. Follow [unit preference checks](DEFAULT_UNIT_PREFERENCE_TESTING.md), including changing the setting while a draft exists and recreating the app.
- Percentage, Tip, Discount, GST and Ratios provide a collapsed Show working section for valid results. Expand, change input, hide, and make input invalid: explanation must match the current formula and disappear for invalid input. Check sequential discounts, zero original price, negative-base percentage changes, inclusive tax, and decimal/negative ratios. Steps expose parsed floating-point values, not arbitrary-precision original input. Results/exports/history remain unchanged.
- Working sections use localized labels and expanded/collapsed accessibility state. Check large font, landscape and TalkBack. Automated resource/formula and UI regression tests are added; device execution remains the owner's testing step.

## Workbench organization and comparison

- Rename a saved result and a preset. Cancel once, then save a trimmed valid name. IDs, timestamps, order, inputs and outputs must not change; reject blank or overlong names without silently truncating.
- Enter selection mode, select multiple items, and change tabs or exit with Back. Selection must clear rather than leak between presets/results. Select/delete must never activate Restore, Copy or Share.
- Delete selected items: inspect the exact count/collection, cancel once, then confirm. Only selected IDs in that collection are removed; history, remembered results and other saves remain untouched. Check failures do not falsely remove visible items.
- Compare two compatible saved numeric results. Show both stored names, input/mode context and outputs without rerunning either tool or automatically sharing/importing. Same tool alone is not enough if mode or units differ. Incompatible, sensitive text and live/game results must not be offered as a comparable pair.
- Check two-column comparison on wide screens and stacked readable cards on narrow screens/large fonts. Back closes the comparison first. Changing/deleting selection should not show stale items.
- Confirm all explicit saves stay local, existing 100-item limits remain, and no automatic cleanup or data migration occurs.

## Incoming text shares and typed handoffs

- From another app, share plain text to GearBox. Preview the exact text, choose an existing destination, then explicitly confirm import. Selecting a destination alone must not import. Cancel, Back, a newer share and recreation must discard stale previews without processing, saving or navigating to a tool.
- Only bounded plain text is supported. Test empty text, 4,096 characters, 4,097 characters, an attachment/stream, unsupported MIME and malformed extras. Reject rather than truncate. Links remain literal text; nothing is fetched, and no clipboard is read.
- The preview explains that Text/Morse use their normal live conversion after confirmation. QR/Hash import inputs only and require Generate. Before confirmation no tool action occurs. Existing tool inputs may be replaced only after the explicit confirmation.
- With automatic history and remembered results enabled (including each relevant private-tool opt-in), import shared content and wait beyond debounce. The initial imported state must not create those records. Repeat after rotation. A later intentional edit or explicit Generate may save under current settings. Presets/Workbench still require explicit saves. Android task-state drafts after import are separate from these disk-retention settings; the preview itself is not saved in instance state.
- Check shares arriving while a tool/clock is open, repeated shares, and returning with Back. Do not stop/reset active clocks or replay older shortcut requests.
- Typed handoffs now include supported exact whole Decimal-to-Fraction input to integer converters, and an explicitly labeled Ratio quotient to Decimal-to-Fraction. Test fractional/nonfinite/out-of-range values; unsupported destinations must not be offered. No formatted output is parsed to infer a currency, unit or number, and handoffs never automatically press an action button.

Incoming-share launch mode is `singleTop`: a topmost GearBox Activity can receive a newer share without creating another instance. Android task/intent flags may still create a separate instance; verify your share sources and launcher on-device. An older confirmation must not claim a newer share. Preview cancellation never grants approval to process content.

## Color Picker image sampling and inspection

- Choose an image through Android's picker. No broad photo permission or persisted URI grant is requested. The image/URI stays transient and is never saved to History/Workbench; an explicitly sampled RGB color follows the existing color-memory settings.
- Check cancel, failed/oversized/unsupported image, a newer selection during decode, Remove image, navigating away and recreation. No stale decode should change the chosen color. Limits: 12 MiB encoded data, source edge 12,000 pixels, 40 million source pixels, preview edge 1,024 pixels. Decoder applies orientation, targets sRGB, and composites transparency on white; sampling is from this downsampled image, not original sensor pixels or physical screen color.
- Tap corners/center of a known image and compare the applied RGB. The preview height is bounded even for extreme aspect ratios. Use the labeled X/Y fields and Sample pixel without touch; reject invalid/out-of-bounds coordinates rather than sampling a truncated entry. Check TalkBack, large fonts and landscape.
- Image sampling updates applied RGB/HEX and resets stale HSL drafts/errors. Existing saved palette, history, Copy/Share and HSL Apply remain consistent.
- Color inspection distinguishes HSL lightness, relative luminance and physical display brightness. Foreground/background are assigned explicitly from the applied color; changing the picker alone must not change the comparison pair. Swap preserves the ratio. Black/white should be 21:1, identical colors 1:1.
- AA normal/large text indicators use unrounded 4.5:1 and 3:1 thresholds. Large text depends on actual size/weight; opaque sRGB pair inspection does not assess transparency, font rendering, image background or overall accessibility. Formula reference: [W3C contrast minimum](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).
- Image provider/decoder, physical display, picker task lifecycle and accessibility behavior need device testing; CI builds and tests are not a device sign-off.

## Repeated calculations and session editing

- Unit Converter offers Single and Batch modes. Batch requires Convert batch; use up to 100 lines/4,096 characters. Blank, invalid and nonfinite lines remain visible with row errors, never omitted. Changing any input/unit/category invalidates generated output; Copy/Share is disabled until another explicit conversion.
- Test mixed valid/invalid lines, CRLF, empty lines, scientific notation, conversion overflow, exactly 100 lines and 101 lines. Reject over-limit edits visibly without truncating. A restored malformed batch must remain visible with an error, not silently become a different batch.
- Completed batch history/Workbench uses a versioned batch-input schema. Restore imports only batch text and units, requiring Convert again. Test both explicit History/Workbench restore and automatic remembered-result hydration from a fresh Single screen; the next explicit conversion must be recordable under settings. Oversized full results warn that retention is unavailable rather than truncating. Batch side-by-side comparison is intentionally unsupported.
- Single Reverse this conversion takes the unrounded converted value as the new input and swaps units. Compare with Swap units, which retains the original input. Test temperature offsets, negatives, tiny/large values and invalid results. Floating-point round trips may differ slightly.
- Text and Unit editors have Undo/Redo for intentional input/configuration edits and Clear, bounded to 20 combined steps/64 KiB estimated retained payload. The stack is RAM-only, clears on imports/restores/recreation/navigation, and may clear on large edits. Current drafts retain existing Android task-state behavior; Undo does not retract Copy/Share or delete saved records.
- Test edit→undo→redo, undo→new edit clears redo, no-op edits, Clear undo, case/category/unit changes, import while undo is available and recreation. Unit undo restores editor state, not generated batch output. Text privacy/share review must remain intact; normal history settings apply to later deliberate edits.

## Group presentation, launch tiles and accessible status

- Team Picker presents only an already picked winner or existing teams. Open Present existing result: it starts hidden. Reveal, Hide, Next/Previous, Close, Back and recreation must never pick again, reshuffle, change names or save another copy. Each new page starts hidden; underlying names/results must not remain discoverable in accessibility semantics. Closing returns to the ordinary tool where results are visible. This is a presentation aid, not a security lock or screen-capture protection.
- Scoreboard keeps its existing read-only focus view; this phase does not add game/clock Undo or a new tool.
- Add Open Timer/Open Stopwatch manually from Android Quick Settings Edit. Settings explains discovery only; the app never adds tiles or requests placement automatically. Each tile opens the named view without Start/Pause/Reset or rewriting saved clock mode/duration. Test idle and active clocks, app open/background/cold start, repeated taps, locked-device cancellation/unlock, rotation and both installed preview identities. Existing notifications/widgets and their deliberate action buttons must behave as before.
- Tile selection is presentation-only until a deliberate tool interaction; stored mode and configuration must not change merely because the tile opened. Test conflicting remembered mode/setup, explicit tab choice, explicit History/Preset restore, snapshot capture and returning through a normal notification. Timer/Stopwatch snapshots must describe the visible clock, not another stored tab.
- Verify the Quick Settings PendingIntent launch on Android 14+ and the legacy launch on Android 11–13. OEM lockscreen, tile placement and task-stack behavior require device testing. Tiles are launch shortcuts, not live running-state indicators.
- Compact selectors return keyboard focus to their trigger after choice, Close or Back. Test keyboard navigation and TalkBack separately; keyboard focus restoration does not establish screen-reader focus guarantees.
- Compass/Level status changes are polite announcements, not continuous readout announcements. Unreliable/low-accuracy sensors must not display an apparently trustworthy live heading/level verdict. Existing explicitly captured snapshots remain labeled as snapshots. Check unavailable sensors, startup, failure, accuracy recovery, focus views and large fonts on hardware.


## Final source cross-check

The [final source cross-check](implementation-scope-audit.md#final-improvement-cross-check) records selected coverage and deliberate limits. Test the selected implementations, not unsupported universal batch/Undo/split-pane behavior. No additional tools are included. The [rolling developer Preview channel](preview-testing.md) uses a fixed package and persistent signer for future updates; do not uninstall or clear previous test data.

Individual history deletion must preserve unrelated records. For a deliberately corrupted test-only history file, deleting one visible row must fail without rewriting the collection or invalidating queued observations. Valid rows still delete normally; explicit Clear history retains its confirmed whole-collection behavior. Never inject corruption into data you need to preserve.


## Rolling Preview update checks

Install the current release-notes APK, create non-sensitive sample history/settings, then install a later higher-version channel APK. Expect one GearBox Dev Preview app with preserved data, not another app. Verify source SHA/version/checksum and signer fingerprint in the manifest. Old Preview/Preview2, beta and stable installs remain separate and untouched. Check launch tiles/widgets/notifications/sharing remain scoped to the correct install. Test failed build, stale run, signer mismatch, missing key and unowned tag collisions through automated mocks or a disposable repository, never destructive production experiments.

Preview publisher validation accepts the app-scoped FileProvider and AndroidX Startup provider merged by the current dependencies. Unknown, foreign-install, duplicated or malformed authorities must fail closed. The guard is checked against actual signed distribution manifests as well as fixtures; it must not assume FileProvider is the only merged provider.


## CI efficiency verification

Follow [CI run consolidation and measurement](ci-efficiency.md). Verify unsigned CI validation per main push and a separate manual-only Developer Preview workflow, the unchanged PR status name, safe superseded-PR cancellation, manual main publication continuity and optional debug/test artifact availability. Test docs-only versus mixed/source/workflow changes; any classifier uncertainty must take the full path. Main documentation pushes still build but do not publish Preview. Cache measurements are experiments, not enabled defaults; retention planning is advisory and never deletes assets.

## Focus Timer: Pomodoro and intervals

This is a separate tool, not a replacement for Timer & Stopwatch. Test both clocks running independently. A Focus session has a finite number of work rounds, with breaks only **between** rounds. Pomodoro uses a long break after every configured number of completed work rounds; the final work round ends the session without a trailing break. Interval mode uses the same work/rest duration each round.

- Start with short durations for testing. Verify work → break → next work, long-break frequency, final completion, pause/resume, and confirmed reset/replacement. Merely opening the tool, changing a draft, following a shortcut or returning from a notification must not start a session.
- Reject empty, invalid, negative, overflow and out-of-range durations/rounds visibly. Changing the editor must not silently mutate the currently running session. Verify mode switches and cancellation of destructive confirmations.
- Background the app, lock the device, switch tools, rotate, recreate the Activity and terminate the process without force-stopping. The displayed phase catches up from monotonic elapsed time instead of counting UI ticks. A delayed alarm must not emit a burst of alerts for every missed boundary. Stale alarms from a replaced/reset session must not affect the new one.
- Test notification permission denied/granted/revoked and channel disabled. Exact-alarm access, battery restrictions and Android idle policies can delay alerts; the UI must explain capability limits rather than promise exact delivery. Force-stop prevents Android alarms until the app is opened again. This is not a medical, safety or unattended-critical timer.
- Reboot during an active session: require an explicit resume/restart decision rather than automatically running an obsolete schedule. Changing wall-clock time or time zone must not change remaining duration on the same boot. Verify package update recovery and notification navigation.
- Current-session recovery is local operational state, not a History/Workbench result or focus-activity log. Generic result retention and Clear history must not reset live clocks. No task names or secret content are collected. Verify existing Timer & Stopwatch notifications and widgets remain unaffected.
- Check large fonts, TalkBack labels/selected mode, keyboard focus and narrow/wide layouts. Countdown updates should not continuously interrupt TalkBack announcements. Android notifications, reboot recovery, power-management behavior and actual device timing remain device-test items, not claims established by JVM/CI tests.

## Color Blend

Color Blend is a separate two-color calculation tool. It interpolates opaque sRGB **encoded channels**; it does not simulate paint, physical light, transparency or a calibrated display. The weight is the percentage of the second color. Both endpoints and the method must remain visible so the result is not mistaken for another blending model.

- Test valid upper/lowercase six-digit HEX values, black/white, identical colors and weight endpoints. At 0% the first color wins; at 100% the second wins. Verify midpoint channel rounding and invalid/empty/out-of-range input handling.
- Editing either color or weight must invalidate stale generated output. Copy, Share and saved-result actions must use only the current validated result, not a former mix. Import/restore must follow the documented explicit-generation and result-retention policy.
- Verify default-off history, enabled history with bounded count, explicit restore, mode/result retention settings, presets and Clear history without changing unrelated tools. The tool must not silently store arbitrary invalid text or retain a previous result under a new input.
- Test rotation, background/foreground, narrow/wide layout, large fonts, keyboard navigation and TalkBack. Swatches need textual color values, not color-only labels. Test share cancellation and clipboard output independently on-device. Existing Color Picker sampling/inspection must remain unchanged.

Color Blend restoration distinction: explicit History/Workbench/preset/example imports restore validated inputs and require Blend again. When the user enables automatic remembered results, a validated last-result snapshot reconstructs the deterministic local mix on reopening. No external content is automatically processed, and malformed snapshots must not produce a result.

## Bill Splitter

- Verify exact minor-unit arithmetic and conservation: the sum of all shares equals bill + tax + rounded tip. Remainder cents are distributed explicitly, not lost through independent per-person rounding. Test a total not divisible by the diner count and one diner.
- Check the stated tip basis (pre-tax bill), supported decimal precision, zero amounts, maximum bounds, malformed/negative values and invalid diner counts. This tool does not process payments, convert currencies or determine tax obligations.
- Editing inputs invalidates stale output. Explicit restore/example/preset imports require calculation again; remembered-result hydration follows the user's setting and must not replay history. Test lowercase/format normalization equivalents where relevant, opt-in retention and clearing results.
- Verify copy/share conveys total, diner count and differing remainder shares rather than falsely promising one rounded amount for everyone. Check narrow/wide layouts, large fonts, TalkBack, keyboard input and share cancellation on-device.

## Business-day Planner

This is local date arithmetic with user-selected weekend days and explicit excluded dates, not a jurisdiction-aware holiday or legal-deadline service.

- Verify the displayed counting convention: starting date excluded, destination included when it is a business day. Test Friday plus one business day with Saturday/Sunday weekends, subtraction, zero offset, leap day, reversed ranges and exclusion dates on weekends.
- Test custom weekend sets, duplicate exclusions, invalid ISO dates, empty fields, bounds and an all-days-excluded weekend configuration. Reject excessive work rather than freezing or silently changing dates. No network holiday calendar or timezone conversion should occur.
- Edits invalidate stale output. Explicit imports/examples/presets require Calculate; remembered-result hydration follows settings without history replay. Mode retention must not silently preserve date/exclusion content under a mode-only preference.
- Check keyboard/TalkBack weekday selection and error messages, large fonts, adaptive layout and copy/share endpoint explanations. Device UI testing remains separate from pure date arithmetic tests.

## Scientific Calculator

- Verify operator precedence, parentheses, unary signs, right-associative powers and scientific notation. Test `-2^2`, `(-2)^2`, `2^3^2`, division by zero and malformed expressions. The parser must never evaluate arbitrary code or access files/network.
- Check DEG/RAD for trigonometric and inverse functions, constants, square roots and logarithms. Domain and nonfinite results must be errors, not misleading numeric output. Floating-point calculations are approximate and do not provide symbolic algebra or a proof of exactness.
- Exercise expression length/nesting/token limits and repeated operators. Reject excessive work safely. Editing expression or angle mode clears the prior result; explicit imports/examples require Calculate. Remembered-result hydration honors settings without replaying history; mode retention stores angle mode only.
- Test accessible syntax instructions, keyboard input, copy/share, narrow/wide layouts and large fonts. Existing basic calculator behavior must remain unchanged. JVM tests cover the parser; physical-device UI/accessibility tests remain user-owned.
