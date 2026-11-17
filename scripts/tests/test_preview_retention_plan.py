import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import preview_retention_plan as p
from publish_preview import MARKER

NOW = p.timestamp('2026-11-17T12:00:00Z')


def fixture(count=7):
    assets = []
    for run in range(1, count + 1):
        sha = f'{run:040x}'
        for suffix in ('apk', 'json', 'sha256'):
            assets.append({'id': len(assets) + 1, 'name': f'gearbox-devpreview-{1000000+run}-{sha}.{suffix}',
                           'size': 123, 'state': 'uploaded', 'created_at': '2026-10-01T00:00:00Z',
                           'updated_at': '2026-10-01T00:00:00Z'})
    state = {'sha': f'{count:040x}', 'version_code': 1000000 + count, 'fingerprint': 'a'*64,
             'source_date': '2026-10-01T00:00:00Z', 'assets': {a['name']: 'b'*64 for a in assets[-3:]}}
    return {'release': {'tag_name': 'preview', 'draft': False, 'prerelease': True,
                        'body': MARKER + json.dumps(state) + ' -->'}, 'assets': assets}


class RetentionTest(unittest.TestCase):
    def test_current_only(self):
        result = p.plan(fixture(1), NOW)
        self.assertTrue(result['advisory_only'])
        self.assertEqual(['protected_current'], [b['status'] for b in result['bundles']])

    def test_current_four_previous_and_review_only(self):
        rows = p.plan(fixture(), NOW)['bundles']
        self.assertEqual(['review_candidate']*2 + ['protected_recent']*4 + ['protected_current'], [r['status'] for r in rows])
        self.assertEqual(1, rows[0]['assets'][0]['id'])
        self.assertIn('unverified', rows[0]['reason'])

    def test_age_uses_latest_update_and_exact_boundary(self):
        data = fixture()
        data['assets'][0]['updated_at'] = '2026-11-03T12:00:00Z'
        self.assertEqual('protected_age', p.plan(data, NOW)['bundles'][0]['status'])

    def test_unknown_incomplete_duplicates_future_and_foreign(self):
        for kind in ('incomplete', 'duplicate', 'future', 'type', 'foreign'):
            data = fixture()
            if kind == 'incomplete': data['assets'].pop(0)
            if kind == 'duplicate': data['assets'].append(dict(data['assets'][0]))
            if kind == 'future': data['assets'][0]['updated_at'] = '2027-01-01T00:00:00Z'
            if kind == 'type': data['assets'][0]['size'] = True
            if kind == 'foreign': data['assets'][0]['name'] = 'stable.apk'
            with self.subTest(kind=kind):
                rows = p.plan(data, NOW)['bundles']
                self.assertFalse(any(r.get('version_code') == 1000001 and r['status'] == 'review_candidate' for r in rows))

    def test_staged_newer_not_candidate(self):
        data = fixture(8)
        old = fixture(7)
        data['release'] = old['release']
        self.assertEqual('review_required', p.plan(data, NOW)['bundles'][-1]['status'])

    def test_missing_current_protected_and_unowned_rejected(self):
        data = fixture()
        data['assets'].pop()
        self.assertEqual('protected_current', p.plan(data, NOW)['bundles'][-1]['status'])
        data['release']['body'] = 'not owned'
        with self.assertRaises(p.PublishError): p.plan(data, NOW)

    def test_wrong_channel_and_naive_time_rejected(self):
        data = fixture()
        data['release']['tag_name'] = 'beta'
        with self.assertRaises(ValueError): p.plan(data, NOW)
        with self.assertRaises(ValueError): p.timestamp('2026-11-17T12:00:00')


if __name__ == '__main__':
    unittest.main()
