#!/usr/bin/env python3
"""Conservative local PR diff classifier. Any uncertainty runs Android checks."""
import argparse
import os
import re
import subprocess
from pathlib import Path

SHA = re.compile(r"[0-9a-fA-F]{40}\Z")


def is_documentation(path: bytes) -> bool:
    if path in (b"README.md", b"CONTRIBUTING.md"):
        return True
    parts = path.split(b"/")
    return (len(parts) >= 2 and parts[0] == b"docs"
            and all(part not in (b"", b".", b"..") for part in parts)
            and parts[-1].endswith(b".md"))


def classify(event: str, base: str, head: str) -> tuple[bool, str]:
    if event != "pull_request":
        return False, "event_requires_android"
    if not SHA.fullmatch(base) or not SHA.fullmatch(head):
        return False, "invalid_commit_sha"
    try:
        result = subprocess.run(
            ["git", "diff", "--no-renames", "--name-only", "-z", base, head, "--"],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return False, "git_diff_failed"
    if result.returncode:
        return False, "git_diff_failed"
    raw = result.stdout
    if not raw:
        return False, "empty_diff"
    if not raw.endswith(b"\0"):
        return False, "malformed_diff"
    paths = raw[:-1].split(b"\0")
    if not all(is_documentation(path) for path in paths):
        return False, "non_documentation_change"
    return True, "documentation_only"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--event", default="")
    parser.add_argument("--base", default="")
    parser.add_argument("--head", default="")
    args = parser.parse_args()
    docs_only, reason = classify(args.event, args.base, args.head)
    # Fixed values only: never write untrusted filenames to workflow outputs/logs.
    output = f"docs_only={str(docs_only).lower()}\nreason={reason}\n"
    print(output, end="")
    if destination := os.environ.get("GITHUB_OUTPUT"):
        with Path(destination).open("a", encoding="utf-8") as stream:
            stream.write(output)


if __name__ == "__main__":
    main()
