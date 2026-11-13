# Phase B: correctness baseline

This phase is stacked on Phase A. It does not publish, merge, or change the distribution channel.

## Scope and decisions

- Password strength uses the generated password's original alphabet size, saved alongside its
  output. Editing length or character switches does not re-rate an unchanged password. The meter
  remains an estimate, not a security guarantee.
- Date Calculator stores its selected date as an epoch-day value through saved-state restoration.
  Reopening the picker starts from the current selection. Negating an out-of-range date offset
  fails instead of wrapping.
- Counter screen and widgets share a single atomic preference update path. All widget instances
  refresh after an action, and a composed screen observes preference changes. Int overflow is
  rejected with a visible limit message, not silently wrapped or converted to a different count.
- Calculators reject nonfinite and unrepresentable results. Ratio simplification uses decimal /
  arbitrary-precision integer reduction after parsing; loan and average formulas use more stable
  equivalent calculations. This is not a migration to arbitrary-precision financial accounting.
- QR encoding explicitly uses UTF-8 and preserves the entered text, including whitespace.
  Image write failures and unavailable sharing handlers surface errors instead of crashing.
- Timer completion follows the approved OS-alarm approach. See [Timer behavior](timer-behavior.md)
  for permissions, reboot restoration, notification behavior, and limitations.

## Regression coverage added

- JVM tests: required password character classes and strength boundaries; counter overflow;
  numeric edge cases; Unicode QR round-trip and invalid payloads; timer state transitions.
- Android instrumentation: date saved-state restoration, password rating across settings changes
  and restoration, and shared counter-store updates. These are focused regressions, not exhaustive
  navigation, sensor, multi-widget, accessibility, or process-death tests.

## Verification required before release

The implementation was reviewed statically. Local Gradle execution is blocked before compilation
by distribution-download network access. Neither Android compilation nor JVM/instrumentation
passes are claimed until the Phase A CI job and a connected-device run complete.

Run the documented CI commands, then `./gradlew :app:connectedDebugAndroidTest` on a clean test
emulator. The counter instrumentation test resets the debug app's counter state.

Device acceptance must cover multiple counter widgets, rotation, background timer expiry,
notification and exact-alarm denial/revocation, reboot and unlock, force-stop and reopening,
pause/reset shortly before expiry, repeated starts, notification dismissal, and QR sharing
with low storage / no matching target app.

Most numeric tools still use Double. Decimal precision, rounding policy and localization remain
separate work; this phase prevents known overflow/nonfinite failures without redefining formulas.
