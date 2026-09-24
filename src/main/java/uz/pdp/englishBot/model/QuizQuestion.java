package uz.pdp.englishBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizQuestion {
    private Word word;
    private List<String> options;
    private String CorrectAnswer;
}
