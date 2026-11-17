import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("measure_cache", Path(__file__).resolve().parents[1] / "measure_build_cache.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class CacheMeasurementTests(unittest.TestCase):
    def fake_result(self, code=0):
        return {"exit_code": code, "duration_seconds": 1.0, "task_outcomes": {}, "task_counts": {}, "cache_hits": 0}

    def test_commands_isolate_task_cache_and_preserve_user_home(self):
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp)
            calls = []
            def runner(command, cwd, log, env):
                calls.append((command, env))
                return self.fake_result()
            with patch.object(MODULE.subprocess, "check_output", side_effect=["abc123\n", ""]):
                path, success = MODULE.measure(workspace, workspace / "build" / "measure", runner)
            self.assertTrue(success)
            report = json.loads(path.read_text())
            self.assertEqual(["warmup", "baseline", "cold", "warm"], [r["name"] for r in report["runs"]])
            self.assertEqual(8, len(calls))
            for index, (command, env) in enumerate(calls):
                self.assertIn("--no-configuration-cache", command)
                self.assertNotIn("KEYSTORE_PASSWORD", env)
                self.assertNotIn("--gradle-user-home", command)
                if index % 2 == 0:
                    self.assertIn(":app:clean", command)
                    self.assertIn("--no-build-cache", command)
                else:
                    self.assertIn(":app:compileDebugKotlin", command)
                    self.assertIn(":app:testDebugUnitTest", command)
                    self.assertIn("--build-cache" if index >= 5 else "--no-build-cache", command)
            init = (path.parent / "measurement.init.gradle").read_text()
            self.assertIn("remote.enabled = false", init)
            self.assertTrue((path.parent / "task-cache").is_dir())

    def test_failure_stops_trials_and_preserves_report(self):
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp)
            calls = []
            def runner(*args):
                calls.append(args)
                return self.fake_result(1 if len(calls) == 4 else 0)
            with patch.object(MODULE.subprocess, "check_output", side_effect=["sha\n", ""]):
                path, success = MODULE.measure(workspace, workspace / "build", runner)
            self.assertFalse(success)
            self.assertEqual(4, len(calls))
            self.assertEqual("baseline build failed", json.loads(path.read_text())["failure"])

    def test_log_is_bounded_but_all_output_drained_and_cache_hits_counted(self):
        class Process:
            stdout = io.BytesIO(b"noise\n" * 50 + b"> Task :app:compileDebugKotlin FROM-CACHE\n> Task :app:testDebugUnitTest\n")
            def wait(self):
                self.drained = self.stdout.read() == b""
                return 0
        process = Process()
        with tempfile.TemporaryDirectory() as temp, patch.object(MODULE.subprocess, "Popen", return_value=process), patch.object(MODULE, "MAX_LOG_BYTES", 16):
            log = Path(temp) / "test.log"
            result = MODULE.run_command(["gradlew"], Path(temp), log, {})
            self.assertTrue(process.drained)
            self.assertEqual(16, log.stat().st_size)
            self.assertTrue(result["log_truncated"])
            self.assertEqual(1, result["cache_hits"])
            self.assertEqual(1, result["task_counts"]["EXECUTED"])

    def test_external_output_and_signing_environment_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            workspace = Path(temp) / "repo"
            workspace.mkdir()
            with self.assertRaises(ValueError):
                MODULE.within_workspace(Path(temp) / "outside", workspace)
            (workspace / "escape").symlink_to(Path(temp), target_is_directory=True)
            with self.assertRaises(ValueError):
                MODULE.within_workspace(workspace / "escape" / "out", workspace)
        with patch.dict(os.environ, {"KEYSTORE_PATH": "private", "KEYSTORE_PASSWORD": "secret", "GRADLE_USER_HOME": "/same/cache"}):
            env = MODULE.clean_environment()
            self.assertNotIn("KEYSTORE_PATH", env)
            self.assertNotIn("KEYSTORE_PASSWORD", env)
            self.assertEqual("/same/cache", env["GRADLE_USER_HOME"])


if __name__ == "__main__":
    unittest.main()
