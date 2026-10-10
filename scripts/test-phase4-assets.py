"""Meaningful source/export/link/localization regression checks; Python stdlib only."""
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlparse, unquote
import json
import importlib.util
import re
import struct
import tarfile
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zlib

ROOT = Path(__file__).resolve().parents[1]
LOCALES = ('en','id','es','pt-BR','hi')
FOLDERS = ('values','values-in','values-es','values-pt-rBR','values-hi')
NS = '{http://schemas.android.com/apk/res/android}'

def png(path):
    raw = path.read_bytes()
    assert raw[:8] == b'\x89PNG\r\n\x1a\n', path
    pos, chunks, info = 8, [], None
    while pos < len(raw):
        length = struct.unpack('>I',raw[pos:pos+4])[0]
        kind, data = raw[pos+4:pos+8],raw[pos+8:pos+8+length]
        assert zlib.crc32(kind+data)&0xffffffff == struct.unpack('>I',raw[pos+8+length:pos+12+length])[0], path
        if kind == b'IHDR': info = struct.unpack('>IIBBBBB',data)
        if kind == b'IDAT': chunks.append(data)
        pos += length+12
        if kind == b'IEND': break
    assert pos == len(raw) and info and zlib.decompress(b''.join(chunks)), path
    return info

class Links(HTMLParser):
    def __init__(self):
        super().__init__(); self.links=[];self.scripts=[];self.lang=None;self.tags=[]
    def handle_starttag(self,tag,attrs):
        data=dict(attrs);self.tags.append(tag)
        if tag=='html': self.lang=data.get('lang')
        for key in ('href','src'):
            if key in data:self.links.append(data[key])
        if tag in ('script','iframe','form'):self.scripts.append(tag)

class Phase4Assets(unittest.TestCase):
    def test_owner_identity_and_proposed_law_in_five_languages(self):
        for locale in LOCALES:
            data=json.loads((ROOT/f'content/legal/{locale}.json').read_text())
            with self.subTest(locale=locale):
                for page in ('privacy','terms','premium','support','about'):
                    text=' '.join(x[1] for x in data[page])
                    self.assertIn('Digital Future Solutions',text)
                    self.assertIn('Google Play Console',text)
                    self.assertIn('Personal',text)
                    self.assertIn('komprexo.support@gmail.com',text)
                self.assertIn('OWNER ACTION REQUIRED',data['draft'])
                self.assertIn('OWNER ACTION REQUIRED',data['terms'][-2][1])
        english=json.loads((ROOT/'content/legal/en.json').read_text())
        self.assertIn('individual registered and verified',english['privacy'][0][1])
        self.assertIn('not identify a separate company',english['privacy'][0][1])
        self.assertIn('Indonesian law, subject to applicable mandatory consumer and privacy laws',english['terms'][-2][1])
        self.assertIn('Mandatory consumer rights',english['terms'][-2][1])
        self.assertIn('not an effective date',english['draft'])

    def test_pages_archive_is_only_public_static_files_and_cannot_publish_drafts(self):
        spec=importlib.util.spec_from_file_location('pages',ROOT/'scripts/prepare-pages-artifact.py')
        pages=importlib.util.module_from_spec(spec);spec.loader.exec_module(pages)
        archive=ROOT/'website/pages-preview/pages-artifact.tar.gz'
        with tarfile.open(archive) as tar:
            self.assertEqual(pages.EXPECTED,{m.name for m in tar.getmembers()})
            for m in tar.getmembers():
                self.assertTrue(m.isfile(),m.name)
                self.assertEqual((ROOT/'website/build'/m.name).read_bytes(),tar.extractfile(m).read())
        with self.assertRaisesRegex(ValueError,'Publication blocked'):
            pages.validate_site(ROOT/'website/build',publication=True)
        # Reject accidental private files or symlinks before packaging them.
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            (root/'private.env').write_text('fixture only')
            with self.assertRaisesRegex(ValueError,'Unexpected Pages input'):
                pages.validate_site(root)
            (root/'private.env').unlink()
            (root/'linked').symlink_to(ROOT/'website/build/style.css')
            with self.assertRaisesRegex(ValueError,'symbolic or hard links'):
                pages.validate_site(root)

    def test_pages_template_is_inactive_and_active_ci_cannot_deploy(self):
        template=ROOT/'.github/pages-deploy.yml.template'
        self.assertTrue(template.is_file())
        for path in (ROOT/'.github/workflows').glob('*'):
            if path.suffix not in ('.yml','.yaml'):continue
            text=path.read_text()
            self.assertNotRegex(text,r'(deploy-pages|upload-pages-artifact|configure-pages)@')
            self.assertNotRegex(text,r'(pages|id-token):\s*write')
        text=template.read_text()
        for required in ('workflow_dispatch:','approved_commit:','PUBLISH KOMPREXO PAGES',
                         'needs: build','name: github-pages','--publication'):
            self.assertIn(required,text)
        self.assertIn('persist-credentials: false',text)
        self.assertNotIn('uses: actions/configure-pages@',text)
        self.assertEqual(1,len(re.findall(r'pages:\s*write',text)))
        self.assertEqual(1,len(re.findall(r'id-token:\s*write',text)))
        self.assertNotRegex(text,r'(?m)^\s+(push|pull_request):')
        self.assertFalse(list((ROOT/'website/build').rglob('CNAME')))

    def test_host_privacy_disclosure_is_present_in_all_offline_and_web_policies(self):
        url='https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement'
        for locale in LOCALES:
            with self.subTest(locale=locale):
                source=json.loads((ROOT/f'content/legal/{locale}.json').read_text())
                disclosure=source['privacy'][5][1]
                self.assertIn('GitHub Pages',disclosure)
                self.assertIn('IP',disclosure)
                self.assertIn(url,disclosure)
                offline=(ROOT/f'app/src/main/assets/legal/{locale}/privacy.txt').read_text()
                self.assertIn(disclosure,offline)
                html=(ROOT/f'website/build/{locale}/privacy/index.html').read_text()
                parsed=Links();parsed.feed(html)
                self.assertIn(url,parsed.links)
        english=json.loads((ROOT/'content/legal/en.json').read_text())['privacy'][5][1]
        for required in ('logs and stores','for security','not signed in',
                         'does not upload images','not verified a site-specific retention',
                         'not publicly deployed yet'):
            self.assertIn(required,english)

    def test_release_sdk_inventory_preserves_all_artifacts_and_evidence_boundaries(self):
        inventory=json.loads((ROOT/'docs/google-play/SDK_PRIVACY_INVENTORY.json').read_text())
        licenses=json.loads((ROOT/'docs/legal/RUNTIME_DEPENDENCIES.json').read_text())
        self.assertEqual({x['coordinate'] for x in licenses},{x['artifact'] for x in inventory})
        self.assertEqual(len(inventory),len({x['artifact'] for x in inventory}))
        required={'artifact','resolved_version','why_included','automatic_initialization',
            'introduced_permissions_and_components','relevant_data_types','data_leaves_device',
            'possible_recipients','processing_purposes','collection_and_sharing','encryption_and_deletion',
            'evidence_sources','confidence_level','unresolved_questions'}
        by_license={x['coordinate']:x for x in licenses}
        for row in inventory:
            with self.subTest(artifact=row['artifact']):
                self.assertTrue(required<=set(row))
                self.assertTrue(all(row[key] for key in required))
                self.assertEqual(row['artifact'].split(':')[-1],row['resolved_version'])
                self.assertEqual(by_license[row['artifact']]['pom_sha256'],row['pom_sha256'])
                self.assertRegex(row['artifact_sha256'],r'^[a-f0-9]{64}$')
                self.assertIn('UNKNOWN',row['confidence_level'])
        text=json.dumps(inventory)
        self.assertNotIn('/tmp/',text);self.assertNotIn('/home/',text)
        edges=json.loads((ROOT/'docs/google-play/RELEASE_DEPENDENCY_EDGES.json').read_text())
        for row in inventory:self.assertTrue(any(e['to']==row['artifact'] for e in edges))
        decision=(ROOT/'docs/legal/OWNER_PUBLICATION_DECISION.md').read_text()
        self.assertIn('**BLOCKED — SPECIFIC EVIDENCE REQUIRED.**',decision)

    def test_free_startup_google_checks_are_disclosed_without_all_offline_claim(self):
        labels={'en':'Free','id':'Gratis','es':'Gratis','pt-BR':'Grátis','hi':'मुफ़्त'}
        listings=json.loads((ROOT/'content/store-listing.json').read_text())
        for locale,label in labels.items():
            with self.subTest(locale=locale):
                data=json.loads((ROOT/f'content/legal/{locale}.json').read_text())
                purchases=data['privacy'][4][1]
                self.assertIn('Google Play',purchases);self.assertIn(label,purchases)
                self.assertIn('24',purchases)
                self.assertIn(label,listings[locale]['full'].split('\n\n')[-1])
        english=json.loads((ROOT/'content/legal/en.json').read_text())['privacy'][4][1]
        for fact in ('when the app opens or becomes active','Free users who have not started a purchase',
                     'may use the internet','does not begin only after checkout'):
            self.assertIn(fact,english)

    def test_store_listing_limits_and_contact(self):
        listings=json.loads((ROOT/'content/store-listing.json').read_text())
        self.assertEqual(set(LOCALES),set(listings))
        for locale,listing in listings.items():
            with self.subTest(locale=locale):
                self.assertEqual('Komprexo',listing['title'])
                for key,maximum in (('title',30),('short',80),('full',4000)):
                    self.assertGreater(len(listing[key]),0)
                    self.assertLessEqual(len(listing[key]),maximum)
                self.assertIn('komprexo.support@gmail.com',listing['full'])
                self.assertIn(listing['short'],(ROOT/'docs/google-play/STORE_LISTING.md').read_text())
    def test_catalog_parity_placeholders_and_generated_resources(self):
        base=json.loads((ROOT/'content/i18n/en.json').read_text())
        for locale,folder in zip(LOCALES,FOLDERS):
            with self.subTest(locale=locale):
                strings=json.loads((ROOT/f'content/i18n/{locale}.json').read_text())
                self.assertEqual(set(base),set(strings));self.assertEqual('Komprexo',strings['app_name'])
                xml=ET.parse(ROOT/f'app/src/main/res/{folder}/strings.xml').getroot()
                resources={x.get('name'):x.text or '' for x in xml}
                self.assertEqual(set(strings),set(resources))
                for key,value in strings.items():
                    self.assertTrue(value.strip(),key)
                    self.assertEqual(Counter(re.findall(r'%\d+\$[ds]',base[key])),Counter(re.findall(r'%\d+\$[ds]',value)),key)
                    self.assertEqual(value,resources[key].replace("\\'","'").replace('\\"','"'),key)
    def test_hindi_script_and_locale_configuration(self):
        strings=json.loads((ROOT/'content/i18n/hi.json').read_text())
        self.assertGreater(sum(bool(re.search('[\u0900-\u097f]',x)) for x in strings.values()),140)
        locales=ET.parse(ROOT/'app/src/main/res/xml/locales_config.xml').getroot()
        self.assertEqual(set(LOCALES),{x.get(NS+'name') for x in locales})
    def test_legal_drafts_and_offline_license_notices(self):
        for locale in LOCALES:
            for name in ('privacy','terms','premium','support','about'):
                text=(ROOT/f'app/src/main/assets/legal/{locale}/{name}.txt').read_text()
                self.assertIn('DRAFT',text);self.assertIn('komprexo.support@gmail.com',text)
                self.assertNotIn('support@example',text)
            licenses=(ROOT/f'app/src/main/assets/legal/{locale}/licenses.txt').read_text()
            self.assertIn('Apache License',licenses);self.assertIn('BSD',licenses)
            for dependency in json.loads((ROOT/'docs/legal/RUNTIME_DEPENDENCIES.json').read_text()):
                self.assertIn(dependency['coordinate'],licenses)
    def test_editorial_policy_facts_and_publication_gates(self):
        # Billing validity must not become a promise of permanent offline access;
        # each independent quota and pending legal decision must remain visible.
        shapes={'privacy':9,'terms':6,'premium':5,'support':5,'about':2}
        for locale in LOCALES:
            data=json.loads((ROOT/f'content/legal/{locale}.json').read_text())
            with self.subTest(locale=locale):
                self.assertIn('DRAFT',data['draft'])
                self.assertIn('OWNER ACTION REQUIRED',data['draft'])
                for page,size in shapes.items():
                    self.assertEqual(size,len(data[page]))
                    self.assertTrue(all(title.strip() and body.strip() for title,body in data[page]))
                for page in ('terms','premium','support'):
                    body=' '.join(x[1] for x in data[page])
                    for number in ('5','2','20','24'):
                        self.assertRegex(body,r'(?<![0-9])'+number+r'(?![0-9])')
                    self.assertIn('komprexo.support@gmail.com',body)
                for page in ('terms','premium'):
                    self.assertIn('Rp49.000',' '.join(x[1] for x in data[page]))
                self.assertIn('24',json.loads((ROOT/f'content/i18n/{locale}.json').read_text())['purchase_disclosure'])
                emails=re.findall(r'[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}',json.dumps(data))
                self.assertEqual({'komprexo.support@gmail.com'},set(emails))
        text=json.dumps(json.loads((ROOT/'content/legal/en.json').read_text()))
        for misleading in ('No automatic telemetry is sent','unlimited offline Premium','no data is collected','guaranteed refund'):
            self.assertNotIn(misleading,text)

    def test_adult_audience_is_distinct_from_age_enforcement_and_publication(self):
        listings=json.loads((ROOT/'content/store-listing.json').read_text())
        for locale in LOCALES:
            data=json.loads((ROOT/f'content/legal/{locale}.json').read_text())
            with self.subTest(locale=locale):
                sections=[data[page][-1] for page in ('privacy','terms','premium','support','about')]
                self.assertTrue(all(s==sections[0] for s in sections))
                self.assertRegex(sections[0][1],r'(?<![0-9])18(?![0-9])')
                self.assertIn('Google Play',sections[0][1])
                self.assertIn('18',listings[locale]['full'].split('\n\n')[0])
                self.assertIn('OWNER ACTION REQUIRED',data['draft'])
        english=json.loads((ROOT/'content/legal/en.json').read_text())
        audience=english['privacy'][-1][1]
        for required in ('general image utility','adults aged 18 and older','not marketed to children',
                         'does not verify age','does not guarantee that Google Play prevents minors',
                         'does not establish compliance with child-protection requirements'):
            self.assertIn(required,audience)
        self.assertIn('only after final approval and actual publication',english['draft'])
        declaration=(ROOT/'docs/google-play/TARGET_AUDIENCE_DECLARATION.md').read_text()
        for required in ('18 and over','NOT SUBMITTED','Restrict Minor Access','No in-app age verification',
                         'under 21','OWNER ACTION REQUIRED'):
            self.assertIn(required,declaration)

    def test_offline_policy_generation_matches_canonical_drafts(self):
        # Prevent divergence between the in-app text and shared website policy source.
        for locale in LOCALES:
            data=json.loads((ROOT/f'content/legal/{locale}.json').read_text())
            for page in ('privacy','terms','premium','support','about'):
                expected=data['draft']+'\n\n'+'\n\n'.join('# '+title+'\n\n'+body for title,body in data[page])+'\n'
                self.assertEqual(expected,(ROOT/f'app/src/main/assets/legal/{locale}/{page}.txt').read_text(),(locale,page))

    def test_svg_exports_are_self_contained(self):
        for path in (ROOT/'artwork').glob('*.svg'):
            tree=ET.parse(path)
            self.assertEqual('{http://www.w3.org/2000/svg}svg',tree.getroot().tag)
            for element in tree.iter():
                self.assertFalse(element.tag.endswith(('script','image','foreignObject')))
                self.assertFalse(any('href' in key for key in element.attrib))
                self.assertNotIn('url(http',str(element.attrib))
    def test_png_integrity_and_store_specification(self):
        for path in (ROOT/'artwork').glob('*.png'):png(path)
        self.assertEqual((512,512,8,6,0,0,0),png(ROOT/'artwork/play-store-icon-512.png'))
        for density,size in (('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)):
            for resource in ('ic_launcher','ic_launcher_round'):
                self.assertEqual((size,size),png(ROOT/f'app/src/main/res/mipmap-{density}/{resource}.png')[:2])
        self.assertGreater((ROOT/'artwork/brand-contact-sheet.png').stat().st_size,1000)
        self.assertGreater((ROOT/'artwork/launcher-mask-preview.png').stat().st_size,1000)
    def test_adaptive_icon_refs_and_safe_zone(self):
        manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot().find('application')
        self.assertEqual('@mipmap/ic_launcher',manifest.get(NS+'icon'))
        self.assertEqual('@mipmap/ic_launcher_round',manifest.get(NS+'roundIcon'))
        for version in ('v26','v33'):
            for resource in ('ic_launcher','ic_launcher_round'):
                tree=ET.parse(ROOT/f'app/src/main/res/mipmap-anydpi-{version}/{resource}.xml').getroot()
                self.assertEqual('adaptive-icon',tree.tag)
                self.assertEqual('@drawable/ic_launcher_foreground',tree.find('foreground').get(NS+'drawable'))
                self.assertEqual(version=='v33',tree.find('monochrome') is not None)
        # Every K vertex is inside the central 66dp safe circle of the 108dp layer.
        vertices=[(170,156),(216,156),(216,356),(170,356),(216,244),(300,156),(358,156),(250,269),(216,252),(248,235),(358,356),(298,356)]
        radius=512*66/108/2
        for x,y in vertices:self.assertLessEqual((x-256)**2+(y-256)**2,radius**2)
    def test_site_all_routes_and_internal_assets(self):
        output=ROOT/'website/build'
        pages=list(output.rglob('*.html'));self.assertEqual(36,len(pages))
        for locale in LOCALES:
            for route in ('','privacy','terms','premium','support','about'):
                path=output/locale/route/'index.html'
                parsed=Links();parsed.feed(path.read_text())
                self.assertEqual(locale,parsed.lang)
                self.assertFalse(parsed.scripts)
                self.assertTrue(set(('main','nav','header','footer')) <= set(parsed.tags))
                self.assertIn('mailto:komprexo.support@gmail.com',parsed.links)
        for path in pages:
            parsed=Links();parsed.feed(path.read_text())
            for href in parsed.links:
                url=urlparse(href)
                if url.scheme or not url.path:continue
                dest=(path.parent/unquote(url.path)).resolve()
                self.assertTrue(dest.is_relative_to(output.resolve()),href)
                if dest.is_dir():dest=dest/'index.html'
                self.assertTrue(dest.exists(),(path,href))
        self.assertFalse(list(output.rglob('CNAME')))
        self.assertTrue((output/'.nojekyll').exists())

if __name__=='__main__':unittest.main(verbosity=2)
