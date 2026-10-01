package uz.pdp.englishBot.service;

import uz.pdp.englishBot.model.QuizQuestion;
import uz.pdp.englishBot.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class QuizService {
    private final WordService wordService;

    public QuizService(WordService wordService) {
        this.wordService = wordService;
    }

    public QuizQuestion createQuestion(List<Word> seenWords) {
        if (seenWords == null || seenWords.isEmpty()) {
            return null;
        }
        Word word = seenWords.get(ThreadLocalRandom.current().nextInt(seenWords.size()));
        List<String> translations = seenWords.stream().map(Word::getUzbek).distinct().toList();
        List<String> distractors = new ArrayList<>(translations);
        distractors.remove(word.getUzbek());
        Collections.shuffle(distractors);
        List<String> options = new ArrayList<>(
                distractors.subList(0, Math.min(3, distractors.size()))
        );
        options.add(word.getUzbek());
        Collections.shuffle(options);
        return new QuizQuestion(word, options, word.getUzbek());
    }
}
