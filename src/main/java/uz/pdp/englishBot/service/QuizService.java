package uz.pdp.englishBot.service;

import uz.pdp.englishBot.model.QuizQuestion;
import uz.pdp.englishBot.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizService {
    private WordService wordService;

    public QuizService(WordService wordService) {
        this.wordService = wordService;
    }

    public QuizQuestion createQuestion() {
        Word randomWord = wordService.getRandomWord();
        List<Word> allWords = wordService.getAllWords();
        allWords.removeIf(w -> w.getId() == randomWord.getId());
        Collections.shuffle(allWords);
        List<String> options = new ArrayList<>();
        options.add(randomWord.getUzbek());
        for (int i = 0; i < 3; i++) {
            options.add(allWords.get(i).getUzbek());
        }
        Collections.shuffle(options);
        return new QuizQuestion(
                randomWord,
                options,
                randomWord.getUzbek()
        );
    }
}
