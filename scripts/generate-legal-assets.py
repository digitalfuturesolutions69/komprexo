"""Single-source localized legal drafts for offline Android and static pages."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
for source in (ROOT/'content/legal').glob('*.json'):
    locale=source.stem;data=json.loads(source.read_text())
    folder=ROOT/f'app/src/main/assets/legal/{locale}';folder.mkdir(parents=True,exist_ok=True)
    for page,sections in data.items():
        if page=='draft':continue
        text=data['draft']+'\n\n'+'\n\n'.join('# '+title+'\n\n'+body for title,body in sections)+'\n'
        (folder/f'{page}.txt').write_text(text)
        if locale=='en':
            names={'privacy':'PRIVACY_POLICY','terms':'TERMS_OF_USE','premium':'PREMIUM_PURCHASE_POLICY','support':'SUPPORT_GUIDE','about':'ABOUT'}
            target=ROOT/('docs/support' if page=='support' else 'docs/legal');target.mkdir(parents=True,exist_ok=True)
            (target/f'{names[page]}.md').write_text('# Komprexo — '+page.title()+'\n\n'+text)
print('Generated five-language offline legal/support documents; all policies remain DRAFT')
