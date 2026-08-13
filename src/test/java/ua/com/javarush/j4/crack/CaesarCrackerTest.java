package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.cipher.CaesarCipher;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaesarCrackerTest {

    private static final Alphabet EN = LanguageProfiles.ENGLISH.alphabet();

    private static Cracker cracker(LanguageProfile profile) {
        return new CaesarCracker(profile.alphabet(), new DictionaryScorer(profile));
    }

    /** Long enough that the sweep scores a sample rather than the whole text. */
    private static String longEnglishText() {
        StringBuilder text = new StringBuilder();
        while (text.length() < CaesarCracker.SAMPLE_CHARS * 4) {
            text.append("to be or not to be that is the question whether it is nobler in the mind ");
        }
        return text.toString();
    }

    /**
     * Reference implementation: score every candidate over the <em>whole</em> ciphertext, which
     * is what the cracker did before it learned to sample.
     */
    private static CrackResult naiveCrack(Alphabet alphabet, FitnessScorer scorer, String ciphertext) {
        int bestKey = 0;
        String bestText = ciphertext;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int key = 0; key < alphabet.keyspaceSize(); key++) {
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

    @Test
    void recoversEnglishPlaintextExactly() {
        String original = "The quick brown fox jumps over the lazy dog. "
                + "And the dog was not amused, for that is what dogs do.";
        String ciphertext = new CaesarCipher(EN, 7).encrypt(original);

        CrackResult result = cracker(LanguageProfiles.ENGLISH).crack(ciphertext);

        assertEquals(7, result.key());
        assertEquals(original, result.plaintext());
    }

    @Test
    void recoversUkrainianPlaintextExactly() {
        String original = "Він був високий і худий, а на обличчі його застигла "
                + "усмішка. Це не та людина, що боїться зими.";
        String ciphertext = new CaesarCipher(LanguageProfiles.UKRAINIAN.alphabet(), 12).encrypt(original);

        CrackResult result = cracker(LanguageProfiles.UKRAINIAN).crack(ciphertext);

        assertEquals(12, result.key());
        assertEquals(original, result.plaintext());
    }

    /** Below the sample size nothing is sampled, so the answer must match a full-text sweep. */
    @Test
    void matchesAFullTextSweepForShortCiphertexts() {
        Random random = new Random(20260813L);
        FitnessScorer scorer = new DictionaryScorer(LanguageProfiles.ENGLISH);
        String words = "the quick brown fox jumps over a lazy dog and then it is time for us all ";

        for (int trial = 0; trial < 25; trial++) {
            StringBuilder plaintext = new StringBuilder();
            while (plaintext.length() < random.nextInt(CaesarCracker.SAMPLE_CHARS)) {
                plaintext.append(words.charAt(random.nextInt(words.length())));
            }
            String ciphertext = new CaesarCipher(EN, random.nextInt(26)).encrypt(plaintext.toString());

            assertEquals(naiveCrack(EN, scorer, ciphertext),
                    new CaesarCracker(EN, scorer).crack(ciphertext),
                    "trial " + trial);
        }
    }

    /** Above the sample size the key still comes back exactly, and the whole text is decrypted. */
    @Test
    void recoversEveryKeyFromATextLongerThanTheSample() {
        String plaintext = longEnglishText();
        Cracker cracker = cracker(LanguageProfiles.ENGLISH);

        for (int key = 0; key < EN.keyspaceSize(); key++) {
            CrackResult result = cracker.crack(new CaesarCipher(EN, key).encrypt(plaintext));

            assertEquals(key, result.key(), "wrong key recovered for key " + key);
            assertEquals(plaintext, result.plaintext(), "wrong plaintext for key " + key);
        }
    }

    @Test
    void breaksScoreTiesTowardTheLowestKey() {
        // A scorer that rates everything equally: the strict '>' in the sweep keeps key 0.
        FitnessScorer flat = text -> 1.0;

        assertEquals(0, new CaesarCracker(EN, flat).crack(longEnglishText()).key());
    }

    @Test
    void isStableAcrossRepeatedRuns() {
        String ciphertext = new CaesarCipher(EN, 7).encrypt(longEnglishText());
        Cracker cracker = cracker(LanguageProfiles.ENGLISH);
        CrackResult first = cracker.crack(ciphertext);

        for (int run = 0; run < 20; run++) {
            assertEquals(first, cracker.crack(ciphertext), "run " + run + " differed");
        }
    }
}
