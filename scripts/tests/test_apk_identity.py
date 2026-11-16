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
