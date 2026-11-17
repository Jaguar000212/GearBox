import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import io
from unittest.mock import Mock
import unittest

SPEC = importlib.util.spec_from_file_location('publisher', Path(__file__).parents[1] / 'publish_preview.py')
p = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(p)
DATE = '2026-11-17T00:00:00+00:00'
A, B, CERT = 'a' * 40, 'b' * 40, 'c' * 64


class FakeGitHub:
    def __init__(self):
        self.repository = "o/r"
        self.head = A
        self.release = None
        self.tag = None
        self.assets = []
        self.bytes = {}
        self.calls = []
        self.corrupt = False
        self.fail_patch = False
        self.latest = None
        self.advance_after_upload = False

    def request(self, method, path, payload=None, **kwargs):
        self.calls.append((method, path, payload))
        if method == 'GET':
            if path == '/git/ref/heads/main': return {'object': {'sha': self.head}}
            if path == '/git/ref/tags/preview': return copy.deepcopy(self.tag)
            if path == '/releases/tags/preview': return copy.deepcopy(self.release)
            if path == '/releases/latest': return self.latest
            if path == '/releases?per_page=100': return []
            if path.startswith('/releases/1/assets?'): return copy.deepcopy(self.assets)
        if method == 'POST' and path == '/releases':
            self.release = {**payload, 'id': 1, 'immutable': False, 'upload_url': 'https://uploads.github.com/repos/o/r/releases/1/assets{?name,label}'}
            return copy.deepcopy(self.release)
        if method == 'POST' and path.startswith('https://uploads.github.com/'):
            name = p.urllib.parse.parse_qs(p.urllib.parse.urlparse(path).query)['name'][0]
            asset = {'id': len(self.assets) + 1, 'name': name, 'size': len(kwargs['data']), 'state': 'uploaded'}
            self.assets.append(asset)
            self.bytes[asset['id']] = kwargs['data']
            if self.advance_after_upload: self.head = B
            return asset
        if method == 'POST' and path == '/git/refs':
            self.tag = {'object': {'sha': payload['sha'], 'type': 'commit'}}
            return self.tag
        if method == 'PATCH' and path == '/git/refs/tags/preview':
            self.tag['object']['sha'] = payload['sha']
            return self.tag
        if method == 'PATCH' and path == '/releases/1':
            if self.fail_patch: raise p.PublishError('Patch failed')
            self.release.update(payload)
            return self.release
        raise AssertionError((method, path))

    def asset_bytes(self, asset, expected_size):
        return b'corrupt' if self.corrupt else self.bytes[asset['id']]


class PublisherTest(unittest.TestCase):
    def setUp(self): self.api = FakeGitHub()
    def publish(self, sha=A, run=10, apk=b'signed apk', cert=CERT):
        return p.publish(self.api, apk, sha, run, cert, DATE)
    def test_first_release_stages_verifies_then_publishes(self):
        self.assertEqual('published', self.publish())
        self.assertFalse(self.api.release['draft'])
        self.assertTrue(self.api.release['prerelease'])
        self.assertEqual('false', self.api.release['make_latest'])
        self.assertEqual(A, self.api.tag['object']['sha'])
        self.assertEqual(3, len(self.api.assets))
        self.assertFalse(any(method == 'DELETE' for method, _, _ in self.api.calls))
    def test_upgrade_retains_good_assets(self):
        self.publish()
        old = copy.deepcopy(self.api.bytes)
        self.api.head = B
        self.assertEqual('published', self.publish(B, 11, b'next apk'))
        self.assertEqual(6, len(self.api.assets))
        self.assertTrue(all(self.api.bytes[key] == data for key, data in old.items()))
        self.assertEqual(1000011, p.owned_state(self.api.release)['version_code'])
    def test_same_commit_new_run_is_verified_noop(self):
        self.publish()
        self.api.calls.clear()
        self.assertEqual('unchanged', self.publish(run=11))
        self.assertTrue(all(method == 'GET' for method, _, _ in self.api.calls))
    def test_stale_head_does_not_mutate(self):
        self.api.head = B
        with self.assertRaises(p.StaleBuild): self.publish()
        self.assertTrue(all(method == 'GET' for method, _, _ in self.api.calls))
    def test_stale_after_upload_does_not_promote(self):
        self.publish()
        previous = copy.deepcopy(self.api.release)
        self.api.head = B
        # Simulate main moving again only after upload (third commit).
        original = self.api.asset_bytes
        def download(asset, size):
            value = original(asset, size)
            self.api.head = 'd' * 40
            return value
        self.api.asset_bytes = download
        with self.assertRaises(p.StaleBuild): self.publish(B, 11)
        self.assertEqual(previous, self.api.release)
        self.assertEqual(A, self.api.tag['object']['sha'])
    def test_guards_certificate_code_ownership_immutable_latest(self):
        for mutation in ('cert', 'code', 'owner', 'immutable', 'latest', 'tag'):
            with self.subTest(mutation=mutation):
                self.api = FakeGitHub(); self.publish(); self.api.head = B
                if mutation == 'owner': self.api.release['body'] = 'unowned'
                if mutation == 'immutable': self.api.release['immutable'] = True
                if mutation == 'latest': self.api.latest = {'id': 1}
                if mutation == 'tag': self.api.tag['object']['sha'] = 'd' * 40
                self.api.calls.clear()
                with self.assertRaises(p.PublishError):
                    self.publish(B, 9 if mutation == 'code' else 11, cert='d'*64 if mutation == 'cert' else CERT)
                self.assertTrue(all(method == 'GET' for method, _, _ in self.api.calls))
    def test_orphan_tag_refused(self):
        self.api.tag = {'object': {'sha': A, 'type': 'commit'}}
        with self.assertRaises(p.PublishError): self.publish()
        self.assertIsNone(self.api.release)
    def test_bad_upload_keeps_old_release(self):
        self.publish(); old = copy.deepcopy(self.api.release)
        self.api.head = B; self.api.corrupt = True
        with self.assertRaises(p.PublishError): self.publish(B, 11)
        self.assertEqual(old, self.api.release)
        self.assertEqual(A, self.api.tag['object']['sha'])
    def test_partial_finalization_recovers_without_deleting_assets(self):
        self.publish(); self.api.head = B; self.api.fail_patch = True
        with self.assertRaises(p.PublishError): self.publish(B, 11)
        self.assertEqual(A, p.owned_state(self.api.release)['sha'])
        self.assertEqual(B, self.api.tag['object']['sha'])
        self.api.fail_patch = False
        self.assertEqual('published', self.publish(B, 11))
        self.assertEqual(6, len(self.api.assets))
    def test_new_main_recovers_after_prior_tag_move_body_failure(self):
        self.publish(); self.api.head = B; self.api.fail_patch = True
        with self.assertRaises(p.PublishError): self.publish(B, 11)
        self.api.fail_patch = False
        self.api.head = 'd' * 40
        self.assertEqual('published', self.publish('d' * 40, 12, b'third apk'))
        self.assertEqual('d' * 40, p.owned_state(self.api.release)['sha'])
        self.assertEqual(9, len(self.api.assets))
    def test_interrupted_bundle_corruption_does_not_allow_recovery(self):
        self.publish(); self.api.head = B; self.api.fail_patch = True
        with self.assertRaises(p.PublishError): self.publish(B, 11)
        self.api.fail_patch = False; self.api.head = 'd' * 40
        self.api.bytes[4] = b'bad second apk'
        with self.assertRaises(p.PublishError): self.publish('d' * 40, 12)
        self.assertEqual(A, p.owned_state(self.api.release)['sha'])
    def test_first_draft_upload_failure_rerun(self):
        self.api.corrupt = True
        with self.assertRaises(p.PublishError): self.publish()
        self.assertTrue(self.api.release['draft'])
        self.api.corrupt = False
        self.assertEqual('published', self.publish())
    def test_manifest_validates_job_identity_and_checksum(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); filename = p.APP_ID + '-preview.10+aaaaaaa.apk'
            apk = b'apk'; digest = hashlib.sha256(apk).hexdigest()
            metadata = {'app_id':p.APP_ID,'source_sha':A,'run_number':10,'version_code':1000010,
                'version_name':'preview.10+aaaaaaa','apk':filename,'sha256':digest,
                'certificate_sha256':CERT,'debuggable':False,'label':'GearBox Dev Preview',
                'provider_authorities':[p.APP_ID+'.fileprovider'], 'source_date':DATE}
            (root/filename).write_bytes(apk)
            (root/'SHA256SUMS').write_text(digest+'  '+filename+'\n')
            path=root/'preview-manifest.json';path.write_text(json.dumps(metadata))
            self.assertEqual((apk,A,10,CERT,DATE),p.load_manifest(path,A,'10'))
            for sha,run in [(B,'10'),(A,'11')]:
                with self.assertRaises(p.PublishError):p.load_manifest(path,sha,run)
            (root/filename).write_bytes(b'changed')
            with self.assertRaises(p.PublishError):p.load_manifest(path,A,'10')

class TransportTest(unittest.TestCase):
    def test_redirect_never_forwards_token(self):
        api = p.GitHub("owner/repo", "secret-test-token")
        api.opener = Mock()
        redirect = p.urllib.error.HTTPError("https://api.github.com/asset", 302, "redirect",
            {"Location":"https://release-assets.githubusercontent.com/file?signature=value"}, None)
        api.opener.open.side_effect = [redirect, io.BytesIO(b"apk")]
        self.assertEqual(b"apk", api.asset_bytes({"id":1}, 3))
        first, second = [call.args[0] for call in api.opener.open.call_args_list]
        self.assertEqual("Bearer secret-test-token", first.get_header("Authorization"))
        self.assertIsNone(second.get_header("Authorization"))
    def test_unexpected_redirect_rejected(self):
        api = p.GitHub("owner/repo", "secret-test-token")
        api.opener = Mock()
        api.opener.open.side_effect = p.urllib.error.HTTPError("https://api.github.com/asset",302,"redirect",
            {"Location":"https://attacker.invalid/file"},None)
        with self.assertRaises(p.PublishError): api.asset_bytes({"id":1},3)
        self.assertEqual(1, api.opener.open.call_count)
    def test_unexpected_authenticated_host_rejected(self):
        api = p.GitHub("owner/repo", "secret-test-token")
        api.opener = Mock()
        with self.assertRaises(p.PublishError): api.request("POST","https://attacker.invalid/upload",data=b"apk")
        api.opener.open.assert_not_called()

if __name__ == '__main__': unittest.main()
