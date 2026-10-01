package uz.pdp.englishBot.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TranslationService {
    static final String GOOGLE_TRANSLATE_URL =
            "https://translate.googleapis.com/translate_a/single";
    private static final int MAX_TEXT_LENGTH = 1500;

    private final HttpClient client;
    private final String endpoint;

    public TranslationService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                GOOGLE_TRANSLATE_URL);
    }

    public TranslationService(HttpClient client, String endpoint) {
        this.client = client;
        this.endpoint = endpoint;
    }

    public String translate(String text, Direction direction) {
        if (text == null || text.isBlank()) {
            throw new TranslationException("Tarjima uchun matn bo‘sh bo‘lmasligi kerak");
        }
        String value = text.trim();
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new TranslationException("Matn 1500 ta belgidan oshmasligi kerak");
        }

        String url = endpoint + "?client=gtx&sl=" + direction.sourceLanguage()
                + "&tl=" + direction.targetLanguage() + "&dt=t&q="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new TranslationException("Google Translate HTTP " + response.statusCode());
            }
            return readTranslatedText(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Tarjima so‘rovi to‘xtatildi", e);
        } catch (IOException e) {
            throw new TranslationException("Google Translate bilan bog‘lanib bo‘lmadi", e);
        }
    }

    private String readTranslatedText(String body) {
        try {
            JsonArray root = JsonParser.parseString(body).getAsJsonArray();
            JsonArray segments = root.get(0).getAsJsonArray();
            StringBuilder translated = new StringBuilder();
            for (JsonElement segmentElement : segments) {
                JsonArray segment = segmentElement.getAsJsonArray();
                if (!segment.isEmpty() && !segment.get(0).isJsonNull()) {
                    translated.append(segment.get(0).getAsString());
                }
            }
            if (translated.toString().isBlank()) {
                throw new TranslationException("Google Translate bo‘sh javob qaytardi");
            }
            return translated.toString().trim();
        } catch (IllegalStateException | IndexOutOfBoundsException | JsonParseException e) {
            throw new TranslationException("Google Translate javobini o‘qib bo‘lmadi", e);
        }
    }

    public enum Direction {
        UZ_TO_EN("uz", "en", "🇺🇿 UZ → EN"),
        EN_TO_UZ("en", "uz", "🇬🇧 EN → UZ");

        private final String sourceLanguage;
        private final String targetLanguage;
        private final String title;

        Direction(String sourceLanguage, String targetLanguage, String title) {
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.title = title;
        }

        public String sourceLanguage() { return sourceLanguage; }
        public String targetLanguage() { return targetLanguage; }
        public String title() { return title; }
    }

    public static class TranslationException extends RuntimeException {
        public TranslationException(String message) { super(message); }
        public TranslationException(String message, Throwable cause) { super(message, cause); }
    }
}
