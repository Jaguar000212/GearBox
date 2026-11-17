import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('preview_artifact', Path(__file__).parents[1] / 'preview_artifact.py')
preview = importlib.util.module_from_spec(spec)
spec.loader.exec_module(preview)
SHA = 'a' * 40
CERT = 'b' * 64
META = preview.preview_metadata('12', SHA)
BADGING = (f"package: name='{preview.APP_ID}' versionCode='1000012' versionName='preview.12+aaaaaaa'\n"
           "application-label:'GearBox Dev Preview'\nlaunchable-activity: name='com.jaguar.gearbox.MainActivity'\n")
TREE = f'E: manifest\n  E: application\n    E: provider\n      A: android:authorities(0x01010018)="{preview.APP_ID}.fileprovider" (Raw: "{preview.APP_ID}.fileprovider")\n'


class PreviewTests(unittest.TestCase):
    def test_metadata_bounds(self):
        self.assertEqual(META['version_name'], 'preview.12+aaaaaaa')
        self.assertEqual(preview.preview_metadata('2099000000', SHA)['version_code'], 2100000000)
        for run in ('0', '-1', '01', '1\n', '١', '2099000001', '999999999999999'):
            with self.subTest(run=run), self.assertRaises(ValueError):
                preview.preview_metadata(run, SHA)
        for sha in ('a' * 7, 'z' * 40, SHA + '\n', '../' + SHA):
            with self.assertRaises(ValueError):
                preview.preview_metadata('1', sha)

    def test_identity_guards(self):
        preview.validate_package(BADGING, TREE, META)
        bad_cases = [BADGING.replace(preview.APP_ID, 'com.jaguar.gearbox'),
                     BADGING.replace('1000012', '1000011'), BADGING.replace('aaaaaaa', 'bbbbbbb'),
                     BADGING + 'application-debuggable\n', BADGING.replace('Dev Preview', 'Beta'),
                     BADGING.replace('MainActivity', 'OtherActivity')]
        for bad in bad_cases:
            with self.assertRaises(ValueError):
                preview.validate_package(bad, TREE, META)
        for tree in ('', TREE.replace(preview.APP_ID, 'com.jaguar.gearbox'), TREE + '\n E: instrumentation',
                     TREE + '\n A: android:authorities="foreign.provider"'):
            with self.assertRaises(ValueError):
                preview.validate_package(BADGING, tree, META)

    def test_certificate_guards(self):
        self.assertEqual(preview.certificate_fingerprint(f'Signer #1 certificate SHA-256 digest: {CERT}'), CERT)
        for text in ('', 'not signed', f'Signer #1 certificate SHA-256 digest: {CERT}\nSigner #2 certificate SHA-256 digest: {CERT}'):
            with self.assertRaises(ValueError):
                preview.certificate_fingerprint(text)

    def test_package_and_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'source.apk').write_bytes(b'APK fixture')
            output = root / 'dist'
            argv = ['preview_artifact.py', '12', SHA, '--apk-dir', str(root), '--build-tools', str(root), '--output', str(output)]
            with patch('sys.argv', argv), patch.object(preview.subprocess, 'check_output', side_effect=subprocess.CalledProcessError(1, 'apksigner')):
                with self.assertRaises(subprocess.CalledProcessError):
                    preview.main()
            self.assertFalse(output.exists())
            with patch('sys.argv', argv), patch.object(preview.subprocess, 'check_output', side_effect=[f'Signer #1 certificate SHA-256 digest: {CERT}', BADGING, TREE, SHA, '2026-11-17T12:30:00+00:00']) as calls:
                preview.main()
            self.assertEqual(calls.call_count, 5)
            manifest = json.loads((output / 'preview-manifest.json').read_text())
            self.assertEqual(manifest['source_sha'], SHA)
            self.assertEqual(manifest['source_date'], '2026-11-17T12:30:00+00:00')
            self.assertEqual(manifest['certificate_sha256'], CERT)
            self.assertEqual((output / manifest['apk']).read_bytes(), b'APK fixture')
            self.assertEqual((output / 'SHA256SUMS').read_text(), f"{manifest['sha256']}  {manifest['apk']}\n")

    def test_missing_or_multiple_apks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            argv = ['preview_artifact.py', '12', SHA, '--apk-dir', str(root), '--build-tools', str(root)]
            for names in ((), ('one.apk', 'two.apk')):
                for name in names:
                    (root / name).touch()
                with patch('sys.argv', argv), self.assertRaises(ValueError):
                    preview.main()

    def test_source_date_requires_exact_commit_and_valid_timestamp(self):
        with patch.object(preview.subprocess, 'check_output', side_effect=[SHA, '2026-11-17T12:30:00+05:30']):
            self.assertEqual(preview.source_commit_date(SHA), '2026-11-17T12:30:00+05:30')
        for values in (["b" * 40], [SHA, '2026-99-17T12:30:00+00:00'], [SHA, 'today']):
            with patch.object(preview.subprocess, 'check_output', side_effect=values), self.assertRaises(ValueError):
                preview.source_commit_date(SHA)
