"""Verify release entitlement isolation after compileReleaseKotlin; no release APK created."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
assert not manifest.findall('uses-permission'), 'Unexpected application permissions'
merged = root / 'app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml'
assert merged.exists(), 'Compile release manifest before this check'
permissions = {p.get(ns + 'name') for p in ET.parse(merged).getroot().findall('uses-permission')}
assert permissions <= {'com.komprexo.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}, permissions
application = manifest.find('application')
provider = application.find('provider')
assert provider.get(ns + 'exported') == 'false'
assert provider.get(ns + 'grantUriPermissions') == 'true'
release = root / 'app/build/tmp/kotlin-classes/release/com/komprexo/app/access'
classes = list(release.glob('EntitlementProviderFactory*.class'))
assert classes, 'Compile release Kotlin before running this check'
for compiled in classes:
    data = compiled.read_bytes()
    assert b'setTestingPremium' not in data, compiled
    assert b'EntitlementState$Premium' not in data, compiled
    assert b'debugEntitlement' not in data, compiled
for source in (root / 'app/src/main').rglob('*.kt'):
    assert 'setTestingPremium' not in source.read_text(), source
build = (root / 'app/build.gradle').read_text().lower()
assert 'play-services-ads' not in build and 'billingclient' not in build
print('PASS: release provider is Free-only; debug unlock absent; manifest permissions and FileProvider unchanged')
