package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.ParallelPolicy;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelCipherTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final Alphabet MIXED = Alphabets.DEFAULT;

    /** Comfortably past MIN_CHARS_FOR_TRANSFORM so the chunked path really engages. */
    private static String largeText(int minLength) {
        StringBuilder text = new StringBuilder(minLength + 128);
        while (text.length() < minLength) {
            text.append("Ukrainian та English mixed, з punctuation 123 — and newlines\n");
        }
        return text.toString();
    }

    private static List<Cipher> allCiphers() {
        return List.of(
                new CaesarCipher(MIXED, 7),
                new Rot13Cipher(MIXED),
                new AtbashCipher(MIXED),
                new VigenereCipher(MIXED, "ключ"));
    }

    @Test
    void matchesTheDelegateForEveryCipher() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 3);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            for (Cipher delegate : allCiphers()) {
                Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

                assertEquals(delegate.encrypt(text), parallel.encrypt(text),
                        delegate.getClass().getSimpleName() + " encrypt mismatch");
                assertEquals(delegate.decrypt(text), parallel.decrypt(text),
                        delegate.getClass().getSimpleName() + " decrypt mismatch");
            }
        }
    }

    @Test
    void roundTripsThroughTheParallelPath() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            for (Cipher delegate : allCiphers()) {
                Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

                assertEquals(text, parallel.decrypt(parallel.encrypt(text)),
                        delegate.getClass().getSimpleName() + " round trip failed");
            }
        }
    }

    @Test
    void handlesTextsThatAreNotAWholeNumberOfChunks() {
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new VigenereCipher(EN, "lemon");
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            for (int extra : new int[]{0, 1, 7, 4_099}) {
                String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM + extra);
                assertEquals(delegate.encrypt(text), parallel.encrypt(text), "extra=" + extra);
            }
        }
    }

    @Test
    void delegatesDirectlyBelowTheThreshold() {
        String small = "the quick brown fox";
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new CaesarCipher(EN, 3);
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            assertEquals(delegate.encrypt(small), parallel.encrypt(small));
            assertEquals("", parallel.encrypt(""));
        }
    }

    @Test
    void delegatesDirectlyWithASingleThreadedExecutor() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2);
        try (TaskExecutor executor = new DirectTaskExecutor()) {
            Cipher delegate = new VigenereCipher(EN, "lemon");
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            assertEquals(delegate.encrypt(text), parallel.encrypt(text));
        }
    }

    @Test
    void neverSplitsASurrogatePair() {
        // Emoji are outside every alphabet ring, so they must pass through byte-for-byte.
        StringBuilder text = new StringBuilder();
        Random random = new Random(20260803L);
        while (text.length() < ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2) {
            text.append("abc 😀 ").append(random.nextInt(10));
        }
        String withEmoji = text.toString();

        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new CaesarCipher(EN, 5);
            Cipher parallel = new ParallelCipher(delegate, executor, ParallelPolicy.of(4));

            assertEquals(delegate.encrypt(withEmoji), parallel.encrypt(withEmoji));
        }
    }

    @Test
    void isStableAcrossRepeatedRuns() {
        String text = largeText(ParallelPolicy.MIN_CHARS_FOR_TRANSFORM * 2);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher parallel = new ParallelCipher(
                    new VigenereCipher(MIXED, "ключ"), executor, ParallelPolicy.of(4));
            String first = parallel.encrypt(text);

            for (int run = 0; run < 50; run++) {
                assertEquals(first, parallel.encrypt(text), "run " + run + " differed");
            }
        }
    }
}
