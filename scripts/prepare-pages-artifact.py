"""Create a private, reviewable Pages-compatible archive; never deploy anything."""
from pathlib import Path
import argparse
import shutil
import tarfile

ROOT = Path(__file__).resolve().parents[1]
LOCALES = ('en', 'id', 'es', 'pt-BR', 'hi')
ROUTES = ('', 'privacy', 'terms', 'premium', 'support', 'about')
CUSTOM_DOMAIN = 'komprexo.digitalfuturesolutions.my.id'
EXPECTED = {'.nojekyll', 'CNAME', 'style.css', 'assets/icon.svg'} | {
    str(Path(locale) / route / 'index.html')
    for locale in ('', *LOCALES) for route in ROUTES
}


def validate_site(site, publication=False):
    paths = list(site.rglob('*'))
    if any(p.is_symlink() or (p.is_file() and p.stat().st_nlink > 1) for p in paths):
        raise ValueError('Pages input must not contain symbolic or hard links')
    files = {str(p.relative_to(site)) for p in paths if p.is_file()}
    if files != EXPECTED:
        raise ValueError(f'Unexpected Pages input: missing={sorted(EXPECTED-files)}, extra={sorted(files-EXPECTED)}')
    if (site / 'CNAME').read_bytes() != (CUSTOM_DOMAIN + '\n').encode():
        raise ValueError('Invalid custom-domain CNAME: expected one approved hostname')
    if publication:
        for p in paths:
            if p.suffix == '.html' and any(marker in p.read_text() for marker in ('DRAFT', 'OWNER ACTION REQUIRED')):
                raise ValueError('Publication blocked: legal drafts or unresolved owner decisions remain')
    return sorted(files)


def prepare(site, output, publication=False):
    files = validate_site(site, publication)
    if output.exists():
        shutil.rmtree(output)
    preview = output / 'komprexo'
    preview.mkdir(parents=True)
    for name in files:
        target = preview / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(site / name, target)
    archive = output / 'pages-artifact.tar.gz'
    with tarfile.open(archive, 'w:gz') as tar:
        for name in files:
            tar.add(preview / name, arcname=name, recursive=False)
    print(f'Pages preparation: {len(files)} public-site files, project-path preview and single tar.gz. NOT DEPLOYED.')
    return archive


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publication', action='store_true', help='Fail if drafts or owner gates remain; does not deploy')
    args = parser.parse_args()
    prepare(ROOT / 'website/build', ROOT / 'website/pages-preview', args.publication)
