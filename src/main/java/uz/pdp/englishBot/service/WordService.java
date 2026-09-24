package uz.pdp.englishBot.service;

import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.WordRepository;

import java.util.List;
import java.util.Random;

public class WordService {
    private WordRepository wordRepository;

    public WordService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    public List<Word> getAllWords() {
        return wordRepository.findAll();
    }

    public Word addWord(Word word) {
        List<Word> allWords = wordRepository.findAll();
        allWords.add(word);
        wordRepository.save(allWords);
        return word;
    }

    public Word getWordById(int id) {
        List<Word> all = wordRepository.findAll();
        for (Word word : all) {
            if (word.getId() == id) {
                return word;
            }
        }
        return null;
    }


    public Word getRandomWord() {
      /*  List<Word> all = wordRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("Hozircha bazada so'zlar yo'q");
        }
        Random random = new Random();
        return all.get(random.nextInt(all.size()));
        if(shuffledWords.isEmpty() || currentIndex >= shuffledWords.size()) {
            shuffledWords = wordRepository.findAll();
            Collections.shuffle(shuffledWords);
            currentIndex = 0;
        }
        return shuffledWords.get(currentIndex++);*/
        List<Word> all = wordRepository.findAll();

        if (all.isEmpty()) {
            return null;
        }

        Random random = new Random();

        return all.get(random.nextInt(all.size()));
    }
}
