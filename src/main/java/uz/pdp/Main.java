package uz.pdp;

import uz.pdp.englishBot.bot.EnglishBot;
import uz.pdp.englishBot.model.User;
import uz.pdp.englishBot.model.Word;
import uz.pdp.englishBot.repository.WordRepository;
import uz.pdp.englishBot.service.WordService;

import java.util.ArrayList;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
             EnglishBot englishBot = new EnglishBot("8979412777:AAHnOJDGyv-F2W6ccrP5V--LLtnwfSoWBL8");
        System.out.println(englishBot);
    }
}
