"""Render original Komprexo vector masters and deterministic Android/PNG exports.
Optional regeneration: CairoSVG 2.8.2 + fonttools; DejaVu Sans outlines.
No network request, generator service or font embedding in the application.
"""
from pathlib import Path
import math
import xml.etree.ElementTree as ET
import cairosvg
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'artwork'
ART.mkdir(exist_ok=True)
BLUE, TEAL, WHITE = '#123B68', '#52E0D1', '#F5F8FC'
K = 'M170 156H216V356H170Z M216 244L300 156H358L250 269Z M216 252L248 235L358 356H298Z'
font_path = Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
font = TTFont(font_path)
glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
units = font['head'].unitsPerEm

def wordmark(color):
    paths, x = [], 360
    scale = 142 / units
    for char in 'Komprexo':
        glyph = glyphs[cmap[ord(char)]]
        pen = SVGPathPen(glyphs)
        glyph.draw(pen)
        paths.append(f'<path fill="{color}" transform="translate({x:.4f} 210) scale({scale:.8f} {-scale:.8f})" d="{pen.getCommands()}"/>')
        x += glyph.width * scale
    return ''.join(paths)

def svg(name, width, height, body):
    text = f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title"><title id="title">Komprexo</title>{body}</svg>\n'
    path = ART / f'{name}.svg'
    path.write_text(text)
    ET.fromstring(text)
    cairosvg.svg2png(bytestring=text.encode(),write_to=str(ART / f'{name}.png'))
    return text

icon = svg('icon',512,512,f'<rect width="512" height="512" fill="{BLUE}"/><path fill="{TEAL}" d="{K}"/>')
(ART/'komprexo-master.svg').write_text(icon)
for name,bg,text in [('logo-light',WHITE,BLUE),('logo-dark',BLUE,WHITE)]:
    svg(name,1120,320,f'<rect width="1120" height="320" fill="{bg}"/><g transform="scale(.625)"><rect width="512" height="512" fill="{BLUE}"/><path fill="{TEAL}" d="{K}"/></g>'+wordmark(text))
svg('logo-horizontal',1120,320,f'<g transform="scale(.625)"><rect width="512" height="512" fill="{BLUE}"/><path fill="{TEAL}" d="{K}"/></g>'+wordmark(BLUE))
svg('logo-primary',1120,320,f'<rect width="1120" height="320" fill="{WHITE}"/><g transform="scale(.625)"><rect width="512" height="512" fill="{BLUE}"/><path fill="{TEAL}" d="{K}"/></g>'+wordmark(BLUE))
# Store artwork is square, opaque and unmasked; Play applies its own corner treatment.
cairosvg.svg2png(bytestring=icon.encode(),write_to=str(ART/'play-store-icon-512.png'))
Image.open(ART/'play-store-icon-512.png').convert('RGBA').save(ART/'play-store-icon-512.png')
res=ROOT/'app/src/main/res'
for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    folder=res/f'mipmap-{density}';folder.mkdir(exist_ok=True)
    for resource in ['ic_launcher','ic_launcher_round']:
        shape='<circle cx="256" cy="256" r="256"' if resource.endswith('_round') else '<rect width="512" height="512" rx="96"'
        legacy=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">{shape} fill="{BLUE}"/><path fill="{TEAL}" d="{K}"/></svg>'
        cairosvg.svg2png(bytestring=legacy.encode(),output_width=size,output_height=size,write_to=str(folder/f'{resource}.png'))
# Foreground in 108dp coordinates. K vertices fit the central 66dp safe circle.
for name,color in [('ic_launcher_foreground',TEAL),('ic_launcher_monochrome','#000000')]:
    text=f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="512" android:viewportHeight="512"><path android:fillColor="{color}" android:pathData="{K}"/></vector>\n'
    (res/'drawable'/f'{name}.xml').write_text(text)
for version in ['v26','v33']:
    folder=res/f'mipmap-anydpi-{version}';folder.mkdir(exist_ok=True)
    mono='<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>' if version=='v33' else ''
    for name in ['ic_launcher','ic_launcher_round']:
        (folder/f'{name}.xml').write_text('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/brand_blue"/><foreground android:drawable="@drawable/ic_launcher_foreground"/>'+mono+'</adaptive-icon>\n')
(res/'values'/'brand_colors.xml').write_text(f'<resources><color name="brand_blue">{BLUE}</color><color name="brand_teal">{TEAL}</color></resources>\n')
# Mask and safe-zone evidence rendered from the actual master.
im=Image.open(ART/'icon.png').convert('RGBA')
sheet=Image.new('RGB',(1200,800),WHITE)
for index,name in enumerate(['logo-light','logo-dark','logo-horizontal']):
    logo=Image.open(ART/f'{name}.png').convert('RGBA');logo.thumbnail((1100,210));sheet.paste(logo,(50,20+index*220),logo)
sheet.save(ART/'brand-contact-sheet.png')
preview=Image.new('RGB',(1120,330),WHITE)
for index,shape in enumerate(['circle','rounded','square','safe-zone']):
    item=im.resize((240,240));mask=Image.new('L',(240,240));d=ImageDraw.Draw(mask)
    if shape=='circle':d.ellipse((0,0,239,239),fill=255)
    elif shape=='rounded':d.rounded_rectangle((0,0,239,239),radius=60,fill=255)
    else:d.rectangle((0,0,239,239),fill=255)
    preview.paste(item,(20+index*280,20),mask)
    draw=ImageDraw.Draw(preview);draw.text((20+index*280,280),shape,fill=BLUE)
    if shape=='safe-zone':draw.ellipse((20+index*280+47,67,20+index*280+193,213),outline=TEAL,width=2)
preview.save(ART/'launcher-mask-preview.png')
print('Generated SVG masters, PNG exports, 512x512 store icon and API23/26/33 launcher resources')
