package uz.pdp.englishBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

    private List<Integer> dailyWordIds;
    private String dailyDate;

    private Map<Integer, Integer> wordCorrectCounts;

    private String level;

    // Bugun "Yangi so‘z" orqali userga haqiqatan ko‘rsatilgan so‘zlar.
    // Test faqat shu ro‘yxatdagi, hali o‘rganilmagan so‘zlardan tuziladi.
    private List<Integer> dailySeenWordIds;

    // ID fayl ichida o‘zgarib qolsa progress boshqa so‘zga ko‘chib ketmasligi uchun
    // yangi progress daraja + inglizcha so‘z kaliti bilan saqlanadi.
    private Map<String, Integer> wordCorrectCountsByKey;

    // 2-versiyada bir so‘z ikki marta topilgach faqat bir marta ball beradi.
    private int scoreCalculationVersion;

    // Eski constructor chaqiruvlari bilan moslik
    public User(
            Long telegramId,
            String name,
            int score,
            int correctAnswer,
            int wrongAnswer,
            List<Integer> wordOrder,
            int currentWordIndex,
            List<Integer> dailyWordIds,
            String dailyDate,
            Map<Integer, Integer> wordCorrectCounts
    ) {
        this(
                telegramId,
                name,
                score,
                correctAnswer,
                wrongAnswer,
                wordOrder,
                currentWordIndex,
                dailyWordIds,
                dailyDate,
                wordCorrectCounts,
                null,
                new java.util.ArrayList<>(),
                new java.util.HashMap<>(),
                0
        );
    }
}
