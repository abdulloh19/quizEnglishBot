import json,csv,re
from pathlib import Path
p=Path('vocabulary-data/sources')
words=json.loads((p/'openjam-words.json').read_text(encoding='utf-8-sig'))
lookup={w['english'].lower():w for w in words}
rows=list(csv.DictReader((p/'cefrj-vocabulary-profile-1.5.csv').open(encoding='utf-8-sig')))
for level in ['A2','B1','B2']:
    selected={r['headword'].lower() for r in rows if r['CEFR']==level}
    matched=[lookup[w] for w in selected if w in lookup]
    with_example=[w for w in matched if any(s.get('example_en') and re.search(r'\b'+re.escape(w['english'])+r'(?:s|es|ed|ing)?\b',s['example_en'],re.I) for s in w['senses'])]
    print(level,len(selected),len(matched),len(with_example))
