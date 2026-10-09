"""Generate the five complete Android catalogs from reviewed, versioned JSON sources."""
import json
from pathlib import Path
from xml.sax.saxutils import escape
ROOT=Path(__file__).resolve().parents[1]
FOLDERS={'en':'values','id':'values-in','es':'values-es','pt-BR':'values-pt-rBR','hi':'values-hi'}
for locale,folder in FOLDERS.items():
    data=json.loads((ROOT/f'content/i18n/{locale}.json').read_text())
    target=ROOT/f'app/src/main/res/{folder}'
    target.mkdir(exist_ok=True)
    lines=['<resources>']
    for key,text in data.items():
        text=text.replace("\\'","'").replace('\\"','"')
        escaped=escape(text).replace("'","\\'").replace('"','\\"')
        lines.append(f'    <string name="{key}">{escaped}</string>')
    lines.append('</resources>')
    (target/'strings.xml').write_text('\n'.join(lines)+'\n')
print('Generated complete en/id/es/pt-BR/hi Android resource catalogs')
