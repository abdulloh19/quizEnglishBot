import json,hashlib,re,zipfile
from pathlib import Path
p=Path(__file__).resolve().parent
report={'validation_date':'2026-10-01','bot_repository_load':'passed for all three files over local HTTP','linguistic_review':'partial; not fully expert-reviewed','files':{}}
for level,count in [('a2',1000),('b1',1500),('b2',2000)]:
 file=p/(level+'.json'); data=json.loads(file.read_text(encoding='utf-8'))
 assert len(data)==count
 assert len({w['id'] for w in data})==count
 assert len({w['english'].casefold() for w in data})==count
 assert all(set(w)=={'id','english','uzbek','example'} for w in data)
 assert all(isinstance(w['id'],int) and w['id']>0 and all(isinstance(w[k],str) and w[k].strip() for k in ('english','uzbek','example')) for w in data)
 report['files'][file.name]={'records':count,'unique_ids':count,'unique_words':count,'blank_fields':0,'unique_translations':len({w['uzbek'] for w in data}),'sha256':hashlib.sha256(file.read_bytes()).hexdigest()}
(p/'validation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
with zipfile.ZipFile(p/'english-bot-A2-B1-B2.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for name in ['a2.json','b1.json','b2.json','UPLOAD-README.md','DATA-SOURCES.md','validation-report.json']:
  archive.write(p/name,name)
print(json.dumps(report,ensure_ascii=False,indent=2))
print('ZIP:',(p/'english-bot-A2-B1-B2.zip').stat().st_size,'bytes')
