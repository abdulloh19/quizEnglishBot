package uz.pdp.englishBot.repository;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import uz.pdp.englishBot.model.Word;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WordRepository {
    public static final String BASE_URL =
            "https://raw.githubusercontent.com/abdulloh19/english-bot-data/refs/heads/main/";
    public static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1", "C2");
    private final Map<String, List<Word>> cache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final HttpClient client;
    private final String baseUrl;

    public WordRepository() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), BASE_URL);
    }

    public WordRepository(HttpClient client, String baseUrl) {
        this.client = client;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public static String normalizeLevel(String level) {
        String normalized = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        if (!LEVELS.contains(normalized)) {
            throw new IllegalArgumentException("Noto‘g‘ri daraja: " + level);
        }
        return normalized;
    }

    public static boolean isValidLevel(String level) {
        return level != null && LEVELS.contains(level.trim().toUpperCase(Locale.ROOT));
    }

    public List<Word> loadWords(String level) {
        return cache.computeIfAbsent(normalizeLevel(level), this::download);
    }

    public List<Word> refreshLevel(String level) {
        // Xatoda avvalgi cache saqlanadi; bir daraja uchun yuklashlar ketma-ket.
        return cache.compute(normalizeLevel(level), (key, previous) -> download(key));
    }

    private List<Word> download(String level) {
        String fileName = level.toLowerCase(Locale.ROOT) + ".json";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + fileName))
                .timeout(Duration.ofSeconds(20)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WordLoadingException(level + ": HTTP " + response.statusCode());
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new WordLoadingException(level + ": bo‘sh javob");
            }
            Word[] words = gson.fromJson(response.body(), Word[].class);
            if (words == null || words.length == 0) {
                throw new WordLoadingException(level + ": so‘zlar topilmadi");
            }
            Set<Integer> ids = new HashSet<>();
            for (Word word : words) {
                if (word == null || word.getId() <= 0 || !ids.add(word.getId())
                        || blank(word.getEnglish()) || blank(word.getUzbek()) || blank(word.getExample())) {
                    throw new WordLoadingException(level + ": noto‘g‘ri so‘z yoki takroriy ID");
                }
                // Darajaning yagona manbasi — tanlangan fayl, JSONdagi field shart emas.
                word.setLevel(level);
            }
            return List.copyOf(Arrays.asList(words));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WordLoadingException(level + ": yuklash to‘xtatildi", e);
        } catch (IOException | JsonParseException e) {
            throw new WordLoadingException(level + ": bazani yuklash yoki JSONni o‘qishda xato", e);
        }
    }

    private boolean blank(String text) {
        return text == null || text.isBlank();
    }

    public static class WordLoadingException extends RuntimeException {
        public WordLoadingException(String message) { super(message); }
        public WordLoadingException(String message, Throwable cause) { super(message, cause); }
    }
}
