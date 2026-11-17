import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / "classify_android_changes.py"
spec = importlib.util.spec_from_file_location("classifier", SCRIPT)
classifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(classifier)
BASE, HEAD = "a" * 40, "b" * 40


class ClassifierTests(unittest.TestCase):
    def classify_paths(self, paths):
        result = subprocess.CompletedProcess([], 0, b"\0".join(paths) + b"\0", b"")
        with patch.object(classifier.subprocess, "run", return_value=result) as run:
            answer = classifier.classify("pull_request", BASE, HEAD)
        self.assertEqual(run.call_args.args[0], ["git", "diff", "--no-renames", "--name-only", "-z", BASE, HEAD, "--"])
        self.assertNotIn("shell", run.call_args.kwargs)
        return answer[0]

    def test_allowlist_and_special_filenames(self):
        self.assertTrue(self.classify_paths([b"README.md", b"CONTRIBUTING.md", b"docs/guide.md", b"docs/a/b.md", b"docs/space newline\nquote'$.md"]))

    def test_code_workflow_unknown_and_mixed(self):
        for path in [b"app/Main.kt", b".github/workflows/android.yml", b"docs/image.png", b"readme.md", b"README.md.bak", b"other/docs/a.md", b"docs/../a.md", b"docs//a.md", b"docs/a.MD", b""]:
            with self.subTest(path=path):
                self.assertFalse(self.classify_paths([b"docs/good.md", path]))

    def test_invalid_sha_and_events_do_not_run_git(self):
        with patch.object(classifier.subprocess, "run") as run:
            for event in ["push", "workflow_dispatch", "pull_request_target", "", "unknown"]:
                self.assertFalse(classifier.classify(event, BASE, HEAD)[0])
            for sha in ["", "a" * 39, "a" * 41, "z" * 40, "--help", BASE + "\n"]:
                self.assertFalse(classifier.classify("pull_request", sha, HEAD)[0])
                self.assertFalse(classifier.classify("pull_request", BASE, sha)[0])
            run.assert_not_called()

    def test_failure_empty_and_malformed(self):
        for code, output in [(1, b"docs/a.md\0"), (0, b""), (0, b"docs/a.md")]:
            with patch.object(classifier.subprocess, "run", return_value=subprocess.CompletedProcess([], code, output, b"")):
                self.assertFalse(classifier.classify("pull_request", BASE, HEAD)[0])
        with patch.object(classifier.subprocess, "run", side_effect=OSError("missing git")):
            self.assertFalse(classifier.classify("pull_request", BASE, HEAD)[0])

    def test_real_deletion_rename_and_output_file(self):
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=repo).decode().strip()
            git("init", "-q")
            git("config", "user.email", "test@example.invalid")
            git("config", "user.name", "Test")
            (repo / "docs").mkdir()
            (repo / "docs/old.md").write_text("same content")
            (repo / "source.kt").write_text("source")
            git("add", ".")
            git("commit", "-qm", "base")
            base = git("rev-parse", "HEAD")
            (repo / "docs/old.md").unlink()
            git("add", "-A")
            git("commit", "-qm", "delete docs")
            deleted = git("rev-parse", "HEAD")
            output = repo / "outputs"
            def cli(head):
                return subprocess.check_output(["python3", str(SCRIPT), "--event", "pull_request", "--base", base, "--head", head], cwd=repo, env={**os.environ, "GITHUB_OUTPUT": str(output)}).decode()
            self.assertIn("docs_only=true", cli(deleted))
            git("mv", "source.kt", "docs/source.md")
            git("commit", "-qm", "rename source to docs")
            self.assertIn("docs_only=false", cli(git("rev-parse", "HEAD")))
            self.assertEqual(output.read_text().count("docs_only="), 2)


if __name__ == "__main__":
    unittest.main()
