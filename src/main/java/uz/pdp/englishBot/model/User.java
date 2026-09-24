package uz.pdp.englishBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long telegramId;
    private String name;
    private int score;
    private int correctAnswer;
    private int wrongAnswer;
    private List<Integer> wordOrder;
    private int currentWordIndex;
}
