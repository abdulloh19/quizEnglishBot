import json,re,random
from pathlib import Path
p=Path('vocabulary-data')
for level in ['a2','b1','b2']:
 rows=json.loads((p/(level+'.json')).read_text(encoding='utf-8'))
 suspicious=[w for w in rows if len(w['uzbek'])<3 or re.search(r'\b(?:adj|adv|vt|vi|noun|verb|pl|comp|superl|past|pp|aux|prep|pron|the|and)\b|[=~\[\]\d]',w['uzbek'])]
 print(level,'suspicious',len(suspicious))
 for w in suspicious: print(w['english'],'|',w['uzbek'],'|',w['example'])
 random.seed(42)
 print('REVIEW SAMPLE',level)
 for w in random.sample(rows,25):print(w['english'],'|',w['uzbek'],'|',w['example'])
