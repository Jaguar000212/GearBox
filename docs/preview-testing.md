# Isolated preview testing

CI builds the ordinary debug/release checks first, then an opt-in isolated preview:

```sh
./gradlew -PisolatedPreview=true :app:assembleDebug :app:assembleDebugAndroidTest
```

Only debug identity changes: `com.jaguar.gearbox.preview`, launcher label **GearBox Preview**. Ordinary debug remains `com.jaguar.gearbox.beta`; release remains `com.jaguar.gearbox`. No release signing key, tag or release is created.

The preview can be installed beside the earlier beta test APK without replacing its data, even though CI debug signing certificates differ across runs. It starts fresh, with separate permissions, notification settings, widgets, shortcuts, preferences and history. Do not uninstall or clear the old app. A future preview with a different debug signing key may still require a separate profile or a locally consistent debug signing key; isolation is not a signing-key migration.

## Identity and fidelity checks

- CI checks regular/release package IDs, preview package and distinct label, every merged provider authority, unchanged launcher class, and `.preview.test` instrumentation targeting `.preview`.
- File sharing uses the current package's FileProvider. Shortcuts/widgets/notification actions use explicit components with the current app context. Channel IDs, files and preferences are package-scoped; internal action strings and `gearbox://` identities are not exported public deep links.
- Source code and tool behavior are unchanged. The preview gets its own unit/lint pass and compiled instrumentation APK. Android instrumentation and hands-on testing remain unexecuted.
- The preview validates fresh-install behavior, not in-place beta/release upgrades or migration. Upgrade tests require the matching original application ID and signing key.
- Widgets may have the same individual labels in the picker: choose the **GearBox Preview** app group. Launch each app's own shortcut, widget and notification to check isolation.
- Close the other app's Flashlight when testing, because two apps can compete for the same physical camera/torch hardware.

Follow [the complete testing guide](testing-guide.md). Run instrumentation only in a disposable test profile, never against data or active clocks you need to preserve.
