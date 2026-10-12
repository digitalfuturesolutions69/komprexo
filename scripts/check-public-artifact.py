"""Audit the review ZIP and scan public bytes; never publish or print secret values."""
from pathlib import Path
import hashlib
import importlib.util
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PATTERNS = {
    'private-key': rb'-----BEGIN (?:RSA |EC |OPENSSH |DSA |ENCRYPTED )?PRIVATE KEY-----',
    'github-token': rb'\b(?:gh[pousr]_[A-Za-z0-9_]{30,}|github_pat_[A-Za-z0-9_]{30,})\b',
    'aws-access-key': rb'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b',
    'google-api-key': rb'\bAIza[A-Za-z0-9_-]{35}\b',
}


def scan_public_bytes(files):
    for name, data in files:
        for category, pattern in PATTERNS.items():
            if re.search(pattern, data):
                raise ValueError(f'Public artifact blocked: {category} pattern in {name}; value omitted')


def audit(output):
    spec = importlib.util.spec_from_file_location('static', ROOT/'scripts/prepare-static-artifact.py')
    static = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(static)
    archive = output/'komprexo-static.zip'
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    if (output/'komprexo-static.zip.sha256').read_text() != digest+'  komprexo-static.zip\n':
        raise ValueError('ZIP checksum mismatch')
    manifest = json.loads((output/'manifest.json').read_text())
    if len(manifest['files']) != len(static.EXPECTED) or {r['path'] for r in manifest['files']} != static.EXPECTED:
        raise ValueError('Manifest allowlist mismatch')
    with zipfile.ZipFile(archive) as z:
        if z.testzip() is not None or len(z.infolist()) != len(static.EXPECTED) or set(z.namelist()) != static.EXPECTED:
            raise ValueError('ZIP CRC/allowlist/duplicate entries mismatch')
        for row in manifest['files']:
            data = z.read(row['path'])
            if len(data) != row['bytes'] or hashlib.sha256(data).hexdigest() != row['sha256']:
                raise ValueError('Manifest hash/length mismatch: '+row['path'])
            if data != (ROOT/'website/build'/row['path']).read_bytes():
                raise ValueError('Public source parity mismatch: '+row['path'])
        scan_public_bytes((name,z.read(name)) for name in z.namelist())
    return digest


if __name__ == '__main__':
    print('PASS: public allowlist, CRC, manifest, checksum, source parity and known secret-pattern scan; SHA256 '+audit(ROOT/'website/deployment'))
