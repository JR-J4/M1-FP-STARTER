package ua.com.javarush.j4.bruteforce;

import java.util.List;

public class FrequencyAnalyzer {
    private static final List<String> ENGLISH_WORDS = List.of(
            "the", "and", "of", "to", "in", "is", "it", "that", "was", "he",
            "she", "you", "are", "for", "on", "with", "as", "at", "be", "this"
    );

    private static final List<String> UKRAINIAN_WORDS = List.of(
            "що", "не", "і", "та", "у", "він", "як", "але", "ми", "ви",
            "на", "за", "це", "від", "до", "по", "які", "вже", "або", "так"
    );

    public static int score(String text) {
        String lower = text.toLowerCase();
        int count = 0;
        for (String word : ENGLISH_WORDS) {
            count += countOccurrences(lower, word);
        }
        for (String word : UKRAINIAN_WORDS) {
            count += countOccurrences(lower, word);
        }
        return count;
    }

    private static int countOccurrences(String text, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
    }
}
