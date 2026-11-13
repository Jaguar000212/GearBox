import importlib.util
from pathlib import Path
import unittest
import tempfile
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("release_artifact", Path(__file__).parents[1] / "release_artifact.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseMetadataTest(unittest.TestCase):
    def test_approved_beta_tag(self):
        self.assertEqual(release.release_metadata("v2.30.0-beta"),
                         {"version_name": "2.30.0-beta", "version_code": 23000, "prerelease": True})

    def test_stable(self):
        self.assertFalse(release.release_metadata("v2.30.1")["prerelease"])

    def test_invalid_tags_and_colliding_components(self):
        for tag in ("v2.30", "2.30.0", "v2.30.0-beta.1", "v2.30.0-rc", "v02.30.0", "v2.100.0", "v2.30.100", "v0.0.0", "v210001.0.0", "v2.30.0\n", "v2.30.0;echo bad"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release.release_metadata(tag)

    def test_apk_metadata(self):
        metadata = release.release_metadata("v2.30.0-beta")
        valid = "package: name='com.jaguar.gearbox' versionCode='23000' versionName='2.30.0-beta' platformBuildVersionName='15'\n"
        release.validate_package(valid, metadata)
        for badging in ("", valid.replace("23000", "22900"), valid.replace("gearbox'", "gearbox.beta'"), valid.replace("2.30.0-beta", "2.30.0"), valid + "application-debuggable\n"):
            with self.subTest(badging=badging), self.assertRaises(ValueError):
                release.validate_package(badging, metadata)


class PackagingTest(unittest.TestCase):
    def test_packages_verified_apk_and_checksum(self):
        import hashlib
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            apk_dir = root / "apk"
            apk_dir.mkdir()
            (apk_dir / "app-release.apk").write_bytes(b"fixture APK")
            output = root / "dist"
            args = ["release_artifact.py", "v2.30.0-beta", "--apk-dir", str(apk_dir), "--build-tools", str(root / "tools"), "--output", str(output)]
            badging = "package: name='com.jaguar.gearbox' versionCode='23000' versionName='2.30.0-beta'"
            with patch("sys.argv", args), patch.object(release.subprocess, "run") as verify, patch.object(release.subprocess, "check_output", return_value=badging):
                release.main()
            verify.assert_called_once()
            self.assertTrue(verify.call_args.kwargs["check"])
            name = "com.jaguar.gearbox-v2.30.0-beta.apk"
            self.assertEqual((output / name).read_bytes(), b"fixture APK")
            self.assertEqual((output / "SHA256SUMS").read_text(), hashlib.sha256(b"fixture APK").hexdigest() + "  " + name + "\n")

    def test_unsigned_apk_does_not_produce_distribution(self):
        import subprocess
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "app.apk").write_bytes(b"unsigned fixture")
            output = root / "dist"
            args = ["release_artifact.py", "v2.30.0-beta", "--apk-dir", str(root), "--build-tools", str(root), "--output", str(output)]
            with patch("sys.argv", args), patch.object(release.subprocess, "run", side_effect=subprocess.CalledProcessError(1, "apksigner")), self.assertRaises(subprocess.CalledProcessError):
                release.main()
            self.assertFalse(output.exists())

    def test_missing_or_multiple_apks_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            args = ["release_artifact.py", "v2.30.0-beta", "--apk-dir", str(root), "--build-tools", str(root)]
            with patch("sys.argv", args), self.assertRaises(ValueError):
                release.main()
            (root / "one.apk").touch()
            (root / "two.apk").touch()
            with patch("sys.argv", args), self.assertRaises(ValueError):
                release.main()


if __name__ == "__main__":
    unittest.main()
