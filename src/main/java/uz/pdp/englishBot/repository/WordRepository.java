package uz.pdp.englishBot.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import uz.pdp.englishBot.model.Word;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WordRepository {
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void main(String[] args) throws Exception {
        List<Word> words = new ArrayList<>();
        WordRepository wordRepository = new WordRepository();

        Word word = new Word(
                1,
                "improve",
                "yaxshilamoq",
                "I want to improve my English."
        );
        words.add(word);
        wordRepository.save(words);
        List<Word> wordsFromJson = wordRepository.findAll();
        for (Word word1 : wordsFromJson) {
            System.out.println(word1.getEnglish());
        }

        wordRepository.findAll();

    }

    public List<Word> findAll() {

        try (FileReader file = new FileReader("words.json")) {

            Type type = new TypeToken<List<Word>>() {
            }.getType();

            List<Word> wordList = gson.fromJson(file, type);

            return wordList;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void save(List<Word> words) {

        String json = gson.toJson(words);

        try (FileWriter file = new FileWriter("words.json")) {
            file.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
