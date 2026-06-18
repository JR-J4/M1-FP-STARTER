package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;

/** Sweeps every Caesar shift and returns the decryption the scorer likes best. */
public final class CaesarCracker implements Cracker {
    private final Alphabet alphabet;
    private final FitnessScorer scorer;

    public CaesarCracker(Alphabet alphabet, FitnessScorer scorer) {
        this.alphabet = alphabet;
        this.scorer = scorer;
    }

    @Override
    public CrackResult crack(String ciphertext) {
        int keyspace = alphabet.keyspaceSize();
        int bestKey = 0;
        String bestText = ciphertext;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int key = 0; key < keyspace; key++) {
            String candidate = new CaesarCipher(alphabet, key).decrypt(ciphertext);
            double score = scorer.score(candidate);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
                bestText = candidate;
            }
        }
        return new CrackResult(bestKey, bestText);
    }
}
