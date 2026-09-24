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
import uz.pdp.englishBot.service.UserService;
import uz.pdp.englishBot.service.WordService;

import java.util.*;

public class EnglishBot {

    private final TelegramBot bot;

    UserRepository userRepository = new UserRepository();
    WordRepository wordRepository = new WordRepository();

    WordService wordService = new WordService(wordRepository);

    QuizService quizService = new QuizService(wordService);

    UserService userService = new UserService(
            userRepository,
            wordService
    );

    // Har bir userning hozirgi test savoli
    Map<Long, QuizQuestion> currentQuestions = new HashMap<>();


    public EnglishBot(String token) {

        bot = new TelegramBot(token);

        bot.setUpdatesListener(updates -> {

            for (Update update : updates) {

                // =====================================================
                // MESSAGE
                // =====================================================

                if (update.message() != null) {

                    Long telegramId =
                            update.message().chat().id();

                    String text =
                            update.message().text();

                    String name =
                            update.message().from().firstName();

                    // =================================================
                    // /start
                    // =================================================

                    if ("/start".equals(text)) {

                        User user =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (user == null) {

                            User newUser = new User(
                                    telegramId,
                                    name,
                                    0,
                                    0,
                                    0,
                                    new ArrayList<>(),
                                    0
                            );

                            userService.addUser(newUser);
                        }

                        ReplyKeyboardMarkup menu =
                                new ReplyKeyboardMarkup(
                                        new String[]{
                                                "🆕 Yangi so‘z",
                                                "❓ Test"
                                        },
                                        new String[]{
                                                "📅 Bugungi so‘zlar",
                                                "📊 Mening natijam"
                                        }
                                ).resizeKeyboard(true);

                        bot.execute(
                                new SendMessage(
                                        telegramId,
                                        "🇬🇧 English Vocabulary Bot\n\n" +
                                                "Salom! Ingliz tilini o‘rganamiz.\n\n" +
                                                "📚 Kerakli bo‘limni tanlang:"
                                ).replyMarkup(menu)
                        );
                    }

                    // =================================================
                    // 🆕 Yangi so‘z
                    // =================================================

                    if ("🆕 Yangi so‘z".equals(text)) {

                        User currentUser =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (currentUser == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        Word word =
                                userService.getNextWord(
                                        currentUser
                                );

                        if (word == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Hozircha so‘zlar mavjud emas."
                                    )
                            );

                            continue;
                        }

                        String message2 =
                                "🇬🇧 " + word.getEnglish() + "\n\n" +
                                        "🇺🇿 " + word.getUzbek() + "\n\n" +
                                        "📝 Example:\n" +
                                        word.getExample();

                        InlineKeyboardMarkup inlineKeyboardMarkup =
                                new InlineKeyboardMarkup(
                                        new InlineKeyboardButton[]{
                                                new InlineKeyboardButton(
                                                        "⏭️ Keyingi so'z"
                                                ).callbackData(
                                                        "keyingi_soz"
                                                )
                                        }
                                );

                        bot.execute(
                                new SendMessage(
                                        telegramId,
                                        message2
                                ).replyMarkup(
                                        inlineKeyboardMarkup
                                )
                        );
                    }

                    // =================================================
                    // ❓ TEST
                    // =================================================

                    if ("❓ Test".equals(text)) {

                        User currentUser =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (currentUser == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        QuizQuestion question =
                                quizService.createQuestion();

                        currentQuestions.put(
                                telegramId,
                                question
                        );

                        String questionText =
                                "❓ " +
                                        question.getWord().getEnglish() +
                                        " nima degani?";

                        List<String> options =
                                question.getOptions();

                        InlineKeyboardMarkup quizKeyboard =
                                new InlineKeyboardMarkup(
                                        new InlineKeyboardButton[]{
                                                new InlineKeyboardButton(
                                                        "A) " + options.get(0)
                                                ).callbackData("quiz:0"),

                                                new InlineKeyboardButton(
                                                        "B) " + options.get(1)
                                                ).callbackData("quiz:1"),

                                                new InlineKeyboardButton(
                                                        "C) " + options.get(2)
                                                ).callbackData("quiz:2"),

                                                new InlineKeyboardButton(
                                                        "D) " + options.get(3)
                                                ).callbackData("quiz:3")
                                        }
                                );

                        bot.execute(
                                new SendMessage(
                                        telegramId,
                                        questionText
                                ).replyMarkup(
                                        quizKeyboard
                                )
                        );
                    }

                    // =================================================
                    // 📅 BUGUNGI SO‘ZLAR
                    // =================================================

                    if ("📅 Bugungi so‘zlar".equals(text)) {

                        User currentUser =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (currentUser == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        List<Word> words =
                                wordService.getAllWords();

                        if (words.isEmpty()) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Hozircha so‘zlar mavjud emas."
                                    )
                            );

                            continue;
                        }

                        Collections.shuffle(words);

                        int count =
                                Math.min(5, words.size());

                        StringBuilder textBuilder =
                                new StringBuilder(
                                        "📅 Bugungi 5 ta so‘z:\n\n"
                                );

                        for (int i = 0; i < count; i++) {

                            Word word = words.get(i);

                            textBuilder
                                    .append(i + 1)
                                    .append(". 🇬🇧 ")
                                    .append(word.getEnglish())
                                    .append(" — 🇺🇿 ")
                                    .append(word.getUzbek())
                                    .append("\n");
                        }

                        bot.execute(
                                new SendMessage(
                                        telegramId,
                                        textBuilder.toString()
                                )
                        );
                    }

                    // =================================================
                    // 📊 MENING NATIJAM
                    // =================================================

                    if ("📊 Mening natijam".equals(text)) {

                        User currentUser =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (currentUser == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        int total =
                                currentUser.getCorrectAnswer()
                                        + currentUser.getWrongAnswer();

                        double percentage = 0;

                        if (total > 0) {

                            percentage =
                                    currentUser.getCorrectAnswer()
                                            * 100.0
                                            / total;
                        }

                        String result =
                                "📊 Sizning natijangiz:\n\n" +
                                        "❓ Savollar: " + total + "\n" +
                                        "✅ To‘g‘ri: " +
                                        currentUser.getCorrectAnswer() + "\n" +
                                        "❌ Noto‘g‘ri: " +
                                        currentUser.getWrongAnswer() + "\n" +
                                        "🏆 Ball: " +
                                        currentUser.getScore() + "\n" +
                                        "📈 Natija: " +
                                        String.format("%.0f", percentage) +
                                        "%";

                        bot.execute(
                                new SendMessage(
                                        telegramId,
                                        result
                                )
                        );
                    }
                }

                // =====================================================
                // CALLBACK QUERY
                // =====================================================

                if (update.callbackQuery() != null) {

                    String data =
                            update.callbackQuery().data();

                    Long telegramId =
                            update.callbackQuery()
                                    .from()
                                    .id();

                    var message =
                            update.callbackQuery()
                                    .maybeInaccessibleMessage();

                    // Telegramga callback qabul qilinganini aytamiz
                    bot.execute(
                            new AnswerCallbackQuery(
                                    update.callbackQuery().id()
                            )
                    );

                    // =================================================
                    // ⏭️ KEYINGI SO‘Z
                    // =================================================

                    if ("keyingi_soz".equals(data)) {

                        if (message == null) {
                            continue;
                        }

                        User user =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (user == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        Word randomWord =
                                userService.getNextWord(
                                        user
                                );

                        if (randomWord == null) {

                            continue;
                        }

                        String text1 =
                                "🇬🇧 " +
                                        randomWord.getEnglish() +
                                        "\n\n" +

                                        "🇺🇿 " +
                                        randomWord.getUzbek() +
                                        "\n\n" +

                                        "📝 Example:\n" +
                                        randomWord.getExample();

                        InlineKeyboardMarkup inlineKeyboardMarkup =
                                new InlineKeyboardMarkup(
                                        new InlineKeyboardButton[]{
                                                new InlineKeyboardButton(
                                                        "⏭️ Keyingi so'z"
                                                ).callbackData(
                                                        "keyingi_soz"
                                                )
                                        }
                                );

                        bot.execute(
                                new EditMessageText(
                                        message.chat().id(),
                                        message.messageId(),
                                        text1
                                ).replyMarkup(
                                        inlineKeyboardMarkup
                                )
                        );
                    }
                    if (data.startsWith("quiz:")) {

                        if (message == null) {
                            continue;
                        }

                        QuizQuestion question =
                                currentQuestions.get(
                                        telegramId
                                );

                        if (question == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Test topilmadi. Qaytadan ❓ Test ni bosing."
                                    )
                            );

                            continue;
                        }

                        int selectedIndex =
                                Integer.parseInt(
                                        data.substring(5)
                                );

                        String selectedAnswer =
                                question.getOptions()
                                        .get(selectedIndex);

                        User user =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (user == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        boolean correct =
                                selectedAnswer.equals(
                                        question.getCorrectAnswer()
                                );

                        String resultText;

                        if (correct) {

                            user.setScore(
                                    user.getScore() + 1
                            );

                            user.setCorrectAnswer(
                                    user.getCorrectAnswer() + 1
                            );

                            resultText =
                                    "✅ To‘g‘ri!\n\n" +
                                            "🎉 Javobingiz to‘g‘ri.\n" +
                                            "🏆 Ball: " +
                                            user.getScore();

                        } else {

                            user.setWrongAnswer(
                                    user.getWrongAnswer() + 1
                            );

                            resultText =
                                    "❌ Noto‘g‘ri!\n\n" +
                                            "✅ To‘g‘ri javob: " +
                                            question.getCorrectAnswer() +
                                            "\n" +
                                            "🏆 Ball: " +
                                            user.getScore();
                        }

                        userService.updateUser(user);

                        InlineKeyboardMarkup nextQuizButton =
                                new InlineKeyboardMarkup(
                                        new InlineKeyboardButton[]{
                                                new InlineKeyboardButton(
                                                        "➡️ Keyingi test"
                                                ).callbackData(
                                                        "next_quiz"
                                                )
                                        }
                                );

                        bot.execute(
                                new EditMessageText(
                                        message.chat().id(),
                                        message.messageId(),
                                        resultText
                                ).replyMarkup(
                                        nextQuizButton
                                )
                        );

                        currentQuestions.remove(
                                telegramId
                        );
                    }
                    if ("next_quiz".equals(data)) {

                        if (message == null) {
                            continue;
                        }

                        User user =
                                userService.getUserByTelegramId(
                                        telegramId
                                );

                        if (user == null) {

                            bot.execute(
                                    new SendMessage(
                                            telegramId,
                                            "Avval /start bosing."
                                    )
                            );

                            continue;
                        }

                        QuizQuestion question =
                                quizService.createQuestion();

                        currentQuestions.put(
                                telegramId,
                                question
                        );

                        String questionText =
                                "❓ " +
                                        question.getWord().getEnglish() +
                                        " nima degani?";

                        List<String> options =
                                question.getOptions();

                        InlineKeyboardMarkup quizKeyboard =
                                new InlineKeyboardMarkup(
                                        new InlineKeyboardButton[]{
                                                new InlineKeyboardButton(
                                                        "A) " + options.get(0)
                                                ).callbackData("quiz:0"),

                                                new InlineKeyboardButton(
                                                        "B) " + options.get(1)
                                                ).callbackData("quiz:1"),
                                        },
                                        new InlineKeyboardButton[]{
                                                new InlineKeyboardButton(
                                                        "C) " + options.get(2)
                                                ).callbackData("quiz:2"),

                                                new InlineKeyboardButton(
                                                        "D) " + options.get(3)
                                                ).callbackData("quiz:3")
                                        }
                                );

                        bot.execute(
                                new EditMessageText(
                                        message.chat().id(),
                                        message.messageId(),
                                        questionText
                                ).replyMarkup(
                                        quizKeyboard
                                )
                        );
                    }
                }
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}