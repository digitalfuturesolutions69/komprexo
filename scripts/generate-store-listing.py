"""Validate Play text limits and generate owner-review listing drafts."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
data=json.loads((ROOT/'content/store-listing.json').read_text())
text='# Google Play Store listing — DRAFT\n\nOwner/native-speaker review required; no Console submission. Title≤30, short≤80, full≤4000 characters. All counts validated against the canonical JSON. Current Play guidance must be confirmed again before upload.\n\n'
for locale,listing in data.items():
    for key,maximum in [('title',30),('short',80),('full',4000)]:
        assert 0<len(listing[key])<=maximum,(locale,key,len(listing[key]))
    text+=f"## {locale}\n\nTitle ({len(listing['title'])}): {listing['title']}\n\nShort ({len(listing['short'])}): {listing['short']}\n\nFull ({len(listing['full'])}):\n\n{listing['full']}\n\n"
text+='''## Assets and owner checklist

App title Komprexo; positioning Photo Compressor, Resize & Convert; category photo/image utility subject to owner Console selection. No fixed compression ratio, rating/download/award/certification claims.

Store icon artwork/play-store-icon-512.png: actual512x512 RGBA32-bit opaque unmasked artwork. Feature graphic **specification only**,1024x500 JPEG or24-bit PNG/no transparency per current Play requirements; owner-approved final graphic not created. Keep essential K/title central and legible, no fake rating/award/download/price badge.

Screenshot plan: real Android Home/free three counters, compression before/after target/dimensions, batch settings/count, resize dimensions, convert format, Premium real Play localized price only after configured, language/settings/offline policies. At least2 phone screenshots per listing,320–3840px accepted current Play size range, longest dimension≤2×shortest; use high-resolution portrait/landscape real captures with no personal photos/payment IDs, and recommended16:9/9:16 at least1080px assets for eligibility where applicable. Do not use mock transactions as real payment evidence. CI compact screenshots are test evidence, not automatically approved Store marketing assets.

Support/developer public contact: komprexo.support@gmail.com (owner manually configures). Privacy URL **PLACEHOLDER — NOT LIVE**: proposed https://digitalfuturesolutions69.github.io/komprexo/privacy/ only after legal approval and authorized publication. Developer legal identity/address not supplied.

Owner must confirm target audience/children ages and country policy, complete content rating questionnaire, current ads declaration No, no-login app access, Billing/transport Data Safety and consumer rights. Review five-language translations and final assets, then Console setup/checklist. Policies remain DRAFT.

Official reference: https://support.google.com/googleplay/android-developer/answer/9866151
Text limit reference: https://support.google.com/googleplay/android-developer/answer/9859152
'''
(ROOT/'docs/google-play/STORE_LISTING.md').write_text(text)
print('PASS: five store listings, title/short/full limits; generated DRAFT')
