import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("prepare_update", Path(__file__).resolve().parents[1] / "prepare_update.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class UpdateDescriptorTest(unittest.TestCase):
    def test_release_is_bound_to_apk_version_digest_and_signers(self):
        result = module.descriptor(3, "1.2.0", "debug", 100, "a" * 64, {"b" * 64}, 26, "Aktualizator")
        self.assertEqual("CrooLyyTimer-1.2.0-debug.apk", result["apkName"])
        self.assertEqual("v1.2.0", result["tag"])
        self.assertEqual(["b" * 64], result["signerSha256"])
        with self.assertRaises(ValueError): module.descriptor(3, "../other", "debug", 100, "a" * 64, {"b" * 64}, 26, "")
        with self.assertRaises(ValueError): module.descriptor(3, "1.2.0", "debug", module.MAX_APK + 1, "a" * 64, {"b" * 64}, 26, "")
