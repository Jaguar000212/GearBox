# Optional active timer notification QA

Off by default, independent from the completion channel. No foreground service or periodic worker.
The system chronometer renders a countdown. Deadline alarms continue to use the repository's
monotonic clock; reboot fallback is unchanged. A timeout removes expired status even if an inexact
alarm is late. The notification never promises precise completion delivery.

The Android public `android.requestPromotedOngoing` extra is requested on API 36+, with the
manifest permission. The current API reference documents the extra from 36.1; unsupported builds
ignore it and keep the normal notification. Promotion remains an OS/OEM/user decision. Paused
notifications never request promotion. No target/compile SDK change is needed.

Official references:
- https://developer.android.com/develop/ui/compose/notifications/live-update
- https://developer.android.com/reference/android/app/Notification#EXTRA_REQUEST_PROMOTED_ONGOING

## Physical-device checks (not executed by CI)
- Android 16 and 17: enable setting with permission denied/granted; disable active channel separately
  from completion channel. Verify the timer still completes independently.
- Start, pause, resume, cancel from screen/widget/notification; rapidly repeat actions. An old action
  must not control a replacement or resumed countdown. Tap notification to open timer.
- Swiping status away must not repost on screen return or pause/resume; Show this timer again or a
  newly started timer may restore it. Disabling the setting removes status without stopping timer.
- Kill process (not force-stop), return to app; reboot/unlock while running and while paused.
  Check completion delivered once, status removed. Force-stop is explicitly not guaranteed.
- Change wall clock/time zone during a running timer. Countdown/alarm must continue by elapsed time.
- Deny precise alarm access; after deadline, status should expire even if completion alert is delayed.
- Confirm quiet status channel, normal paused fallback, no forced promotion when disabled by user,
  no persistent service, no high-frequency reposting, and accurate TalkBack controls.

## Stopwatch (same opt-in)
- Run timer and stopwatch simultaneously. Both notifications and their controls must be independent.
- Running stopwatch counts up with Pause; paused shows a frozen elapsed value with Resume.
- Reset opens in-app confirmation and must not discard laps until confirmed. Tapping opens Stopwatch.
- Dismiss stopwatch, pause/resume in app, reboot/process kill; it must stay hidden for that session.
- Toggle the setting off to remove both without stopping either. A reset/new session restores eligibility.
- Stopwatch never issues a completion alert. Validate very long elapsed durations and wall-clock changes.
