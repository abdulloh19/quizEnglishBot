exec(open('vocabulary-data/inspect_sources.py',encoding='utf-8-sig').read().split('for level')[0])
for level in ['A2','B1','B2']:
    selected={r['headword'].lower() for r in rows if r['CEFR']==level}
    candidates=[]
    for w in selected:
        if w not in lookup: continue
        examples=[s['example_en'] for s in lookup[w]['senses'] if s.get('example_en') and len(s['example_en'].split())>=5 and re.search(r'\b'+re.escape(w)+r'(?:s|es|ed|ing)?\b',s['example_en'],re.I)]
        if examples: candidates.append(w)
    print(level,len(candidates))
