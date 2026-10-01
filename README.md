# English Vocabulary Bot — darajalar

Java 25, Maven va Pengrad Telegram Bot API ishlatiladi. So‘zlar faqat GitHub Raw orqali olinadi; local `words.json` endi o‘qilmaydi. `users.json` mavjud ma’lumotlari avtomatik o‘chirib yuborilmaydi.

## O‘zgargan fayllar

- `EnglishBot.java`: A1–C2 inline menyusi, `/start` tekshiruvi, darajani almashtirish, umumiy menyu metodlari va foydalanuvchiga xato xabarlari. `keyingi_soz`, `quiz:0`–`quiz:3`, `next_quiz` saqlangan. Savol user, daraja va xabar ID bilan bog‘langan; takroriy javob ball bermaydi.
- `User.java`: `level` fieldi va Lombok getter/setterlari; oldingi 10 argumentli constructor ham saqlangan.
- `WordRepository.java`: Java HttpClient, timeout, JSON validatsiyasi, darajalar bo‘yicha ConcurrentHashMap cache, `loadWords(level)` va `refreshLevel(level)`. Xatolar cache qilinmaydi; refresh xatosida oldingi cache saqlanadi.
- `WordService.java`: barcha so‘z qidirish metodlari darajani argument sifatida oladi. Local bazaga yozish olib tashlangan.
- `UserService.java`: yangi user uchun baza yuklanmaydi; daily words va so‘zlar ketma-ketligi user darajasidan olinadi. Kun almashganda daily ro‘yxat yangilanadi. Ikki to‘g‘ri javobdan keyin so‘z o‘rganilgan hisoblanadi.
- `QuizService.java`: savol bugungi so‘zlardan, variantlar shu darajadan; to‘rtta turli tarjima talab qilinadi.
- `UserRepository.java`: UTF-8 o‘qish/yozish, vaqtinchalik fayl orqali saqlash; o‘qish/yozish xatolari yashirilmaydi. Buzilgan JSON avtomatik bo‘sh ro‘yxat bilan almashtirilmaydi.
- `pom.xml`: takrorlangan Gson dependency olib tashlangan, Java 25 uchun Lombok annotation processor aniq ko‘rsatilgan, JUnit va Surefire qo‘shilgan.
- `src/test/java/uz/pdp/englishBot/LevelFlowTest.java`: local HTTP server bilan 9 ta avtomatik test.

`Word.java` va `QuizQuestion.java` mavjud fieldlari yetarli bo‘lgani uchun o‘zgartirilmadi. `Word.level` fayl nomidan to‘ldiriladi; JSON ichida uni yozish shart emas.

## JSON formati

Manba: https://raw.githubusercontent.com/abdulloh19/english-bot-data/refs/heads/main/

Repozitoriyning `main` branch ildiziga `a1.json`, `a2.json`, `b1.json`, `b2.json`, `c1.json`, `c2.json` joylashtiring. Har bir fayl UTF-8 JSON array bo‘lishi kerak. Masalan, test uchun yetarli minimal `a1.json`:

```json
[
  {"id": 1, "english": "apple", "uzbek": "olma", "example": "I eat an apple every day."},
  {"id": 2, "english": "book", "uzbek": "kitob", "example": "This is my book."},
  {"id": 3, "english": "water", "uzbek": "suv", "example": "I drink water."},
  {"id": 4, "english": "house", "uzbek": "uy", "example": "This is my house."}
]
```

`id` musbat va bitta daraja ichida unique bo‘lsin. Mavjud so‘zlarning IDlarini keyingi tahrirlarda o‘zgartirmang yoki boshqa so‘zga bermang. `english`, `uzbek`, `example` bo‘sh bo‘lmasin. So‘zlarni ko‘rsatish uchun 1 ta valid so‘z yetadi; testga kamida 4 ta turli tarjima kerak.

2026-09-30 tekshiruvi: remote `a1.json` HTTP 200, lekin unda faqat 3 ta `english`/`uzbek` obyekt bor; `id` va `example` yo‘q. Qolgan besh fayl HTTP 404 qaytardi. Shu sabab remote ma’lumotlar tuzatilmaguncha bot yuklash xabarini ko‘rsatadi. Kod testlari local HTTP serverdagi to‘liq format bilan bajarildi.

## Progress va cache

Daraja muvaffaqiyatli o‘zgarganda `dailyWordIds`, `wordOrder`, `currentWordIndex`, `dailyDate` va so‘z progressi reset qilinadi, so‘ng yangi daily words yaratiladi. Yangi progress `wordCorrectCountsByKey` ichida `level:english` kaliti bilan saqlanadi. Shu sabab JSONdagi ID boshqa so‘zga berilib qolsa, eski natija yangi so‘zga ko‘chmaydi. Eski `wordCorrectCounts` faqat backward compatibility uchun modelda qolgan va progress hisobida ishlatilmaydi. Eski user birinchi marta yangi versiyada ochilganda ishonchsiz ID progressi tozalanadi; `score`, `correctAnswer`, `wrongAnswer` umumiy tarixi saqlanadi.

Yuklash muvaffaqiyatsiz bo‘lsa daraja va progress o‘zgarmaydi. Eski userda `level` yo‘q bo‘lsa, keyingi `/start` yoki menu amali daraja tanlashni so‘raydi.

Har daraja birinchi foydalanilganda yuklanadi. GitHub yangilangach botni qayta ishga tushiring yoki shu repository instance uchun `refreshLevel("A1")` chaqiring. Refresh tugmasi UI da yo‘q. Bitta bot processida update’lar ketma-ket bajariladi; bir xil `users.json` bilan bir nechta bot processini parallel ishga tushirish qo‘llab-quvvatlanmaydi.

## Yangi so‘z va test oqimi

`🆕 Yangi so‘z` orqali userga ko‘rsatilgan so‘z IDsi `dailySeenWordIds`da saqlanadi. `❓ Test` savoli ham, noto‘g‘ri javob variantlari ham faqat bugun ko‘rsatilgan va hali o‘rganilmagan so‘zlardan olinadi. Ko‘rilmagan so‘z testga kirmaydi. User faqat bitta so‘z ko‘rgan bo‘lsa test bitta variant, ikki yoki uchta ko‘rgan bo‘lsa shuncha variant, to‘rt yoki undan ko‘p ko‘rgan bo‘lsa to‘rtta variant ko‘rsatadi.

`📅 Bugungi so‘zlar` tarixdagi barcha o‘rganilgan so‘zlarni aralashtirmaydi. Unda faqat bugun real ko‘rsatilgan so‘zlardan hali qolganlari va bugun testda o‘rganilganlari chiqadi.

Har bir so‘z o‘rganilishi uchun ikki marta to‘g‘ri topilishi kerak. Birinchi to‘g‘ri javob `1/2` progress beradi, lekin ball qo‘shmaydi. Ikkinchi to‘g‘ri javobda so‘z bitta o‘rganilgan so‘z sifatida hisoblanadi va faqat `+1 ball` beriladi. Shu so‘zga takroriy callback yoki javob yana ball qo‘shmaydi. `scoreCalculationVersion=2` eski ikki martalik ballarni bir martalik hisobga o‘tkazilganini belgilaydi; eski `score` va `correctAnswer` qiymatlari migratsiyada ikkiga bo‘linadi.

Joriy 15 ta so‘zning barchasi testda ikki martadan to‘g‘ri topilib 100% tugatilgach, `🆕 Yangi so‘z` bosilganda bot “Bugun yana yangi so‘z o‘rganasizmi?” deb so‘raydi. `✅ Ha` tanlansa keyingi 15 ta o‘rganilmagan so‘zdan yangi guruh yaratiladi va birinchi so‘z darhol ko‘rsatiladi. Bu darajadagi barcha so‘zlar tugaguncha shu tartib davom etadi. `❌ Yo‘q` bugungi mashg‘ulotni yakunlaydi.

## Google Translate

Asosiy menyuda ikkita mustaqil tarjima rejimi bor:

- `🇺🇿 UZ → EN` — o‘zbekcha matnni inglizchaga tarjima qiladi.
- `🇬🇧 EN → UZ` — inglizcha matnni o‘zbekchaga tarjima qiladi.

Yo‘nalish tanlangach user ketma-ket bir nechta matn yuborishi mumkin. Asosiy menyudagi boshqa amal bosilsa tarjima rejimi yopiladi. Bitta matn 1500 belgidan oshmasligi kerak. Google HTTP xatosi, limit yoki noto‘g‘ri javob qaytarsa bot ishlashda davom etadi va userga qayta urinish xabarini beradi.

`TranslationService` kalitsiz `translate.googleapis.com/translate_a/single` endpointidan foydalanadi. Bu Google Cloud Translation rasmiy SLAli API emas va Google uni limitlashi mumkin. Katta trafik yoki production kafolati kerak bo‘lsa, servisni Google Cloud Translation API autentifikatsiyasiga almashtirish kerak.

Mavjud A1–C2 fayllari uchun kodni o‘zgartirish shart emas. Boshqa daraja qo‘shilsa, `WordRepository.LEVELS` ro‘yxatiga nomini kiriting va GitHubga shu nomning kichik harfli `.json` faylini qo‘shing; keyboard avtomatik hosil bo‘ladi.

## Tekshirish

Java 25 va Maven bilan:

```shell
mvn test
```

Testlar yangi/eski user, ikki userning darajalari ajratilishi, level persistence/reset, statistika, kun almashishi, barcha so‘zlar o‘rganilishi, quiz variantlari yetishmasligi, cache/concurrent loading/refresh, HTTP 404, noto‘g‘ri/bo‘sh JSON, takroriy ID va tarmoq uzilishini qamrab oladi. Testlar real `users.json`ni o‘zgartirmaydi va Telegramga xabar yubormaydi.

GitHub fayllarini to‘ldirgach, `uz.pdp.Main`ni ishga tushirib Telegramda:

1. Yangi user: `/start` → A1–C2 keyboard → A1 → asosiy menyu.
2. `🆕 Yangi so‘z` → `⏭️ Keyingi so‘z`: faqat A1 daily words.
3. `❓ Test` → javob → `➡️ Keyingi test`: to‘rtta A1 varianti. Bir so‘zga ikki to‘g‘ri javobdan keyin u qolgan ro‘yxatdan chiqadi.
4. `📅 Bugungi so‘zlar` va `📊 Mening natijam`ni tekshiring.
5. Boshqa user B1 tanlasin; birinchi user hali A1 olishini tekshiring.
6. `⚙️ Darajani o‘zgartirish` → B1: so‘z progressi yangilanadi, umumiy statistika saqlanadi. Oldingi test javobi yangi progressga ta’sir qilmaydi.
7. Bot qayta ishga tushgach `/start`: saqlangan darajali userga asosiy menyu chiqadi.
8. GitHubdan yuklash xatosida ogohlantirish chiqishi va bot boshqa update’larni davom ettirishini tekshiring.

Real Telegram end-to-end sinovi bu o‘zgarishlar davomida bajarilmadi.
