package ua.com.javarush.j4.crack;

import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.support.Scored;

import java.util.ArrayList;
import java.util.List;

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
        List<CrackResult> candidates = new ArrayList<>(keyspace);
        for (int key = 0; key < keyspace; key++) {
            candidates.add(new CrackResult(key, new CaesarCipher(alphabet, key).decrypt(ciphertext)));
        }
        // The generic Scored.bestOf owns the "pick the highest score" logic; here T = CrackResult.
        return Scored.bestOf(candidates, candidate -> scorer.score(candidate.plaintext())).value();
    }
}
