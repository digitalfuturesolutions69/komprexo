"""Verify authorized Billing permissions, release isolation and audited runtime graph."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
ns = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
assert {p.get(ns+'name') for p in manifest.findall('uses-permission')} == {'com.android.vending.BILLING'}
merged = root/'app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml'
assert merged.exists(), 'Compile release manifest first'
merged_root = ET.parse(merged).getroot()
assert merged_root.get('package') == 'com.digitalfuturesolutions.komprexo'
merged_provider = merged_root.find("application/provider[@"+ns+"name='androidx.core.content.FileProvider']")
assert merged_provider is not None and merged_provider.get(ns+'authorities') == 'com.digitalfuturesolutions.komprexo.files'
permissions = {p.get(ns+'name') for p in ET.parse(merged).getroot().findall('uses-permission')}
assert permissions == {'com.android.vending.BILLING','android.permission.INTERNET',
    'android.permission.ACCESS_NETWORK_STATE','com.digitalfuturesolutions.komprexo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}, permissions
app = manifest.find('application')
assert app.get(ns+'allowBackup') == 'false'
provider = app.find('provider')
assert provider.get(ns+'exported') == 'false' and provider.get(ns+'grantUriPermissions') == 'true'
paths = ET.parse(root/'app/src/main/res/xml/file_paths.xml').getroot()
assert len(paths) == 1 and paths[0].tag == 'cache-path' and paths[0].get('path') == 'shared/'
release = root/'app/build/tmp/kotlin-classes/release'
classes = list((release/'com/komprexo/app/access').glob('EntitlementProviderFactory*.class'))
assert classes, 'Compile release Kotlin first'
for compiled in classes:
    data = compiled.read_bytes()
    for forbidden in (b'setTestingPremium',b'EntitlementState$Premium',b'debugEntitlement'):
        assert forbidden not in data, compiled
assert not list(release.rglob('*FakeBilling*')) and not list(release.rglob('*MemoryOwnershipStore*'))
factory = (root/'app/src/release/java/com/komprexo/app/access/EntitlementProviderFactory.kt').read_text()
assert 'BillingServices.entitlement' in factory
for source in (root/'app/src/main').rglob('*.kt'):
    text = source.read_text()
    assert 'setTestingPremium' not in text, source
    assert not re.search(r'Log\.\w+\([^\n]*(purchaseToken|originalJson|offerToken)',text), source
    for forbidden in ('consumeAsync','ProductType.SUBS','FirebaseAnalytics','FusedLocationProviderClient'):
        assert forbidden not in text, (source,forbidden)
transport = (root/'app/src/main/java/com/komprexo/app/billing/PlayBillingTransport.kt').read_text()
assert 'ProductType.INAPP' in transport and 'acknowledgePurchase' in transport
models = (root/'app/src/main/java/com/komprexo/app/billing/BillingModels.kt').read_text()
assert 'komprexo_premium_lifetime' in models
assert not any('token' in line for line in models.splitlines() if 'override fun toString()' in line)
resolved = json.loads((root/'app/build/reports/runtime-licenses/dependencies.json').read_text())
audited = json.loads((root/'docs/legal/RUNTIME_DEPENDENCIES.json').read_text())
assert {x['coordinate'] for x in resolved} == {x['coordinate'] for x in audited}, 'Runtime graph changed; audit licenses and Data Safety again'
for entry in resolved:
    coordinate = entry['coordinate'].lower()
    assert not any(x in coordinate for x in ('play-services-ads','firebase-analytics','facebook','adjust','appsflyer')), coordinate
for file in list((root/'app/src/main').rglob('*')) + list((root/'content').rglob('*')):
    if file.is_file() and file.suffix in ('.kt','.xml','.json','.txt'):
        text = file.read_text()
        assert '-----BEGIN PRIVATE KEY-----' not in text and '"type": "service_account"' not in text, file
print('PASS: four audited permissions, secure FileProvider, real release Billing, no fake unlock/consumption/ads/analytics, runtime licenses match')
