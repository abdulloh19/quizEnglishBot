package uz.pdp.englishBot;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.pdp.englishBot.service.TranslationService;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationServiceTest {
    private HttpServer server;
    private final AtomicReference<String> query = new AtomicReference<>();
    private final AtomicReference<String> response = new AtomicReference<>(
            "[[[\"Hello \",\"Salom \",null,null,10],[\"world\",\"dunyo\",null,null,10]],null,\"uz\"]"
    );
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private TranslationService service;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/translate_a/single", exchange -> {
            query.set(URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8));
            byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        service = new TranslationService(HttpClient.newHttpClient(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/translate_a/single");
    }

    @AfterEach void stop() { server.stop(0); }

    @Test void translatesUzbekToEnglishAndCombinesResponseSegments() {
        assertEquals("Hello world", service.translate("Salom dunyo", TranslationService.Direction.UZ_TO_EN));
        assertTrue(query.get().contains("sl=uz"));
        assertTrue(query.get().contains("tl=en"));
        assertTrue(query.get().contains("q=Salom dunyo"));
    }

    @Test void translatesEnglishToUzbek() {
        response.set("[[[\"Salom dunyo\",\"Hello world\",null,null,10]],null,\"en\"]");
        assertEquals("Salom dunyo", service.translate("Hello world", TranslationService.Direction.EN_TO_UZ));
        assertTrue(query.get().contains("sl=en"));
        assertTrue(query.get().contains("tl=uz"));
    }

    @Test void rejectsBlankLongInvalidAndHttpErrorResponses() {
        assertThrows(TranslationService.TranslationException.class,
                () -> service.translate(" ", TranslationService.Direction.EN_TO_UZ));
        assertThrows(TranslationService.TranslationException.class,
                () -> service.translate("a".repeat(1501), TranslationService.Direction.EN_TO_UZ));

        response.set("invalid json");
        assertThrows(TranslationService.TranslationException.class,
                () -> service.translate("hello", TranslationService.Direction.EN_TO_UZ));

        status.set(429);
        assertThrows(TranslationService.TranslationException.class,
                () -> service.translate("hello", TranslationService.Direction.EN_TO_UZ));
    }
}
