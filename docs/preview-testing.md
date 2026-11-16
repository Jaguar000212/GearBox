# Isolated preview testing

CI builds the ordinary debug/release checks first, then an opt-in isolated preview:

```sh
./gradlew -PisolatedPreview=true -PpreviewCheckpoint=2 :app:assembleDebug :app:assembleDebugAndroidTest
```

The current checkpoint changes debug identity to `com.jaguar.gearbox.preview2`, launcher label **GearBox Preview 2**. The bounded `previewCheckpoint` property accepts only `1` or `2`; omitted/default `1` preserves the original opt-in `.preview` identity and **GearBox Preview** label. Ordinary debug remains `com.jaguar.gearbox.beta`; release remains `com.jaguar.gearbox`. No release signing key, tag or release is created.

The preview can be installed beside both the earlier beta test APK and original Preview without replacing its data, even though CI debug signing certificates differ across runs. It starts fresh, with separate permissions, notification settings, widgets, shortcuts, preferences and history. Do not uninstall or clear the old app. A future preview with a different debug signing key may still require a separate profile or a locally consistent debug signing key; isolation is not a signing-key migration.

## Identity and fidelity checks

- CI checks regular/release package IDs, preview package and distinct label, every merged provider authority, unchanged launcher class, and `.preview2.test` instrumentation targeting `.preview2`.
- File sharing uses the current package's FileProvider. Shortcuts/widgets/notification actions use explicit components with the current app context. Channel IDs, files and preferences are package-scoped; internal action strings and `gearbox://` identities are not exported public deep links.
- Source code and tool behavior are unchanged. The preview gets its own unit/lint pass and compiled instrumentation APK. Android instrumentation and hands-on testing remain unexecuted.
- The preview validates fresh-install behavior, not in-place beta/release upgrades or migration. Upgrade tests require the matching original application ID and signing key.
- Widgets may have the same individual labels in the picker: choose the **GearBox Preview 2** app group. Launch each app's own shortcut, widget and notification to check isolation.
- Close the other app's Flashlight when testing, because two apps can compete for the same physical camera/torch hardware.

Follow [the complete testing guide](testing-guide.md). Run instrumentation only in a disposable test profile, never against data or active clocks you need to preserve.

The current package contains `GearBox-Preview-2.apk` and its matching instrumentation APK. Install only the main APK for ordinary hands-on testing. No uninstall, data clear, signing secret or signing-key migration is required or performed. Future rebuilds of Preview 2 can still encounter a CI debug-certificate mismatch; do not delete data to work around it.
