package ua.com.javarush.j4.concurrent;

/**
 * Decides when parallel execution is worth its overhead.
 *
 * <p>Thresholds live here so there is a single place to tune them, but the decision to
 * apply them lives in each parallel component — that is where the input size is known.
 * Lengths are measured in characters, not bytes.
 */
public final class ParallelPolicy {

    /** Cracking does {@code keyspace × 2} full passes, so it clears handoff cost early. */
    public static final int MIN_CHARS_FOR_CRACK = 8_192;

    /** A transform is a single pass, so it needs far more text to be worth splitting. */
    public static final int MIN_CHARS_FOR_TRANSFORM = 65_536;

    public static final int MIN_FILES_FOR_BATCH = 2;

    private final int threads;

    private ParallelPolicy(int threads) {
        this.threads = threads;
    }

    /** {@code requested <= 0} means "every available core"; {@code 1} means fully sequential. */
    public static ParallelPolicy of(int requested) {
        return new ParallelPolicy(requested <= 0
                ? Runtime.getRuntime().availableProcessors()
                : requested);
    }

    public int threads() {
        return threads;
    }

    public boolean shouldParallelizeCrack(int textLength) {
        return threads > 1 && textLength >= MIN_CHARS_FOR_CRACK;
    }

    public boolean shouldParallelizeTransform(int textLength) {
        return threads > 1 && textLength >= MIN_CHARS_FOR_TRANSFORM;
    }

    public boolean shouldParallelizeBatch(int fileCount) {
        return threads > 1 && fileCount >= MIN_FILES_FOR_BATCH;
    }
}
