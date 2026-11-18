# CI run consolidation and measurement

## Trigger and safety contract

- Pull requests: one **CI** workflow, with the existing **Android build, tests and lint** job/status name. New commits cancel obsolete PR runs. No signing secrets or publication jobs are present.
- Main pushes: one **Developer Preview** workflow, at its existing `preview.yml` path/identity. Its full Android checks feed the signed build and publisher. Main publication remains serialized and non-cancellable; a failed check keeps the prior release intact. The preview run counter, package, signer and version-code formula stay unchanged.
- Version tags: existing `release.yml` behavior is unchanged. It validates/signs and creates a draft release for owner review.
- Manual CI: full validation remains available, with optional debug/test APK upload and optional cache measurement. It never publishes Preview.

The shared local composite action retains the ordinary debug/test/release builds, JVM tests, lint, regular APK identity checks and unsigned developer-preview identity checks. Signed Preview still receives its own release unit tests/lint/signature/provenance verification. This reduces main workflow runs from two to one and removes repeated normal quality gates, not required checks. Preview now waits for the full validation stage, so publication latency can increase compared with the former parallel workflows.

Branch-protection settings could not be read through the available integration. No protection settings were changed; PR workflow and check/job names are preserved. Verify repository-required status configuration if a future rename is proposed.

## Conservative docs-only PR path

Only changes entirely within `docs/**/*.md`, root `README.md` or root `CONTRIBUTING.md` qualify. Empty or failed diffs, malformed commit IDs, workflows, actions, scripts, build files, manifests, resources, source code and mixed changes take the full path. Git diff disables rename detection so a renamed/deleted source path cannot disappear behind a Markdown destination. Fixed classifier outputs never interpolate filenames into shell commands or workflow output.

The same status job still runs Python guardrails and explicitly records that Android tasks were not run. Main pushes and manual runs always use full validation, including documentation-only main pushes: Preview publication now requires a separate explicit manual run on main.

## Artifacts on demand

Normal diagnostic reports retain 14 days. Preview distribution retains 14 days, and its diagnostics/obfuscation mapping retain 30 days. Existing artifacts are not deleted by this change.

Debug and instrumentation APK uploads are now opt-in: run **CI → Run workflow**, select the desired branch, and enable **Upload debug and instrumentation APKs**. The same APK/checksum artifact format and 7-day retention remain. Full checks still build both APKs even when they are not uploaded. Only the main APK is needed for ordinary manual testing; use a disposable device/profile for instrumentation.

## Cache experiment, not a default switch

`gradle/actions/setup-gradle` already caches dependency/Gradle state. Normal task-output caching has not been enabled. To measure it, manually run CI with **Run the controlled build-cache experiment** enabled. The separate read-only job runs a no-cache dependency warm-up, then no-cache baseline, cold-cache and warm-cache debug Kotlin/unit-test trials. Each trial cleans only app-generated outputs. A fresh experiment-local task cache is used; remote and configuration caches are disabled. Signing variables are stripped.

The artifact contains bounded logs and JSON timings/cache-hit counts, not cache contents. Dependency/JIT/filesystem warm-up and a single ordered trial can bias results; the report states these limitations. No speedup or permanent cache setting should be claimed until actual measurements are reviewed. Compare correctness and repeated runs before deciding whether to enable caching.

## Advisory rolling-release retention policy

The publisher still retains all previous assets. `scripts/preview_retention_plan.py` is an offline, read-only planner over a supplied complete release/asset inventory. It proposes protecting the current bundle and four recent complete bundles, and flags older bundles only after all their timestamps exceed 14 days. Unknown, incomplete, ambiguous or staged bundles require review.

A structurally complete old bundle is not proof of successful historical publication or signature verification. Candidates are **not deletion authorization**. The planner has no network mutation/deletion path, no scheduled cleanup is enabled, and no existing release asset is removed. Any future permanent deletion requires a separate owner confirmation naming the exact assets after rechecking current protection and provenance.

## Baseline, not a billing estimate

Before consolidation, a sampled PR consumed 7m15s of runner time; its main CI consumed 9m14s and Preview build/publish 7m07s, totaling 23m36s across the PR and merge. Timing varies with caches and load. The sampled cycle produced about 44.5 MB of compressed artifacts, mostly duplicate debug/test APK archives. GearBox is public and uses standard Ubuntu-hosted runners, whose compute is free under GitHub's current public-repository policy. No account billing total or dollar saving has been verified.
