"""Publish ONLY the rolling preview prerelease after a separately authorized green main build.

Tag movement and release-body updates cannot be atomic. Assets are versioned and verified
before either, and previous assets are never deleted. On interruption rerun the same build;
verify the tag/body agree before announcing success. The workflow must serialize publishers.
No APK inspection output, API response body, or credentials are logged on failure.
"""
import argparse
from datetime import datetime
import hashlib
import json
import os
import re
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

APP_ID = "com.jaguar.gearbox.devpreview"
MARKER = "<!-- gearbox-preview-v1:"
SHA = re.compile(r"[0-9a-f]{40}")
HEX = re.compile(r"[0-9a-f]{64}")


class PublishError(Exception):
    pass


class StaleBuild(PublishError):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class GitHub:
    def __init__(self, repository, token):
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository) or not token:
            raise PublishError("Repository and GITHUB_TOKEN are required")
        self.repository = repository
        self.base = "https://api.github.com/repos/" + repository
        self.token = token
        self.opener = urllib.request.build_opener(NoRedirect)

    def request(self, method, path, payload=None, *, data=None, content_type=None, accept="application/vnd.github+json"):
        url = path if path.startswith("https://") else self.base + path
        if urllib.parse.urlparse(url).netloc not in ("api.github.com", "uploads.github.com"):
            raise PublishError("Unexpected authenticated API host")
        if payload is not None:
            data = json.dumps(payload).encode()
            content_type = "application/json"
        headers = {"Authorization": "Bearer " + self.token, "Accept": accept,
                   "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "GearBox-preview-publisher"}
        if content_type:
            headers["Content-Type"] = content_type
        try:
            with self.opener.open(urllib.request.Request(url, data=data, headers=headers, method=method), timeout=120) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            if error.code == 404 and method == "GET":
                return None
            raise PublishError(f"GitHub {method} failed (HTTP {error.code})") from None
        except (OSError, ValueError):
            raise PublishError("GitHub request failed") from None

    def asset_bytes(self, asset, expected_size):
        url = self.base + "/releases/assets/" + str(asset["id"])
        request = urllib.request.Request(url, headers={"Authorization": "Bearer " + self.token,
            "Accept": "application/octet-stream", "User-Agent": "GearBox-preview-publisher"})
        try:
            try:
                response = self.opener.open(request, timeout=120)
            except urllib.error.HTTPError as error:
                if error.code not in (301, 302, 303, 307, 308):
                    raise PublishError("Asset verification download failed") from None
                location = error.headers.get("Location", "")
                parsed = urllib.parse.urlparse(location)
                if parsed.scheme != "https" or parsed.hostname != "release-assets.githubusercontent.com":
                    raise PublishError("Unexpected asset download destination")
                # Never forward the token to storage or follow another redirect.
                response = self.opener.open(urllib.request.Request(location), timeout=120)
            with response:
                return response.read(expected_size + 1)
        except (OSError, ValueError):
            raise PublishError("Asset verification download failed") from None


def owned_state(release):
    body = release.get("body") or ""
    if body.count(MARKER) != 1:
        raise PublishError("Existing preview release has no unique publisher ownership marker")
    try:
        state = json.loads(body.split(MARKER, 1)[1].split(" -->", 1)[0])
        if set(state) != {"sha", "version_code", "fingerprint", "assets", "source_date"}:
            raise ValueError()
        if not SHA.fullmatch(state["sha"]) or type(state["version_code"]) is not int or not HEX.fullmatch(state["fingerprint"]):
            raise ValueError()
        if not 1_000_000 < state["version_code"] <= 2_100_000_000:
            raise ValueError()
        validate_date(state["source_date"])
        prefix = f"gearbox-devpreview-{state['version_code']}-{state['sha']}"
        if not isinstance(state["assets"], dict) or set(state["assets"]) != {prefix + suffix for suffix in (".apk", ".sha256", ".json")}:
            raise ValueError()
        if any(not re.fullmatch(r"gearbox-devpreview-[0-9]+-[0-9a-f]{40}\.(apk|sha256|json)", name) or not HEX.fullmatch(digest)
               for name, digest in state["assets"].items()):
            raise ValueError()
        return state
    except (ValueError, KeyError, TypeError, AttributeError):
        raise PublishError("Invalid preview ownership marker") from None


def validate_date(value):
    if not isinstance(value, str) or not re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:Z|[+-]\d{2}:\d{2})", value):
        raise ValueError("Invalid source date")
    datetime.fromisoformat(value.replace("Z", "+00:00"))


def notes(state, repository):
    run = state["version_code"] - 1_000_000
    prefix = f"gearbox-devpreview-{state['version_code']}-{state['sha']}"
    download = f"https://github.com/{repository}/releases/download/preview/"

    return ("## GearBox Developer Preview\n\n"
        "Unstable, public developer build from main. Separate app installation; not a beta or stable release.\n\n"
        f"Version: `preview.{run}+{state['sha'][:7]}` · Run: `{run}`\n"
        f"Source commit date: `{state['source_date']}`\n"
        f"Commit: `{state['sha']}`\nVersion code: `{state['version_code']}`\n"
        f"Signing certificate SHA-256: `{state['fingerprint']}`\n\n"
        f"[Download current APK]({download}{prefix}.apk) · [SHA-256 checksum]({download}{prefix}.sha256) · [Build metadata]({download}{prefix}.json)\n\n"
        "Install this APK over your existing GearBox Dev Preview to upgrade. The package and signing certificate stay fixed. Previous versioned assets are retained below.\n\n"
        + MARKER + json.dumps(state, sort_keys=True, separators=(",", ":")) + " -->\n")


def all_assets(api, release_id):
    result = []
    for page in range(1, 101):
        batch = api.request("GET", f"/releases/{release_id}/assets?per_page=100&page={page}")
        if not isinstance(batch, list):
            raise PublishError("Cannot inventory release assets")
        result.extend(batch)
        if len(batch) < 100:
            return result
    raise PublishError("Too many retained assets; manual review required")


def verify_asset(api, asset, digest, size):
    if asset.get("state") != "uploaded" or asset.get("size") != size:
        raise PublishError("Asset upload size/state mismatch")
    data = api.asset_bytes(asset, size)
    if len(data) != size or hashlib.sha256(data).hexdigest() != digest:
        raise PublishError("Uploaded asset checksum mismatch")


def verify_interrupted_target(api, release_id, tag_sha, previous):
    """A moved tag may lead the body after PATCH failure. Trust only a complete retained
    publisher-shaped bundle with matching bytes, commit, signer, and increasing version.
    Never infer ownership merely from an arbitrary tag target or asset filename.
    """
    assets = all_assets(api, release_id)
    pattern = re.compile(r"gearbox-devpreview-([0-9]+)-" + re.escape(tag_sha) + r"\.json")
    candidates = [asset for asset in assets if pattern.fullmatch(asset.get("name", ""))]
    if len(candidates) != 1:
        raise PublishError("Interrupted preview tag lacks unique retained metadata")
    candidate = candidates[0]
    size = candidate.get("size")
    if candidate.get("state") != "uploaded" or type(size) is not int or not 0 < size <= 16384:
        raise PublishError("Invalid interrupted preview metadata size")
    raw = api.asset_bytes(candidate, size)
    try:
        metadata = json.loads(raw)
        code = int(pattern.fullmatch(candidate["name"])[1])
        run = code - 1_000_000
        if (len(raw) != size or set(metadata) != {"application_id", "commit", "version_code", "certificate_sha256", "apk_sha256", "version_name", "run_number", "source_date"} or
                metadata["application_id"] != APP_ID or metadata["commit"] != tag_sha or
                metadata["certificate_sha256"] != previous["fingerprint"] or
                type(metadata["version_code"]) is not int or metadata["version_code"] != code or
                type(metadata["run_number"]) is not int or metadata["run_number"] != run or
                metadata["version_name"] != f"preview.{run}+{tag_sha[:7]}" or
                not previous["version_code"] < code <= 2_100_000_000 or not HEX.fullmatch(metadata["apk_sha256"])):
            raise ValueError()
        validate_date(metadata["source_date"])
    except (ValueError, KeyError, TypeError):
        raise PublishError("Invalid interrupted preview metadata") from None
    prefix = candidate["name"][:-5]
    for suffix in (".apk", ".sha256"):
        matching = [asset for asset in assets if asset.get("name") == prefix + suffix]
        if len(matching) != 1:
            raise PublishError("Interrupted preview bundle incomplete")
        asset = matching[0]
        if type(asset.get("size")) is not int or not 0 < asset["size"] <= 128 * 1024 * 1024:
            raise PublishError("Interrupted preview asset size invalid")
        expected = metadata["apk_sha256"] if suffix == ".apk" else hashlib.sha256(
            (metadata["apk_sha256"] + "  " + prefix + ".apk\n").encode()).hexdigest()
        verify_asset(api, asset, expected, asset["size"])
    return code


def publish(api, apk, sha, run_number, fingerprint, source_date):
    try:
        validate_date(source_date)
    except ValueError:
        raise PublishError("Invalid source commit date") from None
    if not SHA.fullmatch(sha) or not HEX.fullmatch(fingerprint) or type(run_number) is not int or not 0 < run_number <= 2_099_000_000:
        raise PublishError("Invalid commit, workflow run number, or certificate fingerprint")
    code = 1_000_000 + run_number
    base = f"gearbox-devpreview-{code}-{sha}"
    payloads = {base + ".apk": apk,
                base + ".sha256": (hashlib.sha256(apk).hexdigest() + "  " + base + ".apk\n").encode(),
                base + ".json": (json.dumps({"application_id": APP_ID, "commit": sha, "version_code": code,
                    "certificate_sha256": fingerprint, "apk_sha256": hashlib.sha256(apk).hexdigest(),
                    "version_name": f"preview.{run_number}+{sha[:7]}", "run_number": run_number, "source_date": source_date}, sort_keys=True) + "\n").encode()}
    state = {"sha": sha, "version_code": code, "fingerprint": fingerprint, "source_date": source_date,
             "assets": {name: hashlib.sha256(data).hexdigest() for name, data in payloads.items()}}

    def check_main():
        head = api.request("GET", "/git/ref/heads/main")
        if not head or head.get("object", {}).get("sha") != sha:
            raise StaleBuild("Skipped stale main build")

    check_main()
    release = api.request("GET", "/releases/tags/preview")
    if release is None:
        # A draft need not yet have a public tag. Recover staging without creating duplicates.
        drafts = api.request("GET", "/releases?per_page=100") or []
        matches = [item for item in drafts if item.get("tag_name") == "preview"]
        if len(matches) > 1:
            raise PublishError("Ambiguous preview releases")
        release = matches[0] if matches else None
    tag = api.request("GET", "/git/ref/tags/preview")
    latest = api.request("GET", "/releases/latest")
    same_commit = False
    if release:
        if release.get("tag_name") != "preview" or release.get("prerelease") is not True or release.get("immutable") is not False:
            raise PublishError("Preview must be an explicitly mutable prerelease")
        if latest and latest.get("id") == release.get("id"):
            raise PublishError("Preview is marked latest; refusing to alter release")
        previous = owned_state(release)
        if previous["fingerprint"] != fingerprint:
            raise PublishError("Preview signing certificate changed")
        same_commit = previous["sha"] == sha
        if not same_commit and code <= previous["version_code"]:
            raise PublishError("Preview version code must increase")
        if same_commit and release.get("draft") and state != previous:
            raise PublishError("Draft rerun does not match staged artifact metadata")
        if (not tag and not release.get("draft")) or (tag and tag.get("object", {}).get("type") != "commit"):
            raise PublishError("Preview tag missing or not a lightweight commit ref")
        if tag and tag["object"]["sha"] != previous["sha"]:
            advanced = verify_interrupted_target(api, release["id"], tag["object"]["sha"], previous)
            if code < advanced or (code == advanced and tag["object"]["sha"] != sha):
                raise PublishError("Version must not regress interrupted preview target")
            if same_commit:
                raise PublishError("Cannot roll an interrupted preview back to the old release commit")
    else:
        if tag:
            raise PublishError("Orphan preview tag requires manual review")
        # Draft staging is private; no public release until uploads verify.
        release = api.request("POST", "/releases", {"tag_name": "preview", "target_commitish": sha,
            "name": "Preview", "body": notes(state, api.repository), "draft": True,
            "prerelease": True, "make_latest": "false"})

    assets = all_assets(api, release["id"])
    if same_commit and not release.get("draft"):
        for name, digest in previous["assets"].items():
            matches = [asset for asset in assets if asset.get("name") == name]
            if len(matches) != 1 or type(matches[0].get("size")) is not int or not 0 < matches[0]["size"] <= 128 * 1024 * 1024:
                raise PublishError("Published asset missing or invalid")
            verify_asset(api, matches[0], digest, matches[0]["size"])
        check_main()
        return "unchanged"
    for name, data in payloads.items():
        matches = [asset for asset in assets if asset.get("name") == name]
        if len(matches) > 1:
            raise PublishError("Duplicate versioned asset name")
        if matches:
            asset = matches[0]
        else:
            if same_commit and not release.get("draft"):
                raise PublishError("Published same-commit release is missing an asset")
            upload_url = release["upload_url"].split("{", 1)[0]
            asset = api.request("POST", upload_url + "?name=" + urllib.parse.quote(name),
                                data=data, content_type="application/octet-stream")
        verify_asset(api, asset, state["assets"][name], len(data))
    check_main()
    if same_commit and not release.get("draft") and tag["object"]["sha"] == sha:
        return "unchanged"
    if tag:
        api.request("PATCH", "/git/refs/tags/preview", {"sha": sha, "force": True})
    else:
        api.request("POST", "/git/refs", {"ref": "refs/tags/preview", "sha": sha})
    api.request("PATCH", f"/releases/{release['id']}", {"tag_name": "preview", "target_commitish": sha,
        "name": "Preview", "body": notes(state, api.repository), "draft": False,
        "prerelease": True, "make_latest": "false"})
    final = api.request("GET", "/releases/tags/preview")
    final_tag = api.request("GET", "/git/ref/tags/preview")
    final_latest = api.request("GET", "/releases/latest")
    if (not final or final.get("draft") or final.get("prerelease") is not True or
            final.get("tag_name") != "preview" or final.get("immutable") is not False or
            (final_latest and final_latest.get("id") == final.get("id")) or
            owned_state(final) != state or not final_tag or final_tag.get("object", {}).get("sha") != sha):
        raise PublishError("Publication verification failed; retained assets permit recovery")
    return "published"


def valid_provider_authorities(value):
    # AndroidX Startup is merged by dependencies alongside our FileProvider. Keep this
    # exact app-scoped allowlist: a new provider requires deliberate source review.
    allowed = {APP_ID + ".fileprovider", APP_ID + ".androidx-startup"}
    return (isinstance(value, list) and 1 <= len(value) <= len(allowed)
            and all(isinstance(item, str) for item in value)
            and len(set(value)) == len(value)
            and APP_ID + ".fileprovider" in value
            and set(value).issubset(allowed))


def load_manifest(path, expected_sha, expected_run):
    try:
        manifest = json.loads(path.read_text())
        sha, run = manifest["source_sha"], manifest["run_number"]
        if not SHA.fullmatch(sha) or type(run) is not int or not 0 < run <= 2_099_000_000:
            raise ValueError()
        expected_name = f"preview.{run}+{sha[:7]}"
        filename = f"{APP_ID}-{expected_name}.apk"
        if (sha != expected_sha or str(run) != expected_run or manifest["app_id"] != APP_ID or
                manifest["version_name"] != expected_name or manifest["version_code"] != 1_000_000 + run or
                manifest["debuggable"] is not False or manifest["label"] != "GearBox Dev Preview" or
                manifest["apk"] != filename or not HEX.fullmatch(manifest["certificate_sha256"]) or
                not HEX.fullmatch(manifest["sha256"]) or
                not valid_provider_authorities(manifest["provider_authorities"])):
            raise ValueError()
        apk_path = path.parent / filename
        if apk_path.is_symlink() or not 0 < apk_path.stat().st_size <= 128 * 1024 * 1024:
            raise ValueError()
        apk = apk_path.read_bytes()
        if hashlib.sha256(apk).hexdigest() != manifest["sha256"]:
            raise ValueError()
        if (path.parent / "SHA256SUMS").read_text() != manifest["sha256"] + "  " + filename + "\n":
            raise ValueError()
        validate_date(manifest["source_date"])
        return apk, sha, run, manifest["certificate_sha256"], manifest["source_date"]
    except (OSError, ValueError, KeyError, TypeError):
        raise PublishError("Build artifact manifest or checksum does not match this workflow run") from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--manifest", required=True, type=Path)
    args = parser.parse_args()
    try:
        artifact = load_manifest(args.manifest, os.environ.get("GITHUB_SHA"), os.environ.get("GITHUB_RUN_NUMBER"))
        api = GitHub(args.repository, os.environ.get("GITHUB_TOKEN", ""))
        print(publish(api, *artifact))
    except StaleBuild:
        print("skipped: main advanced; no release announcement")
    except PublishError as error:
        parser.exit(1, str(error) + "\n")


if __name__ == "__main__":
    main()
