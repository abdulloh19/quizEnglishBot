package uz.pdp.englishBot.service;

import uz.pdp.englishBot.model.User;
import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.UserRepository;

import java.util.*;

public class UserService {
    private UserRepository userRepository;
    private WordService wordService;




    public UserService(UserRepository userRepository, WordService wordService) {
        this.userRepository = userRepository;
        this.wordService = wordService;
    }

    public List<User> getAllUsers() {
        return userRepository.findUser();
    }

    public User addUser(User users) {
        List<User> user = userRepository.findUser();
        List<Word> words = wordService.getAllWords();
        List<Integer> ids = new ArrayList<>();
        for (Word word : words) {
            ids.add(word.getId());
        }
        Collections.shuffle(ids);
        users.setWordOrder(ids);
        users.setCurrentWordIndex(0);
        user.add(users);
        userRepository.save(user);
        return users;
    }
    public User getUserByTelegramId(Long id) {
        List<User> users = userRepository.findUser();
        for (User user : users) {
            if (Objects.equals(user.getTelegramId(), id)) {
                return user;
            }
        }
        return null;
    }

    public void updateUser(User user) {
        List<User> user1 = userRepository.findUser();
        for (User user2 : user1) {
            if (Objects.equals(user2.getTelegramId(), user.getTelegramId())) {
                user2.setScore(user.getScore());
                user2.setCorrectAnswer(user.getCorrectAnswer());
                user2.setWrongAnswer(user.getWrongAnswer());
                user2.setName(user.getName());
                user2.setWordOrder(user.getWordOrder());
                user2.setCurrentWordIndex(user.getCurrentWordIndex());
            }
        }
        userRepository.save(user1);
    }
    public Word getNextWord(User user) {

        List<Integer> wordOrder = user.getWordOrder();

        int index = user.getCurrentWordIndex();

        if (index >= wordOrder.size()) {

            List<Word> words = wordService.getAllWords();

            List<Integer> newOrder = new ArrayList<>();

            for (Word word : words) {
                newOrder.add(word.getId());
            }

            Collections.shuffle(newOrder);

            user.setWordOrder(newOrder);
            user.setCurrentWordIndex(0);

            wordOrder = newOrder;
            index = 0;
        }

        int wordId = wordOrder.get(index);

        Word word = wordService.getWordById(wordId);

        user.setCurrentWordIndex(index + 1);

        updateUser(user);

        return word;
    }


    public User getRandomWord() {
        List<User> users = userRepository.findUser();
        if (users.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return users.get(random.nextInt(users.size()));
    }
}
