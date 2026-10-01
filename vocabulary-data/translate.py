import json,re,time,urllib.request,urllib.parse,concurrent.futures
from pathlib import Path
root=Path(__file__).resolve().parent
selected=json.loads((root/'selected-source.json').read_text(encoding='utf-8'))
cachefile=root/'translation-cache.json'
cache=json.loads(cachefile.read_text(encoding='utf-8')) if cachefile.exists() else {}
terms=sorted({('to '+w['english']) if w['pos']=='verb' else w['english'] for words in selected.values() for w in words})
pending=[t for t in terms if t not in cache]
def translate(batch):
    query='\n'.join(f'{i+1}. {term}' for i,term in enumerate(batch))
    url='https://translate.googleapis.com/translate_a/single?'+urllib.parse.urlencode({'client':'gtx','sl':'en','tl':'uz','dt':'t','q':query})
    for attempt in range(4):
        try:
            req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'})
            with urllib.request.urlopen(req,timeout=45) as response:data=json.load(response)
            text=''.join(part[0] or '' for part in data[0])
            found=re.findall(r'(?:^|\n)\s*(\d+)\s*[.)]\s*(.*?)(?=\n\s*\d+\s*[.)]|$)',text,re.S)
            mapped={int(i):value.strip() for i,value in found}
            if set(mapped)!=set(range(1,len(batch)+1)):raise ValueError('Translation line count differs')
            return dict(zip(batch,[mapped[i+1] for i in range(len(batch))]))
        except Exception:
            if attempt==3:raise
            time.sleep(2**attempt)
    return {}
batches=[pending[i:i+35] for i in range(0,len(pending),35)]
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
    jobs={executor.submit(translate,batch):batch for batch in batches}
    for n,future in enumerate(concurrent.futures.as_completed(jobs),1):
        cache.update(future.result())
        cachefile.write_text(json.dumps(cache,ensure_ascii=False,indent=2),encoding='utf-8')
        if n%10==0 or n==len(batches):print(f'Translated {len(cache)}/{len(terms)} unique terms',flush=True)
print('Translation complete',flush=True)
