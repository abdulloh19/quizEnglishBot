exec(open('vocabulary-data/build_draft.py',encoding='utf-8-sig').read().split('selected=')[0])
for word in ['appropriate','material','equity','inflate','upright','deep','challenge','tablet','application','punctuation']:
 print(word,lookup[word])
