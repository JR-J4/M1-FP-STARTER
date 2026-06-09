package ua.com.javarush.j4.language;

import java.util.List;

public class AlphabetDetector {
    public static final List<Alphabet> ALPHABETS = List.of(
            new EnglishAlphabet(),
            new UkrainianAlphabet()
    );

    public static Alphabet detect(String text) {
        for (char ch : text.toCharArray()) {
            for (Alphabet alphabet : ALPHABETS) {
                if (alphabet.indexOf(ch) != -1) {
                    return alphabet;
                }
            }
        }
        return ALPHABETS.get(0);
    }
}
