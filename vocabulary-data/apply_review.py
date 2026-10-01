import json
from pathlib import Path
p=Path(__file__).resolve().parent
fixes={}
for line in (p/'review-corrections.psv').read_text(encoding='utf-8-sig').splitlines():
 en,uz,ex=line.split('|',2);fixes[en]=(uz,ex)
for level in ['a2','b1','b2']:
 file=p/(level+'.json');words=json.loads(file.read_text(encoding='utf-8'))
 for w in words:
  if w['english'] in fixes:w['uzbek'],w['example']=fixes[w['english']]
 file.write_text(json.dumps(words,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Applied reviewed corrections')
