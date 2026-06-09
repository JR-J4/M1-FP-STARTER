package ua.com.javarush.j4.bruteforce;

import ua.com.javarush.j4.crypto.Cypher;

public class BruteForce {
    private final Cypher cypher;

    public BruteForce(Cypher cypher) {
        this.cypher = cypher;
    }

    public String crack(String encryptedText) {
        String bestResult = encryptedText;
        int bestScore = -1;
        int alphabetSize = cypher.alphabet().getSize();

        for (int key = 0; key < alphabetSize; key++) {
            String candidate = cypher.decrypt(encryptedText, key);
            int score = FrequencyAnalyzer.score(candidate);
            if (score > bestScore) {
                bestScore = score;
                bestResult = candidate;
            }
        }
        return bestResult;
    }
}
