"""Verify resolved release bytes against preserved SDK evidence; never infer runtime behavior."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def verify(inventory, rows, report_root):
    expected = {r['artifact']:r for r in inventory}
    resolved = {r['coordinate']:r for r in rows}
    if len(expected) != len(inventory) or len(resolved) != len(rows) or set(expected) != set(resolved):
        raise ValueError('Release SDK coordinate inventory drift or duplicate records')
    for coordinate, item in expected.items():
        row = resolved[coordinate]
        try:
            data = Path(row['artifact']).read_bytes()
            pom = (report_root/row['pom']).read_bytes()
        except OSError:
            raise ValueError('Resolved SDK/POM file unavailable: '+coordinate) from None
        if hashlib.sha256(data).hexdigest() != item['artifact_sha256']:
            raise ValueError('Release SDK artifact hash drift: '+coordinate)
        if hashlib.sha256(pom).hexdigest() != item['pom_sha256']:
            raise ValueError('Release SDK POM hash drift: '+coordinate)
        with zipfile.ZipFile(row['artifact']) as z:
            if z.testzip() is not None:
                raise ValueError('Release SDK ZIP integrity failed: '+coordinate)
            jar = hashlib.sha256(z.read('classes.jar')).hexdigest() if 'classes.jar' in z.namelist() else None
        if jar != item['classes_jar_sha256']:
            raise ValueError('Release SDK classes hash drift: '+coordinate)
    return len(expected)


if __name__ == '__main__':
    inventory = json.loads((ROOT/'docs/google-play/SDK_PRIVACY_INVENTORY.json').read_text())
    report_root = ROOT/'app/build/reports/runtime-licenses'
    count = verify(inventory,json.loads((report_root/'dependencies.json').read_text()),report_root)
    if count != 99:
        raise ValueError('Unexpected preserved runtime artifact count')
    print(f'PASS: {count} resolved release artifacts/POMs/classes match preserved SDK hashes; static evidence only')
