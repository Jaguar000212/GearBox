# Rolling developer Preview channel

The public GitHub release **Preview** (tag `preview`) is the opt-in developer channel. Successful builds from pushes to `main` update this one release. It is always a prerelease and never GitHub's Latest release. Stable/beta remain on the existing `v*` tag workflow, unchanged. Failed or superseded builds do not replace the last successful downloadable build.

## One installation, ordinary updates

Every channel APK uses application ID `com.jaguar.gearbox.devpreview` and label **GearBox Dev Preview**, not a per-build identity. It is a non-debuggable release build signed by the existing authorized persistent signing key. Version code is `1000000 + GITHUB_RUN_NUMBER`; version name is `preview.RUN+SHA7`. The workflow identity/run sequence must remain stable. The publisher rejects changed signer fingerprints, non-increasing versions and mismatched provenance; there is no debug-key fallback. Installing a later channel APK updates the same Preview installation and preserves its data under Android's ordinary update behavior.

This is a **one-time new channel installation**, not an upgrade from the older randomly signed `.preview` or `.preview2` test APKs. Those older installs remain untouched. Do not uninstall or clear their data. Stable (`com.jaguar.gearbox`) and ordinary debug (`com.jaguar.gearbox.beta`) also remain separate. No automatic cross-package migration is implemented.

## Build and publish safeguards

- PR CI uses the shared full checks without signing secrets and validates the unsigned developer-preview package/label/providers. Conservative Markdown-only PRs run lightweight guardrails with the same status name; main always runs every check. PR CI does not publish releases.
- One main-only Preview workflow checks out the exact push SHA, runs shared full Python/JVM/build/lint/identity gates, then signs the isolated release, verifies its certificate and non-debuggable identity, and preserves a distribution artifact. Its existing workflow identity/run sequence remains unchanged.
- The publish job alone gets `contents: write`. It verifies source SHA, run number, version, APK checksum and signer continuity before publication. It refuses unowned `preview` tag/release collisions and immutable releases rather than changing repository security settings.
- Publishing is serialized and rechecks `main` to prevent an older finished run replacing a newer commit. A failed newer build leaves the previous successful Preview available. Intermediate main pushes may be superseded rather than each being published.
- Versioned assets are uploaded and verified before updating release metadata/tag. Prior assets are retained, so a failed update does not delete the previous APK. GitHub has no atomic transaction for multiple assets/tag/notes: transient inconsistency is possible; use the commit/version/checksum manifest linked in the release notes as the source of truth, not asset ordering or the moving source ZIP alone.
- The `preview` tag is excluded from ordinary Git version lookup. Existing stable/beta version-code ordering is unchanged.
- This channel intentionally produces public downloads, not access-restricted developer distribution. Source commit, build date/run, certificate fingerprint and checksums are recorded for traceability.

## Testing

Download the APK identified by the current release notes and verify its checksum. Only the main APK is needed for hands-on testing. Check that a second newer Preview APK updates **GearBox Dev Preview** rather than adding another app, and that sample history/settings remain intact. Never use important data as the first upgrade test.

Check this install's own sharing FileProvider, shortcut, widget, notification and Quick Settings tile. Provider authorities use the current application ID; explicit components, notification channels and storage are app-scoped. Close another install's flashlight before testing shared physical camera/torch hardware.

Follow [the complete guide](testing-guide.md). Instrumentation runtime, hardware, TalkBack, large-font/landscape behavior and actual same-channel upgrade tests remain outstanding until performed. The first fresh channel install cannot validate upgrade/migration. Stable signing is necessary for future updates, but source checks alone do not prove Android device behavior.

See [CI consolidation, on-demand artifacts and measurement](ci-efficiency.md). No release-asset deletion policy is active.

Historical opt-in `isolatedPreview` checkpoint properties remain available for reproducing old builds; CI no longer distributes numbered preview checkpoints. They are not the rolling Preview channel.
