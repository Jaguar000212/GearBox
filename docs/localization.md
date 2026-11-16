# Presentation resources

User-facing presentation is resolved through Android string/plural/array resources. New groups separate catalog/help, app screens, calculators, games/clocks, special tools, shared components and system surfaces. Existing resource groups remain intact.

Use `stringResource` in composable presentation, or a captured `Context.getString` / `Resources.getQuantityString` in callbacks, exports, notifications and widgets. Do not call composable functions from callbacks. Use positional arguments for formatted text and real plurals for counts; literal percent prose is marked `formatted="false"`. Keep translator-facing complete sentences rather than sentence fragments when practical.

Catalog/domain models retain canonical English fallbacks for pure tests and compatibility. Production UI uses contextual localized adapters. Do not replace stable route IDs, preferences, enum names, input-map keys, action identifiers, MIME/URI strings, unit IDs, algorithm names or typed snapshot data with translations. Translating displayed names must not change lookup, sorting identity, restore or handoff safety. Localized numeric display must not become a storage format.

Number-to-Words intentionally produces English words. User-entered names and previously stored History/Workbench outputs are not automatically translated. Legacy result-label adapters map original domain output to localized presentation once, without parsing translated text back into state. Morse partial status uses explicit metadata and retains an old-record compatibility check.

This migration adds the default English resource contract, not translations. Additional locales belong in `values-<locale>` with matching argument types/order, plural categories and semantic meaning. Validate locale recreation, long translations, accessibility announcements, formatting, notification/widget/shortcut text and every user action. Device/instrumentation execution remains a separate testing step.
