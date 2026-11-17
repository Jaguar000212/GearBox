# Combined redesign scope audit

This is a source-level inventory, not a device-test sign-off. It includes corrective, privacy and UI-completion follow-ups after the initial preview. Check PR checks for the exact delivered build; instrumented tests are compiled but have not been executed here.

| Area | Implemented | Deliberate limit / verification remaining |
| --- | --- | --- |
| Categories | Seven categories, all 41 current tools assigned; distinct icons, subtle theme-aware accents and accessible counts | No custom categories, multi-category tags or automatic fallback for future unassigned tools |
| Home | Compact cards, category chooser/counts, ranked task-language and operation search, recents, capability hints, optional active-clock shortcut | Selected category narrows results; Search all fallback. No universal dashboard customization |
| Favorites | Launcher/edit modes, searchable chooser, drag edge autoscroll, accessible move actions, removal Undo, shortcut synchronization | Launcher/device behavior and TalkBack need runtime testing |
| Presets access | Favorites launcher and tool menu, configuration-only explicit imports | Live calculators may recalculate; never implicitly Generate/Start. No preset folders/rename |
| Quick switch | Shared ranked task/operation search, optional recents without duplicate entries, Navigation draft preservation | Password excluded; Back discards current destination. Android task state is separate from disk-retention controls |
| Operation discovery | Curated existing selector modes, canonical ID validation and explicit mode-only confirmation; no duplicate tools | No AI/freeform interpretation, new tools or automatic Generate/Start; ambiguous matches open the tool without selecting a mode |
| Tool guidance | Help for all 41 tools; confirmed example-input imports for 23 deterministic tools | Games/random/password/hardware/clock examples are reference-only; no automatic action execution |
| Selectors | Compact Base, Geometry, Morse, Date unit, Hash, Text, Percentage and Unit controls; Password Customize | Some tools appropriately retain specialized controls |
| Adaptive layout | 15 input/result split-pane tools; large fonts revert to one column | Not a universal two-pane UI. Compact single-input converters and live tools use their own layouts |
| Result actions | Consistent Copy/Share/Clear across deterministic calculators; expanded Clear preserves modes | Date Clear resets date to today; QR shares image; Password deliberately has no Share. No universal sticky bottom bar |
| Calculation clarity | Show working for Percentage, Tip, Discount, GST and Ratios; Text transformations separated from source-input information | Formula explanations use parsed finite-precision values; not a universal symbolic solver or new text operations |
| Starting units | Metric/imperial preference for fresh BMI, Unit Converter and Ruler | Existing drafts, saved modes/results and imports win; no conversion of existing inputs, no Fuel or category-switch changes |
| Color | HEX/RGB/HSL plus explicit local-image sampling and opaque-sRGB luminance/contrast inspection | Bounded/downsampled white-composited image; selected color only enters normal memory, no image/URI save; not physical color calibration or full accessibility certification |
| Sensors | Availability/accuracy/error hints, focused Compass/Level views, flashlight contention handling | Level requires rotation-vector support; no accelerometer/gravity fallback. Ruler is estimated, not calibrated |
| Focus | Counter, Clock, Scoreboard, Chess Clock, Compass, Level; visible Exit and Back | Scoreboards are read-only in focus. Explicit TalkBack focus return is not guaranteed; test on device |
| Motion | Coin/Bottle reduce-motion preference and system animation scale | Not a blanket suppression of every platform transition |
| Incoming shares | Bounded plain-text preview and explicit destination import to Text/QR/Hash/Morse; initial automatic retention suppressed | No attachments/HTML/fetch; Text/Morse live-convert after confirmation, QR/Hash require Generate; Android task drafts after import remain separate |
| Typed handoffs | Existing text/integer paths plus supported exact whole decimals and explicit ratio quotient | No arbitrary pipeline or formatted-output parsing; numeric destination limits enforced |
| Widgets | Existing Counter and Timer/Stopwatch widgets preserved; Settings/help discovery instructions | Launcher must support widgets. No automatic widget placement |
| History | Top-bar History for 40 nonpassword tools; copy/share/restore/delete/clear; global n=1–100, default20 | Valid completed results after debounce, not every keystroke. Bounded fields/output; oversized records rejected |
| Outcome reuse | Six random tools replay validated typed outcomes, without RNG/tally changes | Older records without typed outcomes restore inputs only. Physical readings cannot be replayed |
| Last-result retention | Separate global opt-in and supported restoration | 11 live/destructive exclusions; QR/Hash require Generate; current-input Clear does not erase remembered results |
| Private text | Text/QR/Hash automatic history/result opt-ins default OFF even on upgrade; storage enforcement and queue invalidation | Other tools can still contain private data. No automatic secret detection |
| Review/deletion | Settings per-private-tool counts/review/confirmed clear; manual saves previewed | Existing data retained unless explicitly deleted; no secure-erase guarantee |
| Clipboard/sharing | Sensitive explicit Copy hint and reviewed exact-payload sharing for Text/QR/Hash/history/Workbench | Editable input selection uses OS clipboard behavior. Hint is not clipboard access control |
| Workbench | Explicit local named presets/results, copy/share/import, rename, filter, selected deletion and compatible snapshot comparison, max100 each | No folders or payload editing; comparison restricted to supported same-mode numeric snapshots; no automatic eviction of explicit saves |
| Local storage | New memory/recents under noBackupFilesDir, atomic bounded storage | Not encrypted vault. Legacy preferences/backups and Android task-state drafts remain separate |
| Games/clocks | Game retention controls, explicit snapshot/restore guards; active clocks preserved | New Preview identity has separate clocks/data and cannot verify in-place migration |
| Build/test | Normal+preview builds, JVM tests, lint, APK identity guards; expanded isolated instrumentation tests | Instrumentation runtime, hardware, TalkBack, landscape/large-font visual QA and upgrade migration still unverified |

## Fresh preview versus upgrade testing

GearBox Preview 2 uses a separate identity to avoid replacing either the earlier Preview or beta app. It starts with fresh data and permissions. Do not delete old app data. Fresh-install feature tests cannot establish migration correctness, and a future build with another CI debug certificate may not update this installation. Stable debug signing remains unresolved; no signing secret has been introduced.
