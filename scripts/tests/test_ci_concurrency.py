"""Static policy guards plus representative group examples, not a GitHub expression interpreter."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
GROUP = "ci-${{ github.workflow }}-${{ github.event_name == 'pull_request' && format('pr-{0}', github.event.pull_request.number) || format('run-{0}-{1}', github.run_id, github.run_attempt) }}"
CANCEL = "${{ github.event_name == 'pull_request' }}"

def policy(event, run_id, attempt=1, pr=None):
    """Model the exact guarded expression to exercise intended grouping."""
    return ("ci-CI-" + (f"pr-{pr}" if event == "pull_request" else f"run-{run_id}-{attempt}"),
            event == "pull_request")

class CiConcurrencyTest(unittest.TestCase):
    def test_exact_workflow_policy(self):
        source = (ROOT / ".github/workflows/ci.yml").read_text()
        block = source.split("\nconcurrency:\n", 1)[1].split("\njobs:\n", 1)[0]
        self.assertEqual(block.strip(), f"group: {GROUP}\n  cancel-in-progress: {CANCEL}")
        self.assertNotIn("queue:", block)

    def test_same_pr_supersedes_but_different_pr_is_independent(self):
        self.assertEqual(policy("pull_request", 1, pr=90), policy("pull_request", 2, pr=90))
        self.assertNotEqual(policy("pull_request", 1, pr=90)[0], policy("pull_request", 2, pr=91)[0])
        self.assertTrue(policy("pull_request", 1, pr=90)[1])

    def test_non_pr_runs_and_attempts_are_independent(self):
        samples = [policy("push", 1), policy("push", 2),
                   policy("workflow_dispatch", 3), policy("workflow_dispatch", 4),
                   policy("workflow_dispatch", 4, attempt=2)]
        self.assertEqual(len(samples), len({group for group, _ in samples}))
        self.assertTrue(all(not cancel for _, cancel in samples))
        self.assertTrue(all(group != "developer-preview-publisher" for group, _ in samples))
