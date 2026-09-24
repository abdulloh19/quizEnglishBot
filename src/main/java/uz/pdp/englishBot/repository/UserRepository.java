package uz.pdp.englishBot.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import uz.pdp.englishBot.model.User;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void main(String[] args) throws Exception {
        List<User> users = new ArrayList<>();
        UserRepository userRepository = new UserRepository();

        userRepository.save(users);
        userRepository.findUser();

    }

    public void save(List<User> users) {
        String json = gson.toJson(users);
        try (FileWriter file = new FileWriter("users.json")) {
            file.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<User> findUser() {
        System.out.println(new java.io.File("users.json").getAbsolutePath());
        try (FileReader fileReader = new FileReader("users.json")) {
            Type type = new TypeToken<List<User>>() {
            }.getType();
            List<User> users = gson.fromJson(fileReader, type);

            if (users == null) {
                return new ArrayList<>();
            }
            return users;
        } catch (Exception e) {
            System.out.println("USERS.JSON O'QISHDA XATO:");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
