package ua.com.javarush.j4.language;

import java.util.List;

public final class LanguageDetector {

    private LanguageDetector() {
    }

    public static List<Character> detect(String text) {

        int englishScore = score(text, Alphabet.english());
        int ukrainianScore = score(text, Alphabet.ukrainian());

        return (englishScore >= ukrainianScore ? Alphabet.english() : Alphabet.ukrainian());
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