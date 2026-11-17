"""Validate isolated signed developer Preview APKs. Never accesses signing secrets."""
import argparse
from datetime import datetime
import hashlib
import json
import re
import shutil
import subprocess
from pathlib import Path

APP_ID = "com.jaguar.gearbox.devpreview"
APP_LABEL = "GearBox Dev Preview"


def preview_metadata(run_number, source_sha):
    if not re.fullmatch(r"[1-9][0-9]{0,9}", str(run_number)):
        raise ValueError("Run number must be a positive ASCII integer")
    run = int(run_number)
    code = 1_000_000 + run
    if code > 2_100_000_000:
        raise ValueError("Preview version code exceeds Android distribution limit")
    if not re.fullmatch(r"[0-9a-f]{40}", source_sha):
        raise ValueError("Expected full lowercase source SHA")
    return {"app_id": APP_ID, "run_number": run, "source_sha": source_sha,
            "version_code": code, "version_name": f"preview.{run}+{source_sha[:7]}"}


def validate_package(badging, tree, metadata):
    package = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging, re.M)
    if not package or package.groups() != (APP_ID, str(metadata['version_code']), metadata['version_name']):
        raise ValueError("Preview APK identity/version mismatch")
    if re.search(r"^application-debuggable(?:\s|$)", badging, re.M):
        raise ValueError("Preview must not be debuggable")
    if f"application-label:'{APP_LABEL}'" not in badging.splitlines():
        raise ValueError("Preview application label mismatch")
    launcher = re.search(r"^launchable-activity: name='([^']+)'", badging, re.M)
    if not launcher or launcher.group(1) != "com.jaguar.gearbox.MainActivity":
        raise ValueError("Preview launcher mismatch")
    if re.search(r"E: instrumentation(?:\s|$)", tree):
        raise ValueError("Distribution APK must not contain instrumentation")
    authorities = re.findall(r'android:authorities[^=]*="([^"]+)"', tree)
    if APP_ID + '.fileprovider' not in authorities or any(
            not authority.startswith(APP_ID + '.') for entry in authorities for authority in entry.split(';')):
        raise ValueError("Preview provider authorities are not isolated")
    return authorities


def certificate_fingerprint(output):
    fingerprints = re.findall(r"^Signer #[0-9]+ certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$", output, re.M)
    if len(fingerprints) != 1:
        raise ValueError("Expected exactly one verified signing certificate")
    return fingerprints[0].lower()


def source_commit_date(source_sha):
    """Committer timestamp, stable across reruns; never the packaging wall clock."""
    resolved = subprocess.check_output(['git', 'rev-parse', '--verify', source_sha + '^{commit}'], text=True).strip()
    if resolved != source_sha:
        raise ValueError('Source SHA does not resolve to the exact commit')
    value = subprocess.check_output(['git', 'show', '-s', '--format=%cI', source_sha], text=True).strip()
    if not re.fullmatch(r'[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[+-][0-9]{2}:[0-9]{2}', value):
        raise ValueError('Expected ISO source commit timestamp')
    datetime.fromisoformat(value)  # Reject invalid calendar/time fields as well.
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run_number')
    parser.add_argument('source_sha')
    parser.add_argument('--apk-dir', type=Path)
    parser.add_argument('--build-tools', type=Path)
    parser.add_argument('--output', type=Path, default=Path('dist'))
    args = parser.parse_args()
    metadata = preview_metadata(args.run_number, args.source_sha)
    if args.apk_dir is None:
        print(json.dumps(metadata))
        return
    if args.build_tools is None:
        parser.error('--build-tools is required with --apk-dir')
    apks = list(args.apk_dir.glob('*.apk'))
    if len(apks) != 1:
        raise ValueError('Expected exactly one Preview APK')
    apk = apks[0]
    cert = subprocess.check_output([str(args.build_tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], text=True)
    fingerprint = certificate_fingerprint(cert)
    badging = subprocess.check_output([str(args.build_tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
    tree = subprocess.check_output([str(args.build_tools / 'aapt'), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
    authorities = validate_package(badging, tree, metadata)
    name = f"{APP_ID}-{metadata['version_name']}.apk"
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    manifest = {**metadata, 'source_date': source_commit_date(args.source_sha), 'apk': name, 'sha256': digest, 'certificate_sha256': fingerprint,
                'label': APP_LABEL, 'provider_authorities': authorities, 'debuggable': False}
    args.output.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(apk, args.output / name)
    (args.output / 'SHA256SUMS').write_text(f'{digest}  {name}\n')
    (args.output / 'preview-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(manifest))


if __name__ == '__main__':
    main()
