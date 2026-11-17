"""Static channel boundaries; real Actions/signing execution is a separate verification."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class PreviewWorkflowTest(unittest.TestCase):
    def test_publisher_is_main_push_only_and_serialized(self):
        text = (ROOT / '.github/workflows/preview.yml').read_text()
        self.assertIn('push:\n    branches: [main]', text)
        for trigger in ('pull_request:', 'pull_request_target:', 'workflow_run:', 'workflow_dispatch:'):
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
        self.assertIn('-PdeveloperPreview=true', text)

    def test_existing_release_is_tag_driven_and_separate(self):
        text = (ROOT / '.github/workflows/release.yml').read_text()
        self.assertIn('tags: ["v*"]', text)
        self.assertNotIn('developerPreview', text)
        self.assertIn('draft: true', text)
        self.assertIn('make_latest: false', text)
