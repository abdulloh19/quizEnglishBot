import json
from pathlib import Path
p=Path('vocabulary-data')
o={line.split('|')[0]:line for line in (p/'editorial-overrides.psv').read_text(encoding='utf-8-sig').splitlines() if line.strip()}
m=json.loads((p/'missing-examples.json').read_text())
print('Missing editorial entries:',[w['english'] for w in m if w['english'] not in o])
cache=json.loads((p/'translation-cache.json').read_text(encoding='utf-8')) if (p/'translation-cache.json').exists() else {}
print('Translations:',len(cache))
print('Sample:',dict(list(cache.items())[:30]))
