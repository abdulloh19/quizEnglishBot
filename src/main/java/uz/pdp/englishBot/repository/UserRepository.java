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
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    public UserRepository() { this(Path.of("users.json")); }
    public UserRepository(Path path) { this.path = path.toAbsolutePath(); }

    public synchronized void save(List<User> users) {
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
}
