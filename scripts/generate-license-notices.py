"""Generate notices from the resolved runtime graph and published POM licenses.
Run :app:auditRuntimeDependencies first. Preserve license text and binary NOTICE.
"""
from pathlib import Path
import json
import xml.etree.ElementTree as ET
import hashlib
import io
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / 'app/build/reports/runtime-licenses'
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
exceptions = {
    'javax.inject:javax.inject:1': ('Apache-2.0', 'https://repo.maven.apache.org/maven2/javax/inject/javax.inject/1/javax.inject-1-sources.jar'),
    'com.google.guava:listenablefuture:1.0': ('Apache-2.0', 'https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0-sources.jar'),
}
rows, records, notices = [], [], []
for item in json.loads((REPORT / 'dependencies.json').read_text()):
    coordinate = item['coordinate']
    raw = (REPORT / item['pom']).read_bytes()
    tree = ET.fromstring(raw)
    values = [(x.findtext('m:name', namespaces=NS), x.findtext('m:url', namespaces=NS))
              for x in tree.findall('m:licenses/m:license', NS)]
    if not values:
        assert coordinate in exceptions, 'Unreviewed license: ' + coordinate
        values = [exceptions[coordinate]]
    label = ', '.join(v[0] for v in values)
    evidence = ', '.join(v[1] for v in values)
    group, name, version = coordinate.split(':')
    repository = 'https://dl.google.com/dl/android/maven2/' if group.startswith(('androidx.', 'com.android.', 'com.google.android.')) else 'https://repo.maven.apache.org/maven2/'
    pom_url = repository + group.replace('.', '/') + '/' + name + '/' + version + '/' + name + '-' + version + '.pom'
    records.append({'coordinate': coordinate, 'license': label, 'reference': evidence,
                    'pom': pom_url, 'pom_sha256': hashlib.sha256(raw).hexdigest()})
    rows.append('| ' + coordinate + ' | ' + label + ' | ' + evidence + ' |')
    if 'artifact' in item:
        with zipfile.ZipFile(item['artifact']) as archive:
            archives = [archive]
            if 'classes.jar' in archive.namelist():
                archives.append(zipfile.ZipFile(io.BytesIO(archive.read('classes.jar'))))
            for inner in archives:
                for path in inner.namelist():
                    basename = Path(path).name.lower()
                    if basename.startswith(('license', 'notice')) and not path.endswith('/'):
                        text = inner.read(path).decode('utf-8', errors='replace')
                        notices.append('# ' + coordinate + ' — ' + path + '\n\n' + text)

(ROOT / 'docs/legal/RUNTIME_DEPENDENCIES.json').write_text(json.dumps(records, indent=2) + '\n')
intro = """# Komprexo — open-source and third-party notices

Runtime graph audited from the actual releaseRuntimeClasspath, including direct and transitive dependencies.
Google Play Billing and its Google Play services dependencies use Android SDK terms, not Apache-2.0.
No license was inferred from a shared group name. Two POMs omit licenses: their published sources were inspected.
javax.inject Inject.java states Copyright (C) 2009 The JSR-330 Expert Group, Apache-2.0;
Guava ListenableFuture.java states Copyright (C) 2007 The Guava Authors, Apache-2.0.

Apache-2.0: include the license, retain applicable copyright/NOTICE and indicate modifications when redistributing source.
BSD-3-Clause: retain copyright, conditions and disclaimer; no endorsement.
Android SDK License: proprietary SDK terms require developer acceptance; these libraries are not advertised as open source.
DejaVu Sans wordmark outlines: Bitstream Vera license; notices included with artwork. Android system fonts are not bundled.

No app changes were made to these libraries. Test-only/build-only tools are not shipped in the app:
JUnit (EPL-1.0), Robolectric (MIT), Espresso/AndroidX test (Apache-2.0), Playwright (Apache-2.0),
CairoSVG (LGPL-3.0), fonttools (MIT), Pillow (HPND). These are development tools, not runtime SDK additions.
Before publication, owner/legal review must verify all attribution obligations and SDK terms.

| Library and version | License | Reference |
|---|---|---|
"""
doc = intro + '\n'.join(rows) + '\n\nComplete license texts: [Apache-2.0](licenses/APACHE-2.0.txt), [Protobuf BSD-3](licenses/PROTOBUF-BSD-3.txt), [DejaVu](../../artwork/DEJAVU_LICENSE.txt).\n'
(ROOT / 'docs/legal/OPEN_SOURCE_LICENSES.md').write_text(doc)
extra = '\n\n'.join(notices)
(ROOT / 'docs/legal/licenses/BINARY_NOTICES.txt').write_text(extra + '\n')
full = '\n\n'.join((ROOT / p).read_text() for p in [
    'docs/legal/licenses/APACHE-2.0.txt', 'docs/legal/licenses/PROTOBUF-BSD-3.txt', 'artwork/DEJAVU_LICENSE.txt'])
for source in (ROOT / 'content/i18n').glob('*.json'):
    locale = source.stem
    title = json.loads(source.read_text())['open_source_licenses']
    text = '# ' + title + '\n\n' + '\n'.join(
        r['coordinate'] + ' — ' + r['license'] + '\n' + r['reference'] for r in records)
    text += '\n\n' + full + '\n\n' + extra
    (ROOT / f'app/src/main/assets/legal/{locale}/licenses.txt').write_text(text)
print('Audited ' + str(len(records)) + ' runtime libraries; full license texts bundled offline.')
