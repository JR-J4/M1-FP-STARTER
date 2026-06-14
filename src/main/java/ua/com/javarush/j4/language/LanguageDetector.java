package ua.com.javarush.j4.language;

import java.util.List;

public class LanguageDetector {
    public static List<Character> detect(String text) {

        int englishScore = score(text, Alphabet.ENGLISH);
        int ukrainianScore = score(text, Alphabet.UKRAINIAN);

        return (englishScore >= ukrainianScore ? Alphabet.ENGLISH : Alphabet.UKRAINIAN);
    }

    private static int score(String text, List<Character> alphabet) {

        int score = 0;

        for (char ch : text.toCharArray()) {
            if (alphabet.contains(ch)) {
                score++;
            }
        }
        return score;
    }
}