# New-tool and extension batch: source audit

Baseline audited: local `aa87c2d`, tree `a6e25c9f24aa9fbfaff181745f21ade8027aa645`,
matching the cumulative PR107 source tree. This document describes that stacked
implementation plus the accompanying icon/help corrections, **not features all
merged into main**. At audit time main contained Focus Timer and Color Blend;
the remaining tool stack still required authorized merges. PR/check status is
intentionally not recorded here because it changes independently of this audit.

## Inventory and evidence

Paths below are under `app/src/main/java/com/jaguar/gearbox` unless stated otherwise.
Tests are authored source evidence, not an assertion that they were executed.

| Tool or extension | Source and test evidence | Important limitation |
|---|---|---|
| Focus Timer | `focus/FocusState`, repository/alerts; Focus state/storage tests | Device notification and lifecycle checks required |
| Color Blend | `logic/ColorBlend`; logic/snapshot/screen tests | Encoded-sRGB, not perceptual mixing |
| Bill Splitter and shared expenses | `logic/BillSplitter`, `SharedBill`; calculation/snapshot/hydration/screen tests | Bounded minor-unit arithmetic; settlement suggestions do not send payments |
| Business-day Planner | `logic/BusinessDates`; logic/snapshot/screen tests | Explicit weekend mask and excluded dates, no holiday service |
| Scientific Calculator | `logic/ScientificCalculator`; engine/snapshot/screen tests | Bounded supported expressions, not symbolic algebra |
| Text Diff | `logic/TextDiff`; engine/snapshot/screen tests | Exact or ASCII-whitespace-ignore; 16,000 characters and 200 lines per input |
| Time Zone Converter | `logic/TimeZoneConversion`; DST/snapshot/screen tests | Device timezone database; explicit gap rejection and overlap choice |
| Decision Matrix | `logic/DecisionMatrix`; engine/snapshot/screen tests | 2–6 options, 1–6 criteria; transparent weighted scores, not objective advice |
| Aspect Ratio / Image Size | `logic/AspectSizing`; engine/snapshot/screen tests | Calculates dimensions; does not edit files |
| Deadline / Milestone Planner | `logic/DeadlinePlanner`; engine/snapshot/screen tests | Backward scheduling, not calendar creation |
| JSON Code Formatter | `logic/JsonFormatter`; engine/snapshot/screen tests | Strict bounded grammar; not schema validation |
| XML mode | `logic/XmlFormatter`; Android parser fixtures | XML 1.0, attribute layout only; conservative declaration preflight also rejects inert matching text |
| HTML mode | `logic/HtmlFormatter`; engine/schema tests | Full-document normalization repairs/wraps input; not sanitization or rendering |
| CSS mode | `logic/CssFormatter`; scanner/schema tests | Top-level line separation only; not validation or minification |
| Image Studio crop/resize/export | `images/ImageStudio`, policy; bitmap/policy/screen tests | PNG/JPEG/lossy WebP; working/output cap 2048 edge and 4 million pixels |
| Image markup and regional effects | `images/ImageStudioEdits`, policy; pixel/arrow tests | Rectangle, arrow, solid cover, blur, mosaic; blur is not secure redaction |
| Image text | Annotation validation/drawing; text policy/pixel tests | Bounded single-line text, clipping and device-font limits; flattened pixels |
| Collage and adjustments | Image composition policy/renderer; pixel/logic tests | 2–4 sources, contain/cover; basic brightness/contrast/saturation, not AI correction |
| Light Meter | Light controller, lifecycle and UI tests | Hardware lux estimate, not calibrated |
| Sound-level Meter | Audio source/controller; math/controller tests | Relative dBFS, not calibrated sound-pressure level |
| Pitch Tuner | Pitch detection/controller; endpoint/lifecycle tests | Sustained single-tone estimate, bounded frequency range |
| Battery Information | Battery model and lifecycle tests | Android-reported values, unavailable fields remain unknown |
| Sensor Checker | Fixed inventory/controller/UI tests | Selected foreground sensors, not hardware certification |
| Magnifier | Camera helper/screen; export tests | Bounded preview freeze/export; asynchronous control generations reject stale completions |
| Barcode / QR Scanner | Bounded decoder/luminance/URL policy tests | Supported formats only; explicit copy/share/reviewed HTTP(S) open |
| Barometer | Pressure/reference controller and lifecycle tests | Standard-atmosphere altitude estimate, not measured elevation |
| Proximity Checker | Classification/controller and lifecycle tests | Frequently binary hardware, not precise distance |
| Angle Finder | `logic/AngleFinder`, orientation remap and isolated UI tests | Extension of Bubble Level; session-only relative zero |
| Weighted Picker | Integer-ticket logic/snapshot/isolated UI tests | Extension of Team Picker; bounded positive weights, no reroll on restore |

There are nine added hardware tools when Light Meter is included. Angle Finder,
Weighted Picker and the formatter/image operations extend existing catalog entries.

## Catalog and resources

The audited catalog has **62 tools in seven categories**:
Device 14; Calculators 11; Utilities 11; Time 9; Games 7; Money 6; Random 4.
The `res/values` directory contains 82 XML files and 2,502 declarations:
2,337 strings, 141 string arrays, 16 plurals, seven colors and one style.
No duplicate resource type/name pairs were found. These are resource definitions,
not a claim that translations for additional languages exist.

The audit found duplicate fallback icons that violated `ToolHelpTest`'s uniqueness
assertion. The accompanying correction keeps that invariant: Proximity uses
TouchApp, Sound-level Meter uses GraphicEq, while Sensor Checker and Pitch Tuner
retain Sensors and Mic. Stale Image Studio help claiming markup/collage/effects
were absent is also corrected in both fallback and localized-resource guidance.

## Privacy, bounds and outstanding verification

Sensitive text tools retain per-route automatic-memory opt-ins. Camera/audio/live
sensor additions have no automatic capture/history/workbench path. Image Studio
remembers only permitted output preferences; generated exports can remain in a
bounded private cache. Re-encoding is not anonymization. Explicit exports/shares
can disclose content and must be reviewed. Camera/microphone tools require explicit
Start and permission, stop on background/disposal, and reject stale callbacks.

This source audit did not execute Android instrumentation or physical-device tests.
CI compilation of instrumentation APKs is **not** execution. Camera, microphone,
sensor accuracy, permission transitions, orientation, background/recreation,
export providers, TalkBack and large-font behavior still need device QA. XML
fixtures depend on the Android parser. Actual dependency/desugaring and release
packaging checks remain CI responsibilities, not inferred from source inspection.

Additional useful coverage: formatter cross-language transitions and stale-share
invalidation; CSS escaped-brace/unquoted data-URL fixtures; Image Studio EXIF
orientation; rapid Magnifier controls and genuine newest-request failure.
See `testing-guide.md` and `MAGNIFIER_CONTROL_TESTING.md` for device steps.
