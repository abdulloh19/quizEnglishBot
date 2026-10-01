package uz.pdp.englishBot.service;

import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.WordRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WordService {
    private final WordRepository wordRepository;

    public WordService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    public List<Word> getAllWords(String level) {
        return wordRepository.loadWords(level);
    }

    public List<Word> getWordsByLevel(String level) {
        return getAllWords(level);
    }

    public Word getWordById(String level, int id) {
        return getAllWords(level).stream().filter(word -> word.getId() == id).findFirst().orElse(null);
    }

    public Word getRandomWord(String level) {
        List<Word> words = getAllWords(level);
        return words.isEmpty() ? null : words.get(ThreadLocalRandom.current().nextInt(words.size()));
    }
}
