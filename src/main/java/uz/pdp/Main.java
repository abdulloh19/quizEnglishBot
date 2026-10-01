package uz.pdp;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import uz.pdp.englishBot.bot.EnglishBot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        String token = requiredEnvironment("TELEGRAM_BOT_TOKEN");
        String externalUrl = System.getenv("RENDER_EXTERNAL_URL");

        if (externalUrl == null || externalUrl.isBlank()) {
            new EnglishBot(token, true);
            System.out.println("Bot polling rejimida ishga tushdi");
            new CountDownLatch(1).await();
            return;
        }

        EnglishBot englishBot = new EnglishBot(token, false);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "10000"));
        String webhookSecret = System.getenv("TELEGRAM_WEBHOOK_SECRET");
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", exchange -> respond(exchange, 200, "English vocabulary bot is running"));
        server.createContext("/telegram-webhook", exchange -> handleWebhook(exchange, englishBot, webhookSecret));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        String webhookUrl = externalUrl.replaceAll("/+$", "") + "/telegram-webhook";
        englishBot.setWebhook(webhookUrl, webhookSecret);
        System.out.println("Bot webhook rejimida ishga tushdi");
        new CountDownLatch(1).await();
    }

    private static void handleWebhook(HttpExchange exchange, EnglishBot bot, String secret) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "Method not allowed");
            return;
        }
        if (secret != null && !secret.isBlank()
                && !secret.equals(exchange.getRequestHeaders().getFirst("X-Telegram-Bot-Api-Secret-Token"))) {
            respond(exchange, 403, "Forbidden");
            return;
        }
        try {
            Update update = BotUtils.parseUpdate(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            bot.processUpdate(update);
            respond(exchange, 200, "OK");
        } catch (RuntimeException e) {
            System.err.println("Webhook xatosi: " + e.getClass().getSimpleName());
            respond(exchange, 400, "Invalid update");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, content.length);
        try (var output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " muhit o‘zgaruvchisi berilmagan");
        }
        return value;
    }
}
