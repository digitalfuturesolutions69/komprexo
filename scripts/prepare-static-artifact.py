"""Prepare a reviewed Rumahweb static ZIP; never upload or deploy."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LOCALES = ('en', 'id', 'es', 'pt-BR', 'hi')
ROUTES = ('', 'privacy', 'terms', 'premium', 'support', 'about')
EXPECTED = {'style.css', 'assets/icon.svg'} | {
    str(Path(locale) / route / 'index.html')
    for locale in ('', *LOCALES) for route in ROUTES
}


def validate_site(site, publication=False):
    paths = list(site.rglob('*'))
    if site.is_symlink() or any(p.is_symlink() or (p.is_file() and p.stat().st_nlink > 1) for p in paths):
        raise ValueError('Static input must not contain symbolic or hard links')
    files = {p.relative_to(site).as_posix() for p in paths if p.is_file()}
    if files != EXPECTED:
        raise ValueError(f'Unexpected static input: missing={sorted(EXPECTED-files)}, extra={sorted(files-EXPECTED)}')
    if publication:
        for name in files:
            if name.endswith('.html') and any(marker in (site/name).read_text() for marker in ('DRAFT', 'OWNER ACTION REQUIRED')):
                raise ValueError('Publication blocked: legal drafts or unresolved owner decisions remain')
    return sorted(files)


def prepare(site, output, publication=False):
    files = validate_site(site, publication)
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    manifest = []
    archive = output/'komprexo-static.zip'
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED) as z:
        for name in files:
            data = (site/name).read_bytes()
            # Fixed metadata makes the same reviewed source produce the same ZIP.
            entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o100644 << 16
            z.writestr(entry, data)
            manifest.append({'path': name, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
    with zipfile.ZipFile(archive) as z:
        if z.testzip() is not None or set(z.namelist()) != EXPECTED:
            raise ValueError('Static ZIP validation failed')
    (output/'manifest.json').write_text(json.dumps({'document_root': 'public_html/komprexo',
        'status': 'PREPARATION ONLY; NOT AUTHORIZED FOR PUBLICATION', 'files': manifest}, indent=2)+'\n')
    (output/'komprexo-static.zip.sha256').write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  komprexo-static.zip\n')
    # Historical prefix regression remains independent of the production root.
    shutil.copytree(site, output/'project-preview/komprexo')
    print(f'Static preparation: {len(files)} public files, root ZIP/checksum/manifest. NOT DEPLOYED.')
    return archive


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publication', action='store_true', help='Reject drafts; does not upload or deploy')
    args = parser.parse_args()
    prepare(ROOT/'website/build', ROOT/'website/deployment', args.publication)
