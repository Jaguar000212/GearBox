"""Validate and package the GitHub-only release APK. Never reads signing secrets."""
import argparse
import hashlib
import json
import re
import shutil
import subprocess
from pathlib import Path

APP_ID = "com.jaguar.gearbox"
TAG_PATTERN = re.compile(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-beta)?")


def release_metadata(tag):
    match = TAG_PATTERN.fullmatch(tag)
    if not match:
        raise ValueError("Expected vMAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH-beta")
    major, minor, patch = map(int, match.group(1, 2, 3))
    base = major * 10000 + minor * 100 + patch
    if major > 20999 or minor > 99 or patch > 99 or base == 0:
        raise ValueError("Tag requires major 0..20999, minor/patch 0..99 and a nonzero version")
    code = base * 10 + (0 if match.group(4) else 1)
    return {"version_name": tag[1:], "version_code": code,
            "prerelease": bool(match.group(4))}


def validate_package(badging, metadata):
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.MULTILINE)
    if not package or package.groups() != (APP_ID, str(metadata["version_code"]), metadata["version_name"]):
        raise ValueError("APK package/version metadata does not match release tag")
    if re.search(r"^application-debuggable(?:\s|$)", badging, re.MULTILINE):
        raise ValueError("Refusing a debuggable release APK")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tag")
    parser.add_argument("--apk-dir", type=Path)
    parser.add_argument("--build-tools", type=Path)
    parser.add_argument("--output", type=Path, default=Path("dist"))
    args = parser.parse_args()
    metadata = release_metadata(args.tag)
    if args.apk_dir is None:
        print(json.dumps(metadata))
        return
    if args.build_tools is None:
        parser.error("--build-tools is required with --apk-dir")
    apks = list(args.apk_dir.glob("*.apk"))
    if len(apks) != 1:
        raise ValueError(f"Expected exactly one release APK, found {len(apks)}")
    apk = apks[0]
    # A successful Gradle build alone does not prove the APK was signed.
    subprocess.run([str(args.build_tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk)], check=True)
    badging = subprocess.check_output([str(args.build_tools / "aapt"), "dump", "badging", str(apk)], text=True)
    validate_package(badging, metadata)
    args.output.mkdir(parents=True, exist_ok=True)
    target = args.output / f"{APP_ID}-{args.tag}.apk"
    shutil.copyfile(apk, target)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    (args.output / "SHA256SUMS").write_text(f"{digest}  {target.name}\n")
    print(json.dumps({**metadata, "apk": str(target), "sha256": digest}))


if __name__ == "__main__":
    main()
