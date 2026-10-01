import json,re
from pathlib import Path
root=Path(__file__).resolve().parent
missing=json.loads((root/'missing-examples.json').read_text())
missing_keys={(w['level'],w['english']) for w in missing}
drafts=json.loads((root/'draft-files.json').read_text(encoding='utf-8'))
source=json.loads((root/'selected-source.json').read_text(encoding='utf-8'))
manual=dict(line.split('|',1) for line in (root/'manual-translations.psv').read_text(encoding='utf-8-sig').splitlines() if line)
overrides={}
for line in (root/'editorial-overrides.psv').read_text(encoding='utf-8-sig').splitlines():
    en,uz,example=line.split('|',2);overrides[en]=(uz,example)
# Aniqlangan ko‘p ma’noli so‘zlar uchun tarjima va misol bir xil ma’noda.
fixes={
'appropriate':('mos','Wear appropriate clothes for the interview.'),
'material':('material','Wood is a useful building material.'),
'equity':('mulkdagi sof ulush','Their equity in the house increased as they paid off the mortgage.'),
'inflate':('shishirmoq','Use this pump to inflate the bicycle tyre.'),
'upright':('tik','Keep the bottle upright to avoid spilling the water.'),
'deep':('chuqur','They travelled deep into the forest.'),
'application':('qo‘llash','The practical application of this theory can save energy.'),
'punctuation':('tinish belgilari','Check the punctuation before you submit your essay.'),
'challenge':('qiyin vazifa','Learning to drive was a real challenge for me.'),
'showcase':('namoyish vitrinası','The museum displayed the jewels in a glass showcase.'),
'it':('axborot texnologiyalari','She works in IT and maintains the school computers.'),
'pacific':('tinchliksevar','The two nations agreed to pursue a pacific solution.'),
'kindly':('mehribon','A kindly neighbour helped us carry the bags.'),
'hammer':('bolg‘a','He used a hammer to drive the nail into the wood.'),
'commission':('komissiya','An independent commission will investigate the incident.'),
'funk':('fank musiqasi','The band combines jazz with funk.'),
'adoption':('farzandlikka olish','The couple completed the adoption process last year.'),
'scale':('ko‘lam','We had not understood the scale of the problem.'),
'commitment':('majburiyat','Signing the contract is a serious commitment.'),
'pro':('professional','She plays tennis like a pro.'),
'max':('eng ko‘p miqdor','Turn the volume down from the max.'),
'cv':('tarjimayi hol hujjati','Please attach your CV to the application.'),
'ph':('kislotalilik ko‘rsatkichi','The scientist measured the pH of the water.'),
'corona':('Quyosh toji','The corona became visible during the solar eclipse.'),
'backup':('zaxira nusxa','Keep a backup of all your important files.'),
'breakdown':('buzilish','A mechanical breakdown delayed the train.'),
'breakup':('ajralish','She moved to another city after the breakup.'),
'getaway':('qochish','The thieves used a stolen car for their getaway.'),
'giveaway':('bepul tarqatiladigan narsa','The shop offered a small giveaway with every purchase.'),
'input':('kiritiladigan ma’lumot','The program checks the input before saving it.'),
'personification':('jonlantirish','The poet uses personification by giving the wind a voice.'),
'roommate':('xonadosh','My roommate studies at the same university.'),
'shooting':('otish','The loud shooting could be heard from the training ground.'),
'soviet':('sovet kengashi','The local soviet met to discuss the new policy.'),
'valentine':('sevgi tabrigi','He sent her a valentine in February.'),
'hammer:verb':('bolg‘alamoq','Please hammer the nail into this piece of wood.'),
'kindly:adverb':('mehr bilan','The nurse spoke kindly to the frightened child.'),
'material:adjective':('moddiy','They lost most of their material possessions in the fire.'),
'challenge:verb':('shubha ostiga qo‘ymoq','New evidence may challenge the accepted explanation.'),
'deep:adjective':('chuqur','The lake is too deep to stand in.'),
}
report={}
for level,words in drafts.items():
    for w,s in zip(words,source[level]):
        en=w['english'];pos=s['pos']
        # Qo‘lda yozilgan misollar faqat kerakli daraja va so‘zga qo‘llanadi.
        if en in overrides and (level,en) not in missing_keys:
            w['example']=s['example']
        if en in manual:w['uzbek']=manual[en]
        if (level,en) in missing_keys:w['uzbek'],w['example']=overrides[en]
        key=en+':'+pos
        if key in fixes or en in fixes:w['uzbek'],w['example']=fixes.get(key,fixes.get(en))
        if en=='showcase':w['uzbek']='namoyish vitrinasi'
        w['example']=w['example'].strip()
        if w['example']:
            w['example']=w['example'][0].upper()+w['example'][1:]
            if w['example'][-1] not in '.!?':w['example']+='.'
        for key in ['english','uzbek','example']:
            assert w[key].strip(),(level,en,key)
        assert w['id']>0
    assert len(words)=={'A2':1000,'B1':1500,'B2':2000}[level]
    assert len({w['english'].lower() for w in words})==len(words)
    assert len({w['id'] for w in words})==len(words)
    file=root/(level.lower()+'.json')
    file.write_text(json.dumps(words,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    report[level]={'words':len(words),'unique_ids':len({w['id'] for w in words}),'unique_english':len({w['english'] for w in words}),'empty_fields':0,'different_translations':len({w['uzbek'] for w in words})}
(root/'validation-report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
