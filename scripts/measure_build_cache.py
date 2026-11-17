#!/usr/bin/env python3
"""Opt-in local task-cache measurement. Does not enable caching for normal builds."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time
import uuid

TASKS = [":app:compileDebugKotlin", ":app:testDebugUnitTest"]
MAX_LOG_BYTES = 2 * 1024 * 1024
TASK_LINE = re.compile(r"^> Task (:[^\s]+)(?: (FROM-CACHE|UP-TO-DATE|NO-SOURCE|SKIPPED|FAILED))?\s*$")
INIT_SCRIPT = """// This init script applies only to this experiment, including included builds.
settingsEvaluated { settings ->
    settings.buildCache {
        local {
            directory = new File(System.getProperty('gearbox.measurementCache'))
            enabled = true
            push = true
        }
    }
    if (settings.buildCache.remote != null) settings.buildCache.remote.enabled = false
}
"""


def within_workspace(path, workspace):
    resolved = Path(path).resolve()
    if not resolved.is_relative_to(workspace.resolve()):
        raise ValueError("Measurement output must stay inside the repository workspace")
    return resolved


def clean_environment():
    env = os.environ.copy()
    # Debug-only experiment never needs signing material. Do not print environment values.
    for key in ("KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEYSTORE_BASE64", "KEY_ALIAS", "KEY_PASSWORD",
                "DEVELOPER_PREVIEW_RUN_NUMBER", "DEVELOPER_PREVIEW_SHA"):
        env.pop(key, None)
    return env


def run_command(command, workspace, log_path, env):
    start = time.monotonic()
    tasks = {}
    written = 0
    truncated = False
    outcomes_truncated = False
    with log_path.open("xb") as log:
        process = subprocess.Popen(command, cwd=workspace, env=env,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        assert process.stdout is not None
        for chunk in iter(lambda: process.stdout.readline(16384), b""):
            remaining = MAX_LOG_BYTES - written
            if remaining > 0:
                saved = chunk[:remaining]
                log.write(saved)
                written += len(saved)
            if len(chunk) > remaining:
                truncated = True
            match = TASK_LINE.fullmatch(chunk.decode("utf-8", errors="replace").strip())
            if match:
                if len(match[1]) <= 512 and (match[1] in tasks or len(tasks) < 1024):
                    tasks[match[1]] = match[2] or "EXECUTED"
                else:
                    outcomes_truncated = True
        code = process.wait()
    counts = {}
    for outcome in tasks.values():
        counts[outcome] = counts.get(outcome, 0) + 1
    return {"exit_code": code, "duration_seconds": round(time.monotonic() - start, 3),
            "task_outcomes": tasks, "task_counts": counts,
            "cache_hits": counts.get("FROM-CACHE", 0), "task_outcomes_truncated": outcomes_truncated,
            "log": log_path.name, "log_bytes": written, "log_truncated": truncated}


def measure(workspace, output_dir, runner=run_command):
    workspace = Path(workspace).resolve()
    output_dir = within_workspace(output_dir, workspace)
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=workspace, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=workspace, text=True).strip())
    run_dir = output_dir / ("run-" + uuid.uuid4().hex)
    run_dir.mkdir(parents=True, exist_ok=False)
    cache_dir = run_dir / "task-cache"
    cache_dir.mkdir()
    init = run_dir / "measurement.init.gradle"
    init.write_text(INIT_SCRIPT, encoding="utf-8")
    report = {"schema": 1, "source_sha": sha, "dirty_worktree": dirty, "tasks": TASKS,
              "runs": [], "success": False,
              "controls": ["One unsigned no-cache warm-up resolves dependencies before timing baseline",
                           "Each timed trial follows :app:clean; no other output/cache deletion",
                           "Cold and warm share one newly created local task cache; remote cache disabled",
                           "Gradle user-home/dependency cache retained; configuration cache disabled"],
              "caveats": ["Single ordered trial per condition, not statistically rigorous",
                          "Daemon/JIT/filesystem warm-up can favor later trials",
                          "buildSrc/included-build outputs are not cleaned; app tasks are the measured scope",
                          "Cache-hit counts are observed outcomes, not a promised speedup"]}
    env = clean_environment()
    base = [str(workspace / "gradlew"), "--console=plain", "--no-configuration-cache",
            "--init-script", str(init), "-Dgearbox.measurementCache=" + str(cache_dir)]
    try:
        for name, cached in (("warmup", False), ("baseline", False), ("cold", True), ("warm", True)):
            clean = runner(base + [":app:clean", "--no-build-cache"], workspace, run_dir / (name + "-clean.log"), env)
            trial = {"name": name, "build_cache": cached, "clean": clean}
            report["runs"].append(trial)
            if clean["exit_code"] != 0:
                report["failure"] = name + " clean failed"
                break
            result = runner(base + TASKS + ["--build-cache" if cached else "--no-build-cache"],
                            workspace, run_dir / (name + ".log"), env)
            trial["build"] = result
            if result["exit_code"] != 0:
                report["failure"] = name + " build failed"
                break
        else:
            report["success"] = True
    except (OSError, subprocess.SubprocessError) as error:
        report["failure"] = type(error).__name__  # Do not echo arbitrary command/environment data.
    (run_dir / "results.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return run_dir / "results.json", report["success"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", default="build/cache-measurement")
    args = parser.parse_args()
    workspace = Path(__file__).resolve().parents[1]
    try:
        path, success = measure(workspace, workspace / args.output_dir)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        parser.exit(1, "Measurement setup failed: " + type(error).__name__ + "\n")
    print(path.relative_to(workspace))
    return 0 if success else 1


if __name__ == "__main__":
    raise SystemExit(main())
