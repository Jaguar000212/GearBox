"""Offline advisory inventory for Preview retention. Never deletes or contacts a service.

Input: {"release": GitHub release JSON, "assets": complete paginated asset list}.
Names and the current publisher marker do not establish historical publication,
signing, or byte integrity. Older bundles are review candidates, never deletion approvals.
"""
import argparse
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import re

from publish_preview import owned_state, PublishError

NAME = re.compile(r"gearbox-devpreview-([1-9][0-9]*)-([0-9a-f]{40})\.(apk|json|sha256)")


def timestamp(value):
    if not isinstance(value, str):
        raise ValueError("Missing asset timestamp")
    result = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if result.tzinfo is None:
        raise ValueError("Timestamp needs timezone")
    return result


def plan(document, now):
    if now.tzinfo is None:
        raise ValueError("Now needs timezone")
    release, assets = document['release'], document['assets']
    if (release.get('tag_name') != 'preview' or release.get('draft') is not False
            or release.get('prerelease') is not True or not isinstance(assets, list)):
        raise ValueError("Expected published Preview prerelease and complete asset inventory")
    current = owned_state(release)
    if any(not isinstance(a, dict) for a in assets):
        raise ValueError("Malformed asset inventory")
    ids = Counter(a.get('id') for a in assets if type(a.get('id')) is int)
    names = Counter(a.get('name') for a in assets if isinstance(a.get('name'), str))
    groups, unknown = defaultdict(list), []
    for asset in assets:
        match = NAME.fullmatch(asset.get('name', '')) if isinstance(asset.get('name'), str) else None
        if match:
            groups[(int(match[1]), match[2])].append(asset)
        else:
            unknown.append(asset)
    codes = Counter(code for code, _ in groups)
    output = []
    complete = []
    for (code, sha), rows in groups.items():
        suffixes = {a['name'].rsplit('.', 1)[1] for a in rows}
        valid = (len(rows) == 3 and suffixes == {'apk', 'json', 'sha256'}
                 and 1_000_000 < code <= 2_100_000_000 and codes[code] == 1)
        dates = []
        for a in rows:
            valid &= (type(a.get('id')) is int and a['id'] > 0 and ids[a['id']] == 1
                      and names[a['name']] == 1 and type(a.get('size')) is int
                      and a['size'] > 0 and a.get('state') == 'uploaded')
            try:
                dates.extend([timestamp(a.get('created_at')), timestamp(a.get('updated_at'))])
            except (ValueError, TypeError):
                valid = False
        if any(date > now for date in dates):
            valid = False
        item = {'version_code': code, 'build_sha': sha, 'assets': [
            {key: a.get(key) for key in ('id', 'name', 'size', 'created_at', 'updated_at')} for a in rows],
            'status': 'review_required', 'reason': 'Incomplete, duplicate, invalid, or ambiguous bundle; preserve.'}
        if code == current['version_code'] and sha == current['sha']:
            item.update(status='protected_current', reason='Current publisher marker; always preserve, even if inventory is incomplete.')
        elif valid and code < current['version_code']:
            complete.append((code, max(dates), item))
        elif valid:
            item['reason'] = 'Not older than current marker; may be staged or interrupted publication. Preserve for review.'
        output.append(item)
    # Only structurally complete bundles, not proven successful historical releases.
    complete.sort(key=lambda entry: entry[0], reverse=True)
    for index, (_, newest_date, item) in enumerate(complete):
        if index < 4:
            item.update(status='protected_recent', reason='One of four newest structurally complete previous bundles; preserve pending provenance review.')
        elif newest_date >= now - timedelta(days=14):
            item.update(status='protected_age', reason='At least one asset was created or updated within 14 days; preserve.')
        else:
            item.update(status='review_candidate', reason='Outside current plus four previous bundles and all asset dates older than 14 days. Historical success, signing, and hashes are unverified; explicit review and per-action approval still required.')
    for asset in unknown:
        output.append({'status': 'review_required', 'reason': 'Unknown asset name; never infer ownership or removal permission.',
                       'assets': [{key: asset.get(key) for key in ('id', 'name', 'size', 'created_at', 'updated_at')}]})
    return {'advisory_only': True, 'generated_at': now.isoformat(),
            'current': {'version_code': current['version_code'], 'sha': current['sha']},
            'limitations': 'Local inventory completeness is caller supplied. No asset bytes, historical successful publication, or signatures verified. No deletion authorized or performed.',
            'bundles': output}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', required=True, type=Path)
    parser.add_argument('--now', help='Timezone-aware ISO date; defaults to current UTC')
    args = parser.parse_args()
    try:
        result = plan(json.loads(args.input.read_text()), timestamp(args.now) if args.now else datetime.now(timezone.utc))
    except (OSError, ValueError, KeyError, TypeError, PublishError) as error:
        parser.error('Invalid inventory: ' + str(error))
    print(json.dumps(result, indent=2, sort_keys=True))


if __name__ == '__main__':
    main()
