import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("apk_identity", Path(__file__).parents[1] / "check_apk_identity.py")
identity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(identity)

class ApkIdentityTest(unittest.TestCase):
    def values(self):
        package = "com.jaguar.gearbox.preview"
        return [f"package: name='{package}'\napplication-label:'GearBox Preview'\nlaunchable-activity: name='com.jaguar.gearbox.MainActivity'",
                f'A: android:authorities(0x1)="{package}.fileprovider"',
                f"package: name='{package}.test'",
                f'A: android:targetPackage(0x2)="{package}"', package, True]
    def test_isolated_identity_passes(self):
        self.assertEqual(identity.validate(*self.values())["application_id"], "com.jaguar.gearbox.preview")
    def test_wrong_instrumentation_target_is_rejected(self):
        values = self.values(); values[3] = 'A: targetPackage="com.jaguar.gearbox.beta"'
        with self.assertRaises(ValueError): identity.validate(*values)
    def test_cross_install_authority_is_rejected(self):
        values = self.values(); values[1] += '\nA: authorities="com.jaguar.gearbox.beta.shared"'
        with self.assertRaises(ValueError): identity.validate(*values)
    def test_preview_label_is_required(self):
        values = self.values(); values[0] = values[0].replace("GearBox Preview", "GearBox")
        with self.assertRaises(ValueError): identity.validate(*values)

    def checkpoint_two(self):
        values = self.values()
        values[:5] = [value.replace("com.jaguar.gearbox.preview", "com.jaguar.gearbox.preview2").replace("GearBox Preview", "GearBox Preview 2") for value in values[:5]]
        return values

    def test_second_preview_identity_passes_with_explicit_label(self):
        result = identity.validate(*self.checkpoint_two(), preview_label="GearBox Preview 2")
        self.assertEqual(result["application_id"], "com.jaguar.gearbox.preview2")
        self.assertEqual(result["instrumentation_target"], "com.jaguar.gearbox.preview2")
        self.assertEqual(result["authorities"], ["com.jaguar.gearbox.preview2.fileprovider"])

    def test_second_preview_rejects_original_label(self):
        values = self.checkpoint_two()
        values[0] = values[0].replace("GearBox Preview 2", "GearBox Preview")
        with self.assertRaises(ValueError):
            identity.validate(*values, preview_label="GearBox Preview 2")

    def test_second_preview_cannot_use_first_preview_provider(self):
        values = self.checkpoint_two()
        values[1] = values[1].replace("preview2", "preview")
        with self.assertRaises(ValueError):
            identity.validate(*values, preview_label="GearBox Preview 2")

    def test_second_preview_cannot_target_first_preview(self):
        values = self.checkpoint_two()
        values[3] = values[3].replace("preview2", "preview")
        with self.assertRaises(ValueError):
            identity.validate(*values, preview_label="GearBox Preview 2")

    def test_second_preview_cannot_reuse_first_instrumentation_id(self):
        values = self.checkpoint_two()
        values[2] = values[2].replace("preview2", "preview")
        with self.assertRaises(ValueError):
            identity.validate(*values, preview_label="GearBox Preview 2")
