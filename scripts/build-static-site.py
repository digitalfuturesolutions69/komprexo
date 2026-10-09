"""Dependency-free Pages-compatible generation. No deployment or CNAME."""
from pathlib import Path
import json
import re
import shutil
from html import escape

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'website/build'
NAMES = {'en': 'English', 'id': 'Bahasa Indonesia', 'es': 'Español',
         'pt-BR': 'Português (Brasil)', 'hi': 'हिन्दी'}
LABELS = {'privacy': 'privacy_policy', 'terms': 'terms_of_use',
          'premium': 'premium_information', 'support': 'contact_support',
          'about': 'about_komprexo'}
COPY = {
    'en': ('Smaller photos. More room.', 'Download from Google Play after publication.',
           'Preview source — not publicly deployed.', 'Skip to content'),
    'id': ('Foto lebih kecil. Ruang lebih lega.', 'Unduh dari Google Play setelah publikasi.',
           'Sumber pratinjau — belum dipublikasikan.', 'Lewati ke konten'),
    'es': ('Fotos más pequeñas. Más espacio.', 'Descarga de Google Play después de publicación.',
           'Vista previa — sin despliegue público.', 'Saltar al contenido'),
    'pt-BR': ('Fotos menores. Mais espaço.', 'Download Google Play após publicação.',
              'Prévia — sem implantação pública.', 'Pular para conteúdo'),
    'hi': ('छोटे चित्र। अधिक जगह।', 'प्रकाशन के बाद Google Play से डाउनलोड करें।',
           'पूर्वावलोकन स्रोत — सार्वजनिक प्रकाशन नहीं।', 'मुख्य सामग्री पर जाएँ'),
}


def rich(text):
    safe = escape(text)
    safe = re.sub(r'https://support\.google\.com/googleplay/answer/\d+',
                  lambda m: f'<a href="{m[0]}" rel="noopener noreferrer">{m[0]}</a>', safe)
    return safe.replace('komprexo.support@gmail.com',
                        '<a href="mailto:komprexo.support@gmail.com">komprexo.support@gmail.com</a>')


if OUT.exists():
    shutil.rmtree(OUT)
OUT.mkdir(parents=True)
for locale in NAMES:
    strings = json.loads((ROOT / f'content/i18n/{locale}.json').read_text())
    legal = json.loads((ROOT / f'content/legal/{locale}.json').read_text())
    hero, download, notice, skip = COPY[locale]
    for page in ['', *LABELS]:
        locations = [Path(locale) / page] + ([Path(page)] if locale == 'en' else [])
        for location in locations:
            prefix = '../' * len(location.parts)
            nav = ''.join(f'<a href="{prefix}{locale}/{key}/">{escape(strings[label])}</a>'
                          for key, label in LABELS.items())
            languages = ''.join(
                f'<a lang="{tag}" hreflang="{tag}" href="{prefix}{tag}/'
                + (page + '/' if page else '') + '"'
                + (' aria-current="true"' if tag == locale else '')
                + f'>{name}</a>' for tag, name in NAMES.items())
            if page:
                title = strings[LABELS[page]]
                body = f'<h1>{escape(title)}</h1>'
                if page in LABELS:
                    body += f'<aside class="draft">{escape(legal["draft"])}</aside>'
                body += ''.join(f'<section><h2>{escape(heading)}</h2><p>{rich(text)}</p></section>'
                                for heading, text in legal[page])
            else:
                title = hero
                body = f'<section class="hero"><p class="eyebrow">Komprexo · Android</p><h1>{escape(hero)}</h1><p class="lead">{escape(strings["home_description"])}</p><p class="download" role="status">{escape(download)}</p><img src="{prefix}assets/icon.svg" width="180" height="180" alt="Komprexo"></section>'
                descriptions = [
                    ('compress', strings['maximum_size'] + ' · ' + strings['quality_first'] + ' / ' + strings['balanced']),
                    ('resize', strings['resize_independent']),
                    ('convert', strings['error_format'] + ' ' + strings['jpeg_alpha']),
                ]
                body += '<section><div class="grid">' + ''.join(
                    f'<article><h2>{escape(strings["home_" + key])}</h2><p>{escape(text)}</p></article>'
                    for key, text in descriptions) + '</div></section>'
                body += f'<section class="local"><h2>{escape(strings["privacy_short"])}</h2></section>'
                benefits = ''.join(f'<li>{escape(strings[key])}</li>' for key in
                                   ['benefit_unlimited', 'benefit_batch', 'benefit_presets', 'benefit_future_no_ads', 'benefit_quality'])
                body += f'<section class="plans"><article><h2>{escape(strings["free_status"])}</h2><p>{escape(strings["free_plan_summary"])}</p></article><article><h2>Komprexo Premium</h2><p>{escape(strings["premium_price"])}</p><p>{escape(strings["price_provisional"])}</p><ul>{benefits}</ul><p>{escape(strings["purchase_disclosure"])}</p><a href="{prefix}{locale}/premium/">{escape(strings["premium_information"])}</a></article></section>'
            html = f'''<!doctype html>
<html lang="{locale}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="description" content="{escape(strings['home_description'], quote=True)}"><title>{escape(title)} · Komprexo</title><link rel="icon" href="{prefix}assets/icon.svg" type="image/svg+xml"><link rel="stylesheet" href="{prefix}style.css"></head>
<body><a class="skip" href="#main">{escape(skip)}</a><header><a class="brand" href="{prefix}{locale}/"><img src="{prefix}assets/icon.svg" width="44" height="44" alt="">Komprexo</a><nav aria-label="{escape(strings['legal_support'])}">{nav}</nav></header><nav class="languages" aria-label="{escape(strings['language'])}">{languages}</nav><aside class="notice">{escape(notice)}</aside><main id="main">{body}</main><footer><p><a href="mailto:komprexo.support@gmail.com">komprexo.support@gmail.com</a></p><a href="{prefix}{locale}/privacy/">{escape(strings['privacy_policy'])}</a> · <a href="{prefix}{locale}/terms/">{escape(strings['terms_of_use'])}</a></footer></body></html>'''
            folder = OUT / location
            folder.mkdir(parents=True, exist_ok=True)
            (folder / 'index.html').write_text(html)
shutil.copy(ROOT / 'website/style.css', OUT / 'style.css')
(OUT / 'assets').mkdir()
shutil.copy(ROOT / 'artwork/icon.svg', OUT / 'assets/icon.svg')
(OUT / '.nojekyll').write_text('')
print('Static build: 30 localized pages + 6 English root aliases. NOT DEPLOYED.')
