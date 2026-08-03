package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;

/** Sweeps every Caesar shift and returns the decryption the scorer likes best. */
public final class CaesarCracker implements Cracker {
    private final Alphabet alphabet;
    // ── SOLID ▸ D — Принцип інверсії залежностей (DIP) ──
    // Високорівнева логіка (перебір ключів) залежить від АБСТРАКЦІЇ FitnessScorer,
    // а не від конкретного DictionaryScorer/FrequencyScorer. Cracker не знає й не
    // хоче знати, ЯК саме оцінюється текст — деталь підставляється ззовні.
    private final FitnessScorer scorer;

    // DIP на практиці — впровадження залежності через конструктор (Constructor
    // Injection): потрібну стратегію обирає викликач (BruteForceCommand), а не сам клас.
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
