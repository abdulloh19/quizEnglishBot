package uz.pdp.englishBot.service;

import uz.pdp.englishBot.model.User;
import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.UserRepository;
import uz.pdp.englishBot.repository.WordRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

public class UserService {
    private final UserRepository userRepository;
    private final WordService wordService;

    public UserService(UserRepository userRepository, WordService wordService) {
        this.userRepository = userRepository;
        this.wordService = wordService;
    }

    public List<User> getAllUsers() { return userRepository.findUser(); }

    public synchronized User addUser(User user) {
        List<User> users = userRepository.findUser();
        for (User saved : users) {
            if (Objects.equals(saved.getTelegramId(), user.getTelegramId())) return saved;
        }
        normalize(user);
        users.add(user);
        userRepository.save(users);
        return user;
    }

    public User getUserByTelegramId(Long id) {
        for (User user : userRepository.findUser()) {
            if (Objects.equals(user.getTelegramId(), id)) {
                normalize(user);
                return user;
            }
        }
        return null;
    }

    private void normalize(User user) {
        if (user.getScoreCalculationVersion() < 2) {
            // Oldingi versiya har bir to‘g‘ri urinishga ball bergan. Har bir so‘z
            // ikki marta topilgani uchun eski jami bir martalik hisobga o‘tkaziladi.
            user.setScore(user.getScore() / 2);
            user.setCorrectAnswer(user.getCorrectAnswer() / 2);
            user.setScoreCalculationVersion(2);
        }
        if (user.getWordOrder() == null) user.setWordOrder(new ArrayList<>());
        if (user.getDailyWordIds() == null) user.setDailyWordIds(new ArrayList<>());
        if (user.getDailySeenWordIds() == null) user.setDailySeenWordIds(new ArrayList<>());
        if (user.getWordCorrectCounts() == null) user.setWordCorrectCounts(new HashMap<>());
        if (user.getWordCorrectCountsByKey() == null) {
            // Eski ID progressini ko‘chirish xavfli: yangi JSONda ayni ID boshqa so‘z
            // bo‘lishi mumkin. Umumiy score saqlanadi, so‘z progressi toza boshlanadi.
            user.setWordCorrectCountsByKey(new HashMap<>());
            user.setWordCorrectCounts(new HashMap<>());
        }
    }

    public synchronized boolean selectLevel(User user, String level) {
        level = WordRepository.normalizeLevel(level);
        // Yuklash muvaffaqiyatli bo‘lmaguncha user progressiga tegmaymiz.
        wordService.getAllWords(level);
        boolean changed = !level.equals(user.getLevel());
        if (changed) {
            user.setLevel(level);
            user.setDailyWordIds(new ArrayList<>());
            user.setWordOrder(new ArrayList<>());
            user.setCurrentWordIndex(0);
            user.setDailyDate(null);
            user.setWordCorrectCounts(new HashMap<>());
            user.setWordCorrectCountsByKey(new HashMap<>());
            user.setDailySeenWordIds(new ArrayList<>());
            createDailyWords(user);
        } else {
            ensureDailyWords(user);
        }
        return changed;
    }

    public void createDailyWords(User user) {
        List<Word> words = wordService.getAllWords(user.getLevel());
        normalize(user);
        List<Integer> ids = new ArrayList<>();
        for (Word word : words) {
            if (getCorrectCount(user, word) < 2) ids.add(word.getId());
        }
        Collections.shuffle(ids);
        user.setDailyWordIds(new ArrayList<>(ids.subList(0, Math.min(15, ids.size()))));
        user.setWordOrder(new ArrayList<>(user.getDailyWordIds()));
        user.setCurrentWordIndex(0);
        user.setDailySeenWordIds(new ArrayList<>());
        user.setDailyDate(LocalDate.now().toString());
        updateUser(user);
    }

    /**
     * Joriy guruh 100% o‘rganilgach keyingi 15 ta o‘rganilmagan so‘zni qo‘shadi.
     * Darajadagi barcha so‘zlar o‘rganilgan bo‘lsa false qaytaradi.
     */
    public boolean addMoreDailyWords(User user) {
        ensureDailyWords(user);
        if (!getDailyWords(user).isEmpty()) return true;

        List<Integer> ids = new ArrayList<>();
        for (Word word : wordService.getAllWords(user.getLevel())) {
            if (getCorrectCount(user, word) < 2) {
                ids.add(word.getId());
            }
        }
        if (ids.isEmpty()) return false;

        Collections.shuffle(ids);
        List<Integer> nextBatch = new ArrayList<>(ids.subList(0, Math.min(15, ids.size())));
        user.setDailyWordIds(nextBatch);
        user.setWordOrder(new ArrayList<>(nextBatch));
        user.setCurrentWordIndex(0);
        updateUser(user);
        return true;
    }

    private void ensureDailyWords(User user) {
        normalize(user);
        if (!LocalDate.now().toString().equals(user.getDailyDate())) createDailyWords(user);
    }

    public List<Word> getDailyWords(User user) {
        ensureDailyWords(user);
        List<Word> words = wordService.getAllWords(user.getLevel());
        return words.stream().filter(word -> user.getDailyWordIds().contains(word.getId())
                && getCorrectCount(user, word) < 2).toList();
    }

    /** Testga faqat bugun user ko‘rgan va hali o‘rganib bo‘lmagan so‘zlar kiradi. */
    public List<Word> getSeenDailyWords(User user) {
        ensureDailyWords(user);
        return wordService.getAllWords(user.getLevel()).stream()
                .filter(word -> user.getDailyWordIds().contains(word.getId()))
                .filter(word -> user.getDailySeenWordIds().contains(word.getId()))
                .filter(word -> getCorrectCount(user, word) < 2)
                .toList();
    }

    /** Bugun real ko‘rsatilgan so‘zlar; o‘rganilganlari ham shu ro‘yxatda qoladi. */
    public List<Word> getAllSeenTodayWords(User user) {
        ensureDailyWords(user);
        return wordService.getAllWords(user.getLevel()).stream()
                .filter(word -> user.getDailySeenWordIds().contains(word.getId()))
                .toList();
    }

    /** Bugun guruhlarga ajratilgan barcha so‘zlar: ochilgan, ochilmagan va o‘rganilganlari. */
    public List<Word> getAllAssignedTodayWords(User user) {
        ensureDailyWords(user);
        return wordService.getAllWords(user.getLevel()).stream()
                .filter(word -> user.getDailyWordIds().contains(word.getId())
                        || user.getDailySeenWordIds().contains(word.getId()))
                .toList();
    }

    public boolean hasMoreLevelWords(User user) {
        ensureDailyWords(user);
        return wordService.getAllWords(user.getLevel()).stream()
                .anyMatch(word -> getCorrectCount(user, word) < 2);
    }

    public synchronized void updateUser(User user) {
        List<User> users = userRepository.findUser();
        for (int i = 0; i < users.size(); i++) {
            if (Objects.equals(users.get(i).getTelegramId(), user.getTelegramId())) {
                users.set(i, user);
                userRepository.save(users);
                return;
            }
        }
        throw new IllegalStateException("User topilmadi");
    }

    public Word getNextWord(User user) {
        List<Word> daily = getDailyWords(user);
        if (daily.isEmpty()) return null;
        for (Integer wordId : user.getWordOrder()) {
            if (user.getDailyWordIds().contains(wordId)
                    && !user.getDailySeenWordIds().contains(wordId)) {
                user.getDailySeenWordIds().add(wordId);
                user.setCurrentWordIndex(user.getDailySeenWordIds().size());
                updateUser(user);
                return wordService.getWordById(user.getLevel(), wordId);
            }
        }
        // Guruhdagi barcha so‘zlar ko‘rsatilgan. Yangisini test tugagach beramiz.
        return null;
    }

    public AnswerResult recordAnswer(User user, QuizQuestionResult answer) {
        normalize(user);
        Word word = wordService.getWordById(user.getLevel(), answer.wordId());
        if (word == null || !user.getDailySeenWordIds().contains(answer.wordId())) {
            throw new IllegalArgumentException("Ko‘rilmagan so‘z uchun test natijasi qabul qilinmaydi");
        }
        int correctCount = getCorrectCount(user, word);
        boolean learnedNow = false;
        if (answer.correct()) {
            if (correctCount < 2) {
                correctCount++;
                user.getWordCorrectCountsByKey().put(progressKey(user, word), correctCount);
            }
            if (correctCount >= 2 && user.getDailyWordIds().contains(answer.wordId())) {
                learnedNow = true;
                user.setScore(user.getScore() + 1);
                user.setCorrectAnswer(user.getCorrectAnswer() + 1);
                user.getDailyWordIds().remove(Integer.valueOf(answer.wordId()));
            }
        } else {
            user.setWrongAnswer(user.getWrongAnswer() + 1);
        }
        updateUser(user);
        return new AnswerResult(correctCount, learnedNow);
    }

    public int getCorrectCount(User user, Word word) {
        normalize(user);
        return user.getWordCorrectCountsByKey().getOrDefault(progressKey(user, word), 0);
    }

    public long getLearnedWordCount(User user) {
        normalize(user);
        return wordService.getAllWords(user.getLevel()).stream()
                .filter(word -> getCorrectCount(user, word) >= 2)
                .count();
    }

    private String progressKey(User user, Word word) {
        return user.getLevel().toUpperCase(Locale.ROOT) + ":"
                + word.getEnglish().trim().toLowerCase(Locale.ROOT);
    }

    public record QuizQuestionResult(int wordId, boolean correct) { }
    public record AnswerResult(int correctCount, boolean learnedNow) { }
}
