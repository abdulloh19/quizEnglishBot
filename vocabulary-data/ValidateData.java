import com.sun.net.httpserver.HttpServer;
import uz.pdp.englishBot.repository.WordRepository;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class ValidateData {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]).toAbsolutePath();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Path file = directory.resolve(exchange.getRequestURI().getPath().substring(1));
            byte[] body = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) { stream.write(body); }
        });
        server.start();
        try {
            var repository = new WordRepository(HttpClient.newHttpClient(), "http://127.0.0.1:" + server.getAddress().getPort());
            for (var entry : Map.of("A2", 1000, "B1", 1500, "B2", 2000).entrySet()) {
                var words = repository.loadWords(entry.getKey());
                if (words.size() != entry.getValue()) throw new AssertionError("Wrong count");
                if (words.stream().map(w -> w.getEnglish().toLowerCase()).distinct().count() != words.size()) throw new AssertionError("Duplicate word");
                if (words.stream().map(w -> w.getUzbek()).distinct().count() < 4) throw new AssertionError("Quiz options insufficient");
                System.out.println(entry.getKey() + ": " + words.size() + " words accepted by bot WordRepository");
            }
        } finally { server.stop(0); }
    }
}
