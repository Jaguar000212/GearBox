# Shared tool actions

Use `ToolActions` for text/result actions rather than hand-built Copy/Share rows.
It accepts optional `onCopy`, `onShare`, and `onClear` callbacks. Omit unsupported
actions. Use `resultEnabled` to prevent copying/sharing invalid or stale output and
`clearEnabled` when input is empty. Callbacks own the content and Android intents;
the component owns labels, icons, spacing, and secondary Clear presentation.

`ResultCard` delegates to this component without changing its public interface.
Average, Percentage and Unit Converter use it directly for custom result layouts.
Image sharing may require a more specific label and is not silently converted to
text sharing. Reset of persistent state is separate from clearing calculator input.

Device review: large text, narrow screen, TalkBack labels, invalid input, clear,
copy exact displayed result, share sheet cancellation. New Compose tests require
an emulator/device; CI compilation alone does not execute them.
