package uz.pdp.englishBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Word {

    private int id;
    private String english;
    private String uzbek;
    private String example;
    private String level;

}