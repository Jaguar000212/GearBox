# Timer alert behavior

The countdown uses an Android `AlarmManager` alarm and a one-shot completion notification.
There is no foreground service or permanent notification, and no ongoing notification controls.

- Android 13+ notification permission is requested only from the explanatory button on the timer screen.
  System/channel notification settings must also permit the alert.
- Android 12+ precise-alarm access is requested through the system Alarms & reminders settings,
  after explaining its purpose. With access, `setExactAndAllowWhileIdle` is used. Without access,
  a best-effort `setAndAllowWhileIdle` alarm is used and the UI warns that background alerts are
  not guaranteed. The timer remains usable when either permission is declined. If notifications
  are disabled, completion is visible in the app only; it does not play a separate fallback sound.
- The countdown uses elapsed realtime within a boot, so changing the wall clock does not change
  an active countdown. Persisted wall time allows an active timer to be restored after reboot
  and unlock; changing the wall clock across a reboot can affect that recovery estimate.
- Pause/reset cancel the alarm. A generation token rejects stale completion broadcasts and a
  persisted claim prevents duplicate notifications from the screen and receiver.
- The screen and every timer widget use the same persisted state owner. Widget updates refresh
  all instances. Widgets remain snapshots rather than second-by-second clocks.
- Boot completion, app replacement and newly granted exact-alarm access reschedule active timers.
  An overdue active timer completes when restoration runs. Already completed/claimed timers do
  not re-alert after reboot. Reopening the timer screen also reconciles a pending completion.

## Important limitations

Force-stopping an app prevents its alarms and receivers from operating until it is opened again.
Reboot restoration waits for normal boot/unlock because this app uses credential-protected storage.
Android/OEM battery restrictions, exact-alarm quotas, notification permissions/channel settings,
Do Not Disturb and system sound settings can delay or silence alerts. Revoking precise-alarm access
may cancel existing alarms; opening the timer screen reschedules a best-effort alarm. No timing or
sound guarantee is made. Use a dedicated alarm for safety-critical deadlines.

## Verification required before release

The pure state/duration tests cover monotonic timing, reboot catch-up, pause/reset behavior,
stale alarms, duplicate completion claims and invalid durations. Android compilation and device
verification are still required. Test Android 11, 12 and 13+; allow/deny/revoke permissions; leave
screen/app, lock/Doze, reboot/unlock, force-stop/reopen, change clock, pause/reset near expiry,
multiple widgets, and notification dismissal. Verify notification occurs once without an ongoing
notification and inspect the exact release build, not just debug.
