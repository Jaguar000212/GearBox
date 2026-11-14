# Timer / stopwatch UI review

This change only adjusts presentation. ClockRepository, countdown transitions,
alarm scheduling, restoration, widget updates and persistence remain unchanged.

Before merging, check the debug build on a device:
- Stopwatch: Ready → Running → Paused → Resume, several laps, reset. Confirm
  split durations and total elapsed values; test backgrounding and reopening.
- Timer: manual input and 1/5/10-minute presets, invalid/zero duration, start,
  pause/resume, completion and Start again. Presets never start a timer directly.
- Alert details: expand/collapse, verify permission warnings and settings links
  remain available, test denied notification and precise-alarm permissions.
- Confirm completion notifications, background execution and reboot restoration
  still behave as before. Do not use this as a safety-critical alarm.
- Light/dark/dynamic themes, TalkBack, 200% text, narrow display, landscape and
  keyboard visibility. Check long (hour-scale) clock strings remain readable.

Added Compose presentation tests do not replace those real-device checks.
