"""Meaningful source/export/link/localization regression checks; Python stdlib only."""
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlparse, unquote
import json
import re
import struct
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
