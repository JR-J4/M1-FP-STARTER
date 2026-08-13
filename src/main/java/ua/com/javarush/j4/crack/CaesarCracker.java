package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;

/**
 * Sweeps every Caesar shift and returns the decryption the scorer likes best.
 *
 * <p>The sweep scores a leading <em>sample</em> rather than the whole ciphertext, then
 * decrypts the full text once with the winning key. Deciding a 26-key Caesar sweep does not
 * need a megabyte of evidence: over a few thousand characters of natural language the correct
 * key scores hundreds of dictionary hits where every wrong key scores almost none. Scoring
 * the whole text instead means decrypting and scoring it {@code keyspace} times over — which
 * is where nearly all of a brute-force run used to go.
 *
 * <p>At or below {@link #SAMPLE_CHARS} the whole ciphertext <em>is</em> the sample, so short
 * inputs behave exactly as a full sweep would.
 */
public final class CaesarCracker implements Cracker {

    /**
     * Evidence enough to identify a Caesar key; scoring beyond this only costs time.
     * Public because it is a behavioural boundary, not just a tuning knob: at or below this
     * length the sample is the whole ciphertext, so the result matches a full sweep exactly.
     */
    public static final int SAMPLE_CHARS = 4_096;

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
        String sample = ciphertext.length() <= SAMPLE_CHARS
                ? ciphertext
                : ciphertext.substring(0, SAMPLE_CHARS);

        int bestKey = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int key = 0; key < alphabet.keyspaceSize(); key++) {
            // Strict '>' so the lowest key wins a tie, as an ascending scan should.
            double score = scorer.score(new CaesarCipher(alphabet, key).decrypt(sample));
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        return new CrackResult(bestKey, new CaesarCipher(alphabet, bestKey).decrypt(ciphertext));
    }
}
