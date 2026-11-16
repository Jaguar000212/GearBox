# Full-screen focus checks

These cases require device testing; they are not reported as executed.

- Open Counter → Focus view. Increment/decrement at each configured step. Exit and verify same count; test widget sync and integer-limit warning. Exiting/back must not reset count.
- Stopwatch → Focus view: Start, Pause, Resume. Exit/re-enter, rotate, background/foreground. Elapsed time must remain consistent. No automatic reset, lap, or snapshot is introduced.
- Timer: set a valid duration before entering focus. Start/Pause/Resume from focus; verify completion and alarm behavior. Invalid zero duration shows an error, not a new timer. Missing alert permissions remain visible in focus. Exit does not pause a running clock.
- Scoreboard/Chess: full-screen names/scores remain current, large text and long lists scroll, and Exit remains reachable. These views are intentionally read-only; scoring/reset/undo remain in the main screen.
- Compass/Level: focus uses the same live sensor controller, displays qualifiers, and hides measurements unless sensor status is reliable. Test no hardware, waiting, unreliable, and restored reliability. No sensor logging or snapshot capture is triggered by focus.
- All focus screens: Android Back exits focus without leaving the tool; visible Exit does the same. Test landscape, large font/display scale, TalkBack traversal, and system bar insets. Time and sensor updates must not continuously interrupt TalkBack announcements.
