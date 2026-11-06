import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import base64
from argon2.low_level import hash_secret_raw, Type

MODULE_PATH = Path(__file__).resolve().parents[1] / "set_admin_password.py"
spec = importlib.util.spec_from_file_location("set_admin_password", MODULE_PATH)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def verify(path, password):
    v = json.loads(path.read_text())
    actual = hash_secret_raw(password.encode(), base64.b64decode(v["salt"]), v["iterations"], v["memoryKiB"],
                             v["parallelism"], 32, Type.ID, version=v["version"])
    return actual == base64.b64decode(v["hash"])

class PasswordRotationTest(unittest.TestCase):
    def test_rotation_changes_salt_and_hash_without_logging_password(self):
        with tempfile.TemporaryDirectory() as temp:
            target = Path(temp) / "verifier.json"
            passwords = ("first-owner-test-password-123456", "second-owner-test-password-123456")
            previous = None
            for password in passwords:
                output = io.StringIO()
                with patch.object(module, "TARGET", target), patch("sys.argv", ["set_admin_password.py"]), \
                        patch("sys.stdin.isatty", return_value=True), patch("sys.stdout", output), \
                        patch.object(module.getpass, "getpass", return_value=password):
                    self.assertEqual(0, module.main())
                self.assertNotIn(password, output.getvalue())
                self.assertNotIn(password, target.read_text())
                self.assertTrue(verify(target, password))
                current = json.loads(target.read_text())
                if previous:
                    self.assertNotEqual(previous["salt"], current["salt"])
                    self.assertNotEqual(previous["hash"], current["hash"])
                    self.assertFalse(verify(target, passwords[0]))
                previous = current

if __name__ == "__main__":
    unittest.main()
