#!/usr/bin/env python3
"""Read-only APK identity guard: side-by-side variants must not target another install."""
import argparse
import json
import re
import subprocess


def package_name(badging):
    match = re.search(r"^package: name='([^']+)'", badging, re.MULTILINE)
    if not match:
        raise ValueError("APK has no package name")
    return match.group(1)


def validate(app_badging, app_xml, test_badging, test_xml, expected, preview=False, preview_label="GearBox Preview"):
    if package_name(app_badging) != expected:
        raise ValueError("Unexpected app application ID")
    if package_name(test_badging) != expected + ".test":
        raise ValueError("Unexpected instrumentation application ID")
    target = re.search(r'targetPackage[^=]*="([^"]+)"', test_xml)
    if target is None or target.group(1) != expected:
        raise ValueError("Instrumentation targets another installation")
    authorities = re.findall(r'authorities[^=]*="([^"]+)"', app_xml)
    if expected + ".fileprovider" not in authorities:
        raise ValueError("Missing app-scoped FileProvider")
    if any(not value.startswith(expected + ".") for item in authorities for value in item.split(";")):
        raise ValueError("Provider authority is not app-scoped")
    if "name='com.jaguar.gearbox.MainActivity'" not in app_badging:
        raise ValueError("Launch activity changed unexpectedly")
    if preview and f"application-label:'{preview_label}'" not in app_badging:
        raise ValueError("Preview must have a distinct launcher label")
    return {"application_id": expected, "instrumentation_target": target.group(1), "authorities": authorities}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--app", required=True)
    parser.add_argument("--test", required=True)
    parser.add_argument("--expected", required=True)
    parser.add_argument("--preview", action="store_true")
    parser.add_argument("--preview-label", default="GearBox Preview")
    parser.add_argument("--release")
    args = parser.parse_args()
    def dump(apk, mode):
        return subprocess.check_output([args.aapt, "dump", mode, apk] + (["AndroidManifest.xml"] if mode == "xmltree" else []), text=True)
    result = validate(dump(args.app, "badging"), dump(args.app, "xmltree"), dump(args.test, "badging"), dump(args.test, "xmltree"), args.expected, args.preview, args.preview_label)
    if args.release:
        release = package_name(dump(args.release, "badging"))
        if release != "com.jaguar.gearbox":
            raise ValueError("Release application ID changed")
        result["release_application_id"] = release
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
