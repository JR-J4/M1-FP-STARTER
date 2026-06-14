package ua.com.javarush.j4.crypher;

import java.util.List;
import java.util.Locale;

public class BruteForce {

    private BruteForce() {
    }

    public static int bruteForce(String text, Cipher cipher) {

        int size = cipher.getAlphabetSize();

        int bestKey = 0;
        int bestScore = Integer.MIN_VALUE;

        for (int key = 0; key < size; key++) {

            String decoded = cipher.decrypt(text, key);
            int score = score(decoded);

            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        return bestKey;
    }

    private static int score(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> vocabulary = List.of(
                " the ", " and ", " of ", " to ", " in ", " is ", " it ", " that ", " for ", " on ", " with ",
                " и ", " в ", " не ", " на ", " що ", " я ", " ти ", " це ", " як ", " але ", " або "
        );
        int score = 0;
        for (String word : vocabulary) {
            if (text.contains(word)) {
                score++;
            }
        }
        score += countSpaces(lower);
        score -= countGarbage(lower);
        return score;
    }

    private static int countSpaces(String text) {
        int count = 0;
        for (char ch : text.toCharArray()) {
            if (ch == ' ') count++;
        }
        return count;
    }

    private static int countGarbage(String text) {
        int garbage = 0;
        for (char ch : text.toCharArray()) {
            if (Character.isLetter(ch) || ch == ' ') continue;
            garbage++;
        }
        return garbage;
    }
}