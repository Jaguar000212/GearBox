# Phase C: usability and accessibility groundwork

This phase builds on Phase B. It does not claim a completed accessibility certification or a complete localization migration.

## Changes
- Tic-tac-toe uses one fully laid-out board in the screen scroll container, with position, mark, winning and disabled states exposed to accessibility services. Rows grow for large text.
- Saved color swatches have visible and spoken exact HEX names; RGB sliders have channel labels.
- Favorites can be reordered with visible up/down buttons and accessibility actions, in addition to drag. Drag uses measured row geometry. It still does not auto-scroll beyond visible rows.
- Home columns respond to available width and font scale. Cards wrap text and provide a named 48dp favorite action. Tool content is centered and width-limited, with keyboard insets.
- Shared decimal inputs distinguish untouched fields from fields actually focused and blurred, and report invalid/non-finite text without changing formulas or domain rules.
- Orientation tools distinguish unavailable, waiting, unreliable and failed sensors; subscriptions follow lifecycle. Compass headings are magnetic, not true north.
- Flashlight reports missing hardware, denied access, busy/unavailable camera and other failures, rather than silently ignoring them.
- The vertical ruler uses vertical display DPI and explicitly asks users to verify physical measurements. Manufacturer DPI can be inaccurate.
- Touched labels and errors live in English string resources. Other tool labels remain hard-coded; no translations or support/privacy promises were added.

## Verification and remaining manual checks
Focused JVM tests cover adaptive columns and numeric syntax/finite boundaries. Added instrumentation tests cover board semantics, swatches, favorites order controls, large-text cards, sensor error state and focus/blur validation.

The editing computer has no Android SDK and cannot download the Gradle distribution. Local checks are source/resource checks, not Android build or device-test results. The PR CI separately compiles debug, release and instrumentation packages and runs JVM tests and lint. Compiling instrumentation does not execute it.

Before public release, run instrumentation on supported API levels and manually test TalkBack/Switch Access, 200% font scale, narrow split screen, landscape, keyboard navigation, physical sensor accuracy, torch ownership changes, and favorites drag. Check all tools for remaining truncation and hard-coded strings; these shared changes are groundwork, not a claim that every tool has been visually audited.
