"""Documentation-only release inventory. Run after auditRuntimeDependencies and privacyResolvedGraph.
Never copies local artifact paths, SDK API keys, image data or account information.
"""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NS = '{http://schemas.android.com/apk/res/android}'
rows = json.loads((ROOT/'app/build/reports/runtime-licenses/dependencies.json').read_text())
graph = json.loads((ROOT/'app/build/reports/privacy-inventory/graph.json').read_text())
licenses = {x['coordinate']: x for x in json.loads((ROOT/'docs/legal/RUNTIME_DEPENDENCIES.json').read_text())}
assert {r['coordinate'] for r in rows} == set(licenses), 'Review dependency changes before inventory generation'
profiles = json.loads((ROOT/'docs/google-play/SDK_ASSESSMENT_PROFILES.json').read_text())
result = []
for row in rows:
    coordinate = row['coordinate']; group, name, version = coordinate.split(':')
    if group == 'com.android.billingclient': profile = 'billing'
    elif group == 'com.google.android.datatransport': profile = 'transport'
    elif group == 'com.google.android.gms': profile = 'gms'
    elif group == 'com.google.firebase': profile = 'encoders'
    elif name.startswith('emoji2'): profile = 'emoji'
    elif group == 'androidx.datastore': profile = 'datastore'
    elif group == 'androidx.exifinterface': profile = 'exif'
    elif group in ('androidx.startup','androidx.profileinstaller'): profile = 'startup'
    else: profile = 'utility'
    artifact = Path(row['artifact']); manifest = None; jar_hash = None
    with zipfile.ZipFile(artifact) as z:
        if 'AndroidManifest.xml' in z.namelist(): manifest = ET.fromstring(z.read('AndroidManifest.xml'))
        if 'classes.jar' in z.namelist(): jar_hash = hashlib.sha256(z.read('classes.jar')).hexdigest()
    permissions = [] if manifest is None else [x.get(NS+'name') for x in manifest.findall('uses-permission')]
    components = []
    if manifest is not None:
        app = manifest.find('application')
        if app is not None:
            for x in app:
                if x.tag in ('activity','service','provider','receiver'):
                    components.append({'type':x.tag,'attributes':{k.replace(NS,'android:'):v for k,v in x.attrib.items()},
                                       'metadata':[{k.replace(NS,'android:'):v for k,v in m.attrib.items()} for m in x.findall('meta-data')]})
    pom_file = ROOT/'app/build/reports/runtime-licenses'/row['pom']
    assert hashlib.sha256(pom_file.read_bytes()).hexdigest() == licenses[coordinate]['pom_sha256'], coordinate
    parents = sorted({e['from'] for e in graph if e['to'] == coordinate})
    assessment = dict(profiles[profile])
    assessment.update({'artifact':coordinate,'resolved_version':version,'assessment_profile':profile,
        'why_included': 'Resolved release edge(s) from: '+', '.join(parents),
        'introduced_permissions_and_components':{'uses_permissions':permissions,'components':components,
            'scope':'Declared artifact manifest only; final merged manifest determines active components/permissions.'},
        'evidence_sources':[licenses[coordinate]['pom'], 'app/build.gradle', 'RELEASE_DEPENDENCY_EDGES.json',
                            'SDK_PRIVACY_REVIEW.md#evidence-method'],
        'artifact_sha256':hashlib.sha256(artifact.read_bytes()).hexdigest(),'classes_jar_sha256':jar_hash,
        'pom_sha256':licenses[coordinate]['pom_sha256']})
    declared = [m.get('android:name') for c in components for m in c['metadata'] if m.get('android:value') == 'androidx.startup']
    assessment['automatic_initialization'] += ' Artifact-declared startup initializers: '+(', '.join(declared) if declared else 'none; caller/framework activation remains separate.')
    result.append(assessment)
(ROOT/'docs/google-play/SDK_PRIVACY_INVENTORY.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
(ROOT/'docs/google-play/RELEASE_DEPENDENCY_EDGES.json').write_text(json.dumps(graph,indent=2)+'\n')
print(f'Audited {len(result)} resolved release artifacts; static evidence only, no runtime transmission claim')
