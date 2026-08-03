package ua.com.javarush.j4.crack;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.cipher.CaesarCipher;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelCaesarCrackerTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final LanguageProfile PROFILE = LanguageProfiles.ENGLISH;

    /** Long enough to clear MIN_CHARS_FOR_CRACK so the parallel path really engages. */
    private static String longEnglishText() {
        StringBuilder text = new StringBuilder();
        while (text.length() < ParallelPolicy.MIN_CHARS_FOR_CRACK * 2) {
            text.append("to be or not to be that is the question whether it is nobler in the mind ");
        }
        return text.toString();
    }

    private static String randomText(Random random, int length) {
        StringBuilder text = new StringBuilder(length);
        String words = "the quick brown fox jumps over a lazy dog and then it is time for us all ";
        while (text.length() < length) {
            text.append(words.charAt(random.nextInt(words.length())));
        }
        return text.toString();
    }

    @Test
    void matchesTheSequentialCrackerForEveryKey() {
        String plaintext = longEnglishText();
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));

            for (int key = 0; key < EN.keyspaceSize(); key++) {
                String ciphertext = new CaesarCipher(EN, key).encrypt(plaintext);
                assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext),
                        "mismatch for key " + key);
            }
        }
    }

    @Test
    void matchesTheSequentialCrackerOnRandomTexts() {
        Random random = new Random(20260803L);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker sequential = new CaesarCracker(EN, new FrequencyScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new FrequencyScorer(PROFILE), executor, ParallelPolicy.of(4));

            for (int trial = 0; trial < 20; trial++) {
                String ciphertext = new CaesarCipher(EN, random.nextInt(26))
                        .encrypt(randomText(random, ParallelPolicy.MIN_CHARS_FOR_CRACK + 500));
                assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext),
                        "mismatch on trial " + trial);
            }
        }
    }

    @Test
    void breaksScoreTiesTowardTheLowestKey() {
        // A scorer that rates everything equally: the sequential loop's strict '>' keeps
        // key 0, and the parallel reduction must reach the same answer.
        FitnessScorer flat = text -> 1.0;
        String ciphertext = longEnglishText();
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker parallel = new ParallelCaesarCracker(EN, flat, executor, ParallelPolicy.of(4));

            assertEquals(0, parallel.crack(ciphertext).key());
        }
    }

    @Test
    void isStableAcrossRepeatedRuns() {
        String ciphertext = new CaesarCipher(EN, 7).encrypt(longEnglishText());
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));
            CrackResult first = parallel.crack(ciphertext);

            for (int run = 0; run < 50; run++) {
                assertEquals(first, parallel.crack(ciphertext), "run " + run + " differed");
            }
        }
    }

    @Test
    void fallsBackToSequentialBelowTheThreshold() {
        String shortText = new CaesarCipher(EN, 3).encrypt("the quick brown fox");
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));

            assertEquals(sequential.crack(shortText), parallel.crack(shortText));
        }
    }

    @Test
    void worksWithASingleThreadedExecutor() {
        String ciphertext = new CaesarCipher(EN, 11).encrypt(longEnglishText());
        try (TaskExecutor executor = new DirectTaskExecutor()) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(4));

            assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext));
        }
    }

    @Test
    void handlesMorePartitionsThanKeys() {
        String ciphertext = new CaesarCipher(EN, 5).encrypt(longEnglishText());
        try (TaskExecutor executor = new PooledTaskExecutor(64)) {
            Cracker sequential = new CaesarCracker(EN, new DictionaryScorer(PROFILE));
            Cracker parallel = new ParallelCaesarCracker(
                    EN, new DictionaryScorer(PROFILE), executor, ParallelPolicy.of(64));

            assertEquals(sequential.crack(ciphertext), parallel.crack(ciphertext));
        }
    }
}
