# GitHub-only release checklist

## Supported tags and publication

The release workflow accepts `vMAJOR.MINOR.PATCH` and `vMAJOR.MINOR.PATCH-beta`.
The currently requested prerelease tag is **`v2.30.0-beta`**. Tags containing `-beta`
create a prerelease; stable tags do not. Both create **drafts only**, for owner
review. The workflow never marks a release as latest. No Play Store upload or
AAB is configured or required.

Do not push a release tag just to test this workflow: a matching tag starts a
signed build and creates a draft release. Merge reviewed phase PRs first, then
the owner explicitly chooses and pushes the release tag. Do not move an existing
published tag or overwrite its APK: users must be able to identify their build.
No new repeated-beta tag convention or version-code scheme is introduced here.

## Signing setup (owner-managed)

GitHub Actions needs these repository secrets: `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Configure them in repository
Settings → Secrets and variables → Actions. Never put their values in a PR,
issue, chat, build log or committed file. `.jks` and `.keystore` files are ignored.
Keep an independent secure backup of the existing production keystore. Reuse
that exact signing identity for upgrades, not a newly generated key.

The workflow checks that all four secrets are present, creates a temporary key
with restricted permissions, and deletes it even after a failed build. Signing
values are scoped to the signing steps, not tests or publishing. Secret presence
and signing continuity have **not** been verified simply by merging this PR.

## Automated gates

Before signing: validate the tag, run packaging unit tests, build debug and
instrumentation APKs, run JVM tests and debug/release lint. Compilation of the
instrumentation APK is not execution on a device.

After signing: require exactly one APK, run `apksigner verify`, reject a debug
package/debuggable APK, and compare package ID (`com.jaguar.gearbox`), versionName
and versionCode against the tag. A SHA-256 checksum is generated and checked
again before attaching the APK and `SHA256SUMS` to the GitHub draft.

The build job has read-only repository permissions. Only the isolated draft job
can write releases. Failed gates prevent that job. No key or mapping file is
attached to the draft. The minification mapping is retained as an Actions
artifact for 90 days; diagnostic reports for 14 days and signed artifacts for 30
days. Archive each release's mapping securely beyond the retention period if
long-term crash decoding is needed. Actions artifact visibility follows repository
access rules; these artifacts are not a secrets vault.

## Mandatory manual checks before publishing the draft

1. Confirm the intended commit/tag and release notes; check successful gates.
2. Download the draft APK and `SHA256SUMS`; verify `sha256sum --check SHA256SUMS`
   (Windows: `Get-FileHash <apk> -Algorithm SHA256`).
3. Compare `apksigner verify --print-certs` certificate SHA-256 digests on the
   old distributed APK and the new one. A valid signature alone does **not** prove
   upgrade compatibility or ownership of the original signing key.
4. Install over the currently distributed signed app **without uninstalling**.
   Verify existing data, settings, widgets and timers are retained, then test a
   clean installation separately. Debug builds use `com.jaguar.gearbox.beta` and
   cannot prove upgrade compatibility for the release package.
5. Smoke-test on physical supported Android devices: permissions denied/granted,
   timer notifications and reboot restoration, widgets, scanner/camera access,
   large font, dark mode, rotation, and offline operation.
6. Confirm the draft prerelease flag for beta, attach screenshots/known limitations
   as appropriate, then publish manually only when satisfied.

## Versioning limitation retained intentionally

The current code is `major * 10000 + minor * 100 + patch`, so `v2.30.0-beta`
and `v2.30.0` both yield **23000**. This phase does not silently change that
policy. Minor and patch components above 99 are rejected to avoid code
collisions; tags must yield an Android-supported positive versionCode. Before
shipping another build with the same numeric version, explicitly decide the
upgrade/versioning policy. Do not infer monotonic upgrades from a changed suffix.

## Validation boundaries

PR CI can test unsigned builds, lint and packaging logic without signing secrets.
It does not run the tag-only signed workflow or create any release. Its first
owner-approved tag run is still required to verify the complete signing and draft
publication path. A green PR is not a substitute for signed-device upgrade tests.
