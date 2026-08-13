package ua.com.javarush.j4.cipher;

import org.junit.jupiter.api.Test;
import ua.com.javarush.j4.alphabet.Alphabet;
import ua.com.javarush.j4.alphabet.Alphabets;
import ua.com.javarush.j4.concurrent.DirectTaskExecutor;
import ua.com.javarush.j4.concurrent.PooledTaskExecutor;
import ua.com.javarush.j4.concurrent.TaskExecutor;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelCipherTest {

    private static final Alphabet EN = Alphabets.ENGLISH;
    private static final Alphabet MIXED = Alphabets.DEFAULT;

    /**
     * Most of these tests care about chunking, not about where the shipped gate sits, so they
     * inject a tiny threshold and work on kilobytes. Chunk count is what exercises the code —
     * it is a function of thread count, not text size — so a 40 KB text over this threshold
     * splits into as many pieces as a 40 MB one would, in a thousandth of the time.
     *
     * <p>{@link #delegatesBelowTheShippedThreshold} and {@link #splitsAtTheShippedThreshold}
     * are the two that use the real constant, so the gate itself stays covered.
     */
    private static final int TINY_THRESHOLD = 4_096;
    private static final int CHUNKED_TEXT = 40_000;

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

    private static Cipher chunking(Cipher delegate, TaskExecutor executor) {
        return new ParallelCipher(delegate, executor, TINY_THRESHOLD);
    }

    @Test
    void matchesTheDelegateForEveryCipher() {
        String text = largeText(CHUNKED_TEXT);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            for (Cipher delegate : allCiphers()) {
                Cipher parallel = chunking(delegate, executor);

                assertEquals(delegate.encrypt(text), parallel.encrypt(text),
                        delegate.getClass().getSimpleName() + " encrypt mismatch");
                assertEquals(delegate.decrypt(text), parallel.decrypt(text),
                        delegate.getClass().getSimpleName() + " decrypt mismatch");
            }
        }
    }

    @Test
    void roundTripsThroughTheParallelPath() {
        String text = largeText(CHUNKED_TEXT);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            for (Cipher delegate : allCiphers()) {
                Cipher parallel = chunking(delegate, executor);

                assertEquals(text, parallel.decrypt(parallel.encrypt(text)),
                        delegate.getClass().getSimpleName() + " round trip failed");
            }
        }
    }

    @Test
    void handlesTextsThatAreNotAWholeNumberOfChunks() {
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new VigenereCipher(EN, "lemon");
            Cipher parallel = chunking(delegate, executor);

            for (int extra : new int[]{0, 1, 7, 4_099}) {
                String text = largeText(CHUNKED_TEXT + extra);
                assertEquals(delegate.encrypt(text), parallel.encrypt(text), "extra=" + extra);
            }
        }
    }

    @Test
    void delegatesDirectlyBelowItsThreshold() {
        String small = "the quick brown fox";
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new CaesarCipher(EN, 3);
            Cipher parallel = chunking(delegate, executor);

            assertEquals(delegate.encrypt(small), parallel.encrypt(small));
            assertEquals("", parallel.encrypt(""));
        }
    }

    @Test
    void delegatesDirectlyWithASingleThreadedExecutor() {
        String text = largeText(CHUNKED_TEXT);
        try (TaskExecutor executor = new DirectTaskExecutor()) {
            Cipher delegate = new VigenereCipher(EN, "lemon");
            Cipher parallel = chunking(delegate, executor);

            assertEquals(delegate.encrypt(text), parallel.encrypt(text));
        }
    }

    @Test
    void neverSplitsASurrogatePair() {
        // Emoji are outside every alphabet ring, so they must pass through byte-for-byte.
        StringBuilder text = new StringBuilder();
        Random random = new Random(20260803L);
        while (text.length() < CHUNKED_TEXT) {
            text.append("abc 😀 ").append(random.nextInt(10));
        }
        String withEmoji = text.toString();

        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new CaesarCipher(EN, 5);
            Cipher parallel = chunking(delegate, executor);

            assertEquals(delegate.encrypt(withEmoji), parallel.encrypt(withEmoji));
        }
    }

    @Test
    void isStableAcrossRepeatedRuns() {
        String text = largeText(CHUNKED_TEXT);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher parallel = chunking(new VigenereCipher(MIXED, "ключ"), executor);
            String first = parallel.encrypt(text);

            for (int run = 0; run < 50; run++) {
                assertEquals(first, parallel.encrypt(text), "run " + run + " differed");
            }
        }
    }

    /** The shipped gate: a text one character short of it must not be chunked. */
    @Test
    void delegatesBelowTheShippedThreshold() {
        String justUnder = largeText(ParallelCipher.MIN_CHARS_FOR_TRANSFORM)
                .substring(0, ParallelCipher.MIN_CHARS_FOR_TRANSFORM - 1);
        try (TaskExecutor executor = new DirectTaskExecutor()) {
            // A same-thread executor would give identical output either way, so instead assert
            // on the decision itself: a pooled executor must leave this text alone.
            Cipher delegate = new CaesarCipher(EN, 3);
            assertEquals(delegate.encrypt(justUnder),
                    new ParallelCipher(delegate, executor).encrypt(justUnder));
        }
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            assertEquals(false, executor.worthSplitting(justUnder.length(),
                    ParallelCipher.MIN_CHARS_FOR_TRANSFORM));
        }
    }

    /** And at the gate exactly, the real constructor really does chunk, byte-identically. */
    @Test
    void splitsAtTheShippedThreshold() {
        String atThreshold = largeText(ParallelCipher.MIN_CHARS_FOR_TRANSFORM)
                .substring(0, ParallelCipher.MIN_CHARS_FOR_TRANSFORM);
        try (TaskExecutor executor = new PooledTaskExecutor(4)) {
            Cipher delegate = new VigenereCipher(MIXED, "ключ");

            assertEquals(true, executor.worthSplitting(atThreshold.length(),
                    ParallelCipher.MIN_CHARS_FOR_TRANSFORM));
            assertEquals(delegate.encrypt(atThreshold),
                    new ParallelCipher(delegate, executor).encrypt(atThreshold));
        }
    }
}
