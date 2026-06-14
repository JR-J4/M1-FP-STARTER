package ua.com.javarush.j4.crypty;

public class BruteForce {

    public static int bruteForce(String text, Cipher cipher) {

        int bestKey = 0;
        int bestScore = -1;
        for (int key = 0; key < 100; key++) {

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
        String[] vocabulary = {
                " the ", " and ", " of ", " to ", " in ",
                " и ", " в ", " не ", " на ", " що "
        };
        int score = 0;
        for (String word : vocabulary) {
            if (text.contains(word)) {
                score++;
            }
            if (text.contains("the")) score += 2;
            if (text.contains("and")) score += 2;
        }
        return score;
    }
}