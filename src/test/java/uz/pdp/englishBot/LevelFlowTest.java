package uz.pdp.englishBot;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uz.pdp.englishBot.model.User;
import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.UserRepository;
import uz.pdp.englishBot.repository.WordRepository;
import uz.pdp.englishBot.service.QuizService;
import uz.pdp.englishBot.service.UserService;
import uz.pdp.englishBot.service.WordService;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LevelFlowTest {
    @TempDir Path temporary;
    private HttpServer server;
    private final Map<String, String> bodies = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();
    private WordRepository words;
    private WordService wordService;
    private UserRepository users;
    private UserService userService;

    @BeforeEach void setup() throws Exception {
        bodies.put("/a1.json", data("a", 6));
        bodies.put("/b1.json", data("b", 6));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();
            String body = bodies.get(path);
            byte[] bytes = (body == null ? "missing" : body).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(body == null ? 404 : 200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        words = new WordRepository(HttpClient.newHttpClient(), "http://127.0.0.1:" + server.getAddress().getPort());
        wordService = new WordService(words);
        users = new UserRepository(temporary.resolve("users.json"));
        userService = new UserService(users, wordService);
    }

    @AfterEach void stop() { server.stop(0); }

    private String data(String prefix, int count) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 1; i <= count; i++) {
            if (i > 1) json.append(',');
            json.append("{\"id\":").append(i).append(",\"english\":\"").append(prefix).append(i)
                    .append("\",\"uzbek\":\"tarjima ").append(prefix).append(i)
                    .append("\",\"example\":\"Example.\"}");
        }
        return json.append(']').toString();
    }

    private User user(long id) {
        User user = new User();
        user.setTelegramId(id);
        return userService.addUser(user);
    }

    @Test void newAndLegacyUsersNeedLevelWithoutLoadingWords() throws Exception {
        User fresh = user(1);
        assertNull(fresh.getLevel());
        assertTrue(fresh.getDailyWordIds().isEmpty());
        assertTrue(requests.isEmpty());
        Files.writeString(temporary.resolve("users.json"), "[{\"telegramId\":2,\"score\":7}]");
        User legacy = userService.getUserByTelegramId(2L);
        assertNull(legacy.getLevel());
        assertNotNull(legacy.getWordCorrectCounts());
        assertNotNull(legacy.getWordCorrectCountsByKey());
        userService.selectLevel(legacy, "A1");
        assertEquals(3, userService.getUserByTelegramId(2L).getScore());
        assertEquals("A1", userService.getUserByTelegramId(2L).getLevel());
    }

    @Test void usersAndQuizOptionsStayInTheirOwnLevel() {
        User a = user(1), b = user(2);
        userService.selectLevel(a, "A1");
        userService.selectLevel(b, "B1");
        assertTrue(userService.getNextWord(a).getEnglish().startsWith("a"));
        for (int i = 0; i < 4; i++) assertTrue(userService.getNextWord(b).getEnglish().startsWith("b"));
        assertTrue(userService.getDailyWords(a).stream().allMatch(w -> "A1".equals(w.getLevel())));
        var quiz = new QuizService(wordService).createQuestion(userService.getSeenDailyWords(b));
        assertEquals(4, quiz.getOptions().size());
        assertTrue(quiz.getOptions().stream().allMatch(o -> o.startsWith("tarjima b")));
        assertTrue(quiz.getOptions().contains(quiz.getCorrectAnswer()));
    }

    @Test void changeResetsWordProgressButKeepsTotalsAndSameLevelPreservesProgress() {
        User a = user(1);
        userService.selectLevel(a, "A1");
        while (userService.getNextWord(a) != null) { }
        userService.recordAnswer(a, new UserService.QuizQuestionResult(1, true));
        userService.recordAnswer(a, new UserService.QuizQuestionResult(1, true));
        userService.recordAnswer(a, new UserService.QuizQuestionResult(2, false));
        assertFalse(a.getDailyWordIds().contains(1));
        assertFalse(userService.selectLevel(a, "A1"));
        assertEquals(2, userService.getCorrectCount(a, wordService.getWordById("A1", 1)));
        assertTrue(userService.selectLevel(a, "B1"));
        assertTrue(a.getWordCorrectCountsByKey().isEmpty());
        assertTrue(a.getDailyWordIds().contains(1));
        assertEquals(0, a.getCurrentWordIndex());
        assertEquals(1, a.getScore());
        assertEquals(1, a.getCorrectAnswer());
        assertEquals(1, a.getWrongAnswer());
        assertEquals("B1", userService.getUserByTelegramId(1L).getLevel());
    }

    @Test void failedSelectionPreservesOldLevelAndProgress() {
        User a = user(1);
        userService.selectLevel(a, "A1");
        while (!a.getDailySeenWordIds().contains(1)) userService.getNextWord(a);
        userService.recordAnswer(a, new UserService.QuizQuestionResult(1, true));
        assertThrows(WordRepository.WordLoadingException.class, () -> userService.selectLevel(a, "C2"));
        assertEquals("A1", a.getLevel());
        assertEquals(1, userService.getCorrectCount(a, wordService.getWordById("A1", 1)));
        assertEquals("A1", userService.getUserByTelegramId(1L).getLevel());
    }

    @Test void legacyIdProgressCannotMarkDifferentWordsAsLearned() throws Exception {
        Files.writeString(temporary.resolve("users.json"), """
                [{"telegramId":9,"level":"A1","dailyDate":"2099-01-01",
                  "dailyWordIds":[1,2],"dailySeenWordIds":[],
                  "wordCorrectCounts":{"1":2,"2":2},"score":4,"correctAnswer":4}]
                """);
        User legacy = userService.getUserByTelegramId(9L);
        assertEquals(0, userService.getLearnedWordCount(legacy));
        assertTrue(legacy.getWordCorrectCounts().isEmpty());
        assertEquals(2, legacy.getScore());
        assertEquals(2, legacy.getCorrectAnswer());
    }

    @Test void twoCorrectAnswersCountAsOneWordAndOnePoint() {
        User a = user(1);
        userService.selectLevel(a, "A1");
        while (!a.getDailySeenWordIds().contains(1)) userService.getNextWord(a);

        var first = userService.recordAnswer(a, new UserService.QuizQuestionResult(1, true));
        assertEquals(1, first.correctCount());
        assertFalse(first.learnedNow());
        assertEquals(0, a.getScore());
        assertEquals(0, a.getCorrectAnswer());

        var second = userService.recordAnswer(a, new UserService.QuizQuestionResult(1, true));
        assertEquals(2, second.correctCount());
        assertTrue(second.learnedNow());
        assertEquals(1, a.getScore());
        assertEquals(1, a.getCorrectAnswer());

        var repeated = userService.recordAnswer(a, new UserService.QuizQuestionResult(1, true));
        assertFalse(repeated.learnedNow());
        assertEquals(1, a.getScore());
        assertEquals(1, a.getCorrectAnswer());
    }

    @Test void cacheRefreshAndConcurrentFirstLoad() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = List.<java.util.concurrent.Callable<List<Word>>>of(
                    () -> words.loadWords("A1"), () -> words.loadWords("a1"),
                    () -> words.loadWords("A1"), () -> words.loadWords("A1"));
            for (var future : executor.invokeAll(tasks)) assertEquals(6, future.get().size());
        }
        assertEquals(1, requests.get("/a1.json").get());
        bodies.put("/a1.json", data("new", 5));
        assertEquals(5, words.refreshLevel("A1").size());
        bodies.put("/a1.json", "bad json");
        assertThrows(WordRepository.WordLoadingException.class, () -> words.refreshLevel("A1"));
        assertEquals(5, words.loadWords("A1").size());
    }

    @Test void invalidEmptyDuplicateAndHttpErrorsAreRecoverable() {
        for (String bad : List.of(" ", "null", "[]", "{}", "[null]", "bad json",
                "[{\"id\":1}]", data("a", 2).replace("\"id\":2", "\"id\":1"))) {
            bodies.put("/a2.json", bad);
            assertThrows(WordRepository.WordLoadingException.class, () -> words.loadWords("A2"), bad);
        }
        assertThrows(WordRepository.WordLoadingException.class, () -> words.loadWords("C1"));
        assertThrows(IllegalArgumentException.class, () -> words.loadWords("../secrets"));
        bodies.put("/a2.json", data("ok", 4));
        assertEquals(4, words.loadWords("A2").size());
    }

    @Test void quizUsesOnlySeenWordsAndSupportsFewerThanFourOptions() {
        QuizService quiz = new QuizService(wordService);
        User a = user(1);
        userService.selectLevel(a, "A1");
        assertNull(quiz.createQuestion(userService.getSeenDailyWords(a)));

        Word first = userService.getNextWord(a);
        var oneWordQuestion = quiz.createQuestion(userService.getSeenDailyWords(a));
        assertEquals(List.of(first.getUzbek()), oneWordQuestion.getOptions());

        Word second = userService.getNextWord(a);
        var twoWordQuestion = quiz.createQuestion(userService.getSeenDailyWords(a));
        assertEquals(2, twoWordQuestion.getOptions().size());
        assertTrue(twoWordQuestion.getOptions().contains(first.getUzbek()));
        assertTrue(twoWordQuestion.getOptions().contains(second.getUzbek()));
        assertTrue(twoWordQuestion.getOptions().stream()
                .allMatch(option -> option.equals(first.getUzbek()) || option.equals(second.getUzbek())));
    }

    @Test void dailyRolloverAndAllLearnedDoNotCrash() {
        User a = user(1);
        userService.selectLevel(a, "A1");
        a.setDailyDate(LocalDate.now().minusDays(1).toString());
        a.setCurrentWordIndex(100);
        userService.getDailyWords(a);
        assertEquals(LocalDate.now().toString(), a.getDailyDate());
        assertEquals(0, a.getCurrentWordIndex());
        for (int id = 1; id <= 6; id++) {
            while (!a.getDailySeenWordIds().contains(id)) userService.getNextWord(a);
            userService.recordAnswer(a, new UserService.QuizQuestionResult(id, true));
            userService.recordAnswer(a, new UserService.QuizQuestionResult(id, true));
        }
        assertTrue(userService.getDailyWords(a).isEmpty());
        assertNull(userService.getNextWord(a));
        assertNull(new QuizService(wordService).createQuestion(userService.getSeenDailyWords(a)));
    }

    @Test void learnedBatchCanContinueWithMoreWords() {
        bodies.put("/a1.json", data("a", 20));
        User a = user(1);
        userService.selectLevel(a, "A1");
        assertEquals(15, a.getDailyWordIds().size());

        while (userService.getNextWord(a) != null) { }
        for (Integer id : List.copyOf(a.getDailySeenWordIds())) {
            userService.recordAnswer(a, new UserService.QuizQuestionResult(id, true));
            userService.recordAnswer(a, new UserService.QuizQuestionResult(id, true));
        }

        assertTrue(userService.getDailyWords(a).isEmpty());
        assertTrue(userService.hasMoreLevelWords(a));
        assertTrue(userService.addMoreDailyWords(a));
        assertEquals(5, userService.getDailyWords(a).size());
        assertNotNull(userService.getNextWord(a));
        assertEquals(16, a.getDailySeenWordIds().size());
    }

    @Test void networkFailureDoesNotReturnOrCacheEmptyWords() {
        server.stop(0);
        assertThrows(WordRepository.WordLoadingException.class, () -> words.loadWords("A1"));
    }
}
