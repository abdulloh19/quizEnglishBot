import csv,json,re
from pathlib import Path
root=Path(__file__).resolve().parent
source=json.loads((root/'sources/openjam-words.json').read_text(encoding='utf-8-sig'))
lookup={w['english'].lower():w for w in source}
rows=list(csv.DictReader((root/'sources/cefrj-vocabulary-profile-1.5.csv').open(encoding='utf-8-sig')))
result={}
missing=[]
for level,count in [('A2',1000),('B1',1500),('B2',2000)]:
    candidates=[]
    for word in sorted({r['headword'].lower() for r in rows if r['CEFR']==level}):
        if word not in lookup or not re.fullmatch(r'[a-z]+(?:[ -][a-z]+)*',word):continue
        item=lookup[word]
        allowed={r['pos'] for r in rows if r['CEFR']==level and r['headword'].lower()==word}
        senses=[s for s in item['senses'] if s['part_of_speech'] in allowed]
        if not senses: continue
        matches=[s for s in senses if s.get('example_en') and re.search(r'\b'+re.escape(word)+r'(?:s|es|ed|ing)?\b',s['example_en'],re.I)]
        matches.sort(key=lambda s:(len(s['example_en'].split())>=5,-s.get('sense_order',1)),reverse=True)
        sense=matches[0] if matches else senses[0]
        example=sense.get('example_en','').strip() if matches else ''
        candidates.append({'english':word,'example':example,'pos':sense['part_of_speech'],'definition':sense['definition_en'],'rank':item.get('frequency_rank',999999),'level':level})
    candidates.sort(key=lambda w:(not bool(w['example']),w['rank'],w['english']))
    selected=candidates[:count]
    assert len(selected)==count
    selected.sort(key=lambda w:w['english'])
    for i,w in enumerate(selected,1):
        w['id']=i
        if not w['example']:missing.append(w)
    result[level]=selected
(root/'selected-source.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(root/'missing-examples.json').write_text(json.dumps(missing,ensure_ascii=False,indent=2),encoding='utf-8')
print('Selected:',{k:len(v) for k,v in result.items()})
print('Need examples:',len(missing))
for w in missing:print(w['english']+' | '+w['pos']+' | '+w['definition'])

