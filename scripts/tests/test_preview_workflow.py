"""Static channel boundaries; real Actions/signing execution is a separate verification."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class PreviewWorkflowTest(unittest.TestCase):
    def test_publisher_is_manual_main_only_and_serialized(self):
        text = (ROOT / '.github/workflows/preview.yml').read_text()
        self.assertIn('  workflow_dispatch:', text)
        self.assertEqual(3, text.count("if: github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main'"))
        for trigger in ('pull_request:', 'pull_request_target:', 'workflow_run:', '  push:', '  schedule:'):
            self.assertNotIn(trigger, text)
        self.assertIn('cancel-in-progress: false', text)
        self.assertEqual(1, text.count('contents: write'))
        self.assertIn('needs: build', text)
        self.assertIn('ref: ${{ github.sha }}', text)
        self.assertIn('persist-credentials: false', text)
        self.assertIn('scripts/preview_artifact.py', text)
        self.assertIn('scripts/publish_preview.py', text)
        self.assertIn('if: always()\n        run: rm -f "$RUNNER_TEMP/preview.jks"', text)

    def test_regular_ci_has_no_preview_signing_or_numbered_distribution(self):
        text = (ROOT / '.github/workflows/ci.yml').read_text()
        self.assertNotIn('secrets.KEYSTORE', text)
        self.assertNotIn('previewCheckpoint=', text)
        self.assertNotIn('scripts/publish_preview.py', text)
        self.assertIn('uses: ./.github/actions/android-checks', text)
        self.assertIn('push:\n    branches: [main]', text)
        shared = (ROOT / '.github/actions/android-checks/action.yml').read_text()
        self.assertIn('-PdeveloperPreview=true', shared)

    def test_existing_release_is_tag_driven_and_separate(self):
        text = (ROOT / '.github/workflows/release.yml').read_text()
        self.assertIn('tags: ["v*"]', text)
        self.assertNotIn('developerPreview', text)
        self.assertIn('draft: true', text)
        self.assertIn('make_latest: false', text)


class ConsolidationTest(unittest.TestCase):
    def test_main_preview_keeps_identity_counter_and_full_checks(self):
        preview = (ROOT / '.github/workflows/preview.yml').read_text()
        ci = (ROOT / '.github/workflows/ci.yml').read_text()
        self.assertTrue(preview.startswith('name: Developer Preview\n'))
        self.assertIn('  workflow_dispatch:', preview)
        self.assertNotIn('  push:', preview)
        self.assertIn('push:\n    branches: [main]', ci)
        self.assertIn('needs: checks', preview)
        self.assertIn('needs: build', preview)
        self.assertIn('uses: ./.github/actions/android-checks', preview)
        for source in (ci, preview):
            self.assertIn('name: Android build, tests and lint', source)
        self.assertIn('cancel-in-progress: true', ci)
        self.assertIn('cancel-in-progress: false', preview)
        self.assertNotIn('classify_android_changes', preview)
        self.assertIn('DEVELOPER_PREVIEW_RUN_NUMBER: ${{ github.run_number }}', preview)

    def test_shared_checks_and_artifacts_remain_available(self):
        shared = (ROOT / '.github/actions/android-checks/action.yml').read_text()
        for task in (':app:assembleDebug', ':app:assembleDebugAndroidTest', ':app:assembleRelease',
                     ':app:testDebugUnitTest', ':app:testReleaseUnitTest', ':app:lintDebug', ':app:lintRelease'):
            self.assertIn(task, shared)
        self.assertIn('scripts/check_apk_identity.py', shared)
        self.assertIn('validate_package(', shared)
        self.assertIn('retention-days: 14', shared)
        self.assertIn('retention-days: 7', shared)
        self.assertEqual(2, shared.count("inputs.upload-debug-apks == 'true'"))
        ci = (ROOT / '.github/workflows/ci.yml').read_text()
        self.assertIn('upload_debug_apks:', ci)
        self.assertIn("inputs.upload_debug_apks", ci)
        self.assertIn('cache-read-only: true', ci)
        self.assertNotIn('secrets.', ci)
        self.assertNotIn('org.gradle.caching=true', (ROOT / 'gradle.properties').read_text())
        self.assertIn('build/cache-measurement/run-*/results.json', ci)
        self.assertIn('build/cache-measurement/run-*/*.log', ci)
        self.assertNotIn('path: build/cache-measurement/', ci)
