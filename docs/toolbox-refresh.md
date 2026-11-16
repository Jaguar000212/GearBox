# Toolbox refresh: staged implementation

PRs #55–58 were merged after exact-head CI/review checks. Phase 4 builds on the verified Phase 3 tree. No release or tag has been created. Final build compatibility is tracked in [PR #60](https://github.com/Jaguar000212/GearBox/pull/60), with exact-head CI checks before delivery.

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

## Phase 3: guidance and responsive presentation

- Contextual Help & examples catalog for every tool, available from a secondary menu without crowding the prominent History button.
- Compact mode dialogs on Base, Geometry, Morse/Binary, Date Unit, Hash, Text case, Percentage and Unit category; simple two-way choices remain visible.
- Adaptive input/result panes for Percentage, Tip, Unit Converter and Text Tools, with stacked phone/large-text fallback and shared scrolling.
- Selectable result-card output and wrapping/stacking Copy/Share actions for narrow screens and large text.

## Phase 4: workflow and accessibility

- Quick switch saves/restores non-password navigation drafts without a new persistent draft cache; explicit imports take precedence. Generated passwords are transient, not saved-instance data.
- Contextual named presets and compatible result handoffs are explicit, bounded and destination-specific; no automatic clipboard access or hardware/clock actions.
- Favorites edge-hold autoscroll, saveable chooser selections, focus return and Home unfavorite Undo.
- Local reduced-motion preference and optional Home clock shortcut. Home clock observation never starts/resets/selects a clock; hardware capability hints remain informational.
- Full-screen focus for Counter, Clock, Scoreboards, Compass and Level. Counter/clock reuse existing actions; scoreboards intentionally remain read-only in focus.

## Deliberate limits and remaining owner validation

- Device-level layout, TalkBack focus, gesture behavior, font scaling, restore/cancel/navigation and migration checks are deferred to the owner, not represented as automated passes.
- Adaptive split panes apply where implemented (Percentage, Tip, Units and Text); other tools retain readable scrolling layouts. Broader adaptation can follow observed device needs.
- Bubble Level and Compass retain the existing rotation-vector hardware requirement. Capability hints do not add support for previously unsupported sensors or certify measurement accuracy.
- System animation scale remains honored; the new preference additionally skips decorative coin/bottle animation.
- History excludes Password Generator; device snapshots are read-only; clock restore imports configuration only; game restoration requires validation and confirmation.
- Navigation drafts may survive Android task restoration, while automatic disk mode/result/history retention remains independently controlled. See [switching details](PHASE4_SWITCHING_TESTING.md).
- Legacy game/session preferences retain existing Android backup behavior. New history, recents, presets/results and presentation settings use no-backup storage.

## Verification

Local whitespace and Python packaging tests passed. Local Gradle execution was blocked downloading Gradle 8.10.2 by network restrictions. CI run 144 passed on commit `1309ed4cc1cd7f52d576ef049ea2d30fde60f210`: debug/release app builds, instrumentation APK compilation, JVM unit tests, debug/release lint, and version/packaging guardrails. Instrumentation tests were compiled, not executed on a device.

Phase 2 CI43904137433 passed at `d3f0c574db2dd058527b8518792fbec8aca320aa`; Phase 3 CI43904474797 passed at `0e9c73e99b492cbeb51dad8eaa11359acbbb4610`. Phase 4 exact-head verification is recorded in [PR #59 checks](https://github.com/Jaguar000212/GearBox/pull/59/checks).

CI now uploads debug app and instrumentation APKs plus checksums for hands-on testing (seven-day retention). These are development artifacts, not a signed release. Ordinary debug uses `com.jaguar.gearbox.beta`; an existing installation with a different signing key may reject an update. CI also provides an opt-in isolated `com.jaguar.gearbox.preview` build for side-by-side testing; see [preview setup and limits](preview-testing.md). Do not uninstall or clear existing data to bypass that error. Use a separate test device/profile or report the conflict.

### Corrective follow-up

Last-result support now restores validated typed outcomes for six random tools (Coin, Bottle, Dice, Random Number, One of Two and Rock Paper Scissors), including explicit History/Workbench reuse, without generating new outcomes or changing tallies. Legacy records without typed outcomes restore inputs only. QR/Hash continue to require Generate; live/destructive tool exclusions remain deliberate.

Help includes confirmed, cancellable example-input imports for 23 deterministic tools rather than claiming every tool has an executable example. Loan Calculator catalog wording now matches its repayment functionality. See the corrective checklist in `testing-guide.md` for scope and remaining verification/privacy limitations.

### Privacy follow-up

Text, QR and Hash now require separate per-tool opt-ins for automatic history and last-result memory, in addition to global switches. Missing choices default OFF on fresh installs and upgrades. Storage enforces the policy, and disabling invalidates queued writes. This does not detect secrets in other tools or disable Android task-state drafts.

Existing records are not silently deleted: Settings provides counts, read-only review and confirmed per-tool history/result clears. Manual Workbench saves remain deliberate and independent, with a preview and privacy warning. Explicit Copy actions for potentially private text request Android's sensitive clipboard treatment; Share requires review before opening an external chooser. App-private storage is not an encrypted vault; OS editable-input selection and legacy backup behavior remain separate.

### UI completion follow-up

Favorites now exposes saved presets through an explicit configuration-only launcher; Quick Switch searches category labels and has a visibility-controlled recent section without duplicates. HSL is editable with finite/range validation and an Apply step; applied RGB remains 8-bit.

Adaptive panes now also cover Age, Average, BMI, Date, Days Until, Discount, Fuel, Geometry, GST, Loan and Ratios (15 tools total with the earlier four). Simple single-input converters remain compact stacked forms. Percentage/Unit Converter gain Clear; calculator Clear controls preserve relevant modes and do not delete saved data. See the testing guide for date-specific reset semantics and execution limits.
