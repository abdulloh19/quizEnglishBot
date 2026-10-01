package uz.pdp.englishBot.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import uz.pdp.englishBot.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Database database;
    private boolean databaseInitialized;

    public UserRepository() { this(Path.of("users.json"), System.getenv("DATABASE_URL")); }
    public UserRepository(Path path) { this(path, null); }

    UserRepository(Path path, String databaseUrl) {
        this.path = path.toAbsolutePath();
        this.database = databaseUrl == null || databaseUrl.isBlank() ? null : Database.from(databaseUrl);
    }

    public synchronized void save(List<User> users) {
        if (database != null) {
            saveToDatabase(users);
            return;
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile(path.getParent(), "users-", ".tmp");
            Files.writeString(temporary, gson.toJson(users), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("User ma’lumotlarini saqlab bo‘lmadi", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    public synchronized List<User> findUser() {
        if (database != null) return findFromDatabase();
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            List<User> users = gson.fromJson(Files.readString(path, StandardCharsets.UTF_8),
                    new TypeToken<List<User>>() { }.getType());
            return users == null ? new ArrayList<>() : users;
        } catch (IOException | com.google.gson.JsonParseException e) {
            // Buzilgan faylni bo‘sh ro‘yxat bilan almashtirib yubormaymiz.
            throw new IllegalStateException("User ma’lumotlarini o‘qib bo‘lmadi", e);
        }
    }

    private void saveToDatabase(List<User> users) {
        initializeDatabase();
        String sql = "INSERT INTO bot_state (id, users_json) VALUES (1, ?) "
                + "ON CONFLICT (id) DO UPDATE SET users_json = EXCLUDED.users_json";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gson.toJson(users));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("User ma’lumotlarini bazaga saqlab bo‘lmadi", e);
        }
    }

    private List<User> findFromDatabase() {
        initializeDatabase();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT users_json FROM bot_state WHERE id = 1");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) return new ArrayList<>();
            List<User> users = gson.fromJson(result.getString(1), new TypeToken<List<User>>() { }.getType());
            return users == null ? new ArrayList<>() : users;
        } catch (SQLException | com.google.gson.JsonParseException e) {
            throw new IllegalStateException("User ma’lumotlarini bazadan o‘qib bo‘lmadi", e);
        }
    }

    private void initializeDatabase() {
        if (databaseInitialized) return;
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS bot_state (id SMALLINT PRIMARY KEY, users_json TEXT NOT NULL)")) {
            statement.executeUpdate();
            databaseInitialized = true;
        } catch (SQLException e) {
            throw new IllegalStateException("User ma’lumotlari bazasini tayyorlab bo‘lmadi", e);
        }
    }

    private record Database(String jdbcUrl, String username, String password) {
        private static Database from(String value) {
            if (value.startsWith("jdbc:")) return new Database(value, null, null);
            URI uri = URI.create(value);
            String[] userInfo = uri.getUserInfo() == null ? new String[] {null, null} : uri.getUserInfo().split(":", 2);
            int port = uri.getPort() < 0 ? 5432 : uri.getPort();
            String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
            return new Database("jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + query,
                    userInfo[0], userInfo.length > 1 ? userInfo[1] : null);
        }

        private Connection connect() throws SQLException {
            return username == null ? DriverManager.getConnection(jdbcUrl)
                    : DriverManager.getConnection(jdbcUrl, username, password);
        }
    }
}
