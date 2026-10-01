package uz.pdp.englishBot.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import uz.pdp.englishBot.model.QuizQuestion;
import uz.pdp.englishBot.model.User;
import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.UserRepository;
import uz.pdp.englishBot.repository.WordRepository;
import uz.pdp.englishBot.service.QuizService;
import uz.pdp.englishBot.service.TranslationService;
import uz.pdp.englishBot.service.UserService;
import uz.pdp.englishBot.service.WordService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnglishBot {
    private final TelegramBot bot;
    private final WordService wordService = new WordService(new WordRepository());
    private final UserService userService = new UserService(new UserRepository(), wordService);
    private final QuizService quizService = new QuizService(wordService);
    private final TranslationService translationService = new TranslationService();
    private final Map<Long, ActiveQuestion> currentQuestions = new HashMap<>();
    private final Map<Long, TranslationService.Direction> translationModes = new HashMap<>();

    private record ActiveQuestion(QuizQuestion question, int messageId, String level) { }

    public EnglishBot(String token) {
        bot = new TelegramBot(token);
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) processUpdate(update);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    // User fayli va joriy savollar bir vaqtda o‘zgarmaydi.
    private synchronized void processUpdate(Update update) {
        Long chatId = update.message() != null ? update.message().chat().id()
                : update.callbackQuery() != null ? update.callbackQuery().from().id() : null;
        if (chatId == null) return;
        try {
            if (update.message() != null) handleMessage(update);
            else handleCallback(update);
        } catch (WordRepository.WordLoadingException e) {
            System.err.println(e.getMessage());
            send(chatId, "⚠️ So‘zlar bazasini yuklab bo‘lmadi. Birozdan keyin qayta urinib ko‘ring.");
        } catch (TranslationService.TranslationException e) {
            System.err.println(e.getMessage());
            send(chatId, "⚠️ Google Translate hozir javob bermadi. Birozdan keyin qayta urinib ko‘ring.");
        } catch (RuntimeException e) {
            System.err.println("Update xatosi: " + e.getClass().getSimpleName());
            send(chatId, "⚠️ Amalni bajarib bo‘lmadi. Qayta urinib ko‘ring.");
        }
    }

    private void handleMessage(Update update) {
        Long chatId = update.message().chat().id();
        String text = update.message().text();
        if (text == null) return;
        User user = userService.getUserByTelegramId(chatId);
        if ("/start".equals(text)) {
            translationModes.remove(chatId);
            if (user == null) {
                user = new User();
                user.setTelegramId(chatId);
                user.setName(update.message().from() == null ? "" : update.message().from().firstName());
                user = userService.addUser(user);
            }
            if (WordRepository.isValidLevel(user.getLevel())) sendMainMenu(chatId);
            else sendLevelMenu(chatId);
            return;
        }
        if (!ready(chatId, user)) return;
        switch (text) {
            case "🇺🇿 UZ → EN" -> startTranslation(chatId, TranslationService.Direction.UZ_TO_EN);
            case "🇬🇧 EN → UZ" -> startTranslation(chatId, TranslationService.Direction.EN_TO_UZ);
            case "⚙️ Darajani o‘zgartirish" -> {
                translationModes.remove(chatId);
                sendLevelMenu(chatId);
            }
            case "🆕 Yangi so‘z" -> {
                translationModes.remove(chatId);
                sendNextWord(chatId, user, null);
            }
            case "❓ Test" -> {
                translationModes.remove(chatId);
                sendQuiz(chatId, user, null);
            }
            case "📅 Bugungi so‘zlar" -> {
                translationModes.remove(chatId);
                sendDailyWords(chatId, user);
            }
            case "📊 Mening natijam" -> {
                translationModes.remove(chatId);
                sendStats(chatId, user);
            }
            default -> translateOrShowMenu(chatId, text);
        }
    }

    private void handleCallback(Update update) {
        var callback = update.callbackQuery();
        bot.execute(new AnswerCallbackQuery(callback.id()));
        String data = callback.data();
        var message = callback.maybeInaccessibleMessage();
        if (data == null || message == null) return;
        Long chatId = message.chat().id();
        // Bu bot shaxsiy chatdagi user progressini yuritadi.
        if (!chatId.equals(callback.from().id())) return;
        User user = userService.getUserByTelegramId(chatId);
        if (user == null) {
            send(chatId, "Avval /start bosing.");
            return;
        }
        if (data.startsWith("level:")) {
            String level = data.substring(6);
            if (!WordRepository.LEVELS.contains(level)) {
                sendLevelMenu(chatId);
                return;
            }
            boolean changed = userService.selectLevel(user, level);
            currentQuestions.remove(chatId);
            send(chatId, "✅ Siz " + level + " darajasini tanladingiz."
                    + (changed ? "\nSo‘zlar bo‘yicha progress yangilandi. Umumiy ball va javoblar statistikasi saqlandi." : ""));
            sendMainMenu(chatId);
            return;
        }
        if (!ready(chatId, user)) return;
        if ("learn_more:yes".equals(data)) {
            currentQuestions.remove(chatId);
            if (userService.addMoreDailyWords(user)) {
                sendNextWord(chatId, user, message.messageId());
            } else {
                bot.execute(new EditMessageText(chatId, message.messageId(),
                        "🏆 Bu darajadagi barcha so‘zlarni o‘rganib bo‘ldingiz!"));
            }
        } else if ("learn_more:no".equals(data)) {
            bot.execute(new EditMessageText(chatId, message.messageId(),
                    "✅ Bugungi mashg‘ulot yakunlandi. Istagan paytingiz yana davom etishingiz mumkin."));
        } else if ("keyingi_soz".equals(data)) sendNextWord(chatId, user, message.messageId());
        else if ("next_quiz".equals(data)) sendQuiz(chatId, user, message.messageId());
        else if (data.startsWith("quiz:")) answerQuiz(chatId, user, message.messageId(), data);
    }

    private boolean ready(Long chatId, User user) {
        if (user == null) {
            send(chatId, "Avval /start bosing.");
            return false;
        }
        if (!WordRepository.isValidLevel(user.getLevel())) {
            sendLevelMenu(chatId);
            return false;
        }
        return true;
    }

    private void sendMainMenu(Long telegramId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
                new String[]{"🆕 Yangi so‘z", "❓ Test"},
                new String[]{"📅 Bugungi so‘zlar", "📊 Mening natijam"},
                new String[]{"🇺🇿 UZ → EN", "🇬🇧 EN → UZ"},
                new String[]{"⚙️ Darajani o‘zgartirish"}).resizeKeyboard(true);
        bot.execute(new SendMessage(telegramId, "Asosiy menyu:").replyMarkup(keyboard));
    }

    private void startTranslation(Long chatId, TranslationService.Direction direction) {
        translationModes.put(chatId, direction);
        send(chatId, direction.title() + " tarjima rejimi yoqildi.\n\nTarjima qilinadigan matnni yuboring.");
    }

    private void translateOrShowMenu(Long chatId, String text) {
        TranslationService.Direction direction = translationModes.get(chatId);
        if (direction == null) {
            sendMainMenu(chatId);
            return;
        }
        String translated = translationService.translate(text, direction);
        send(chatId, direction.title() + "\n\n" + translated
                + "\n\nYana matn yuborishingiz mumkin. Tarjimani tugatish uchun asosiy menyudagi boshqa tugmani bosing.");
    }

    private void sendLevelMenu(Long telegramId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<String> levels = WordRepository.LEVELS;
        for (int i = 0; i < levels.size(); i += 2) {
            if (i + 1 < levels.size()) keyboard.addRow(levelButton(levels.get(i)), levelButton(levels.get(i + 1)));
            else keyboard.addRow(levelButton(levels.get(i)));
        }
        bot.execute(new SendMessage(telegramId, "🇬🇧 Ingliz tili darajangizni tanlang:").replyMarkup(keyboard));
    }

    private InlineKeyboardButton levelButton(String level) {
        return new InlineKeyboardButton(level).callbackData("level:" + level);
    }

    private void sendNextWord(Long chatId, User user, Integer messageId) {
        Word word = userService.getNextWord(user);
        if (word == null) {
            if (userService.getDailyWords(user).isEmpty()) {
                if (userService.hasMoreLevelWords(user)) {
                    askToLearnMore(chatId, messageId);
                } else {
                    send(chatId, "🏆 Bu darajadagi barcha so‘zlarni o‘rganib bo‘ldingiz!");
                }
            } else {
                send(chatId, "✅ Hozirgi so‘zlarning hammasini ko‘rdingiz. Endi ❓ Test orqali ularni 100% o‘rganing.");
            }
            return;
        }
        currentQuestions.remove(chatId);
        String text = user.getCurrentWordIndex() + ". 🇬🇧 " + word.getEnglish()
                + "\n\n🇺🇿 " + word.getUzbek() + "\n\n📝 Example:\n" + word.getExample();
        show(chatId, messageId, text, new InlineKeyboardMarkup(
                new InlineKeyboardButton("⏭️ Keyingi so‘z").callbackData("keyingi_soz")));
    }

    private void askToLearnMore(Long chatId, Integer messageId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("✅ Ha").callbackData("learn_more:yes"),
                new InlineKeyboardButton("❌ Yo‘q").callbackData("learn_more:no")
        );
        show(chatId, messageId,
                "🎉 Bugungi so‘zlarni 100% o‘rgandingiz!\n\nBugun yana yangi so‘z o‘rganasizmi?",
                keyboard);
    }

    private void sendQuiz(Long chatId, User user, Integer messageId) {
        List<Word> seenWords = userService.getSeenDailyWords(user);
        QuizQuestion question = quizService.createQuestion(seenWords);
        if (question == null) {
            currentQuestions.remove(chatId);
            if (userService.getDailyWords(user).isEmpty()) {
                send(chatId, "🎉 Ko‘rgan so‘zlaringizni 100% o‘rgandingiz. Yangi so‘z olish uchun 🆕 Yangi so‘z ni bosing.");
            } else {
                send(chatId, "⚠️ Test uchun avval 🆕 Yangi so‘z orqali kamida bitta so‘zni ko‘ring.");
            }
            return;
        }
        List<String> options = question.getOptions();
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        for (int i = 0; i < options.size(); i += 2) {
            InlineKeyboardButton first = quizButton(options, i);
            if (i + 1 < options.size()) keyboard.addRow(first, quizButton(options, i + 1));
            else keyboard.addRow(first);
        }
        Integer shownId = show(chatId, messageId, "❓ " + question.getWord().getEnglish() + " nima degani?", keyboard);
        if (shownId != null) currentQuestions.put(chatId, new ActiveQuestion(question, shownId, user.getLevel()));
    }

    private InlineKeyboardButton quizButton(List<String> options, int index) {
        return new InlineKeyboardButton((char) ('A' + index) + ") " + options.get(index))
                .callbackData("quiz:" + index);
    }

    private void answerQuiz(Long chatId, User user, int messageId, String data) {
        ActiveQuestion active = currentQuestions.get(chatId);
        if (active == null || active.messageId() != messageId || !active.level().equals(user.getLevel())) {
            send(chatId, "Test eskirgan. Qaytadan ❓ Test ni bosing.");
            return;
        }
        if (!data.matches("quiz:\\d+")) return;
        QuizQuestion question = active.question();
        int selectedIndex = Integer.parseInt(data.substring(5));
        if (selectedIndex < 0 || selectedIndex >= question.getOptions().size()) return;
        boolean correct = question.getOptions().get(selectedIndex).equals(question.getCorrectAnswer());
        UserService.AnswerResult answerResult = userService.recordAnswer(
                user,
                new UserService.QuizQuestionResult(question.getWord().getId(), correct)
        );
        currentQuestions.remove(chatId);
        String text;
        if (!correct) {
            text = "❌ Noto‘g‘ri!\n✅ To‘g‘ri javob: " + question.getCorrectAnswer();
        } else if (answerResult.learnedNow()) {
            text = "✅ To‘g‘ri! So‘z 2/2 topildi.\n🎉 So‘z o‘rganildi: +1 ball";
        } else {
            text = "✅ To‘g‘ri! So‘z 1/2 topildi.\nYana bir marta to‘g‘ri topsangiz +1 ball beriladi.";
        }
        show(chatId, messageId, text + "\n🏆 Ball: " + user.getScore(), new InlineKeyboardMarkup(
                new InlineKeyboardButton("➡️ Keyingi test").callbackData("next_quiz")));
    }

    private void sendDailyWords(Long chatId, User user) {
        StringBuilder result = new StringBuilder("📅 Bugungi natija — " + user.getLevel()
                + "\n\n📖 Ko‘rilgan, hali o‘rganilmagan so‘zlar:\n");
        int number = 1;
        for (Word word : userService.getSeenDailyWords(user)) appendWord(result, number++, word);
        if (number == 1) result.append("Hozircha qolgan ko‘rilgan so‘z yo‘q.\n");
        result.append("\n✅ Bugun o‘rganilgan so‘zlar:\n");
        number = 1;
        for (Word word : userService.getAllSeenTodayWords(user)) {
            if (userService.getCorrectCount(user, word) >= 2) appendWord(result, number++, word);
        }
        if (number == 1) result.append("Bugun hali o‘rganilgan so‘z yo‘q.\n");
        // Katta ro‘yxatlar Telegram xabar chegarasidan oshmasin.
        StringBuilder chunk = new StringBuilder();
        for (String line : result.toString().split("\n")) {
            if (chunk.length() + line.length() > 3500) {
                send(chatId, chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(line).append('\n');
        }
        if (!chunk.isEmpty()) send(chatId, chunk.toString());
    }

    private void appendWord(StringBuilder result, int number, Word word) {
        result.append(number).append(". 🇬🇧 ").append(word.getEnglish()).append(" — 🇺🇿 ")
                .append(word.getUzbek()).append('\n');
    }

    private void sendStats(Long chatId, User user) {
        int total = user.getCorrectAnswer() + user.getWrongAnswer();
        long learned = userService.getLearnedWordCount(user);
        send(chatId, "📊 Sizning natijangiz:\n\n🇬🇧 Daraja: " + user.getLevel()
                + "\n📚 O‘rganilgan so‘zlar (joriy daraja): " + learned
                + "\n✅ Bir martadan hisoblangan so‘zlar: " + user.getCorrectAnswer()
                + "\n❌ Noto‘g‘ri urinishlar: " + user.getWrongAnswer()
                + "\n🏆 Ball: " + user.getScore() + "\n📈 Natija: "
                + String.format("%.0f", total == 0 ? 0.0 : user.getCorrectAnswer() * 100.0 / total) + "%");
    }

    private void send(Long chatId, String text) { bot.execute(new SendMessage(chatId, text)); }

    private Integer show(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        if (messageId == null) {
            var response = bot.execute(new SendMessage(chatId, text).replyMarkup(keyboard));
            return response.isOk() ? response.message().messageId() : null;
        }
        var response = bot.execute(new EditMessageText(chatId, messageId, text).replyMarkup(keyboard));
        return response.isOk() ? messageId : null;
    }
}
