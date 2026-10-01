import json,re
from collections import defaultdict
from pathlib import Path
p=Path(__file__).resolve().parent
reference=json.loads((p/'sources/eng-uzb-reference.json').read_text(encoding='utf-8-sig'))['words']
lookup=defaultdict(list)
for entry in reference:
    key=re.sub(r'\s*\([^)]*\)','',entry['eng']).strip()
    key=re.sub(r'\s+\d+$','',key).strip().lower()
    lookup[key].append(entry['uzb'])
selected=json.loads((p/'selected-source.json').read_text())
overrides={}
for line in (p/'editorial-overrides.psv').read_text(encoding='utf-8-sig').splitlines():
    word,uz,example=line.split('|',2);overrides[word]=(uz,example)
missing=[]
out={}
for level,words in selected.items():
    entries=[]
    for word in words:
        en=word['english'];pos=word['pos']
        options=lookup.get(en,[])
        pattern={'noun':r'\bn\b','verb':r'\b(?:vt|vi|v)\b','adjective':r'\badj\b','adverb':r'\badv\b'}.get(pos,r'\bUNKNOWN\b')
        preferred=[t for t in options if re.search(pattern,t[:40])]
        text=(preferred or options or [''])[0]
        text=re.sub(r'\([^)]*\)|\[[^]]*\]','',text)
        text=re.sub(r'^(?:\s|\d+[).]?|\b(?:n|vt|vi|v|adj|adv|pron|prep|conj|det|interj|pl|sing|US|UK|infml|fml)\b|[,;:/])+','',text).strip()
        text=re.split(r';|\b2\)|\s:\s|,',text)[0].strip().rstrip('.')
        if '~' in text:text=text.split('~')[0].strip()
        # Faqat qisqa so‘z ekvivalentlari; uzun lug‘at izohlarini ko‘chirmaymiz.
        if len(text.split())>5 or not text or re.search(r'\b(?:vt|vi|adj|adv|pl)\b|\d|[\[\]~]',text):text=''
        example=word['example']
        if en in overrides:text,example=overrides[en]
        text=text.replace("o'",'o‘').replace("g'",'g‘')
        if not text:missing.append(word)
        example=example.strip()
        if example:
            example=example[0].upper()+example[1:]
            if example[-1] not in '.!?':example+='.'
        entries.append({'id':word['id'],'english':en,'uzbek':text,'example':example})
    out[level]=entries
(p/'draft-files.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
(p/'missing-translations.json').write_text(json.dumps(missing,ensure_ascii=False,indent=2),encoding='utf-8')
print('Missing:',len(missing))
for w in missing:print(w['english']+'|'+w['pos'])
print('Sample:')
for level,words in out.items():
    for w in words[::max(1,len(words)//20)]: print(level,w['english'],w['uzbek'],w['example'])
