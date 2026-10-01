# A2, B1 va B2 vocabulary fayllari

Botga yuklash uchun tayyor UTF-8 JSON fayllar:

| Fayl | So‘zlar |
|---|---:|
| a2.json | 1 000 |
| b1.json | 1 500 |
| b2.json | 2 000 |

Har bir yozuvda `id`, `english`, `uzbek`, `example` mavjud. IDlar har bir daraja ichida 1 dan boshlanadi va unique. Bir fayl ichida inglizcha so‘zlar takrorlanmaydi. Ayrim so‘zlar turli ma’nolari yoki so‘z turkumlari sabab boshqa darajada ham uchrashi mumkin.

## GitHubga yuklash

1. `abdulloh19/english-bot-data` repozitoriysini oching.
2. `main` branchida `Add file` → `Upload files` ni tanlang.
3. `a2.json`, `b1.json`, `b2.json` va `DATA-SOURCES.md`ni repo ildiziga yuklang. ZIPni avval oching; JSONlarni ichki papkaga joylamang.
4. Commit qiling va raw havolalarni ochib tekshiring:
   - https://raw.githubusercontent.com/abdulloh19/english-bot-data/refs/heads/main/a2.json
   - https://raw.githubusercontent.com/abdulloh19/english-bot-data/refs/heads/main/b1.json
   - https://raw.githubusercontent.com/abdulloh19/english-bot-data/refs/heads/main/b2.json
5. Oldingi cache yangilanishi uchun botni qayta ishga tushiring. Keyin darajani tanlab, yangi so‘z va testni tekshiring.

## Sifat va tekshiruv

CEFR darajalari CEFR-J ro‘yxatiga asoslangan. Misollar Openjam/WordNet manbalaridan moslashtirilgan yoki qo‘lda yozilgan; ayrimlari to‘liq gap emas, qo‘llanish iborasi bo‘lishi mumkin. Qisqa o‘zbekcha ekvivalentlar lug‘at ma’lumotlari asosida ajratilgan, yetishmaganlari qo‘lda yozilgan. Tanlab ko‘rilgan yozuvlardagi tarjima va misol nomuvofiqliklari tuzatilgan.

Bu tahrirlanadigan dastlabki lug‘at: barcha 4 500 yozuv to‘liq tilshunos ekspertizasidan o‘tmagan. Ayniqsa ko‘p ma’noli so‘zlar va darajaga mos ma’nolarni keyingi tahrirda ko‘rib chiqish kerak. Strukturaviy tekshiruv lingvistik xatosizlik kafolati emas.

Fayllar botning haqiqiy `WordRepository` kodi bilan lokal HTTP orqali yuklab tekshirildi: A2 — 1000, B1 — 1500, B2 — 2000. JSON sintaksisi, bo‘sh maydonlar, noyob ID va so‘zlar, test uchun yetarli turli tarjimalar tekshirildi. Natija va SHA-256 qiymatlari `validation-report.json`da.

GitHubga bu fayllar agent tomonidan yuklanmadi; foydalanuvchi o‘zi yuklaydi.
